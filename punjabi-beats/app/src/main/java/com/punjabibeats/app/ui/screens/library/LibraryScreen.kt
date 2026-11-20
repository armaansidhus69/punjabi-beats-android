package com.punjabibeats.app.ui.screens.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punjabibeats.app.data.local.LocalMusicScanner
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.ui.components.EmptyState
import com.punjabibeats.app.ui.components.LocalPbSnackbar
import com.punjabibeats.app.ui.components.PremiumScreenHeader
import com.punjabibeats.app.ui.components.SongRow
import com.punjabibeats.app.ui.theme.LocalPBTheme

private data class LibraryTabItem(val label: String, val index: Int)

private val LIBRARY_TABS = listOf(
    LibraryTabItem("❤️ Liked", 0),
    LibraryTabItem("📑 Playlists", 1),
    LibraryTabItem("⬇️ Downloads", 2),
    LibraryTabItem("📱 Local", 3),
)

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
    val theme = LocalPBTheme.current
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
            .background(theme.background)
            .statusBarsPadding(),
    ) {
        PremiumScreenHeader(
            title = "Your Library",
            subtitle = "Your saved sounds, ready when you are.",
            actions = {
                IconButton(onClick = onOpenPodcasts) {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = "Podcasts", tint = theme.muted)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = theme.muted)
                }
            },
        )

        Spacer(Modifier.height(10.dp))

        // Horizontal Segmented Pill Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LIBRARY_TABS.forEach { tab ->
                val isSelected = selectedTab == tab.index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) theme.accent.copy(alpha = 0.22f) else theme.surface)
                        .border(1.dp, if (isSelected) theme.accent else theme.panel2, RoundedCornerShape(20.dp))
                        .clickable { selectedTab = tab.index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        tab.label,
                        color = if (isSelected) theme.accent else theme.muted,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        when (selectedTab) {
            0 -> LikedTab(uiState, currentSong?.id, viewModel, controller, onOpenPlayer, bottomPadding)
            1 -> PlaylistsTab(
                uiState = uiState,
                onOpenPlaylist = onOpenPlaylist,
                onCreateClick = { showCreateDialog = true },
                viewModel = viewModel,
                bottomPadding = bottomPadding,
            )
            2 -> DownloadsTab(uiState, currentSong?.id, viewModel, controller, onOpenPlayer, bottomPadding)
            else -> LocalTab(viewModel, currentSong?.id, controller, onOpenPlayer, bottomPadding)
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
    val theme = LocalPBTheme.current
    val songs by viewModel.observePlaylistSongs(playlistId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val currentSong by controller.currentSong.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
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
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = theme.text)
            }
            Text("Playlist", style = MaterialTheme.typography.titleLarge, color = theme.text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(
                enabled = songs.isNotEmpty(),
                onClick = {
                    controller.playQueue(songs, 0)
                    onOpenPlayer()
                },
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play all", tint = theme.accent)
            }
        }
        if (songs.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.QueueMusic,
                title = "Empty playlist",
                subtitle = "Add songs from search, or save AI playlists here.",
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
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
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = theme.muted)
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
    val theme = LocalPBTheme.current
    if (uiState.likedSongs.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = theme.accent,
                modifier = Modifier.size(68.dp),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "No liked songs yet",
                color = theme.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap the heart in the player and your favourites land here instantly.",
                color = theme.muted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onOpenPlayer,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = theme.background),
                modifier = Modifier.height(48.dp),
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = theme.background, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("🎵 Discover Songs", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${uiState.likedSongs.size} Tracks Saved",
                        color = theme.muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(theme.accent)
                        .clickable {
                            controller.playQueue(uiState.likedSongs, 0)
                            onOpenPlayer()
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = "Play all",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            "Play All",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                        )
                    }
                }
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
    val theme = LocalPBTheme.current
    Column(
        modifier = Modifier.padding(bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Your playlists", color = theme.text, fontWeight = FontWeight.Bold, fontSize = 21.sp)
            IconButton(onClick = onCreateClick) {
                Icon(Icons.Rounded.Add, contentDescription = "New playlist", tint = theme.accent)
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
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.surface)
                    .border(1.dp, theme.panel2, RoundedCornerShape(16.dp))
                    .clickable { onOpenPlaylist(playlist.id) }
                    .padding(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(theme.background, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = theme.accent)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                ) {
                    Text(playlist.name, color = theme.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${playlist.songCount} songs",
                        color = theme.muted,
                        fontSize = 13.sp,
                    )
                }
                IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete playlist", tint = theme.muted)
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
    val theme = LocalPBTheme.current
    if (uiState.downloads.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.CloudDownload,
            title = "No offline downloads",
            subtitle = "Download songs from the player — files are stored AES-GCM encrypted on device.",
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
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
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = theme.accent)
                        } else {
                            IconButton(onClick = { viewModel.removeDownload(song) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove download", tint = theme.muted)
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
                        color = theme.accent,
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
    val theme = LocalPBTheme.current
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist", color = theme.text) },
        containerColor = theme.surface,
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("Playlist name", color = theme.muted) },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = theme.text,
                    unfocusedTextColor = theme.text,
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = theme.panel2,
                ),
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name) },
            ) { Text("Create", color = theme.accent, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = theme.muted) }
        },
    )
}

