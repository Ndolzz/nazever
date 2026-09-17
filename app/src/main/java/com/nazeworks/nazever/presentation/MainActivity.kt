package com.nazeworks.nazever.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.nazeworks.nazever.presentation.navigation.NazeVerNavGraph
import dagger.hilt.android.AndroidEntryPoint

/**
 * Deep link `nazever://pair?token=...` masuk lewat activity ini.
 * Ekstraksi token dan pemanggilan `PairingViewModel.consumeInvite` akan
 * dihubungkan penuh saat PairingScreen sudah menerima nav-arg dari intent
 * (bagian UI-polish, disebutkan sebagai TODO supaya tidak diam-diam
 * dianggap sudah berfungsi di phase ini).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    NazeVerNavGraph()
                }
            }
        }
    }
}
