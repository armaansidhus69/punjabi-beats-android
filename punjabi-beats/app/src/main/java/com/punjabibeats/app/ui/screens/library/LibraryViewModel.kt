package com.punjabibeats.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.core.util.userMessage
import com.punjabibeats.app.domain.model.Playlist
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.domain.repository.DownloadRepository
import com.punjabibeats.app.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

data class LibraryUiState(
    val likedSongs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val downloads: List<Song> = emptyList(),
    val downloadProgress: Map<String, Int> = emptyMap(),
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _localSongs = MutableStateFlow<List<Song>>(emptyList())
    val localSongs: StateFlow<List<Song>> = _localSongs.asStateFlow()

    private val _isScanningLocal = MutableStateFlow(false)
    val isScanningLocal: StateFlow<Boolean> = _isScanningLocal.asStateFlow()

    val uiState: StateFlow<LibraryUiState> = combine(
        libraryRepository.observeLikedSongs(),
        libraryRepository.observePlaylists(),
        downloadRepository.observeDownloads(),
        downloadRepository.observeProgress(),
    ) { liked, playlists, downloads, progress ->
        LibraryUiState(
            likedSongs = liked,
            playlists = playlists,
            downloads = downloads,
            downloadProgress = progress,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun toggleLike(song: Song) {
        viewModelScope.launch {
            runSuspendCatching { libraryRepository.toggleLike(song) }
        }
    }

    fun createPlaylist(name: String, onCreated: (Playlist) -> Unit = {}) {
        viewModelScope.launch {
            runSuspendCatching { libraryRepository.createPlaylist(name) }
                .onSuccess { playlist -> onCreated(playlist) }
                .onFailure { _events.tryEmit(it.userMessage()) }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch { libraryRepository.deletePlaylist(playlistId) }
    }

    fun addToPlaylist(playlistId: String, song: Song) {
        viewModelScope.launch {
            runSuspendCatching { libraryRepository.addToPlaylist(playlistId, song) }
                .onSuccess { _events.tryEmit("Added to playlist") }
        }
    }

    fun observePlaylistSongs(playlistId: String): Flow<List<Song>> =
        libraryRepository.observePlaylistSongs(playlistId)

    fun removeFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch { libraryRepository.removeFromPlaylist(playlistId, songId) }
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch {
            _events.tryEmit("Downloading \"${song.title}\"…")
            runSuspendCatching { downloadRepository.download(song) }
                .onSuccess { _events.tryEmit("Saved offline: ${song.title}") }
                .onFailure { _events.tryEmit(it.userMessage()) }
        }
    }

    fun removeDownload(song: Song) {
        viewModelScope.launch {
            downloadRepository.remove(song.id)
            _events.tryEmit("Removed from offline")
        }
    }

    fun scanLocalSongs(context: android.content.Context) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            _isScanningLocal.value = true
            val songs = com.punjabibeats.app.data.local.LocalMusicScanner.scanLocalAudio(appContext)
            _localSongs.value = songs
            _isScanningLocal.value = false
            if (songs.isNotEmpty()) {
                _events.tryEmit("Found ${songs.size} local tracks on device")
            }
        }
    }
}
