package com.naze.nazever.core.network.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterSessionRequest(
    @SerialName("device_name") val deviceName: String,
    val platform: String,
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class RefreshSessionRequest(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class RevokeSessionRequest(
    @SerialName("session_id") val sessionId: String
)

@Serializable
data class RegisterSessionResponse(
    @SerialName("session_id") val sessionId: String,
    @SerialName("device_id") val deviceId: String
)

@Serializable
data class SessionStatusRow(
    val id: String,
    @SerialName("revoked_at") val revokedAt: String? = null
)

@Serializable
data class SessionDeviceRow(
    @SerialName("device_name") val deviceName: String? = null,
    val platform: String? = null,
    @SerialName("last_active_at") val lastActiveAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SessionListRow(
    val id: String,
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("revoked_at") val revokedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val devices: SessionDeviceRow? = null
)
