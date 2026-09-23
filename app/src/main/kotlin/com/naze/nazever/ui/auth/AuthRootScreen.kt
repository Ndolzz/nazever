package com.naze.nazever.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.naze.nazever.ui.theme.NazeVerDimens
import com.naze.nazever.ui.theme.NazeVerType

/**
 * Root of the auth flow (TASK-006/007). Simple state-based routing; full
 * navigation arrives with later feature tasks.
 */
@Composable
fun AuthRootScreen(viewModel: AuthViewModel) {
    val state by viewModel.uiState.collectAsState()
    when (state.screen) {
        AuthScreen.Login -> LoginScreen(
            state = state,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onSignIn = viewModel::signIn,
            onGoToRegister = viewModel::goToRegister
        )
        AuthScreen.Register -> RegisterScreen(
            state = state,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onConfirmChange = viewModel::onConfirmChange,
            onSignUp = viewModel::signUp,
            onBackToLogin = viewModel::backToLogin
        )
        AuthScreen.Home -> HomeScreen(
            loggedInEmail = state.loggedInEmail,
            onSignOut = viewModel::signOut,
            onShowDevices = viewModel::goToDevices
        )
        AuthScreen.Devices -> DevicesScreen(
            state = state,
            onBack = viewModel::backToHome,
            onRevoke = viewModel::revokeDevice
        )
    }
}

/** Placeholder home shown when signed in (real home arrives in later tasks). */
@Composable
fun HomeScreen(
    loggedInEmail: String?,
    onSignOut: () -> Unit,
    onShowDevices: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(NazeVerDimens.screenPaddingH),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("NazeVer", style = NazeVerType.heading, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(NazeVerDimens.spaceM))
        Text(
            text = if (loggedInEmail == null) "" else "Masuk sebagai " + loggedInEmail,
            style = NazeVerType.bodySecondary
        )
        Spacer(modifier = Modifier.height(NazeVerDimens.spaceXL))
        Button(
            onClick = onShowDevices,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Perangkat Aktif", style = NazeVerType.button)
        }
        Spacer(modifier = Modifier.height(NazeVerDimens.spaceS))
        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Keluar", style = NazeVerType.button)
        }
    }
}
