package com.naze.nazever.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.naze.nazever.ui.theme.NazeVerDimens
import com.naze.nazever.ui.theme.NazeVerType

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSignUp: () -> Unit,
    onBackToLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(NazeVerDimens.screenPaddingH),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Buat akun", style = NazeVerType.heading, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(NazeVerDimens.spaceXL))

        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            isError = state.emailError != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        if (state.emailError != null) {
            Text(
                text = state.emailError,
                style = NazeVerType.caption,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(NazeVerDimens.spaceM))

        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text("Kata sandi") },
            isError = state.passwordError != null,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        if (state.passwordError != null) {
            Text(
                text = state.passwordError,
                style = NazeVerType.caption,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(NazeVerDimens.spaceM))

        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = onConfirmChange,
            label = { Text("Ulangi kata sandi") },
            isError = state.confirmError != null,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        if (state.confirmError != null) {
            Text(
                text = state.confirmError,
                style = NazeVerType.caption,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(NazeVerDimens.spaceL))

        if (state.authMessage != null) {
            Text(
                text = state.authMessage,
                style = NazeVerType.bodySecondary,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(NazeVerDimens.spaceM))
        }

        Button(
            onClick = onSignUp,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(NazeVerDimens.minTouchTarget))
            } else {
                Text("Daftar", style = NazeVerType.button)
            }
        }
        Spacer(modifier = Modifier.height(NazeVerDimens.spaceM))
        TextButton(onClick = onBackToLogin) {
            Text("Sudah punya akun? Masuk", style = NazeVerType.label)
        }
    }
}
