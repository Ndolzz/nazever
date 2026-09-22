package com.naze.nazever.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Shared font abstraction (FR-16.3): ONE font for every surface, no free
 * user font-size settings.
 *
 * The design (design §11) names Baloo 2 / Quicksand (SIL OFL — legal to bundle).
 * No font file is committed in this task: this provider resolves to the
 * platform default until an OFL font asset is added under
 * app/src/main/res/font/. Nothing else in the app needs to change then —
 * every style reads [NazeVerFont] through this single object.
 */
object NazeVerFont {
    /** Replace with FontFamily(Font(R.font.baloo2_regular), ...) when asset lands. */
    val family: FontFamily = FontFamily.Default
}

/**
 * Typography system (design §11). All sizes are fixed tokens.
 * Chat bubble spec (FR-16.4) is fixed here: message 16sp, timestamp 11sp.
 */
object NazeVerType {
    val display = TextStyle(fontFamily = NazeVerFont.family, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp)
    val heading = TextStyle(fontFamily = NazeVerFont.family, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp)
    val title = TextStyle(fontFamily = NazeVerFont.family, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp)
    val body = TextStyle(fontFamily = NazeVerFont.family, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp)
    val bodySecondary = TextStyle(fontFamily = NazeVerFont.family, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val caption = TextStyle(fontFamily = NazeVerFont.family, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
    val timestamp = TextStyle(fontFamily = NazeVerFont.family, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 14.sp)
    val label = TextStyle(fontFamily = NazeVerFont.family, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
    val button = TextStyle(fontFamily = NazeVerFont.family, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp)
    val input = TextStyle(fontFamily = NazeVerFont.family, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp)

    /** Chat message text (FR-16.4: fixed 16sp). */
    val chatMessage = body
    /** Chat metadata (timestamps, status, "edited"): FR-16.4 fixed 11sp. */
    val chatMetadata = timestamp
}

val NazeVerTypography = Typography()
