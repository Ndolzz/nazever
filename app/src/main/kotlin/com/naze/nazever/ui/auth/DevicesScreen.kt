package com.naze.nazever.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.naze.nazever.core.network.session.DeviceSessionInfo
import com.naze.nazever.ui.theme.NazeVerDimens
import com.naze.nazever.ui.theme.NazeVerType

/**
 * Device management screen (FR-01.6): lists the user's devices/sessions
 * with an indicator for the current device and a revoke (logout) button
 * for OTHER devices. Displays safe metadata only — never tokens.
 */
@Composable
fun DevicesScreen(
    state: AuthUiState,
    onBack: () -> Unit,
    onRevoke: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(NazeVerDimens.screenPaddingH)
    ) {
        Text("Perangkat Aktif", style = NazeVerType.heading, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(NazeVerDimens.spaceM))
        if (state.devicesLoading) {
            CircularProgressIndicator()
        }
        if (state.devicesMessage != null) {
            Text(
                text = state.devicesMessage,
                style = NazeVerType.bodySecondary,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(NazeVerDimens.spaceM))
        state.devices.forEach { device ->
            DeviceRow(
                device = device,
                isCurrent = device.sessionId == state.currentSessionId,
                onRevoke = onRevoke
            )
            Spacer(modifier = Modifier.height(NazeVerDimens.spaceL))
        }
        Spacer(modifier = Modifier.height(NazeVerDimens.spaceM))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kembali", style = NazeVerType.button)
        }
    }
}

@Composable
private fun DeviceRow(
    device: DeviceSessionInfo,
    isCurrent: Boolean,
    onRevoke: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = device.deviceName + if (isCurrent) " (perangkat ini)" else "",
            style = NazeVerType.title
        )
        Text(text = device.platform, style = NazeVerType.bodySecondary)
        if (device.lastActiveAt != null) {
            Text(text = "Terakhir aktif: " + device.lastActiveAt, style = NazeVerType.caption)
        }
        Spacer(modifier = Modifier.height(NazeVerDimens.spaceS))
        when {
            device.isRevoked -> Text(
                text = "Dikeluarkan",
                style = NazeVerType.caption,
                color = MaterialTheme.colorScheme.error
            )
            isCurrent -> Text(
                text = "Sesi perangkat ini",
                style = NazeVerType.caption,
                color = MaterialTheme.colorScheme.primary
            )
            else -> Button(onClick = { onRevoke(device.sessionId) }) {
                Text("Keluarkan perangkat ini", style = NazeVerType.button)
            }
        }
    }
}
