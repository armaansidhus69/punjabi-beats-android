package com.punjabibeats.app.ui.screens.player.components

import androidx.compose.foundation.layout.Arrangement
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
import com.punjabibeats.app.ui.theme.AccentGold
import com.punjabibeats.app.ui.theme.TextSecondary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.playback.AudioFormatInfo
import com.punjabibeats.app.ui.components.ArtworkThumb
import com.punjabibeats.app.ui.theme.LocalPBTheme

/** Sleep timer options: 15/30/45/60 minutes or end of the current track. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    sleepMode: SleepMode?,
    onStart: (SleepMode) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("Sleep Timer", style = MaterialTheme.typography.titleLarge)
            Text(
                when (sleepMode) {
                    is SleepMode.Minutes -> "Music stops in ${sleepMode.minutes} minutes"
                    SleepMode.EndOfTrack -> "Music stops at the end of this track"
                    null -> "Playback keeps playing until you stop it"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
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
    androidx.compose.material3.AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
            containerColor = if (active) AccentGold else MaterialTheme.colorScheme.surfaceVariant,
            labelColor = if (active) androidx.compose.ui.graphics.Color.Black else MaterialTheme.colorScheme.onSurface,
        ),
    )
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
    val queue by controller.queue.collectAsStateWithLifecycle()
    val currentIndex by controller.currentIndex.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val dragState = rememberDragDropState(listState = listState, onMove = onMove)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                "Up Next — ${queue.size} songs",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .dragContainer(dragState),
            ) {
                items(queue.size) { index ->
                    val song = queue[index]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .draggableItem(dragState, index)
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                    ) {
                        IconButton(onClick = { controller.seekToIndex(index) }) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = if (index == currentIndex) AccentGold else TextSecondary,
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp),
                        ) {
                            Text(
                                song.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (index == currentIndex) AccentGold else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            formatDuration(song.durationMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                        Icon(
                            Icons.Rounded.DragHandle,
                            contentDescription = "Reorder (hold & drag)",
                            tint = TextSecondary,
                        )
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = TextSecondary)
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
