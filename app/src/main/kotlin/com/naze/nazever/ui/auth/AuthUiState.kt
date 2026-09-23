package com.naze.nazever.ui.auth

/** Destinations of the auth flow. */
enum class AuthScreen {
    Login,
    Register,
    Home
}

/**
 * Immutable UI state for the auth flow. Contains no token material —
 * only the last known account email for display purposes.
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
    val loggedInEmail: String? = null
)
