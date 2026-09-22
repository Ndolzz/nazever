package com.naze.nazever.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = NazeVerColors.primary,
    onPrimary = NazeVerColors.onPrimary,
    primaryContainer = NazeVerColors.primaryContainer,
    onPrimaryContainer = NazeVerColors.onPrimaryContainer,
    secondary = NazeVerColors.secondary,
    onSecondary = NazeVerColors.onSecondary,
    secondaryContainer = NazeVerColors.secondaryContainer,
    onSecondaryContainer = NazeVerColors.onSecondaryContainer,
    background = NazeVerColors.background,
    onBackground = NazeVerColors.onBackground,
    surface = NazeVerColors.surfaceVariant,
    onSurface = NazeVerColors.onSurface,
    outline = NazeVerColors.border,
    error = NazeVerColors.error,
)

// NazeVer is a soft, cute, private app: the dark variant is kept warm
// (not generic gray).
private val DarkScheme = darkColorScheme(
    primary = NazeVerColors.primary,
    onPrimary = NazeVerColors.onPrimary,
    secondary = NazeVerColors.secondary,
    onSecondary = NazeVerColors.onSecondary,
    background = Color(0xFF221A1C),
    onBackground = Color(0xFFF3E7E4),
    surface = Color(0xFF2C2224),
    onSurface = Color(0xFFF3E7E4),
    outline = Color(0xFF4A3A3C),
    error = NazeVerColors.error,
)

/** Entry theme for every NazeVer screen. */
@Composable
fun NazeVerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = NazeVerTypography,
        content = content,
    )
}
