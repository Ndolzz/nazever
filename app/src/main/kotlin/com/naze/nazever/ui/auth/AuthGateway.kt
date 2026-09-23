package com.naze.nazever.ui.auth

import com.naze.nazever.core.network.auth.AuthResult
import com.naze.nazever.core.network.auth.NazeVerAuthRepository
import com.naze.nazever.core.network.session.DeviceSessionInfo
import com.naze.nazever.core.security.SessionData

/**
 * UI-facing auth contract. Kept as an interface so the ViewModel is
 * unit-testable on the JVM without Supabase or Android storage.
 * TASK-007 adds device/session management surface (FR-01.6).
 */
interface AuthGateway {
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun signOut()
    suspend fun restoreSession(): SessionData?
    fun hasSession(): Boolean
    suspend fun listDeviceSessions(): List<DeviceSessionInfo>
    suspend fun revokeDeviceSession(sessionId: String)
}

/** Production gateway delegating to the TASK-005/007 repository. */
class SupabaseAuthGateway(private val repository: NazeVerAuthRepository) : AuthGateway {
    override suspend fun signIn(email: String, password: String): AuthResult =
        repository.signIn(email, password)

    override suspend fun signUp(email: String, password: String): AuthResult =
        repository.signUp(email, password)

    override suspend fun signOut() = repository.signOut()

    override suspend fun restoreSession(): SessionData? = repository.restoreSession()

    override fun hasSession(): Boolean = repository.hasSession()

    override suspend fun listDeviceSessions(): List<DeviceSessionInfo> =
        repository.listDeviceSessions()

    override suspend fun revokeDeviceSession(sessionId: String) =
        repository.revokeDeviceSession(sessionId)
}
