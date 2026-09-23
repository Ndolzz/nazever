package com.naze.nazever.core.network.auth

import com.naze.nazever.core.network.session.DeviceSessionInfo
import com.naze.nazever.core.network.session.SessionRemoteApi
import com.naze.nazever.core.security.SessionData
import com.naze.nazever.core.security.SessionStore

/**
 * Result of an authentication operation. Failure messages are safe,
 * fixed strings; they never contain tokens or HTTP bodies.
 */
sealed class AuthResult {
    data class Success(val session: SessionData) : AuthResult()
    data class Failure(val error: AuthError) : AuthResult()
}

/**
 * Coordinates the Supabase Auth client and secure session storage.
 * Pure JVM logic: fully unit-testable with fakes, no Android APIs.
 *
 * TASK-007: when a [SessionRemoteApi] is provided, sign-in/up also
 * register the device + session server-side, refreshes go through the
 * server-side choke point (enforcing revocation), and restore detects
 * revoked sessions and clears the local store.
 */
class NazeVerAuthRepository(
    private val remoteApi: AuthRemoteApi,
    private val sessionStore: SessionStore,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val sessionApi: SessionRemoteApi? = null,
    private val deviceNameProvider: () -> String = { "Android" }
) {

    /** Reads the locally stored session without any network activity. */
    fun currentSession(): SessionData? = sessionStore.read()

    suspend fun signIn(email: String, password: String): AuthResult =
        runAuth { remoteApi.signInWithPassword(email, password) }

    suspend fun signUp(email: String, password: String): AuthResult =
        runAuth { remoteApi.signUp(email, password) }

    /**
     * Signs out remotely (best effort) and always clears the local
     * session, even if the network call fails. Also marks the
     * server-side session row revoked when session management is active.
     */
    suspend fun signOut() {
        val session = sessionStore.read()
        try {
            if (session != null) {
                val api = sessionApi
                if (api != null && session.sessionId != null) {
                    try {
                        api.revokeSession(session.accessToken, session.sessionId!!)
                    } catch (e: Exception) {
                        // Best effort: Supabase logout below still revokes
                        // the refresh token for this device.
                    }
                }
                remoteApi.signOut(session.accessToken)
            }
        } catch (e: Exception) {
            // Remote logout is best effort; local session must still be cleared.
        } finally {
            sessionStore.clear()
        }
    }

    fun hasSession(): Boolean = sessionStore.hasSession()

    /**
     * Restores the session from secure storage. If the access token is
     * (nearly) expired, attempts a refresh through the server-side choke
     * point. Revoked sessions are cleared and null is returned so the
     * user lands on Login. Never throws.
     */
    suspend fun restoreSession(): SessionData? {
        val session = sessionStore.read() ?: return null
        val now = clock()
        return if (session.expiresAtMillis - now > REFRESH_MARGIN_MILLIS) {
            if (isRevokedRemotely(session)) {
                sessionStore.clear()
                null
            } else {
                session
            }
        } else {
            refreshIfNeeded(session)
        }
    }

    /**
     * Lists the caller's devices/sessions (safe metadata only, FR-01.6).
     * Requires session management to be configured.
     */
    suspend fun listDeviceSessions(): List<DeviceSessionInfo> {
        val api = sessionApi ?: return emptyList()
        val session = currentSession() ?: return emptyList()
        return api.listDeviceSessions(session.accessToken)
    }

    /**
     * Revokes another session of the SAME user (device A logs out
     * device B). The current session cannot be revoked through this
     * path; use [signOut] instead.
     */
    suspend fun revokeDeviceSession(sessionId: String) {
        val api = sessionApi
            ?: throw IllegalStateException("Session management is not configured")
        val session = currentSession()
            ?: throw IllegalStateException("No active session")
        require(sessionId != session.sessionId) {
            "Use signOut to end this device's session"
        }
        api.revokeSession(session.accessToken, sessionId)
    }

    private suspend fun isRevokedRemotely(session: SessionData): Boolean {
        val api = sessionApi ?: return false
        val sessionId = session.sessionId ?: return false
        return try {
            // Fail closed on a definitive answer, fail open on network
            // errors: the refresh path still guards revoked sessions.
            api.isSessionRevoked(session.accessToken, sessionId)
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun refreshIfNeeded(session: SessionData): SessionData? {
        val api = sessionApi
        val useChokePoint = api != null && session.sessionId != null
        return try {
            val response = if (useChokePoint) {
                api!!.refreshSession(session.refreshToken)
            } else {
                remoteApi.refresh(session.refreshToken)
            }
            var refreshed = response.toSession(clock(), session)
            if (useChokePoint) {
                // The session row is reused; only its refresh-token hash
                // rotates server-side.
                refreshed = refreshed.copy(
                    sessionId = session.sessionId,
                    deviceId = session.deviceId
                )
            }
            sessionStore.save(refreshed)
            refreshed
        } catch (e: AuthHttpException) {
            if (e.status == 400 || e.status == 401) {
                // Refresh token revoked: drop local session.
                sessionStore.clear()
                null
            } else {
                // Transient failure: keep local session for a later retry.
                session
            }
        } catch (e: Exception) {
            session
        }
    }

    private suspend fun runAuth(block: suspend () -> AuthTokenResponse): AuthResult {
        return try {
            val response = block()
            var session = response.toSession(clock(), null)
            val api = sessionApi
            if (api != null && session.refreshToken.isNotEmpty()) {
                try {
                    val registration = api.registerSession(
                        session.accessToken,
                        deviceNameProvider(),
                        "android",
                        session.refreshToken
                    )
                    session = session.copy(
                        sessionId = registration.sessionId,
                        deviceId = registration.deviceId
                    )
                } catch (e: Exception) {
                    // Best effort: without a server-side session row the
                    // session falls back to the direct refresh path.
                }
            }
            sessionStore.save(session)
            AuthResult.Success(session)
        } catch (e: Exception) {
            AuthResult.Failure(AuthErrorMapper.fromException(e))
        }
    }

    private fun AuthTokenResponse.toSession(now: Long, previous: SessionData?): SessionData =
        SessionData(
            accessToken = accessToken,
            refreshToken = refreshToken ?: previous?.refreshToken ?: "",
            expiresAtMillis = now + expiresIn * 1000L,
            userId = user?.id ?: previous?.userId ?: "",
            email = user?.email ?: previous?.email,
            sessionId = previous?.sessionId,
            deviceId = previous?.deviceId
        )

    companion object {
        private const val REFRESH_MARGIN_MILLIS = 60_000L
    }
}
