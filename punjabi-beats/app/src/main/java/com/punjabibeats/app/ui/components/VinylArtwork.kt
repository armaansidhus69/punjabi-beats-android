package com.punjabibeats.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.punjabibeats.app.ui.theme.BgBase
import com.punjabibeats.app.ui.theme.LocalPBTheme
import com.punjabibeats.app.ui.theme.OutlineDim
import com.punjabibeats.app.ui.theme.SurfaceElevated
import com.punjabibeats.app.ui.theme.SurfaceHigh

/**
 * Album art rendered as a spinning vinyl disc. Rotates continuously while
 * playing, freezes in place when paused.
 */
@Composable
fun VinylArtwork(
    artworkUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
) {
    val theme = LocalPBTheme.current
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                val normalized = rotation.value % 360f
                rotation.snapTo(normalized)
                rotation.animateTo(
                    targetValue = normalized + 360f,
                    animationSpec = tween(durationMillis = 20_000, easing = LinearEasing),
                )
            }
        }
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation.value }
                .clip(CircleShape)
                .background(Brush.sweepGradient(listOf(SurfaceElevated, SurfaceHigh, SurfaceElevated)))
                .border(2.dp, theme.accent.copy(alpha = 0.35f), CircleShape),
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .clip(CircleShape),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)))),
            )
        }
        Box(
            modifier = Modifier
                .size(size * 0.14f)
                .clip(CircleShape)
                .background(BgBase)
                .border(1.dp, OutlineDim, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(theme.accent),
            )
        }
    }
}
