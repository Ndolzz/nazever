package com.naze.nazever.ui.auth

import com.naze.nazever.core.network.auth.AuthResult
import com.naze.nazever.core.network.auth.NazeVerAuthRepository
import com.naze.nazever.core.security.SessionData

/**
 * UI-facing auth contract. Kept as an interface so the ViewModel is
 * unit-testable on the JVM without Supabase or Android storage.
 */
interface AuthGateway {
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun signOut()
    suspend fun restoreSession(): SessionData?
    fun hasSession(): Boolean
}

/** Production gateway delegating to the TASK-005 repository. */
class SupabaseAuthGateway(private val repository: NazeVerAuthRepository) : AuthGateway {
    override suspend fun signIn(email: String, password: String): AuthResult =
        repository.signIn(email, password)

    override suspend fun signUp(email: String, password: String): AuthResult =
        repository.signUp(email, password)

    override suspend fun signOut() = repository.signOut()

    override suspend fun restoreSession(): SessionData? = repository.restoreSession()

    override fun hasSession(): Boolean = repository.hasSession()
}
