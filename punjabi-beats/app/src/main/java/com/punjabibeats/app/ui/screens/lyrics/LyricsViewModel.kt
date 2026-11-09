package com.punjabibeats.app.ui.screens.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punjabibeats.app.core.util.UiState
import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.domain.model.ScriptMode
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.domain.model.SyncedLyrics
import com.punjabibeats.app.domain.repository.LyricsRepository
import com.punjabibeats.app.playback.MusicPlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LyricsUiState(
    val lyrics: UiState<SyncedLyrics?> = UiState.Loading,
    val scriptMode: ScriptMode = ScriptMode.BOTH,
    val selectMode: Boolean = false,
    val selectedIndices: Set<Int> = emptySet(),
)

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val lyricsRepository: LyricsRepository,
    val player: MusicPlayerController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LyricsUiState())
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    /** Fine-grained 50ms ticker only while the lyrics screen is visible —
     *  fast enough for word-by-word karaoke highlighting. */
    val positionMs: StateFlow<Long> = flow {
        while (true) {
            emit(player.positionMs.value)
            delay(50)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    init {
        viewModelScope.launch {
            player.currentSong
                .distinctUntilChangedBy { it?.id }
                .collectLatest { song ->
                    if (song == null) {
                        _uiState.value = LyricsUiState(UiState.Error("Nothing is playing"))
                    } else {
                        loadLyrics(song)
                    }
                }
        }
    }

    private suspend fun loadLyrics(song: Song) {
        _uiState.value = LyricsUiState()
        val lyrics = runSuspendCatching {
            lyricsRepository.fetchLyrics(song.title, song.artist, song.durationMs)
        }.getOrNull()
        _uiState.value = LyricsUiState(
            lyrics = UiState.Success(lyrics),
            scriptMode = _uiState.value.scriptMode,
        )
    }

    fun cycleScriptMode() {
        _uiState.value = _uiState.value.copy(
            scriptMode = when (_uiState.value.scriptMode) {
                ScriptMode.BOTH -> ScriptMode.ROMAN
                ScriptMode.ROMAN -> ScriptMode.GURMUKHI
                ScriptMode.GURMUKHI -> ScriptMode.BOTH
            }
        )
    }

    fun toggleSelectMode() {
        _uiState.value = _uiState.value.copy(
            selectMode = !_uiState.value.selectMode,
            selectedIndices = emptySet(),
        )
    }

    fun toggleSelected(index: Int) {
        val state = _uiState.value
        val next = if (index in state.selectedIndices) {
            state.selectedIndices - index
        } else {
            state.selectedIndices + index
        }
        _uiState.value = state.copy(selectedIndices = next)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedIndices = emptySet(), selectMode = false)
    }

    fun notifyShared(count: Int) {
        _events.tryEmit("Lyric card with $count lines ready to share")
    }
}
