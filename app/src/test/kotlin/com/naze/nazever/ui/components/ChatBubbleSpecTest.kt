package com.naze.nazever.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * TASK-004: verifies the fixed chat bubble spec (FR-16.4) and message state
 * model. Compose rendering cannot run as plain JVM unit tests (no
 * Robolectric/androidTest infra yet — documented); these tests pin the
 * design tokens so no component silently drifts from the spec.
 */
class ChatBubbleSpecTest {

    @Test
    fun fixedBubbleSpecMatchesDesign() {
        assertEquals(16f, ChatBubbleSpec.textSp)
        assertEquals(11f, ChatBubbleSpec.timestampSp)
        assertEquals(20, ChatBubbleSpec.radiusDp)
        assertEquals(16, ChatBubbleSpec.paddingHDp)
        assertEquals(10, ChatBubbleSpec.paddingVDp)
        assertEquals(8, ChatBubbleSpec.spacingDp)
        assertEquals(0.72f, ChatBubbleSpec.maxWidthFraction)
    }

    @Test
    fun deliveryStatesCoverLifecycle() {
        // pending -> sent -> delivered -> read plus failed (design §7, NFR-02).
        assertEquals(5, MessageDeliveryState.entries.size)
    }
}
