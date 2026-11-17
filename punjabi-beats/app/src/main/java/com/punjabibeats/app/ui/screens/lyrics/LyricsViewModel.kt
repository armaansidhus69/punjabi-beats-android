package com.punjabibeats.app.ui.screens.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punjabibeats.app.core.util.UiState
import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.core.util.userMessage
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
    val timingOffsetMs: Long = 0L,
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

    /** Fine-grained 50ms ticker with real-time clock interpolation while the lyrics screen is visible —
     *  delivers continuous word-by-word and line-by-line karaoke highlighting without 500ms stepped freezes. */
    val positionMs: StateFlow<Long> = flow {
        var lastBasePos = player.positionMs.value
        var lastBaseTime = android.os.SystemClock.elapsedRealtime()

        while (true) {
            val isPlaying = player.isPlaying.value
            val currentPos = player.positionMs.value

            if (currentPos != lastBasePos) {
                lastBasePos = currentPos
                lastBaseTime = android.os.SystemClock.elapsedRealtime()
            }

            val interpolated = if (isPlaying) {
                val elapsed = (android.os.SystemClock.elapsedRealtime() - lastBaseTime) * player.playbackSpeed.value
                val duration = player.durationMs.value
                val maxLimit = if (duration > 0) duration else Long.MAX_VALUE
                (lastBasePos + elapsed.toLong()).coerceIn(0L, maxLimit)
            } else {
                currentPos
            }

            emit(interpolated)
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
        val currentScriptMode = _uiState.value.scriptMode
        _uiState.value = LyricsUiState(
            lyrics = UiState.Loading,
            scriptMode = currentScriptMode,
        )
        runSuspendCatching {
            lyricsRepository.fetchLyrics(song.title, song.artist, song.durationMs)
        }.fold(
            onSuccess = { lyrics ->
                _uiState.value = _uiState.value.copy(
                    lyrics = UiState.Success(lyrics),
                    scriptMode = currentScriptMode,
                )
            },
            onFailure = { failure ->
                _uiState.value = _uiState.value.copy(
                    lyrics = UiState.Error(failure.userMessage()),
                    scriptMode = currentScriptMode,
                )
            },
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

    fun adjustTimingOffset(deltaMs: Long) {
        val next = (_uiState.value.timingOffsetMs + deltaMs).coerceIn(-10000L, 10000L)
        _uiState.value = _uiState.value.copy(timingOffsetMs = next)
    }

    fun resetTimingOffset() {
        _uiState.value = _uiState.value.copy(timingOffsetMs = 0L)
    }

    fun notifyShared(count: Int) {
        _events.tryEmit("Lyric card with $count lines ready to share")
    }
}
