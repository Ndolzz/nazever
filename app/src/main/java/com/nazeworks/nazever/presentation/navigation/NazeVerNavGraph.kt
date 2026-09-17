package com.nazeworks.nazever.presentation.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nazeworks.nazever.presentation.auth.AuthScreen
import com.nazeworks.nazever.presentation.pairing.PairingScreen

private object Routes {
    const val AUTH = "auth"
    const val PAIRING = "pairing"
    const val HOME = "home" // placeholder, diisi lengkap di phase Shared Home
}

@Composable
fun NazeVerNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.AUTH) {
        composable(Routes.AUTH) {
            AuthScreen(
                onAuthenticated = {
                    navController.navigate(Routes.PAIRING) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.PAIRING) {
            PairingScreen(
                onPaired = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.PAIRING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            // Placeholder — Shared Home sesungguhnya (avatar, countdown, animal
            // companion, dst) dibangun di phase terpisah setelah setup ini.
            Text(text = "Pairing berhasil. Shared Home menyusul di phase berikutnya.")
        }
    }
}
