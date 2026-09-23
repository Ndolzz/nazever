package com.naze.nazever.ui.auth

import com.naze.nazever.core.network.auth.AuthError
import com.naze.nazever.core.network.auth.AuthResult
import com.naze.nazever.core.network.session.DeviceSessionInfo
import com.naze.nazever.core.security.SessionData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAuthGateway : AuthGateway {
    val session: SessionData =
        SessionData("access-token", "refresh-token", 1000L, "user-1", "user@example.com", "session-A", "device-A")
    var failSignIn = false
    var failSignUp = false
    var sessionAvailable = false
    var restoreResult: SessionData? = session
    var signedOut = false
    var signInCalls = 0
    var signUpCalls = 0
    var listCalls = 0
    val revokedSessions = mutableListOf<String>()
    var deviceSessions = listOf(
        DeviceSessionInfo("session-A", "device-A", "Pixel 8", "android", "2026-09-20T10:00:00Z", "2026-09-19T10:00:00Z", null),
        DeviceSessionInfo("session-B", "device-B", "Galaxy S24", "android", "2026-09-21T10:00:00Z", "2026-09-18T10:00:00Z", null)
    )

    override suspend fun signIn(email: String, password: String): AuthResult {
        signInCalls++
        return if (failSignIn) {
            AuthResult.Failure(AuthError.InvalidCredentials)
        } else {
            AuthResult.Success(session.copy(email = email))
        }
    }

    override suspend fun signUp(email: String, password: String): AuthResult {
        signUpCalls++
        return if (failSignUp) {
            AuthResult.Failure(AuthError.EmailTaken)
        } else {
            AuthResult.Success(session.copy(email = email))
        }
    }

    override suspend fun signOut() {
        signedOut = true
    }

    override suspend fun restoreSession(): SessionData? =
        if (sessionAvailable) restoreResult else null

    override fun hasSession(): Boolean = sessionAvailable

    override suspend fun listDeviceSessions(): List<DeviceSessionInfo> {
        listCalls++
        return deviceSessions.map { d ->
            d.copy(
                revokedAt = if (revokedSessions.contains(d.sessionId)) "2026-09-23T00:00:00Z" else d.revokedAt
            )
        }
    }

    override suspend fun revokeDeviceSession(sessionId: String) {
        revokedSessions.add(sessionId)
    }
}

class AuthViewModelTest {

    private fun viewModel(gateway: FakeAuthGateway): AuthViewModel =
        AuthViewModel(gateway, CoroutineScope(Dispatchers.Unconfined))

    private fun filledLoginViewModel(gateway: FakeAuthGateway): AuthViewModel {
        val vm = viewModel(gateway)
        vm.onEmailChange("user@example.com")
        vm.onPasswordChange("rahasia123")
        return vm
    }

