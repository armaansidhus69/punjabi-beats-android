package com.punjabibeats.app.ui.screens.ai

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punjabibeats.app.core.util.UiState
import com.punjabibeats.app.domain.model.AiMessage
import com.punjabibeats.app.domain.model.AiRole
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.ui.components.ArtworkThumb
import com.punjabibeats.app.ui.components.LocalPbSnackbar
import com.punjabibeats.app.ui.components.PbLoading
import com.punjabibeats.app.ui.theme.AccentGold
import com.punjabibeats.app.ui.theme.BgBase
import com.punjabibeats.app.ui.theme.SurfaceElevated
import com.punjabibeats.app.ui.theme.TextSecondary

private val PROMPT_IDEAS = listOf(
    "Late night highway drive Punjabi songs",
    "Gym rage bhangra, heavy bass",
    "Heartbreak 3AM Punjabi mix",
    "Wedding dance floor bangers",
)

@Composable
fun AiStudioScreen(
    controller: MusicPlayerController,
    onOpenPlayer: () -> Unit,
    bottomPadding: PaddingValues,
    viewModel: AiStudioViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalPbSnackbar.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { message -> snackbar.showSnackbar(message) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        Text(
            "AI Studio",
            style = MaterialTheme.typography.displaySmall,
            color = AccentGold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        AiTabs(
            viewModel = viewModel,
            uiState = uiState,
            controller = controller,
            onOpenPlayer = onOpenPlayer,
            bottomPadding = bottomPadding,
        )
    }
}

@Composable
private fun AiTabs(
    viewModel: AiStudioViewModel,
    uiState: AiStudioUiState,
    controller: MusicPlayerController,
    onOpenPlayer: () -> Unit,
    bottomPadding: PaddingValues,
) {
    var selected by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceElevated),
        ) {
            listOf("AI Playlist", "Assistant").forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected == index) AccentGold else Color.Transparent)
                        .clickable { selected = index }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected == index) Color.Black else TextSecondary,
                    )
                }
            }
        }

        if (selected == 0) {
            PlaylistTab(viewModel, uiState, controller, onOpenPlayer, bottomPadding)
        } else {
            ChatTab(viewModel, uiState, bottomPadding)
        }
    }
}

@Composable
private fun PlaylistTab(
    viewModel: AiStudioViewModel,
    uiState: AiStudioUiState,
    controller: MusicPlayerController,
    onOpenPlayer: () -> Unit,
    bottomPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomPadding.calculateBottomPadding() + 130.dp),
    ) {
        Text(
            "Describe the vibe, get an instant playlist.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceElevated)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = AccentGold)
            Box(Modifier.padding(12.dp)) {
                androidx.compose.foundation.text.BasicTextField(
                    value = uiState.prompt,
                    onValueChange = viewModel::onPromptChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { inner ->
                        if (uiState.prompt.isEmpty()) Text("e.g. Late night highway drive Punjabi songs", color = TextSecondary)
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(PROMPT_IDEAS, key = { it }) { idea ->
                AssistChip(
                    onClick = { viewModel.onPromptChange(idea) },
                    label = { Text(idea, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                )
            }
        }
        Button(
            onClick = { viewModel.generatePlaylist() },
            enabled = uiState.playlistState !is UiState.Loading,
            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            if (uiState.playlistState is UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                Spacer(Modifier.width(10.dp))
                Text("Composing…")
            } else {
                Text("Generate Playlist")
            }
        }

        when (val state = uiState.playlistState) {
            is UiState.Error -> Text(
                state.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            is UiState.Success -> {
                val playlist = state.data
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(playlist.title, style = MaterialTheme.typography.titleLarge)
                    if (playlist.description.isNotBlank()) {
                        Text(playlist.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 10.dp),
                    ) {
                        Button(
                            onClick = {
                                controller.playQueue(playlist.songs, 0)
                                onOpenPlayer()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Play all")
                        }
                        OutlinedButton(
                            onClick = { viewModel.savePlaylistToLibrary() },
                            enabled = !uiState.savingPlaylist,
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save")
                        }
                    }
                }
                playlist.songs.forEachIndexed { index, song ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                controller.playQueue(playlist.songs, index)
                                onOpenPlayer()
                            }
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                    ) {
                        ArtworkThumb(url = song.artworkUrl, size = 44.dp, cornerRadius = 8.dp)
                        Column(Modifier.padding(horizontal = 12.dp)) {
                            Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun ChatTab(
    viewModel: AiStudioViewModel,
    uiState: AiStudioUiState,
    bottomPadding: PaddingValues,
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.messages.isEmpty()) {
                item {
                    Text(
                        "Ask me anything about Punjabi music — artists, meanings, recommendations…",
                        color = TextSecondary,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
            items(uiState.messages, key = { it.timestampMs }) { message ->
                ChatBubble(message)
            }
            if (uiState.isThinking) {
                item { PbLoading(modifier = Modifier.height(48.dp)) }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceElevated),
        ) {
            Box(Modifier.weight(1f)) {
                androidx.compose.foundation.text.BasicTextField(
                    value = uiState.chatInput,
                    onValueChange = viewModel::onChatInputChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { inner ->
                        Box(Modifier.padding(14.dp)) {
                            if (uiState.chatInput.isEmpty()) Text("Ask the music assistant…", color = TextSecondary)
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            IconButton(onClick = { viewModel.sendChatMessage() }) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = AccentGold)
            }
        }
        Spacer(Modifier.height(bottomPadding.calculateBottomPadding() + 130.dp))
    }
}

@Composable
private fun ChatBubble(message: AiMessage) {
    val isUser = message.role == AiRole.USER
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(
            message.content,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isUser) Color.Black else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isUser) AccentGold else SurfaceElevated)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}
