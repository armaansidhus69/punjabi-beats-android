package com.punjabibeats.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punjabibeats.app.core.util.NetworkMonitor
import com.punjabibeats.app.core.util.UiState
import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.core.util.userMessage
import com.punjabibeats.app.domain.model.HomeFeed
import com.punjabibeats.app.domain.model.MoodCategory
import com.punjabibeats.app.domain.repository.SongRepository
import com.punjabibeats.app.playback.MusicPlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val feed: UiState<HomeFeed> = UiState.Loading,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val player: MusicPlayerController,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    init {
        load()
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online && _uiState.value.feed is UiState.Error) {
                    load()
                }
            }
        }
    }

    fun load() {
        _uiState.value = HomeUiState(UiState.Loading)
        viewModelScope.launch {
            runSuspendCatching { songRepository.loadHomeFeed() }
                .fold(
                    onSuccess = { feed -> _uiState.value = HomeUiState(UiState.Success(feed)) },
                    onFailure = { error ->
                        _uiState.value = HomeUiState(UiState.Error(error.userMessage()))
                    },
                )
        }
    }

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _moodLoading = MutableStateFlow<String?>(null)
    val moodLoading: StateFlow<String?> = _moodLoading.asStateFlow()

    /** Mood tile tap: catalogue search for the mood query, then instant playback. */
    fun playMood(mood: MoodCategory) {
        viewModelScope.launch {
            _moodLoading.value = mood.id
            runSuspendCatching { songRepository.searchSongs(mood.query, limit = 20) }
                .fold(
                    onSuccess = { songs ->
                        if (songs.isNotEmpty()) {
                            player.playQueue(songs, 0)
                        } else {
                            _events.tryEmit("No songs found for \"${mood.title}\"")
                        }
                    },
                    onFailure = { error ->
                        _events.tryEmit(error.userMessage())
                    },
                )
            _moodLoading.value = null
        }
    }
}
