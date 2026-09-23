package com.naze.nazever.core.network.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignUpRequest(val email: String, val password: String)

@Serializable
data class PasswordGrant(
    val email: String,
    val password: String,
    @SerialName("grant_type") val grantType: String = "password"
)

@Serializable
data class RefreshGrant(
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("grant_type") val grantType: String = "refresh_token"
)

@Serializable
data class AuthUser(
    val id: String,
    val email: String? = null
) {
    override fun toString(): String = "AuthUser(id=" + id + ", email=" + email + ")"
}

@Serializable
data class AuthTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 0L,
    @SerialName("token_type") val tokenType: String? = null,
    val user: AuthUser? = null
) {
    override fun toString(): String =
        "AuthTokenResponse(accessToken=<redacted>, refreshToken=<redacted>, " +
            "expiresIn=" + expiresIn + ", user=" + user + ")"
}
