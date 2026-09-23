package com.naze.nazever.core.network.auth

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
 */
class NazeVerAuthRepository(
    private val remoteApi: AuthRemoteApi,
    private val sessionStore: SessionStore,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    suspend fun signIn(email: String, password: String): AuthResult =
        runAuth { remoteApi.signInWithPassword(email, password) }

    suspend fun signUp(email: String, password: String): AuthResult =
        runAuth { remoteApi.signUp(email, password) }

    /**
     * Signs out remotely (best effort) and always clears the local
     * session, even if the network call fails.
     */
    suspend fun signOut() {
        val session = sessionStore.read()
        try {
            if (session != null) {
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
     * (nearly) expired, attempts a refresh. If the refresh token is
     * revoked, clears local state and returns null. Never throws.
     */
    suspend fun restoreSession(): SessionData? {
        val session = sessionStore.read() ?: return null
        val now = clock()
        return if (session.expiresAtMillis - now > REFRESH_MARGIN_MILLIS) {
            session
        } else {
            refreshIfNeeded(session)
        }
    }

    private suspend fun refreshIfNeeded(session: SessionData): SessionData? {
        return try {
            val response = remoteApi.refresh(session.refreshToken)
            val refreshed = response.toSession(clock())
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
            val session = response.toSession(clock())
            sessionStore.save(session)
            AuthResult.Success(session)
        } catch (e: Exception) {
            AuthResult.Failure(AuthErrorMapper.fromException(e))
        }
    }

    private fun AuthTokenResponse.toSession(now: Long): SessionData = SessionData(
        accessToken = accessToken,
        refreshToken = refreshToken ?: "",
        expiresAtMillis = now + expiresIn * 1000L,
        userId = user?.id ?: "",
        email = user?.email
    )

    companion object {
        private const val REFRESH_MARGIN_MILLIS = 60_000L
    }
}
