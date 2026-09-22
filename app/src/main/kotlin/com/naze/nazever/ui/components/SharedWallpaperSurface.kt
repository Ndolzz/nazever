package com.naze.nazever.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.naze.nazever.ui.theme.NazeVerColors
import com.naze.nazever.ui.theme.NazeVerDimens

/**
 * Shared wallpaper surface (FR-16.2, design §11).
 *
 * Visual abstraction ONLY — no upload/sync (out of scope for TASK-004).
 * Layers (bottom → top): fallback color → wallpaper image (optional) →
 * scrim overlay → content. Layout never depends on the wallpaper;
 * readability is kept via the overlay scrim (design §11).
 */
@Composable
fun SharedWallpaperSurface(
    modifier: Modifier = Modifier,
    wallpaperPainter: Painter? = null,
    fallbackColor: Color = NazeVerColors.background,
    overlayColor: Color = NazeVerColors.overlay,
    wallpaperContentDescription: String? = "Shared wallpaper",
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(fallbackColor)
            .semantics { wallpaperContentDescription?.let { contentDescription = it } },
    ) {
        if (wallpaperPainter != null) {
            Image(
                painter = wallpaperPainter,
                contentDescription = null, // described at container level
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Fallback soft pattern (FR-16.2 fallback background).
            DecorativePattern(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = NazeVerDimens.spaceXL),
            )
        }
        // Scrim keeps text readable over any wallpaper.
        Box(modifier = Modifier.fillMaxSize().background(overlayColor))
        Box(modifier = Modifier.fillMaxSize()) { content() }
    }
}
