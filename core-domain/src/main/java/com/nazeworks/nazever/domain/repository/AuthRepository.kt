package com.nazeworks.nazever.domain.repository

import com.nazeworks.nazever.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Kontrak authentication. Implementasi konkret (Supabase Auth) ada di
 * core-data, supaya presentation/domain tidak pernah bergantung
 * langsung ke SDK pihak ketiga.
 */
interface AuthRepository {

    /** Emit user saat ini, atau null jika belum login. Reaktif terhadap sign-out/token-expired. */
    val currentUser: Flow<User?>

    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User>

    suspend fun signInWithEmail(email: String, password: String): Result<User>

    /** Meminta refresh access token secara eksplisit (dipanggil oleh interceptor saat 401). */
    suspend fun refreshSession(): Result<Unit>

    /** Sign out hanya di perangkat ini (revoke session lokal). */
    suspend fun signOut(): Result<Unit>

    /** Sign out semua device lain kecuali yang sedang dipakai — dipanggil dari Privacy Center. */
    suspend fun revokeAllOtherSessions(): Result<Unit>
}
