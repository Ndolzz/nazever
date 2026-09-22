package com.naze.nazever.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Fixed shape tokens. Chat bubble spec (FR-16.4) is centralized here:
 * radius 20dp — components must NOT expose free size control.
 */
object NazeVerShapes {
    val chatBubble = RoundedCornerShape(20.dp)
    val card = RoundedCornerShape(16.dp)
    val input = RoundedCornerShape(14.dp)
    val button = RoundedCornerShape(24.dp)
    val sticker = RoundedCornerShape(12.dp)
}

/** Fixed spacing & sizing tokens (dp). */
object NazeVerDimens {
    val bubbleRadius = 20.dp
    val bubblePaddingH = 16.dp
    val bubblePaddingV = 10.dp
    val bubbleSpacing = 8.dp
    /** FR-16.4: bubble max width = 72% of screen width. */
    const val bubbleMaxWidthFraction: Float = 0.72f
    val screenPaddingH = 20.dp
    val spaceXS = 4.dp
    val spaceS = 8.dp
    val spaceM = 12.dp
    val spaceL = 16.dp
    val spaceXL = 24.dp
    val minTouchTarget = 48.dp
}
