package com.naze.nazever.core.network.session

/**
 * Safe device/session metadata for the device management UI (FR-01.6).
 * Carries NO tokens or credentials — display data only.
 */
data class DeviceSessionInfo(
    val sessionId: String,
    val deviceId: String?,
    val deviceName: String,
    val platform: String,
    val lastActiveAt: String?,
    val createdAt: String?,
    val revokedAt: String?
) {
    val isRevoked: Boolean get() = revokedAt != null
}