@Composable
private fun LocalTab(
    viewModel: LibraryViewModel,
    currentSongId: String?,
    controller: MusicPlayerController,
    onOpenPlayer: () -> Unit,
    bottomPadding: PaddingValues,
) {
    val theme = LocalPBTheme.current
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(LocalMusicScanner.hasAudioPermission(context)) }
    val localSongs by viewModel.localSongs.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanningLocal.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.scanLocalSongs(context)
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val current = LocalMusicScanner.hasAudioPermission(context)
                if (current != hasPermission) {
                    hasPermission = current
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && localSongs.isEmpty() && !isScanning) {
            viewModel.scanLocalSongs(context)
        }
    }

    if (!hasPermission) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Rounded.Smartphone,
                contentDescription = null,
                tint = theme.accent,
                modifier = Modifier.size(68.dp),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "Offline Device Audio",
                color = theme.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Scan and play your Punjabi MP3s, M4A, FLAC, and local files directly on Punjabi Beats.",
                color = theme.muted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { permissionLauncher.launch(LocalMusicScanner.getRequiredPermission()) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = theme.background),
                modifier = Modifier.height(48.dp),
            ) {
                Icon(Icons.Rounded.Folder, contentDescription = null, tint = theme.background, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Scan Device Audio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        return
    }

    val filtered = remember(localSongs, searchQuery) {
        if (searchQuery.isBlank()) localSongs
        else localSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true) ||
            it.albumName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = if (isScanning) "Scanning device..." else "${localSongs.size} offline tracks",
                    color = theme.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    text = "Stored locally on your phone",
                    color = theme.muted,
                    fontSize = 12.sp,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.scanLocalSongs(context) }) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = theme.accent)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Rescan", tint = theme.accent)
                    }
                }
                if (filtered.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            controller.playQueue(filtered, 0)
                            onOpenPlayer()
                        }
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Play all", tint = theme.accent)
                    }
                }
            }
        }

        if (localSongs.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                placeholder = { Text("Search local tracks...", color = theme.muted, fontSize = 14.sp) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = theme.text,
                    unfocusedTextColor = theme.text,
                    focusedContainerColor = theme.surface,
                    unfocusedContainerColor = theme.surface,
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = theme.panel2,
                ),
            )
        }

        if (filtered.isEmpty() && !isScanning) {
            EmptyState(
                icon = Icons.Rounded.Smartphone,
                title = if (localSongs.isEmpty()) "No local music found" else "No matching tracks",
                subtitle = if (localSongs.isEmpty()) "We could not find any audio files on your phone." else "Try searching with a different keyword.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                itemsIndexed(filtered, key = { _, song -> song.id }) { index, song ->
                    SongRow(
                        song = song,
                        isPlaying = song.id == currentSongId,
                        onClick = {
                            controller.playQueue(filtered, index)
                            onOpenPlayer()
                        },
                        trailing = {
                            IconButton(onClick = { viewModel.toggleLike(song) }) {
                                Icon(Icons.Rounded.FavoriteBorder, contentDescription = "Like", tint = theme.muted)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryHeroCarousel(
    likedCount: Int,
    downloadCount: Int,
    onPlayLiked: () -> Unit,
    onShuffleDownloads: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalPBTheme.current
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Liked Songs Big Card (Screen 08)
        item {
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF7C3AED), Color(0xFF5B21B6), Color(0xFF1F1B2E)),
                        ),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                "Auto-Offline",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        "Liked Songs",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        "$likedCount tracks • Sidhu, Karan & more",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFFCEBDFF),
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                "Synced Lossless",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFCEBDFF),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable(onClick = onPlayLiked),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = "Play Liked",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }

        // 2. Offline Desi Vault Card (Screen 08)
        item {
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.card)
                    .border(1.dp, Color(0xFF4EDEA3).copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF4EDEA3).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.DownloadForOffline,
                                contentDescription = null,
                                tint = Color(0xFF4EDEA3),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF4EDEA3).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                "Gedhi Ready",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4EDEA3),
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        "Offline Desi Vault",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.text,
                    )
                    Text(
                        "$downloadCount tracks • 24-bit Lossless",
                        fontSize = 11.sp,
                        color = theme.muted,
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "No Data Required",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4EDEA3),
                        )

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38393C))
                                .clickable(onClick = onShuffleDownloads),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle Downloads",
                                tint = Color(0xFF4EDEA3),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
