package com.naze.nazever

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.naze.nazever.ui.auth.AuthRootScreen
import com.naze.nazever.ui.auth.AuthViewModel
import com.naze.nazever.ui.auth.AuthViewModelFactory
import com.naze.nazever.ui.theme.NazeVerTheme

/**
 * TASK-006 entry point: hosts the authentication flow
 * (login / register / logout placeholder home).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NazeVerTheme {
                val authViewModel: AuthViewModel =
                    viewModel(factory = AuthViewModelFactory(application))
                AuthRootScreen(authViewModel)
            }
        }
    }
}
