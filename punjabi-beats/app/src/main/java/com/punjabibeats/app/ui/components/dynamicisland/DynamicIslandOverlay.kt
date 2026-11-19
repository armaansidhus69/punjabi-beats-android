package com.punjabibeats.app.ui.components.dynamicisland

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.ui.components.ArtworkThumb
import com.punjabibeats.app.ui.theme.LocalPBTheme
import kotlin.math.roundToInt

@Composable
fun DynamicIslandOverlay(
    controller: MusicPlayerController,
    onOpenPlayer: () -> Unit,
    onOpenCarMode: () -> Unit,
    modifier: Modifier = Modifier,
    initialMode: DynamicIslandMode = DynamicIslandMode.COMPACT_PILL,
) {
    val currentSong by controller.currentSong.collectAsStateWithLifecycle()
    val isPlaying by controller.isPlaying.collectAsStateWithLifecycle()
    val positionMs by controller.positionMs.collectAsStateWithLifecycle()
    val durationMs by controller.durationMs.collectAsStateWithLifecycle()

    if (currentSong == null) return

    var mode by remember { mutableStateOf(initialMode) }
    var selectedDspBoost by remember { mutableStateOf("808 Gaddi Slam") }

    // Re-surface pill if a new track starts playing
    LaunchedEffect(currentSong?.id) {
        if (mode == DynamicIslandMode.DISMISSED && isPlaying) {
            mode = DynamicIslandMode.COMPACT_PILL
        }
    }

    if (mode == DynamicIslandMode.DISMISSED) return

    val theme = LocalPBTheme.current
    val accentPurple = Color(0xFFA78BFA)
    val amberGold = Color(0xFFFFB95F)
    val neonTeal = Color(0xFF4EDEA3)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .zIndex(99f),
        contentAlignment = Alignment.TopCenter,
    ) {
        when (mode) {
            DynamicIslandMode.COMPACT_PILL -> {
                CompactIslandPill(
                    title = currentSong?.title ?: "295",
                    artist = currentSong?.artist ?: "Sidhu Moose Wala",
                    albumArtUrl = currentSong?.artworkUrl,
                    isPlaying = isPlaying,
                    accentColor = accentPurple,
                    onClick = { mode = DynamicIslandMode.EXPANDED_CARD },
                    onDismiss = { mode = DynamicIslandMode.DISMISSED },
                    onTogglePlay = { controller.playPause() },
                )
            }
            DynamicIslandMode.EXPANDED_CARD -> {
                ExpandedIslandCard(
                    title = currentSong?.title ?: "295",
                    artist = currentSong?.artist ?: "Sidhu Moose Wala",
                    albumArtUrl = currentSong?.artworkUrl,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    selectedDspBoost = selectedDspBoost,
                    accentColor = accentPurple,
                    amberColor = amberGold,
                    tealColor = neonTeal,
                    onDspBoostSelect = { selectedDspBoost = it },
                    onCollapse = { mode = DynamicIslandMode.COMPACT_PILL },
                    onDismiss = { mode = DynamicIslandMode.DISMISSED },
                    onTogglePlay = { controller.playPause() },
                    onSkipNext = { controller.next() },
                    onSkipPrevious = { controller.previous() },
                    onSeek = { controller.seekTo(it) },
                    onOpenPlayer = onOpenPlayer,
                    onOpenCarMode = onOpenCarMode,
                )
            }
            DynamicIslandMode.DISMISSED -> Unit
        }
    }
}

