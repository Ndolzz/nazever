package com.nazeworks.nazever.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nazeworks.nazever.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signUp(email: String, password: String, displayName: String) {
        if (!isPasswordStrongEnough(password)) {
            _uiState.value = AuthUiState.Error("Password minimal 8 karakter, kombinasi huruf & angka")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signUpWithEmail(email.trim(), password, displayName.trim())
            _uiState.value = result.fold(
                onSuccess = { AuthUiState.Success },
                onFailure = { AuthUiState.Error(it.message ?: "Sign up gagal") }
            )
        }
    }

    fun signIn(email: String, password: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signInWithEmail(email.trim(), password)
            _uiState.value = result.fold(
                onSuccess = { AuthUiState.Success },
                onFailure = { AuthUiState.Error(it.message ?: "Sign in gagal") }
            )
        }
    }

    // Validasi ringan di client hanya untuk UX cepat (early feedback).
    // Validasi otoritatif tetap di Supabase Auth (server).
    private fun isPasswordStrongEnough(password: String): Boolean =
        password.length >= 8 && password.any { it.isDigit() } && password.any { it.isLetter() }
}
