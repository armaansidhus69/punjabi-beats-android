package com.punjabibeats.app.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Surface
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punjabibeats.app.core.util.UiState
import com.punjabibeats.app.domain.model.AiMessage
import com.punjabibeats.app.domain.model.AiRole
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.ui.components.ArtworkThumb
import com.punjabibeats.app.ui.components.LocalPbSnackbar
import com.punjabibeats.app.ui.components.PbLoading
import com.punjabibeats.app.ui.theme.LocalPBTheme

private val PROMPT_SUGGESTIONS = listOf(
    Pair("⚡", "UK Punjabi Drill"),
    Pair("🪕", "Folk Tumbi Pop"),
    Pair("🥁", "Old School Bhangra"),
    Pair("🌙", "Lo-Fi Boliyan"),
    Pair("🚗", "Gaddi 808 Bass"),
    Pair("⚠️", "Late night highway drive"),
    Pair("🏋️", "Gym rage"),
    Pair("❤️", "Romantic Punjabi"),
    Pair("🔥", "Party"),
)

@Composable
fun AiStudioScreen(
    controller: MusicPlayerController,
    onOpenPlayer: () -> Unit,
    bottomPadding: PaddingValues,
    viewModel: AiStudioViewModel = hiltViewModel(),
) {
    val theme = LocalPBTheme.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalPbSnackbar.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { message -> snackbar.showSnackbar(message) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .statusBarsPadding(),
    ) {
        // Sunset Skyline Hero Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.heroGradient)
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Column {
                Text(
                    "✨ AI Studio",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.text,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Create playlists, get song suggestions & chat with your music assistant.",
                    color = theme.muted,
                    fontSize = 15.sp,
                )
            }
        }

        AiTabs(
            viewModel = viewModel,
            uiState = uiState,
            controller = controller,
            onOpenPlayer = onOpenPlayer,
            bottomPadding = bottomPadding,
            modifier = Modifier.weight(1f),
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
    modifier: Modifier = Modifier,
) {
    val theme = LocalPBTheme.current
    var selected by remember { mutableIntStateOf(0) }
    Column(modifier = modifier.fillMaxWidth()) {
        // Segmented Switcher Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(theme.surface)
                .border(1.dp, theme.panel2, RoundedCornerShape(20.dp)),
        ) {
            listOf("🎵 AI Playlist", "💬 Assistant").forEachIndexed { index, label ->
                val isSelected = selected == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) theme.accent.copy(alpha = 0.22f) else Color.Transparent)
                        .border(1.dp, if (isSelected) theme.accent else Color.Transparent, RoundedCornerShape(20.dp))
                        .clickable { selected = index }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) theme.accent else theme.muted,
                    )
                }
            }
        }

        if (selected == 0) {
            PlaylistTab(
                viewModel = viewModel,
                uiState = uiState,
                controller = controller,
                onOpenPlayer = onOpenPlayer,
                bottomPadding = bottomPadding,
                modifier = Modifier.weight(1f),
            )
        } else {
            ChatTab(
                viewModel = viewModel,
                uiState = uiState,
                bottomPadding = bottomPadding,
                modifier = Modifier.weight(1f),
            )
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
    modifier: Modifier = Modifier,
) {
    val theme = LocalPBTheme.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Describe the vibe, get an instant playlist.",
            color = theme.muted,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(14.dp))

        // Big Prompt Box with glowing theme border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(theme.surface)
                .border(1.5.dp, theme.accent.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = theme.accent,
                    modifier = Modifier.size(24.dp).padding(top = 2.dp),
                )
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = uiState.prompt,
                    onValueChange = viewModel::onPromptChange,
                    textStyle = TextStyle(color = theme.text, fontSize = 16.sp),
                    minLines = 3,
                    decorationBox = { inner ->
                        if (uiState.prompt.isEmpty()) {
                            Text(
                                "e.g. Late night highway drive Punjabi songs",
                                color = theme.muted,
                                fontSize = 15.sp,
                            )
                        }
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Quick Suggestion Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(PROMPT_SUGGESTIONS, key = { it.second }) { (emoji, text) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(theme.surface)
                        .border(1.dp, theme.panel2, RoundedCornerShape(18.dp))
                        .clickable { viewModel.onPromptChange("$text Punjabi songs") }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(emoji, fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(text, color = theme.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Generate Playlist Golden Button
        Button(
            onClick = { viewModel.generatePlaylist() },
            enabled = uiState.playlistState !is UiState.Loading,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(48.dp),
        ) {
            if (uiState.playlistState is UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                Spacer(Modifier.width(10.dp))
                Text("✨ Composing…", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else {
                Text("✨ Generate Playlist", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        when (val state = uiState.playlistState) {
            is UiState.Error -> {
                Spacer(Modifier.height(12.dp))
                Text(
                    state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            is UiState.Success -> {
                val playlist = state.data
                Spacer(Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(playlist.title, color = theme.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    if (playlist.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(playlist.description, color = theme.muted, fontSize = 14.sp)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Button(
                            onClick = {
                                controller.playQueue(playlist.songs, 0)
                                onOpenPlayer()
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = theme.background),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Play all", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { viewModel.savePlaylistToLibrary() },
                            enabled = !uiState.savingPlaylist,
                            shape = RoundedCornerShape(50),
                            border = androidx.compose.foundation.BorderStroke(1.dp, theme.accent),
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, tint = theme.accent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save", color = theme.accent, fontWeight = FontWeight.Bold)
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
                        ArtworkThumb(url = song.artworkUrl, size = 48.dp, cornerRadius = 10.dp)
                        Column(Modifier.padding(horizontal = 12.dp)) {
                            Text(song.title, color = theme.text, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, color = theme.muted, fontSize = 13.sp, maxLines = 1)
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
    modifier: Modifier = Modifier,
) {
    val theme = LocalPBTheme.current
    val listState = rememberLazyListState()
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (uiState.messages.isEmpty()) {
                item {
                    Text(
                        "Ask me anything about Punjabi music — artists, meanings, recommendations…",
                        color = theme.muted,
                        fontSize = 15.sp,
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

        // Fixed bottom chat input dock with direct IME padding
        Surface(
            color = theme.surface.copy(alpha = 0.98f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(theme.background)
                        .border(1.dp, theme.panel2, RoundedCornerShape(22.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    BasicTextField(
                        value = uiState.chatInput,
                        onValueChange = viewModel::onChatInputChange,
                        textStyle = TextStyle(color = theme.text, fontSize = 15.sp),
                        decorationBox = { inner ->
                            Box(Modifier.padding(vertical = 8.dp)) {
                                if (uiState.chatInput.isEmpty()) {
                                    Text("Ask anything about Punjabi music…", color = theme.muted, fontSize = 14.sp)
                                }
                                inner()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.sendChatMessage() },
                    enabled = uiState.chatInput.isNotBlank() && !uiState.isThinking,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (uiState.chatInput.isNotBlank()) theme.accent else theme.surface),
                ) {
                    if (uiState.isThinking) {
                        CircularProgressIndicator(
                            color = theme.background,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send",
                            tint = if (uiState.chatInput.isNotBlank()) theme.background else theme.muted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: AiMessage) {
    val theme = LocalPBTheme.current
    val isUser = message.role == AiRole.USER
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(
            message.content,
            fontSize = 15.sp,
            color = if (isUser) theme.background else theme.text,
            fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isUser) theme.accent else theme.surface)
                .border(1.dp, if (isUser) theme.accent else theme.panel2, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
