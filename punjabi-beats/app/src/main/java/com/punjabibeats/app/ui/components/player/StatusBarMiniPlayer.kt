package com.punjabibeats.app.ui.components.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punjabibeats.app.core.util.formatDuration
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.ui.components.ArtworkThumb
import com.punjabibeats.app.ui.theme.LocalPBTheme

/**
 * Status-Bar Docked Mini Player.
 * Replaces the stuck Dynamic Island with a sleek, non-intrusive status-bar pill (26dp high)
 * positioned cleanly between status indicators. Tapping expands it into a floating pop-up card.
 */
@Composable
fun StatusBarMiniPlayer(
    controller: MusicPlayerController,
    onOpenPlayer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalPBTheme.current
    val song by controller.currentSong.collectAsStateWithLifecycle()
    val isPlaying by controller.isPlaying.collectAsStateWithLifecycle()
    val positionMs by controller.positionMs.collectAsStateWithLifecycle()
    val durationMs = song?.durationMs?.takeIf { it > 0 } ?: 180_000L

    var isExpanded by remember { mutableStateOf(false) }

    if (song == null) return

    val currentSong = song ?: return

    val infinite = rememberInfiniteTransition(label = "statusBarPulse")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotate",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .zIndex(99f),
        contentAlignment = Alignment.TopCenter,
    ) {
        // 1. Compact Status-Bar Docked Pill (Non-intrusive: 26dp tall, centered between status icons)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 2.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xFF0F0F14).copy(alpha = 0.94f))
                .border(
                    width = 0.8.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            theme.accent.copy(alpha = 0.55f),
                            Color(0xFF00F5FF).copy(alpha = 0.35f),
                            theme.accent.copy(alpha = 0.55f),
                        )
                    ),
                    shape = RoundedCornerShape(13.dp),
                )
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            // Spinning mini album artwork
            ArtworkThumb(
                url = currentSong.artworkUrl,
                size = 18.dp,
                cornerRadius = 9.dp,
                modifier = Modifier.rotate(if (isPlaying) rotation else 0f),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = currentSong.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 130.dp),
            )
            Spacer(Modifier.width(6.dp))
            if (isPlaying) {
                StatusBarWaveform(color = theme.accent)
            } else {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = theme.accent,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { controller.playPause() },
                )
            }
        }

        // 2. Expandable Pop-Up Player View (Floating card overlay)
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(200)) + scaleIn(spring(dampingRatio = 0.8f, stiffness = 400f)),
            exit = fadeOut(tween(150)) + scaleOut(tween(150)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { isExpanded = false },
                contentAlignment = Alignment.TopCenter,
            ) {
                Surface(
                    color = Color(0xFF14141C).copy(alpha = 0.96f),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                theme.accent.copy(alpha = 0.7f),
                                Color(0xFF00F5FF).copy(alpha = 0.3f),
                                Color.Transparent,
                            )
                        ),
                    ),
                    modifier = Modifier
                        .padding(top = 16.dp, start = 18.dp, end = 18.dp)
                        .fillMaxWidth()
                        .shadow(24.dp, RoundedCornerShape(24.dp))
                        .clickable(enabled = false) {}
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount > 20f) isExpanded = false
                            }
                        },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                    ) {
                        // Header row with drag handle and close
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isPlaying) Color(0xFF00FF66) else Color.Gray),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isPlaying) "NOW PLAYING" else "PAUSED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.accent,
                                    letterSpacing = 1.sp,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onOpenEqualizer,
                                    modifier = Modifier.size(30.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.Equalizer,
                                        contentDescription = "Equalizer",
                                        tint = theme.accent,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        isExpanded = false
                                        onOpenPlayer()
                                    },
                                    modifier = Modifier.size(30.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.OpenInFull,
                                        contentDescription = "Full Player",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                IconButton(
                                    onClick = { isExpanded = false },
                                    modifier = Modifier.size(30.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Close",
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Artwork & Track Details Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            ArtworkThumb(
                                url = currentSong.artworkUrl,
                                size = 62.dp,
                                cornerRadius = 14.dp,
                                modifier = Modifier
                                    .border(1.dp, theme.accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentSong.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = currentSong.artist,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FormatBadge("24-BIT LOSSLESS", theme.accent)
                                    FormatBadge("DOLBY ATMOS", Color(0xFF00F5FF))
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Progress Scrubber
                        var scrubPosition by remember { mutableFloatStateOf(-1f) }
                        val displayFraction = if (scrubPosition >= 0f) scrubPosition else {
                            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        }

                        Slider(
                            value = displayFraction,
                            onValueChange = { scrubPosition = it },
                            onValueChangeFinished = {
                                if (scrubPosition >= 0f) {
                                    controller.seekTo((scrubPosition * durationMs).toLong())
                                    scrubPosition = -1f
                                }
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = theme.accent,
                                activeTrackColor = theme.accent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = formatDuration(positionMs),
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                            )
                            Text(
                                text = formatDuration(durationMs),
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Transport Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { controller.previous() }) {
                                Icon(
                                    Icons.Rounded.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(theme.accent)
                                    .clickable { controller.playPause() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = theme.background,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            IconButton(onClick = { controller.next() }) {
                                Icon(
                                    Icons.Rounded.SkipNext,
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.18f))
            .border(0.6.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(text = text, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun StatusBarWaveform(color: Color) {
    val transition = rememberInfiniteTransition(label = "sbWaveform")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Row(
        modifier = Modifier.size(width = 12.dp, height = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        val heights = listOf(
            0.4f + 0.6f * kotlin.math.abs(kotlin.math.sin(phase)),
            0.4f + 0.6f * kotlin.math.abs(kotlin.math.sin(phase + 1.2f)),
            0.4f + 0.6f * kotlin.math.abs(kotlin.math.sin(phase + 2.4f)),
        )
        heights.forEach { fraction ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color),
            )
        }
    }
}
