package com.punjabibeats.app.ui.screens.podcast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punjabibeats.app.core.util.formatDuration
import com.punjabibeats.app.domain.model.PLAYBACK_SPEEDS
import com.punjabibeats.app.domain.model.PodcastChannel
import com.punjabibeats.app.ui.components.ArtworkThumb
import com.punjabibeats.app.ui.components.EmptyState
import com.punjabibeats.app.ui.components.LocalPbSnackbar
import com.punjabibeats.app.ui.components.PbLoading
import com.punjabibeats.app.ui.theme.LocalPBTheme
import com.punjabibeats.app.ui.theme.TextSecondary

@Composable
fun PodcastScreen(
    onBack: () -> Unit,
    bottomPadding: PaddingValues,
    viewModel: PodcastViewModel = hiltViewModel(),
) {
    val theme = LocalPBTheme.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalPbSnackbar.current
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message -> snackbar.showSnackbar(message) }
    }
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            snackbar.showSnackbar(uiState.error.orEmpty())
            viewModel.dismissError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text("Podcasts", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add RSS feed", tint = theme.accent)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Speed:", style = MaterialTheme.typography.labelLarge, color = TextSecondary, modifier = Modifier.align(Alignment.CenterVertically))
            PLAYBACK_SPEEDS.forEach { speed ->
                AssistChip(
                    onClick = { viewModel.setSpeed(speed) },
                    label = { Text("${speed}x") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (uiState.speed == speed) theme.accent else MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = if (uiState.speed == speed) Color.White else TextSecondary,
                    ),
                )
            }
        }

        val channel = uiState.selectedChannel
        if (channel == null) {
            ChannelList(uiState, viewModel, bottomPadding)
        } else {
            EpisodeList(uiState, channel, viewModel, bottomPadding)
        }
    }

    if (showAddDialog) {
        AddFeedDialog(
            onConfirm = { url ->
                showAddDialog = false
                viewModel.addFeed(url)
            },
            onDismiss = { showAddDialog = false },
        )
    }

    if (uiState.transcriptLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PbLoading() }
    }
    uiState.transcript?.let { cues ->
        TranscriptSheet(cues = cues, onDismiss = { viewModel.closeTranscript() })
    }
}

@Composable
private fun ChannelList(
    uiState: PodcastUiState,
    viewModel: PodcastViewModel,
    bottomPadding: PaddingValues,
) {
    if (uiState.channels.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Podcasts,
            title = "No podcast feeds yet",
            subtitle = "Add any Punjabi talk-show or story RSS feed with the + button — episodes and transcripts are parsed on device.",
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding() + 130.dp)) {
        uiState.channels.forEach { channel ->
            item(key = channel.id) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectChannel(channel) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    ArtworkThumb(url = channel.artworkUrl, size = 56.dp, cornerRadius = 12.dp)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    ) {
                        Text(channel.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            channel.description.ifBlank { channel.feedUrl },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { viewModel.removeFeed(channel.id) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Remove feed", tint = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeList(
    uiState: PodcastUiState,
    channel: PodcastChannel,
    viewModel: PodcastViewModel,
    bottomPadding: PaddingValues,
) {
    val theme = LocalPBTheme.current
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        ) {
            Text(
                channel.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { viewModel.closeChannel() }) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }
        if (uiState.loadingEpisodes) {
            PbLoading()
            return@Column
        }
        LazyColumn(contentPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding() + 130.dp)) {
            if (uiState.episodes.isEmpty()) {
                item {
                    Text(
                        "No episodes parsed from this feed.",
                        color = TextSecondary,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
            uiState.episodes.forEach { episode ->
                item(key = episode.id) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ArtworkThumb(url = channel.artworkUrl, size = 48.dp, cornerRadius = 10.dp)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                            ) {
                                Text(episode.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    listOf(
                                        episode.pubDate.ifBlank { null },
                                        if (episode.durationSec > 0) formatDuration(episode.durationSec * 1000) else null,
                                    ).filterNotNull().joinToString(" • ").ifBlank { "Episode" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                            IconButton(onClick = { viewModel.playEpisode(episode) }) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = theme.accent)
                            }
                            if (episode.transcriptUrl != null) {
                                IconButton(onClick = { viewModel.openTranscript(episode) }) {
                                    Icon(Icons.Rounded.Notes, contentDescription = "Transcript", tint = TextSecondary)
                                }
                            }
                        }
                        if (episode.description.isNotBlank()) {
                            Text(
                                episode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary.copy(alpha = 0.7f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp, start = 60.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Auto-scrolling transcript sheet (SRT / WebVTT cues). */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun TranscriptSheet(
    cues: List<com.punjabibeats.app.domain.model.TranscriptCue>,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("Transcript", style = MaterialTheme.typography.titleLarge)
            Text(
                "Scrolls with the episode clock",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 60.dp)) {
            items(cues.size) { index ->
                val cue = cues[index]
                Text(
                    cue.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun AddFeedDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add podcast feed") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                singleLine = true,
                placeholder = { Text("https://example.com/feed.rss") },
            )
        },
        confirmButton = {
            TextButton(enabled = url.isNotBlank(), onClick = { onConfirm(url) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
