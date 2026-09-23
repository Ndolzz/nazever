package com.naze.nazever.core.network.auth

/**
 * Authentication failures mapped to safe, fixed user-facing messages.
 * Error details (HTTP bodies) are never surfaced because Supabase may
 * echo submitted credentials in error responses.
 */
sealed class AuthError(val userMessage: String) {
    object Network : AuthError("Koneksi bermasalah. Periksa koneksi internet Anda.")
    object InvalidCredentials : AuthError("Email atau kata sandi salah.")
    object EmailTaken : AuthError("Email sudah terdaftar.")
    object WeakPassword : AuthError("Kata sandi terlalu lemah. Gunakan minimal 6 karakter.")
    object InvalidEmail : AuthError("Format email tidak valid.")
    object TooManyRequests : AuthError("Terlalu banyak percobaan. Coba lagi nanti.")
    object Unauthorized : AuthError("Sesi tidak valid. Silakan masuk kembali.")
    object Unknown : AuthError("Terjadi kesalahan. Silakan coba lagi.")
}

/** Maps transport-level exceptions to safe [AuthError] values. */
object AuthErrorMapper {
    fun fromException(t: Throwable): AuthError = when (t) {
        is AuthHttpException -> fromStatus(t.status)
        else -> when (t) {
            is java.io.IOException -> AuthError.Network
            else -> AuthError.Unknown
        }
    }

    fun fromStatus(status: Int): AuthError = when (status) {
        400, 401 -> AuthError.InvalidCredentials
        403 -> AuthError.Unauthorized
        422 -> AuthError.InvalidEmail
        423 -> AuthError.WeakPassword
        429 -> AuthError.TooManyRequests
        in 500..599 -> AuthError.Network
        else -> AuthError.Unknown
    }
}
