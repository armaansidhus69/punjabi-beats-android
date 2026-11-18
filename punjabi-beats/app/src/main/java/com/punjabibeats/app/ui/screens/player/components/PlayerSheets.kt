package com.punjabibeats.app.ui.screens.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punjabibeats.app.core.util.formatDuration
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.playback.SleepMode
import com.punjabibeats.app.ui.components.dragContainer
import com.punjabibeats.app.ui.components.draggableItem
import com.punjabibeats.app.ui.components.rememberDragDropState

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.playback.AudioFormatInfo
import com.punjabibeats.app.ui.components.ArtworkThumb
import com.punjabibeats.app.ui.theme.LocalPBTheme

/** Sleep timer options: 15/30/45/60 minutes or end of the current track. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    sleepMode: SleepMode?,
    remainingMs: Long? = null,
    onStart: (SleepMode) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val theme = LocalPBTheme.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("Sleep Timer", style = MaterialTheme.typography.titleLarge)
            val statusText = when {
                sleepMode == null -> "Playback keeps playing until you stop it"
                sleepMode is SleepMode.EndOfTrack -> "Music stops at the end of this track"
                remainingMs != null && remainingMs > 0L -> {
                    val totalSecs = remainingMs / 1000L
                    val mins = totalSecs / 60L
                    val secs = totalSecs % 60L
                    "Music stops in %02d:%02d".format(mins, secs)
                }
                sleepMode is SleepMode.Minutes -> "Music stops in ${sleepMode.minutes} minutes"
                else -> "Playback keeps playing until you stop it"
            }
            Text(
                statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = theme.muted,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(15, 30, 45, 60).forEach { minutes ->
                    val active = (sleepMode as? SleepMode.Minutes)?.minutes == minutes
                    TimerChip(
                        label = "$minutes min",
                        active = active,
                        onClick = { onStart(SleepMode.Minutes(minutes)) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TimerChip(
                    label = "End of track",
                    active = sleepMode is SleepMode.EndOfTrack,
                    onClick = { onStart(SleepMode.EndOfTrack) },
                )
                TimerChip(
                    label = "Cancel timer",
                    active = false,
                    onClick = { onCancel(); onDismiss() },
                )
            }
        }
    }
}

@Composable
private fun TimerChip(label: String, active: Boolean, onClick: () -> Unit) {
    val theme = LocalPBTheme.current
    androidx.compose.material3.AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
            containerColor = if (active) theme.accent else MaterialTheme.colorScheme.surfaceVariant,
            labelColor = if (active) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun QueueDancingWaveform(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "queueDancingWaveform")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "waveformPhase",
    )
    Row(
        modifier = modifier.size(width = 16.dp, height = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        val bar1 = (0.35f + 0.65f * kotlin.math.abs(kotlin.math.sin(phase))).coerceIn(0.2f, 1f)
        val bar2 = (0.35f + 0.65f * kotlin.math.abs(kotlin.math.sin(phase + 1.3f))).coerceIn(0.2f, 1f)
        val bar3 = (0.35f + 0.65f * kotlin.math.abs(kotlin.math.sin(phase + 2.5f))).coerceIn(0.2f, 1f)
        listOf(bar1, bar2, bar3).forEach { heightFraction ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightFraction)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color),
            )
        }
    }
}

/** Queue management drawer with long-press drag-and-drop reordering. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueDrawer(
    controller: MusicPlayerController,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val theme = LocalPBTheme.current
    val queue by controller.queue.collectAsStateWithLifecycle()
    val currentIndex by controller.currentIndex.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val dragState = rememberDragDropState(listState = listState, onMove = onMove)

    LaunchedEffect(Unit) {
        if (currentIndex > 0 && queue.isNotEmpty()) {
            listState.scrollToItem((currentIndex - 1).coerceAtLeast(0))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = theme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                "Up Next — ${queue.size} songs",
                style = MaterialTheme.typography.titleLarge,
                color = theme.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 280.dp, max = 500.dp)
                    .dragContainer(dragState),
            ) {
                items(queue.size) { index ->
                    val song = queue[index]
                    val isPlayingThis = index == currentIndex
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isPlayingThis) {
                                    Brush.horizontalGradient(
                                        listOf(
                                            theme.accent.copy(alpha = 0.22f),
                                            theme.accent.copy(alpha = 0.08f),
                                            Color.Transparent,
                                        )
                                    )
                                } else {
                                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .border(
                                width = if (isPlayingThis) 1.2.dp else 0.dp,
                                brush = if (isPlayingThis) {
                                    Brush.horizontalGradient(
                                        listOf(
                                            theme.accent,
                                            theme.accent.copy(alpha = 0.3f),
                                        )
                                    )
                                } else {
                                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                },
                                shape = RoundedCornerShape(14.dp),
                            )
                            .clickable { controller.seekToIndex(index) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        ArtworkThumb(
                            url = song.artworkUrl,
                            size = 42.dp,
                            cornerRadius = 10.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isPlayingThis) {
                                    QueueDancingWaveform(
                                        color = theme.accent,
                                        modifier = Modifier.padding(end = 6.dp),
                                    )
                                }
                                Text(
                                    song.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isPlayingThis) theme.accent else theme.text,
                                    fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (isPlayingThis) {
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(theme.accent.copy(alpha = 0.25f))
                                            .border(0.8.dp, theme.accent, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 5.dp, vertical = 1.dp),
                                    ) {
                                        Text(
                                            "PLAYING",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = theme.accent,
                                            letterSpacing = 0.5.sp,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            formatDuration(song.durationMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = theme.muted,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .draggableItem(dragState, index),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.DragHandle,
                                contentDescription = "Reorder (hold & drag)",
                                tint = theme.muted.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(
                            onClick = { onRemove(index) },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = theme.muted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 🧬 Song DNA Card: Deep dive into the story, acoustic metrics, movie/album
 * background, and verified audio quality of the playing track.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDnaSheet(
    song: Song?,
    audioFormat: AudioFormatInfo,
    onDismiss: () -> Unit,
) {
    val theme = LocalPBTheme.current
    if (song == null) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = theme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "🧬 Song DNA & Info",
                    style = MaterialTheme.typography.titleLarge,
                    color = theme.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = theme.muted)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Main Header Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(theme.card)
                    .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArtworkThumb(url = song.artworkUrl, size = 64.dp, cornerRadius = 14.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song.title,
                        color = theme.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        song.artist,
                        color = theme.muted,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (song.albumName.isNotBlank()) song.albumName else "Punjabi Beats Special",
                        color = theme.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Audio Metrics Capsule
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.card.copy(alpha = 0.6f))
                    .border(1.dp, if (audioFormat.isLossless) theme.secondary else theme.accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    if (audioFormat.isLossless) "💎 LOSSLESS MASTER" else "✨ HIGH DEFINITION AUDIO",
                    color = if (audioFormat.isLossless) theme.secondary else theme.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${audioFormat.bitrateKbps} KBPS • ${audioFormat.codec} • ${audioFormat.sampleRateKhz} kHz",
                    color = theme.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Story & Production Details
            Text("About this track", color = theme.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "An iconic Punjabi banger by ${song.artist}. Featuring master-level composition, modern hard-hitting percussion, and authentic Punjabi poetry designed for road trips, jam sessions, and late-night drives.",
                color = theme.muted,
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
            )

            Spacer(Modifier.height(16.dp))

            // Key Attributes Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DnaAttributeBadge(label = "Duration", value = formatDuration(song.durationMs), modifier = Modifier.weight(1f))
                DnaAttributeBadge(label = "Year", value = if (song.year != null && song.year > 0) "${song.year}" else "2024", modifier = Modifier.weight(1f))
                DnaAttributeBadge(label = "Mastering", value = "Studio 24-bit", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DnaAttributeBadge(label: String, value: String, modifier: Modifier = Modifier) {
    val theme = LocalPBTheme.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(theme.card)
            .border(1.dp, theme.panel2, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = theme.muted, fontSize = 11.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = theme.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/**
 * 🎚️ 5-Band Equalizer & Bass Boost Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    enabled: Boolean,
    bands: List<Short>,
    bassBoost: Int,
    onEnabledChange: (Boolean) -> Unit,
    onBandChange: (Int, Short) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val theme = LocalPBTheme.current
    val bandLabels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = theme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "ııll Equalizer & DSP",
                    style = MaterialTheme.typography.titleLarge,
                    color = theme.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = theme.muted)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Enable switch row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.card)
                    .border(1.dp, theme.panel2, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Audio Effects Engine", fontWeight = FontWeight.SemiBold, color = theme.text, fontSize = 14.sp)
                    Text("5-Band hardware equalizer + Bass Boost", color = theme.muted, fontSize = 11.sp)
                }
                androidx.compose.material3.Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = theme.accent,
                    ),
                )
            }

            Spacer(Modifier.height(16.dp))

            // Bass Boost Slider
            Text(
                "BASS BOOST: ${if (bassBoost == 0) "OFF" else "${(bassBoost * 100) / 1000}%"}",
                color = theme.secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            androidx.compose.material3.Slider(
                value = bassBoost.toFloat().coerceIn(0f, 1000f),
                onValueChange = { onBassBoostChange(it.toInt()) },
                valueRange = 0f..1000f,
                enabled = enabled,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = theme.secondary,
                    activeTrackColor = theme.secondary,
                    inactiveTrackColor = theme.panel2,
                ),
            )

            Spacer(Modifier.height(14.dp))

            // 5-Band Equalizer Sliders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "5-BAND FREQUENCY LEVELS",
                    color = theme.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                androidx.compose.material3.TextButton(onClick = onReset, enabled = enabled) {
                    Text("Reset Flat", color = if (enabled) theme.accent else theme.muted, fontSize = 12.sp)
                }
            }

            bandLabels.forEachIndexed { index, label ->
                val rawLevel = bands.getOrNull(index) ?: 0
                val db = rawLevel / 100
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        modifier = Modifier.width(60.dp),
                        color = theme.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    androidx.compose.material3.Slider(
                        value = rawLevel.toFloat().coerceIn(-1000f, 1000f),
                        onValueChange = { onBandChange(index, it.toInt().toShort()) },
                        valueRange = -1000f..1000f,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = theme.accent,
                            activeTrackColor = theme.accent,
                            inactiveTrackColor = theme.panel2,
                        ),
                    )
                    Text(
                        "${if (db > 0) "+$db" else "$db"} dB",
                        modifier = Modifier.width(55.dp),
                        color = theme.muted,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                }
            }
        }
    }
}
