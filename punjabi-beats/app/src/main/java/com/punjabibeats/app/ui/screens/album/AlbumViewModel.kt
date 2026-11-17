package com.punjabibeats.app.ui.screens.album

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.domain.repository.SongRepository
import com.punjabibeats.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class AlbumUiState(
    val isLoading: Boolean = true,
    val albumName: String = "",
    val artistName: String = "",
    val year: Int? = null,
    val artworkUrl: String? = null,
    val songs: List<Song> = emptyList(),
    val totalDurationMs: Long = 0L,
)

@HiltViewModel
class AlbumViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val songRepository: SongRepository,
) : ViewModel() {

    private val rawAlbum = savedStateHandle.get<String>(Routes.ARG_ALBUM_NAME).orEmpty()
    val albumName: String = try {
        URLDecoder.decode(rawAlbum, "UTF-8")
    } catch (_: Exception) {
        rawAlbum
    }

    private val rawArtist = savedStateHandle.get<String>(Routes.ARG_ARTIST_NAME).orEmpty()
    val artistName: String = try {
        URLDecoder.decode(rawArtist, "UTF-8")
    } catch (_: Exception) {
        rawArtist
    }

    private val _uiState = MutableStateFlow(AlbumUiState(albumName = albumName, artistName = artistName))
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    init {
        loadAlbumTracks()
    }

    private var loadTracksJob: kotlinx.coroutines.Job? = null

    fun loadAlbumTracks() {
        loadTracksJob?.cancel()
        loadTracksJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            runSuspendCatching {
                val query = if (artistName.isNotBlank() && !artistName.equals("Unknown", ignoreCase = true)) {
                    "$albumName $artistName"
                } else {
                    albumName
                }

                val songs = songRepository.searchSongs(query, limit = 30)
                val topArtwork = songs.firstOrNull { !it.artworkUrl.isNullOrBlank() }?.artworkUrl
                val year = songs.firstOrNull { it.year != null }?.year
                val totalDuration = songs.sumOf { it.durationMs }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    songs = songs,
                    artworkUrl = topArtwork,
                    year = year,
                    totalDurationMs = totalDuration,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
