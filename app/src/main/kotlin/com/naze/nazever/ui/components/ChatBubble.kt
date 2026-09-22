package com.naze.nazever.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.naze.nazever.ui.theme.NazeVerColors
import com.naze.nazever.ui.theme.NazeVerDimens
import com.naze.nazever.ui.theme.NazeVerShapes
import com.naze.nazever.ui.theme.NazeVerType

/** Delivery lifecycle of an outgoing message (design §7). */
enum class MessageDeliveryState { PENDING, SENT, DELIVERED, READ, FAILED }

/** Stable visual identity of a chat bubble. */
enum class BubbleSide { INCOMING, OUTGOING }

/**
 * NazeVer chat bubble (FR-16.4 — fixed spec, no user size control):
 *   text 16sp, radius 20dp, padding H16 V10, max width 72% screen,
 *   inter-bubble spacing 8dp, timestamp 11sp.
 */
object ChatBubbleSpec {
    val textSp = 16f
    val timestampSp = 11f
    val radiusDp = 20
    val paddingHDp = 16
    val paddingVDp = 10
    val spacingDp = 8
    val maxWidthFraction = NazeVerDimens.bubbleMaxWidthFraction
}

data class ReplyPreview(val authorName: String, val previewText: String)

@Composable
fun ChatBubble(
    text: String,
    timestamp: String,
    side: BubbleSide,
    modifier: Modifier = Modifier,
    deliveryState: MessageDeliveryState? = null,
    replyPreview: ReplyPreview? = null,
    edited: Boolean = false,
    deleted: Boolean = false,
) {
    val isOutgoing = side == BubbleSide.OUTGOING
    val bgColor = if (isOutgoing) NazeVerColors.outgoingBubble else NazeVerColors.incomingBubble
    val contentColor = if (isOutgoing) NazeVerColors.onOutgoingBubble else NazeVerColors.onIncomingBubble

    val maxWidthDp = (LocalConfiguration.current.screenWidthDp * ChatBubbleSpec.maxWidthFraction).dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NazeVerDimens.spaceL)
            .semantics {
                contentDescription = if (deleted) "Deleted message" else "Chat message"
            },
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidthDp)
                .background(bgColor, NazeVerShapes.chatBubble)
                .padding(horizontal = NazeVerDimens.bubblePaddingH, vertical = NazeVerDimens.bubblePaddingV),
        ) {
            replyPreview?.let { reply ->
                ReplyPreviewBlock(reply, contentColor)
                Spacer(Modifier.height(NazeVerDimens.spaceXS))
            }
            Text(
                text = if (deleted) "Message was deleted" else text,
                style = NazeVerType.chatMessage,
                color = if (deleted) contentColor.copy(alpha = 0.6f) else contentColor,
                textDecoration = if (deleted) TextDecoration.LineThrough else null,
            )
            Spacer(Modifier.height(NazeVerDimens.spaceXS))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (edited && !deleted) {
                    Text("edited", style = NazeVerType.chatMetadata, color = contentColor.copy(alpha = 0.7f))
                    Spacer(Modifier.width(NazeVerDimens.spaceS))
                }
                Text(timestamp, style = NazeVerType.chatMetadata, color = contentColor.copy(alpha = 0.7f))
                deliveryState?.let { state ->
                    Spacer(Modifier.width(NazeVerDimens.spaceS))
                    DeliveryIndicator(state, contentColor)
                }
            }
        }
    }
}

@Composable
private fun ReplyPreviewBlock(reply: ReplyPreview, contentColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(contentColor.copy(alpha = 0.12f), NazeVerShapes.sticker)
            .padding(horizontal = NazeVerDimens.spaceM, vertical = NazeVerDimens.spaceXS),
    ) {
        Text(reply.authorName, style = NazeVerType.label, color = contentColor.copy(alpha = 0.85f))
        Text(reply.previewText, style = NazeVerType.bodySecondary, color = contentColor.copy(alpha = 0.7f), maxLines = 1)
    }
}

@Composable
private fun DeliveryIndicator(state: MessageDeliveryState, color: Color) {
    val description = when (state) {
        MessageDeliveryState.PENDING -> "Sending"
        MessageDeliveryState.SENT -> "Sent"
        MessageDeliveryState.DELIVERED -> "Delivered"
        MessageDeliveryState.READ -> "Read"
        MessageDeliveryState.FAILED -> "Failed to send"
    }
    val tint = if (state == MessageDeliveryState.FAILED) NazeVerColors.error else color.copy(alpha = 0.8f)
    when (state) {
        // Lightweight status dots — no heavy animation (design §10).
        MessageDeliveryState.PENDING -> Dot(tint, description)
        MessageDeliveryState.SENT -> Dot(tint, description)
        MessageDeliveryState.DELIVERED -> Row { Dot(tint, description); Dot(tint, description) }
        MessageDeliveryState.READ -> Row { Dot(NazeVerColors.info, description); Dot(NazeVerColors.info, description) }
        MessageDeliveryState.FAILED -> Text("!", style = NazeVerType.chatMetadata, color = tint)
    }
}

@Composable
private fun Dot(color: Color, description: String) {
    Box(
        modifier = Modifier
            .size(4.dp)
            .background(color, CircleShape)
            .semantics { contentDescription = description },
    )
}
