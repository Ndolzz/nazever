package com.naze.nazever.ui.auth

import androidx.lifecycle.ViewModel
import com.naze.nazever.core.network.auth.AuthResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the login / register / logout / device-management flow
 * (TASK-006, TASK-007).
 *
 * The scope is injectable so JVM unit tests never touch
 * Dispatchers.Main; production uses the default Main scope.
 * No tokens ever reach the UI state — only display strings.
 */
class AuthViewModel(
    private val gateway: AuthGateway,
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope =
        externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            if (gateway.hasSession()) {
                val session = gateway.restoreSession()
                if (session != null) {
                    _uiState.update {
                        it.copy(
                            screen = AuthScreen.Home,
                            loggedInEmail = session.email,
                            currentSessionId = session.sessionId
                        )
                    }
                }
            }
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, authMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, authMessage = null) }
    }

    fun onConfirmChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmError = null, authMessage = null) }
    }

    fun goToRegister() {
        _uiState.update { it.copy(screen = AuthScreen.Register, authMessage = null) }
    }

    fun backToLogin() {
        _uiState.update { it.copy(screen = AuthScreen.Login, authMessage = null) }
    }

    fun signIn() {
        val current = _uiState.value
        val emailError = AuthFormValidator.validateEmail(current.email)
        val passwordError = AuthFormValidator.validatePassword(current.password)
        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(emailError = emailError, passwordError = passwordError, authMessage = null)
            }
            return
        }
        launchAuth { gateway.signIn(current.email.trim(), current.password) }
    }

    fun signUp() {
        val current = _uiState.value
        val emailError = AuthFormValidator.validateEmail(current.email)
        val passwordError = AuthFormValidator.validatePassword(current.password)
        val confirmError = AuthFormValidator.validateConfirm(current.password, current.confirmPassword)
        if (emailError != null || passwordError != null || confirmError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmError = confirmError,
                    authMessage = null
                )
            }
            return
        }
        launchAuth { gateway.signUp(current.email.trim(), current.password) }
    }

    fun signOut() {
        scope.launch {
            gateway.signOut()
            _uiState.value = AuthUiState(screen = AuthScreen.Login)
        }
    }

    /** Opens the device management screen and loads the session list. */
    fun goToDevices() {
        _uiState.update {
            it.copy(
                screen = AuthScreen.Devices,
                devices = emptyList(),
                devicesLoading = true,
                devicesMessage = null
            )
        }
        scope.launch {
            try {
                val devices = gateway.listDeviceSessions()
                _uiState.update { it.copy(devicesLoading = false, devices = devices) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(devicesLoading = false, devicesMessage = "Tidak dapat memuat daftar perangkat.")
                }
            }
        }
    }

    fun backToHome() {
        _uiState.update {
            it.copy(screen = AuthScreen.Home, devices = emptyList(), devicesMessage = null)
        }
    }

    /**
     * Revokes another device's session (FR-01.6). The current device
     * cannot be revoked through this path — it must use signOut.
     */
    fun revokeDevice(sessionId: String) {
        if (sessionId == _uiState.value.currentSessionId) return
        scope.launch {
            try {
                gateway.revokeDeviceSession(sessionId)
                val devices = gateway.listDeviceSessions()
                _uiState.update { it.copy(devices = devices, devicesMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(devicesMessage = "Gagal mencabut perangkat.") }
            }
        }
    }

    private fun launchAuth(block: suspend () -> AuthResult) {
        _uiState.update { it.copy(isLoading = true, authMessage = null) }
        scope.launch {
            val result = block()
            _uiState.update { current ->
                when (result) {
                    is AuthResult.Success -> current.copy(
                        isLoading = false,
                        screen = AuthScreen.Home,
                        loggedInEmail = result.session.email,
                        currentSessionId = result.session.sessionId,
                        password = "",
                        confirmPassword = "",
                        passwordError = null,
                        confirmError = null,
                        authMessage = null
                    )
                    is AuthResult.Failure -> current.copy(
                        isLoading = false,
                        authMessage = result.error.userMessage
                    )
                }
            }
        }
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}
