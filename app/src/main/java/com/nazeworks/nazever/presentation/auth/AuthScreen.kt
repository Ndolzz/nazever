package com.nazeworks.nazever.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()

    if (uiState is AuthUiState.Success) {
        onAuthenticated()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = if (isSignUpMode) "Buat Akun" else "Masuk")

        if (isSignUpMode) {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Nama tampilan") },
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.padding(top = 12.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.padding(top = 12.dp)
        )

        when (val state = uiState) {
            is AuthUiState.Error -> Text(text = state.message, modifier = Modifier.padding(top = 8.dp))
            is AuthUiState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(top = 12.dp))
            else -> Unit
        }

        Button(
            onClick = {
                if (isSignUpMode) {
                    viewModel.signUp(email, password, displayName)
                } else {
                    viewModel.signIn(email, password)
                }
            },
            modifier = Modifier
                .padding(top = 16.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Text(if (isSignUpMode) "Daftar" else "Masuk")
        }

        TextButton(
            onClick = { isSignUpMode = !isSignUpMode },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(if (isSignUpMode) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar")
        }
    }
}
