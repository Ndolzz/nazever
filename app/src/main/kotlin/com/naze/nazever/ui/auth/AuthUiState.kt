package com.naze.nazever.ui.auth

import com.naze.nazever.core.network.session.DeviceSessionInfo

/** Destinations of the auth flow. */
enum class AuthScreen {
    Login,
    Register,
    Home,
    Devices
}

/**
 * Immutable UI state for the auth flow. Contains no token material —
 * only display metadata (email, device names/timestamps).
 */
data class AuthUiState(
    val screen: AuthScreen = AuthScreen.Login,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val isLoading: Boolean = false,
    val authMessage: String? = null,
    val loggedInEmail: String? = null,
    val currentSessionId: String? = null,
    val devices: List<DeviceSessionInfo> = emptyList(),
    val devicesLoading: Boolean = false,
    val devicesMessage: String? = null
)
