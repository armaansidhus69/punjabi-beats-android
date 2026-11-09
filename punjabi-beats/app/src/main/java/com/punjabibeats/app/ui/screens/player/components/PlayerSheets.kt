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
