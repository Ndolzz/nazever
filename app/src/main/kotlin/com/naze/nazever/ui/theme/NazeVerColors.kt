package com.naze.nazever.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * NazeVer centralized design tokens (FR-16, design §11).
 * Single source of truth for every color used by the app.
 * Cute / soft / scrapbook pastel palette — no generic messenger look.
 */
object NazeVerColors {
    val primary = Color(0xFFE98BA0)          // soft rose
    val primaryContainer = Color(0xFFFFD9DE)
    val onPrimary = Color(0xFFFFFFFF)
    val onPrimaryContainer = Color(0xFF4A0F1E)

    val secondary = Color(0xFF9CCB86)        // soft leaf green
    val secondaryContainer = Color(0xFFDDF3D0)
    val onSecondary = Color(0xFFFFFFFF)
    val onSecondaryContainer = Color(0xFF1B3A10)

    val background = Color(0xFFFFF8F5)       // warm cream
    val onBackground = Color(0xFF3A2C2E)
    val surface = Color(0xFFFFFFFF)
    val surfaceVariant = Color(0xFFFDF1EC)  // surface variant
    val onSurface = Color(0xFF3A2C2E)

    val textPrimary = Color(0xFF3A2C2E)
    val textSecondary = Color(0xFF8C7A7D)

    val border = Color(0xFFF0DCD4)
    val divider = Color(0xFFF5E5DD)

    val success = Color(0xFF7FAE6B)
    val warning = Color(0xFFE8B75C)
    val error = Color(0xFFD26A6A)
    val info = Color(0xFF7FA3C4)
    val disabled = Color(0xFFCDBDB9)
    val onDisabled = Color(0xFF8C7A7D)

    val overlay = Color(0x663A2C2E)          // scrim over wallpaper (readability, design §11)
    val incomingBubble = Color(0xFFFDF1EC)
    val outgoingBubble = Color(0xFFE98BA0)
    val onIncomingBubble = textPrimary
    val onOutgoingBubble = Color(0xFFFFFFFF)

    val scrapbookPaper = Color(0xFFFFFDF9)
    val scrapbookTape = Color(0xFFD9C8A9)    // washi-tape accent
}
