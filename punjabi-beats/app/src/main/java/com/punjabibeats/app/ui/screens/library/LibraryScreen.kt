package com.punjabibeats.app.ui.screens.library

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.ui.components.EmptyState
import com.punjabibeats.app.ui.components.LocalPbSnackbar
import com.punjabibeats.app.ui.components.SongRow
import com.punjabibeats.app.ui.theme.AccentGold
import com.punjabibeats.app.ui.theme.TextSecondary

private val LIBRARY_TABS = listOf("Liked", "Playlists", "Downloads")

@Composable
fun LibraryScreen(
    controller: MusicPlayerController,
    onOpenPlayer: () -> Unit,
    onOpenPodcasts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    bottomPadding: PaddingValues,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSong by controller.currentSong.collectAsStateWithLifecycle()
    val snackbar = LocalPbSnackbar.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message -> snackbar.showSnackbar(message) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Library", style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenPodcasts) {
                Icon(Icons.Rounded.CloudDownload, contentDescription = "Podcasts", tint = AccentGold)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = AccentGold)
            }
        }
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = AccentGold,
        ) {
            LIBRARY_TABS.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label) },
                )
            }
        }

        when (selectedTab) {
            0 -> LikedTab(uiState, currentSong?.id, viewModel, controller, onOpenPlayer, bottomPadding)
            1 -> PlaylistsTab(
                uiState = uiState,
                onOpenPlaylist = onOpenPlaylist,
                onCreateClick = { showCreateDialog = true },
                viewModel = viewModel,
                bottomPadding = bottomPadding,
            )
            else -> DownloadsTab(uiState, currentSong?.id, viewModel, controller, onOpenPlayer, bottomPadding)
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onConfirm = { name ->
                showCreateDialog = false
                viewModel.createPlaylist(name)
            },
            onDismiss = { showCreateDialog = false },
        )
    }
}

/** Playlist detail: songs, play-all and per-song remove. */
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    controller: MusicPlayerController,
    onOpenPlayer: () -> Unit,
    onBack: () -> Unit,
    bottomPadding: PaddingValues,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val songs by viewModel.observePlaylistSongs(playlistId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val currentSong by controller.currentSong.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
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
            Text("Playlist", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(
                enabled = songs.isNotEmpty(),
                onClick = {
                    controller.playQueue(songs, 0)
                    onOpenPlayer()
                },
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play all", tint = AccentGold)
            }
        }
        if (songs.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.QueueMusic,
                title = "Empty playlist",
                subtitle = "Add songs from search, or save AI playlists here.",
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding() + 130.dp)) {
                items(songs.size) { index ->
                    val song = songs[index]
                    SongRow(
                        song = song,
                        isPlaying = song.id == currentSong?.id,
                        onClick = {
                            controller.playQueue(songs, index)
                            onOpenPlayer()
                        },
                        trailing = {
                            IconButton(onClick = { viewModel.removeFromPlaylist(playlistId, song.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = TextSecondary)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LikedTab(
    uiState: LibraryUiState,
    currentSongId: String?,
    viewModel: LibraryViewModel,
    controller: MusicPlayerController,
    onOpenPlayer: () -> Unit,
    bottomPadding: PaddingValues,
) {
    if (uiState.likedSongs.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Favorite,
            title = "No liked songs yet",
            subtitle = "Tap the heart in the player and your favourites land here instantly.",
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding() + 130.dp)) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        controller.playQueue(uiState.likedSongs, 0)
                        onOpenPlayer()
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(AccentGold, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Play all",
                        tint = Color.Black,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text("Play all ${uiState.likedSongs.size} songs", style = MaterialTheme.typography.titleMedium)
            }
        }
        items(uiState.likedSongs.size) { index ->
            val song = uiState.likedSongs[index]
            SongRow(
                song = song,
                isPlaying = song.id == currentSongId,
                isLiked = true,
                onToggleLike = { viewModel.toggleLike(song) },
                onClick = {
                    controller.playQueue(uiState.likedSongs, index)
                    onOpenPlayer()
                },
            )
        }
    }
}

@Composable
private fun PlaylistsTab(
    uiState: LibraryUiState,
    onOpenPlaylist: (String) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: LibraryViewModel,
    bottomPadding: PaddingValues,
) {
    Column(
        modifier = Modifier.padding(bottom = bottomPadding.calculateBottomPadding() + 130.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Your playlists", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onCreateClick) {
                Icon(Icons.Rounded.Add, contentDescription = "New playlist", tint = AccentGold)
            }
        }
        if (uiState.playlists.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.QueueMusic,
                title = "No playlists yet",
                subtitle = "Create one here, or save an AI-generated playlist straight from the AI Studio.",
            )
        }
        uiState.playlists.forEach { playlist ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPlaylist(playlist.id) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = AccentGold)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                ) {
                    Text(playlist.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${playlist.songCount} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete playlist", tint = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun DownloadsTab(
    uiState: LibraryUiState,
    currentSongId: String?,
    viewModel: LibraryViewModel,
    controller: MusicPlayerController,
    onOpenPlayer: () -> Unit,
    bottomPadding: PaddingValues,
) {
    if (uiState.downloads.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.CloudDownload,
            title = "No offline downloads",
            subtitle = "Download the current song from the player — files are stored AES-GCM encrypted on device.",
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding() + 130.dp)) {
        items(uiState.downloads.size) { index ->
            val song = uiState.downloads[index]
            Column {
                SongRow(
                    song = song,
                    isPlaying = song.id == currentSongId,
                    onClick = {
                        controller.playQueue(uiState.downloads, index)
                        onOpenPlayer()
                    },
                    trailing = {
                        val progress = uiState.downloadProgress[song.id]
                        if (progress != null && progress < 100) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AccentGold)
                        } else {
                            IconButton(onClick = { viewModel.removeDownload(song) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove download", tint = TextSecondary)
                            }
                        }
                    },
                )
                if (uiState.downloadProgress[song.id] != null) {
                    LinearProgressIndicator(
                        progress = { (uiState.downloadProgress[song.id] ?: 0) / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(2.dp),
                        color = AccentGold,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("Playlist name") },
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name) },
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
