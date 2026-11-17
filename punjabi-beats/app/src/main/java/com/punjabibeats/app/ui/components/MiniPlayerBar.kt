package com.punjabibeats.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.ui.theme.LocalPBTheme

@Composable
private fun MiniWaveformEqualizer(isPlaying: Boolean, tint: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "miniWave")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1",
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2",
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3",
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "h4",
    )
    val h5 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "h5",
    )
    val heights = listOf(h1, h2, h3, h4, h5, h2, h1, h3, h5, h4, h2, h3, h1, h4)
    Row(
        modifier = modifier.height(10.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEach { factor ->
            val actualH = if (isPlaying) (10.dp * factor).coerceAtLeast(2.dp) else 2.dp
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(actualH)
                    .clip(RoundedCornerShape(1.dp))
                    .background(tint),
            )
        }
    }
}

/**
 * Floating mini player matching the showcase screenshot:
 * Rounded pill (22.dp) with neon theme border, artwork thumbnail,
 * song title, artist, live audio waveform, heart toggle, play/pause and next controls.
 */
@Composable
fun MiniPlayerBar(
    controller: MusicPlayerController,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalPBTheme.current
    val currentSong by controller.currentSong.collectAsStateWithLifecycle()
    val isPlaying by controller.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by controller.isBuffering.collectAsStateWithLifecycle()
    val positionMs by controller.positionMs.collectAsStateWithLifecycle()
    val durationMs by controller.durationMs.collectAsStateWithLifecycle()
    var isLiked by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = currentSong != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        val song = currentSong ?: return@AnimatedVisibility
        val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

        val pillShape = RoundedCornerShape(22.dp)
        val borderGlow = theme.accent

        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clip(pillShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1B1224), Color(0xFF130E1B))
                    )
                )
                .border(1.5.dp, borderGlow, pillShape)
                .clickable(onClick = onOpenPlayer),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            ) {
                ArtworkThumb(url = song.artworkUrl, size = 48.dp, cornerRadius = 14.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 6.dp),
                ) {
                    Text(
                        song.title,
                        color = theme.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        song.artist,
                        color = theme.muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    MiniWaveformEqualizer(
                        isPlaying = isPlaying,
                        tint = theme.accent.copy(alpha = 0.85f),
                    )
                }
                IconButton(
                    onClick = { isLiked = !isLiked },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) Color(0xFFFF2E93) else theme.muted,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = { controller.playPause() },
                    modifier = Modifier.size(40.dp),
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = theme.accent,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = theme.accent,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                IconButton(
                    onClick = { controller.next() },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = theme.text,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(2.dp)
                        .background(theme.accent)
                )
            }
        }
    }
}
