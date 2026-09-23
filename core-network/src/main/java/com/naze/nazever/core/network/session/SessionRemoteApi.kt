package com.naze.nazever.core.network.session

import com.naze.nazever.core.network.auth.AuthTokenResponse

data class RegisteredSession(val sessionId: String, val deviceId: String) {
    override fun toString(): String =
        "RegisteredSession(sessionId=" + sessionId + ", deviceId=" + deviceId + ")"
}

/**
 * Transport contract for server-side session management (TASK-007).
 *
 * - registerSession/refreshSession/revokeSession call Supabase Edge
 *   Functions; refreshSession is the server-side refresh CHOKE POINT
 *   that refuses tokens for revoked sessions (SR-01).
 * - isSessionRevoked/listDeviceSessions use PostgREST with RLS-protected
 *   reads of the caller's own rows only.
 */
interface SessionRemoteApi {
    suspend fun registerSession(
        accessToken: String,
        deviceName: String,
        platform: String,
        refreshToken: String
    ): RegisteredSession

    suspend fun refreshSession(refreshToken: String): AuthTokenResponse

    suspend fun isSessionRevoked(accessToken: String, sessionId: String): Boolean

    suspend fun listDeviceSessions(accessToken: String): List<DeviceSessionInfo>

    suspend fun revokeSession(accessToken: String, sessionId: String)
}
