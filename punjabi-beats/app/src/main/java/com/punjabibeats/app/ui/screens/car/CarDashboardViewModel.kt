package com.punjabibeats.app.ui.screens.car

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punjabibeats.app.data.local.datastore.SettingsDataStore
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.domain.repository.LibraryRepository
import com.punjabibeats.app.domain.repository.SongRepository
import com.punjabibeats.app.playback.MusicPlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CarDashboardViewModel @Inject constructor(
    val player: MusicPlayerController,
    private val songRepository: SongRepository,
    private val libraryRepository: LibraryRepository,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CarDashboardState())
    val uiState: StateFlow<CarDashboardState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                player.currentSong,
                player.isPlaying,
                player.positionMs,
                player.durationMs,
            ) { song, isPlaying, pos, dur ->
                _uiState.update { current ->
                    current.copy(
                        title = song?.title ?: "295",
                        artist = song?.artist ?: "Sidhu Moose Wala • The Kidd",
                        albumArtUrl = song?.artworkUrl,
                        isPlaying = isPlaying,
                        positionMs = pos,
                        durationMs = dur.coerceAtLeast(1L),
                    )
                }
            }.collect {}
        }
    }

    fun playPause() = player.playPause()
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    fun toggleFavorite() {
        val current = _uiState.value.isFavorite
        _uiState.update { it.copy(isFavorite = !current) }
        val song = player.currentSong.value ?: return
        viewModelScope.launch {
            libraryRepository.toggleLike(song)
        }
    }

    fun setGedhiPreset(presetId: String) {
        _uiState.update { it.copy(activeGedhiPreset = presetId) }
    }

    fun selectQuickPick(pick: CarQuickPick) {
        viewModelScope.launch {
            val songs = runCatching {
                songRepository.searchSongs(pick.songQuery, limit = 20)
            }.getOrNull()
            if (!songs.isNullOrEmpty()) {
                player.playQueue(songs, 0)
            }
        }
    }
}
