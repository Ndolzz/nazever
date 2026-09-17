package com.nazeworks.nazever.presentation.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Layar pairing. Ditampilkan setelah login, selama user belum punya
 * `activePairSpace`. Begitu `activePairSpace` != null (terdeteksi lewat
 * realtime, biasanya karena User A meng-approve), navigasi ke Shared Home
 * dilakukan oleh pemanggil (lihat presentation/navigation).
 */
@Composable
fun PairingScreen(
    onPaired: () -> Unit,
    viewModel: PairingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.activePairSpace != null) {
        onPaired()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Hubungkan dengan pasanganmu")
        Text(text = "Buat undangan lalu bagikan link/QR-nya langsung ke pasanganmu (bukan lewat aplikasi lain yang tidak aman).")

        if (uiState.isLoading) {
            CircularProgressIndicator()
        }

        uiState.errorMessage?.let { Text(text = it) }

        val invite = uiState.currentInvite
        if (invite == null) {
            Button(onClick = { viewModel.createInvite() }) {
                Text("Buat Undangan Pairing")
            }
        } else {
            Text(text = "Bagikan link ini ke pasanganmu (berlaku 10 menit):")
            Text(text = invite.deepLink)
            // TODO(phase UI-polish): render QR code dari invite.deepLink
            // memakai library QR lokal (mis. zxing), tanpa upload ke server mana pun.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { viewModel.revokeCurrentInvite() }) {
                    Text("Batalkan")
                }
            }
        }

        if (uiState.incomingRequestInviteIds.isNotEmpty()) {
            Text(text = "Permintaan pairing masuk:")
            LazyColumn {
                items(uiState.incomingRequestInviteIds) { inviteId ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Seseorang ingin pairing denganmu")
                        Button(onClick = { viewModel.confirmPairing(inviteId, accept = true) }) {
                            Text("Terima")
                        }
                        OutlinedButton(onClick = { viewModel.confirmPairing(inviteId, accept = false) }) {
                            Text("Tolak")
                        }
                    }
                }
            }
        }
    }
}