@Composable
private fun CompactIslandPill(
    title: String,
    artist: String,
    albumArtUrl: String?,
    isPlaying: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onTogglePlay: () -> Unit,
) {
    var offsetY by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = Modifier
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetY < -40f) {
                            onDismiss()
                        }
                        offsetY = 0f
                    },
                    onDragCancel = { offsetY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetY = (offsetY + dragAmount.y).coerceAtMost(0f)
                    },
                )
            }
            .clip(RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(accentColor.copy(alpha = 0.45f), Color(0xFFFFB95F).copy(alpha = 0.3f)),
                ),
                shape = RoundedCornerShape(32.dp),
            )
            .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = accentColor),
        color = Color(0xFF131418).copy(alpha = 0.96f),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .height(38.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Spinning / Rounded Artwork Thumbnail
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
            ) {
                ArtworkThumb(url = albumArtUrl, size = 32.dp, cornerRadius = 16.dp)
            }

            // Track Title & Artist
            Column(
                modifier = Modifier.width(130.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    color = Color(0xFFE3E2E6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    color = Color(0xFFCAC4D4),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Animated Equalizer waveform
            IslandMiniEqualizer(isPlaying = isPlaying, tint = accentColor)

            // Play / Pause mini button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ExpandedIslandCard(
    title: String,
    artist: String,
    albumArtUrl: String?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    selectedDspBoost: String,
    accentColor: Color,
    amberColor: Color,
    tealColor: Color,
    onDspBoostSelect: (String) -> Unit,
    onCollapse: () -> Unit,
    onDismiss: () -> Unit,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenCarMode: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(accentColor.copy(alpha = 0.5f), Color(0xFF292A2D).copy(alpha = 0.3f)),
                ),
                shape = RoundedCornerShape(32.dp),
            )
            .shadow(24.dp, RoundedCornerShape(32.dp), spotColor = accentColor),
        color = Color(0xFF16171B).copy(alpha = 0.98f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Row: Destination / Car badge & Collapse handle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Live Stream Activity Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(Color(0xFF292A2D), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    IslandMiniEqualizer(isPlaying = isPlaying, tint = tealColor)
                    Text(
                        text = "LDAC 990k",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Car destination badge - clickable to launch Car Mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(amberColor.copy(alpha = 0.15f))
                        .clickable(onClick = onOpenCarMode)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DirectionsCar,
                        contentDescription = "Car Mode",
                        tint = amberColor,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "PB 31 Thar 4x4",
                        color = amberColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Collapse down arrow
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = Color(0xFFCAC4D4),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body Row: Album Artwork + Title + dancing wave
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPlayer),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                ) {
                    ArtworkThumb(url = albumArtUrl, size = 64.dp, cornerRadius = 16.dp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = title,
                            color = Color(0xFFE3E2E6),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = "Verified",
                            tint = accentColor,
                            modifier = Modifier.size(14.dp),
                        )
                    }

                    Text(
                        text = artist,
                        color = Color(0xFFCAC4D4),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Moosetape • Gaddi DSP Active",
                        color = amberColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dancing Waveform Visualizer
            DancingWaveformRow(isPlaying = isPlaying, accentColor = accentColor, secondaryColor = amberColor)

            Spacer(modifier = Modifier.height(6.dp))

            // Progress Slider
            val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
            Slider(
                value = progress,
                onValueChange = { frac ->
                    onSeek((frac * durationMs).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = Color(0xFF292A2D),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
            )

            // Time stamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatTime(positionMs),
                    color = Color(0xFFCAC4D4),
                    fontSize = 10.sp,
                )
                Text(
                    text = "MOOSETAPE REMASTER",
                    color = Color(0xFF948E9D),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "-" + formatTime((durationMs - positionMs).coerceAtLeast(0L)),
                    color = Color(0xFFCAC4D4),
                    fontSize = 10.sp,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tactile Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { /* Shuffle toggle */ }) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = accentColor,
                    )
                }

                IconButton(onClick = onSkipPrevious) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color(0xFFE3E2E6),
                        modifier = Modifier.size(28.dp),
                    )
                }

                // Hero Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .clickable(onClick = onTogglePlay)
                        .shadow(16.dp, CircleShape, spotColor = accentColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }

                IconButton(onClick = onSkipNext) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Color(0xFFE3E2E6),
                        modifier = Modifier.size(28.dp),
                    )
                }

                IconButton(onClick = { /* Favorite toggle */ }) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = "Favorite",
                        tint = amberColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // One-Tap DSP Quick Boosters Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val boosts = listOf("808 Gaddi Slam", "Live Dholak", "Tumbi Crisp", "Vocal Focus")
                boosts.forEach { boost ->
                    val isSelected = boost == selectedDspBoost
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) amberColor.copy(alpha = 0.25f) else Color(0xFF292A2D),
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) amberColor else Color.Transparent,
                                shape = RoundedCornerShape(20.dp),
                            )
                            .clickable { onDspBoostSelect(boost) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = boost,
                            color = if (isSelected) amberColor else Color(0xFFCAC4D4),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IslandMiniEqualizer(isPlaying: Boolean, tint: Color) {
    val transition = rememberInfiniteTransition(label = "islandWave")
    val h1 by transition.animateFloat(
        initialValue = 0.2f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1",
    )
    val h2 by transition.animateFloat(
        initialValue = 0.8f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2",
    )
    val h3 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3",
    )
    val h4 by transition.animateFloat(
        initialValue = 0.9f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
        label = "h4",
    )

    val factors = listOf(h1, h2, h3, h4)
    Row(
        modifier = Modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        factors.forEach { factor ->
            val height = if (isPlaying) (14.dp * factor).coerceAtLeast(3.dp) else 3.dp
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(height)
                    .clip(CircleShape)
                    .background(tint),
            )
        }
    }
}

@Composable
private fun DancingWaveformRow(
    isPlaying: Boolean,
    accentColor: Color,
    secondaryColor: Color,
) {
    val transition = rememberInfiniteTransition(label = "dancingWave")
    val heights = (0 until 14).map { index ->
        val duration = 300 + (index * 45) % 350
        val anim by transition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                tween(duration, easing = FastOutSlowInEasing),
                RepeatMode.Reverse,
            ),
            label = "bar$index",
        )
        anim
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0D0E11))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEachIndexed { i, factor ->
            val color = if (i % 3 == 0) secondaryColor else accentColor
            val h = if (isPlaying) (20.dp * factor).coerceAtLeast(4.dp) else 4.dp
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
