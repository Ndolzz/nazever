package com.naze.nazever.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * TASK-004: design-token integrity. All colors resolve through
 * NazeVerColors — screens must never hardcode values (FR-16).
 */
class DesignTokenTest {

    @Test
    fun requiredSemanticTokensExist() {
        assertNotNull(NazeVerColors.primary)
        assertNotNull(NazeVerColors.secondary)
        assertNotNull(NazeVerColors.background)
        assertNotNull(NazeVerColors.surface)
        assertNotNull(NazeVerColors.surfaceVariant)
        assertNotNull(NazeVerColors.textPrimary)
        assertNotNull(NazeVerColors.textSecondary)
        assertNotNull(NazeVerColors.border)
        assertNotNull(NazeVerColors.divider)
        assertNotNull(NazeVerColors.success)
        assertNotNull(NazeVerColors.warning)
        assertNotNull(NazeVerColors.error)
        assertNotNull(NazeVerColors.info)
        assertNotNull(NazeVerColors.disabled)
        assertNotNull(NazeVerColors.overlay)
        assertNotNull(NazeVerColors.scrapbookTape)
    }

    @Test
    fun bubbleMaxWidthMatchesSpec() {
        assertEquals(0.72f, NazeVerDimens.bubbleMaxWidthFraction)
    }

    @Test
    fun chatTypographyFollowsFixedSpec() {
        // FR-16.4: message 16sp, metadata/timestamp 11sp.
        assertEquals(16f, NazeVerType.chatMessage.fontSize.value)
        assertEquals(11f, NazeVerType.chatMetadata.fontSize.value)
    }

    @Test
    fun allStylesShareOneFontFamily() {
        // FR-16.3: single font across every surface.
        val styles = listOf(
            NazeVerType.display, NazeVerType.heading, NazeVerType.title,
            NazeVerType.body, NazeVerType.bodySecondary, NazeVerType.caption,
            NazeVerType.timestamp, NazeVerType.label, NazeVerType.button,
            NazeVerType.input, NazeVerType.chatMessage, NazeVerType.chatMetadata,
        )
        val families = styles.map { it.fontFamily }.toSet()
        assertEquals(1, families.size)
    }
}