    @Test
    fun startsOnLoginScreen() {
        val vm = viewModel(FakeAuthGateway())
        assertEquals(AuthScreen.Login, vm.uiState.value.screen)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun restoreSessionNavigatesHome() {
        val gateway = FakeAuthGateway().apply { sessionAvailable = true }
        val vm = viewModel(gateway)
        assertEquals(AuthScreen.Home, vm.uiState.value.screen)
        assertEquals("user@example.com", vm.uiState.value.loggedInEmail)
        assertEquals("session-A", vm.uiState.value.currentSessionId)
    }

    @Test
    fun revokedSessionRestoreKeepsUserOnLogin() {
        // Repository cleared the local store after revocation detection:
        // hasSession() is true but restoreSession() returns null.
        val gateway = FakeAuthGateway().apply {
            sessionAvailable = true
            restoreResult = null
        }
        val vm = viewModel(gateway)
        assertEquals(AuthScreen.Login, vm.uiState.value.screen)
        assertNull(vm.uiState.value.loggedInEmail)
    }

    @Test
    fun invalidFormBlocksSignIn() {
        val gateway = FakeAuthGateway()
        val vm = viewModel(gateway)
        vm.onEmailChange("bukan-email")
        vm.onPasswordChange("123")
        vm.signIn()
        assertEquals(AuthScreen.Login, vm.uiState.value.screen)
        assertNotNull(vm.uiState.value.emailError)
        assertNotNull(vm.uiState.value.passwordError)
        assertEquals(0, gateway.signInCalls)
    }

    @Test
    fun signInSuccessNavigatesHome() {
        val gateway = FakeAuthGateway()
        val vm = filledLoginViewModel(gateway)
        vm.signIn()
        assertEquals(AuthScreen.Home, vm.uiState.value.screen)
        assertEquals("user@example.com", vm.uiState.value.loggedInEmail)
        assertEquals("session-A", vm.uiState.value.currentSessionId)
        assertFalse(vm.uiState.value.isLoading)
        // Password is cleared from UI state after success.
        assertEquals("", vm.uiState.value.password)
    }

    @Test
    fun signInFailureShowsSafeMessage() {
        val gateway = FakeAuthGateway().apply { failSignIn = true }
        val vm = filledLoginViewModel(gateway)
        vm.signIn()
        assertEquals(AuthScreen.Login, vm.uiState.value.screen)
        assertEquals(AuthError.InvalidCredentials.userMessage, vm.uiState.value.authMessage)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun signUpSuccessNavigatesHome() {
        val gateway = FakeAuthGateway()
        val vm = viewModel(gateway)
        vm.onEmailChange("new@example.com")
        vm.onPasswordChange("rahasia123")
        vm.onConfirmChange("rahasia123")
        vm.signUp()
        assertEquals(AuthScreen.Home, vm.uiState.value.screen)
        assertEquals("new@example.com", vm.uiState.value.loggedInEmail)
    }

    @Test
    fun signUpFailureShowsSafeMessage() {
        val gateway = FakeAuthGateway().apply { failSignUp = true }
        val vm = viewModel(gateway)
        vm.goToRegister()
        vm.onEmailChange("new@example.com")
        vm.onPasswordChange("rahasia123")
        vm.onConfirmChange("rahasia123")
        vm.signUp()
        assertEquals(AuthScreen.Register, vm.uiState.value.screen)
        assertEquals(AuthError.EmailTaken.userMessage, vm.uiState.value.authMessage)
    }

    @Test
    fun passwordMismatchBlocksSignUp() {
        val gateway = FakeAuthGateway()
        val vm = viewModel(gateway)
        vm.goToRegister()
        vm.onEmailChange("new@example.com")
        vm.onPasswordChange("rahasia123")
        vm.onConfirmChange("beda12345")
        vm.signUp()
        assertEquals(AuthScreen.Register, vm.uiState.value.screen)
        assertEquals("Konfirmasi kata sandi tidak sama.", vm.uiState.value.confirmError)
        assertEquals(0, gateway.signUpCalls)
    }

    @Test
    fun signOutReturnsToLoginAndClearsForm() {
        val gateway = FakeAuthGateway()
        val vm = filledLoginViewModel(gateway)
        vm.signIn()
        assertEquals(AuthScreen.Home, vm.uiState.value.screen)
        vm.signOut()
        assertEquals(AuthScreen.Login, vm.uiState.value.screen)
        assertTrue(gateway.signedOut)
        assertEquals("", vm.uiState.value.email)
        assertNull(vm.uiState.value.loggedInEmail)
        assertNull(vm.uiState.value.currentSessionId)
    }

    @Test
    fun authMessageNeverContainsPassword() {
        val gateway = FakeAuthGateway().apply { failSignIn = true }
        val vm = viewModel(gateway)
        vm.onEmailChange("user@example.com")
        vm.onPasswordChange("secret-password-value")
        vm.signIn()
        val message = vm.uiState.value.authMessage
        assertNotNull(message)
        assertFalse(message!!.contains("secret-password-value"))
        assertFalse(message.contains("user@example.com"))
    }

    // ===== TASK-007: device management =====

    @Test
    fun devicesListLoads() {
        val gateway = FakeAuthGateway()
        val vm = filledLoginViewModel(gateway)
        vm.signIn()
        vm.goToDevices()
        val state = vm.uiState.value
        assertEquals(AuthScreen.Devices, state.screen)
        assertEquals(2, state.devices.size)
        assertFalse(state.devicesLoading)
        assertEquals("session-A", state.currentSessionId)
    }

    @Test
    fun revokeRefreshesList() {
        val gateway = FakeAuthGateway()
        val vm = filledLoginViewModel(gateway)
        vm.signIn()
        vm.goToDevices()
        vm.revokeDevice("session-B")
        assertTrue(gateway.revokedSessions.contains("session-B"))
        val state = vm.uiState.value
        assertEquals(2, state.devices.size)
        assertTrue(state.devices.first { it.sessionId == "session-B" }.isRevoked)
    }

    @Test
    fun selfRevokeIsBlocked() {
        val gateway = FakeAuthGateway()
        val vm = filledLoginViewModel(gateway)
        vm.signIn()
        vm.goToDevices()
        vm.revokeDevice("session-A")
        assertFalse(gateway.revokedSessions.contains("session-A"))
    }
}
