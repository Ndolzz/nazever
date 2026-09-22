package com.naze.nazever.ui.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.naze.nazever.ui.components.AnimalKind
import com.naze.nazever.ui.components.AnimalTemplate
import com.naze.nazever.ui.components.BubbleSide
import com.naze.nazever.ui.components.ChatBubble
import com.naze.nazever.ui.components.CuteDivider
import com.naze.nazever.ui.components.EmptyState
import com.naze.nazever.ui.components.ErrorState
import com.naze.nazever.ui.components.LoadingState
import com.naze.nazever.ui.components.MessageDeliveryState
import com.naze.nazever.ui.components.ScrapbookCard
import com.naze.nazever.ui.components.ScrapbookContainer
import com.naze.nazever.ui.components.SectionHeader
import com.naze.nazever.ui.components.SharedWallpaperSurface
import com.naze.nazever.ui.components.StickerContainer
import com.naze.nazever.ui.theme.NazeVerColors
import com.naze.nazever.ui.theme.NazeVerDimens
import com.naze.nazever.ui.theme.NazeVerType

/**
 * Internal design-system showcase (TASK-004 only).
 * NOT wired to any backend, navigation, or product feature.
 * Shows: typography, colors, buttons, input, chat bubbles, scrapbook card,
 * animal decorations, wallpaper surface, and loading/empty/error states.
 */
@Composable
fun DesignSystemShowcase() {
    SharedWallpaperSurface(modifier = Modifier.fillMaxSize()) {
        ScrapbookContainer(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Text("NazeVer Design System", style = NazeVerType.display, color = NazeVerColors.textPrimary)

            SectionHeader("Typography")
            Column(verticalArrangement = Arrangement.spacedBy(NazeVerDimens.spaceS)) {
                Text("Display 32sp", style = NazeVerType.display)
                Text("Heading 24sp", style = NazeVerType.heading)
                Text("Title 20sp", style = NazeVerType.title)
                Text("Body 16sp — cute, soft, readable", style = NazeVerType.body)
                Text("Body secondary 14sp", style = NazeVerType.bodySecondary)
                Text("Caption 12sp", style = NazeVerType.caption)
                Text("Timestamp 11sp", style = NazeVerType.timestamp)
            }

            SectionHeader("Buttons & Input")
            var inputText by remember { mutableStateOf("") }
            Button(onClick = {}) { Text("Paw-fect!", style = NazeVerType.button) }
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Write a note...", style = NazeVerType.input) },
            )

            SectionHeader("Chat bubbles (fixed spec FR-16.4)")
            ChatBubble(
                text = "Hi sayang!",
                timestamp = "09:41",
                side = BubbleSide.INCOMING,
            )
            ChatBubble(
                text = "Morning! Chat bubble system online",
                timestamp = "09:42",
                side = BubbleSide.OUTGOING,
                deliveryState = MessageDeliveryState.READ,
            )
            ChatBubble(
                text = "Edited message",
                timestamp = "09:43",
                side = BubbleSide.OUTGOING,
                edited = true,
                deliveryState = MessageDeliveryState.DELIVERED,
            )
            ChatBubble(
                text = "Removed",
                timestamp = "09:44",
                side = BubbleSide.INCOMING,
                deleted = true,
            )

            SectionHeader("Scrapbook & animals")
            ScrapbookCard {
                Text("Scrapbook card with washi tape", style = NazeVerType.title)
                Text("Soft paper, rounded corners, cute but tidy.", style = NazeVerType.bodySecondary)
            }
            AnimalTemplate(kind = AnimalKind.CAT, title = "Cat note template")
            StickerContainer(kind = AnimalKind.BIRD, label = "Bird sticker")
            CuteDivider()

            SectionHeader("States")
            LoadingState(label = "Syncing scrapbook...")
            EmptyState(kind = AnimalKind.RABBIT, label = "Nothing here yet")
            ErrorState(label = "Could not load — will retry", retryLabel = "Try again")
        }
    }
}
