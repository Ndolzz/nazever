package com.naze.nazever.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naze.nazever.ui.theme.NazeVerColors
import com.naze.nazever.ui.theme.NazeVerDimens
import com.naze.nazever.ui.theme.NazeVerShapes
import com.naze.nazever.ui.theme.NazeVerType

/**
 * Scrapbook UI kit (FR-16.5). Light, readable, cute — decoration never
 * crowds the screen (FR-16.5: readability first).
 */

/** Card on soft "paper" with washi-tape accent — the core scrapbook surface. */
@Composable
fun ScrapbookCard(
    modifier: Modifier = Modifier,
    showTape: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NazeVerColors.scrapbookPaper, NazeVerShapes.card)
                .border(1.dp, NazeVerColors.border, NazeVerShapes.card)
                .padding(NazeVerDimens.spaceL),
        ) { content() }
        if (showTape) {
            // Washi tape strip pinned at the top center.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(64.dp)
                    .height(12.dp)
                    .background(NazeVerColors.scrapbookTape, NazeVerShapes.sticker),
            )
        }
    }
}

/** Screen container with NazeVer soft background. */
@Composable
fun ScrapbookContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NazeVerColors.background)
            .padding(horizontal = NazeVerDimens.screenPaddingH, vertical = NazeVerDimens.spaceL),
        verticalArrangement = Arrangement.spacedBy(NazeVerDimens.spaceL),
    ) { content() }
}

/** Animal mascot decoration. Delegates entirely to [AnimalSystem] — no hardcoded assets. */
@Composable
fun AnimalDecoration(kind: AnimalKind, modifier: Modifier = Modifier) {
    val identity = AnimalSystem.identity(kind)
    Box(
        modifier = modifier
            .size(56.dp)
            .background(NazeVerColors.primaryContainer, CircleShape)
            .semantics { contentDescription = identity.kind.label + " mascot" },
        contentAlignment = Alignment.Center,
    ) {
        Text(identity.kind.emoji, style = NazeVerType.heading)
    }
}

/** Repeating soft decorative dots — subtle background garnish only. */
@Composable
fun DecorativePattern(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(5) { i ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        if (i % 2 == 0) NazeVerColors.primary.copy(alpha = 0.35f)
                        else NazeVerColors.secondary.copy(alpha = 0.35f),
                        CircleShape,
                    ),
            )
        }
    }
}

/** Sticker slot: small clipped decorative element (also used by note templates). */
@Composable
fun StickerContainer(
    kind: AnimalKind,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Column(
        modifier = modifier.background(NazeVerColors.surfaceVariant, NazeVerShapes.sticker).padding(NazeVerDimens.spaceM),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimalDecoration(kind)
        label?.let {
            Spacer(Modifier.height(NazeVerDimens.spaceS))
            Text(it, style = NazeVerType.label, color = NazeVerColors.textSecondary)
        }
    }
}

/** Named wrapper for note-template placeholders (FR-12.6 animal templates). */
@Composable
fun AnimalTemplate(kind: AnimalKind, title: String, modifier: Modifier = Modifier) {
    ScrapbookCard(modifier = modifier, showTape = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimalDecoration(kind)
            Spacer(Modifier.width(NazeVerDimens.spaceM))
            Text(title, style = NazeVerType.title, color = NazeVerColors.textPrimary)
        }
    }
}

/** Divider that looks like a dotted paper edge. */
@Composable
fun CuteDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(12) {
            Box(
                modifier = Modifier
                    .size(2.dp)
                    .background(NazeVerColors.divider, CircleShape),
            )
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = NazeVerType.label,
        color = NazeVerColors.textSecondary,
        modifier = modifier.semantics { contentDescription = text + " section" },
    )
}

@Composable
fun LoadingState(label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().heightIn(min = NazeVerDimens.minTouchTarget).semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NazeVerDimens.spaceS),
    ) {
        CircularProgressIndicator(color = NazeVerColors.primary, strokeWidth = 3.dp)
        Text(label, style = NazeVerType.bodySecondary, color = NazeVerColors.textSecondary)
    }
}

@Composable
fun EmptyState(kind: AnimalKind = AnimalKind.RABBIT, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().heightIn(min = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NazeVerDimens.spaceS),
    ) {
        AnimalDecoration(kind)
        Text(label, style = NazeVerType.bodySecondary, color = NazeVerColors.textSecondary)
    }
}

@Composable
fun ErrorState(label: String, modifier: Modifier = Modifier, retryLabel: String? = null) {
    Column(
        modifier = modifier.fillMaxWidth().heightIn(min = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NazeVerDimens.spaceS),
    ) {
        Text("🩹", style = NazeVerType.heading)
        Text(label, style = NazeVerType.bodySecondary, color = NazeVerColors.error)
        retryLabel?.let {
            TextButton(onClick = {}) { Text(it, style = NazeVerType.button, color = NazeVerColors.primary) }
        }
    }
}
