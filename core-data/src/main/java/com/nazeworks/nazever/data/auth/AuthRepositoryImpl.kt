package com.nazeworks.nazever.data.auth

import com.nazeworks.nazever.domain.model.User
import com.nazeworks.nazever.domain.model.UserId
import com.nazeworks.nazever.domain.repository.AuthRepository
import com.nazeworks.nazever.security.SecureSessionStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SessionStatus
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class AuthRepositoryImpl(
    private val supabase: SupabaseClient,
    private val secureSessionStore: SecureSessionStore
) : AuthRepository {

    override val currentUser: Flow<User?>
        get() = supabase.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    // Token disalin ke Keystore-backed storage kita sendiri, supaya
                    // lapisan lain (mis. interceptor network) tidak perlu bergantung
                    // pada in-memory session SDK yang hilang saat proses mati.
                    secureSessionStore.saveSession(
                        accessToken = status.session.accessToken,
                        refreshToken = status.session.refreshToken,
                        expiresAtEpochSeconds = status.session.expiresAt.epochSeconds
                    )
                    status.session.user?.toDomain()
                }
                is SessionStatus.NotAuthenticated -> {
                    secureSessionStore.clear()
                    null
                }
                else -> null // Initializing / RefreshFailure -> jangan anggap logged out prematur
            }
        }

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User> =
        runCatching {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObjectDisplayName(displayName)
            }
            requireNotNull(supabase.auth.currentUserOrNull()?.toDomain()) {
                "Sign up berhasil tapi user info kosong"
            }
        }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> = runCatching {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        requireNotNull(supabase.auth.currentUserOrNull()?.toDomain()) {
            "Sign in berhasil tapi user info kosong"
        }
    }

    override suspend fun refreshSession(): Result<Unit> = runCatching {
        supabase.auth.refreshCurrentSession()
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        supabase.auth.signOut()
        secureSessionStore.clear()
    }

    override suspend fun revokeAllOtherSessions(): Result<Unit> = runCatching {
        // SignOutScope.OTHERS: revoke refresh token di semua device lain,
        // device ini tetap login. Dipanggil dari Privacy Center > Active Devices.
        supabase.auth.signOut(scope = io.github.jan.supabase.auth.SignOutScope.OTHERS)
    }

    private fun UserInfo.toDomain() = User(
        id = UserId(id),
        displayName = (userMetadata?.get("display_name") as? String) ?: "NazeVer User",
        avatarUrl = userMetadata?.get("avatar_url") as? String,
        createdAt = createdAt ?: Clock.System.now()
    )
}
