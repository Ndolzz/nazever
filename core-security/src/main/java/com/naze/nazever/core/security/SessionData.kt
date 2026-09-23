package com.naze.nazever.core.security

/**
 * Holds the current Supabase auth session.
 *
 * Tokens are sensitive data: they must never be logged, included in
 * exception messages, or written to non-encrypted storage. [toString]
 * is intentionally redacted.
 */
data class SessionData(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long,
    val userId: String,
    val email: String?
) {
    override fun toString(): String {
        return "SessionData(accessToken=<redacted>, refreshToken=<redacted>, " +
            "expiresAtMillis=" + expiresAtMillis + ", userId=" + userId + ", email=" + email + ")"
    }
}
