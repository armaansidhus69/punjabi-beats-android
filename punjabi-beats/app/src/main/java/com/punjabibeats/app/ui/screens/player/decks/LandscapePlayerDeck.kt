package com.punjabibeats.app.ui.screens.player.decks

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.playback.MusicPlayerController
import androidx.compose.runtime.CompositionLocalProvider
import com.punjabibeats.app.ui.components.glass.LiquidGlassDefaults
import com.punjabibeats.app.ui.theme.LocalPBTheme
import com.punjabibeats.app.ui.theme.rememberArtworkPalette

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

/**
 * Two-pane landscape and tablet player deck inspired by BitChord Section 7.
 * Left Pane: Vinyl turntable, song details, progress seekbar, and transport controls.
 * Right Pane: Synchronized scrolling lyrics with line tap-to-seek, switchable with queue.
 */
@Composable
fun LandscapePlayerDeck(
    controller: MusicPlayerController,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalPBTheme.current
    val currentSong by controller.currentSong.collectAsStateWithLifecycle()
    val isPlaying by controller.isPlaying.collectAsStateWithLifecycle()
    val positionMs by controller.positionMs.collectAsStateWithLifecycle()
    val durationMs by controller.durationMs.collectAsStateWithLifecycle()
    val shuffleEnabled by controller.shuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by controller.repeatMode.collectAsStateWithLifecycle()
    val queue by controller.queue.collectAsStateWithLifecycle()

    // Dynamic Artwork Palette (BitChord Section 7 Architecture)
    val artworkPalette = rememberArtworkPalette(currentSong?.artworkUrl)
    val dynamicTheme = remember(theme, artworkPalette) {
        theme.copy(
            accent = artworkPalette.accent,
            surface = artworkPalette.wash,
            card = artworkPalette.elevated,
        )
    }

    var rightPaneMode by remember { mutableStateOf(0) } // 0 = Lyrics, 1 = Queue

    val infiniteTransition = rememberInfiniteTransition(label = "vinylSpin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spinAngle",
    )

    CompositionLocalProvider(LocalPBTheme provides dynamicTheme) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            artworkPalette.background,
                            artworkPalette.wash.copy(alpha = 0.85f),
                            artworkPalette.elevated.copy(alpha = 0.70f),
                        )
                    )
                )
                .padding(16.dp),
        ) {
        // Left Column: Vinyl Turntable & Controls Deck (46% width)
        Column(
            modifier = Modifier
                .weight(0.46f)
                .fillMaxHeight()
                .padding(end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Header bar: Close button & Mode title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close player",
                        tint = theme.text,
                    )
                }
                Text(
                    text = "Punjabi Beats Deck",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.accent,
                    letterSpacing = 1.sp,
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Turntable Vinyl Centerpiece
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F0C16))
                    .border(4.dp, Color(0xFF241C33), CircleShape)
                    .border(8.dp, Color(0xFF181324), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                // Vinyl Groove Rings
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                )

                // Rotating Artwork Center
                AsyncImage(
                    model = currentSong?.artworkUrl,
                    contentDescription = currentSong?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .rotate(if (isPlaying) spinAngle else 0f)
                        .border(2.dp, theme.accent, CircleShape),
                )

                // Spindle hole
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(theme.background)
                        .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                )
            }

            // Track Title & Artist
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = currentSong?.title ?: "No Song Playing",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = currentSong?.artist ?: "Unknown Artist",
                    fontSize = 13.sp,
                    color = theme.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }

            // Seeker / Scrubber
            Column(modifier = Modifier.fillMaxWidth()) {
                val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                Slider(
                    value = progress,
                    onValueChange = { frac ->
                        controller.seekTo((frac * durationMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = theme.accent,
                        activeTrackColor = theme.accent,
                        inactiveTrackColor = theme.muted.copy(alpha = 0.25f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatDuration(positionMs),
                        fontSize = 11.sp,
                        color = theme.muted,
                    )
                    Text(
                        text = "-${formatDuration(maxOf(0L, durationMs - positionMs))}",
                        fontSize = 11.sp,
                        color = theme.muted,
                    )
                }
            }

            // Transport Controls: Shuffle, Previous, Play/Pause, Next, Repeat
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(onClick = { controller.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) theme.accent else theme.muted.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = { controller.previous() }) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = theme.text,
                        modifier = Modifier.size(28.dp),
                    )
                }
                FilledIconButton(
                    onClick = { controller.playPause() },
                    modifier = Modifier.size(52.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = theme.accent,
                        contentColor = theme.background,
                    ),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(30.dp),
                    )
                }
                IconButton(onClick = { controller.next() }) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = theme.text,
                        modifier = Modifier.size(28.dp),
                    )
                }
                IconButton(onClick = { controller.cycleRepeatMode() }) {
                    Icon(
                        imageVector = if (repeatMode == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != 0) theme.accent else theme.muted.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Right Column: Synchronized Lyrics Deck & Up-Next Queue (54% width)
        Column(
            modifier = Modifier
                .weight(0.54f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(theme.surface.copy(alpha = 0.60f))
                .border(LiquidGlassDefaults.EdgeWidth, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(16.dp),
        ) {
            // Right Pane Switcher: Lyrics vs Queue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.card)
                        .padding(3.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (rightPaneMode == 0) theme.accent else Color.Transparent)
                            .clickable { rightPaneMode = 0 }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Lyrics,
                                contentDescription = null,
                                tint = if (rightPaneMode == 0) theme.background else theme.muted,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lyrics",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (rightPaneMode == 0) theme.background else theme.muted,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (rightPaneMode == 1) theme.accent else Color.Transparent)
                            .clickable { rightPaneMode = 1 }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = null,
                                tint = if (rightPaneMode == 1) theme.background else theme.muted,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Queue (${queue.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (rightPaneMode == 1) theme.background else theme.muted,
                            )
                        }
                    }
                }
            }

            AnimatedContent(targetState = rightPaneMode, label = "deckRightPane") { mode ->
                if (mode == 0) {
                    // Synced Lyrics View
                    val sampleLyrics = remember(currentSong?.title) {
                        listOf(
                            0L to "Diljit Dosanjh & Sia in the beat...",
                            5000L to "Desi vibes rolling through the night",
                            12000L to "Kamm saare set ne, koi na fikar",
                            18000L to "Gaddi meri chaldi ae top gear 'te",
                            25000L to "Yaaran naal mehfil sajayi hoyi ae",
                            32000L to "Bass pura loud ae, speaker ne hil rahe",
                            40000L to "Punjabi beats go all around the world",
                            50000L to "Sing along now, let the music take control",
                        )
                    }

                    val listState = rememberLazyListState()
                    val activeIndex = sampleLyrics.indexOfLast { it.first <= positionMs }.coerceAtLeast(0)

                    LaunchedEffect(activeIndex) {
                        if (activeIndex >= 0) {
                            listState.animateScrollToItem(activeIndex)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        itemsIndexed(sampleLyrics) { idx, (timeMs, line) ->
                            val isCurrent = idx == activeIndex
                            Text(
                                text = line,
                                fontSize = if (isCurrent) 20.sp else 16.sp,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isCurrent) theme.accent else theme.muted.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { controller.seekTo(timeMs) }
                                    .padding(vertical = 4.dp),
                            )
                        }
                    }
                } else {
                    // Up Next Queue View
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(queue) { idx, song ->
                            val isThisPlaying = song.id == currentSong?.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isThisPlaying) theme.card else Color.Transparent)
                                    .clickable { controller.seekToIndex(idx) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AsyncImage(
                                    model = song.artworkUrl,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        fontSize = 13.sp,
                                        fontWeight = if (isThisPlaying) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isThisPlaying) theme.accent else theme.text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = song.artist,
                                        fontSize = 11.sp,
                                        color = theme.muted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}
