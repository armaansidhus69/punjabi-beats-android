package com.punjabibeats.app.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.core.util.userMessage
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.domain.repository.DownloadRepository
import com.punjabibeats.app.domain.repository.LibraryRepository
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.playback.ResolutionBus
import com.punjabibeats.app.playback.SleepMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

/**
 * Bridges the shared [MusicPlayerController] into Compose state, handles the
 * optimistic like toggle, play-history recording, auto-skip on stream errors
 * and offline download of the current track.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    val player: MusicPlayerController,
    private val libraryRepository: LibraryRepository,
    private val downloadRepository: DownloadRepository,
    private val settingsDataStore: com.punjabibeats.app.data.local.datastore.SettingsDataStore,
    resolutionBus: ResolutionBus,
) : ViewModel() {

    val seekbarStyle = settingsDataStore.seekbarStyle()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "classic_waveform")

    val isTurntableMode = settingsDataStore.turntableMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val equalizerEnabled = settingsDataStore.equalizerEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val equalizerBands = settingsDataStore.equalizerBands()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf<Short>(0, 0, 0, 0, 0))

    val bassBoostStrength = settingsDataStore.bassBoostStrength()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setEqualizerEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.saveEqualizerEnabled(enabled) }
    }

    fun setBandLevel(bandIndex: Int, levelMilliBel: Short) {
        viewModelScope.launch {
            val current = equalizerBands.value
            val next = current.toMutableList()
            if (bandIndex in next.indices) {
                next[bandIndex] = levelMilliBel
                settingsDataStore.saveEqualizerBands(next)
            }
        }
    }

    fun resetEqualizer() {
        viewModelScope.launch {
            settingsDataStore.saveEqualizerBands(listOf<Short>(0, 0, 0, 0, 0))
            settingsDataStore.saveBassBoostStrength(0)
        }
    }

    fun setBassBoost(strength: Int) {
        viewModelScope.launch { settingsDataStore.saveBassBoostStrength(strength) }
    }

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val downloadProgress = downloadRepository.observeProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private var consecutiveErrors = 0

    init {
        viewModelScope.launch {
            player.currentSong
                .flatMapLatest { song ->
                    if (song == null) flowOf(false) else libraryRepository.observeIsLiked(song.id)
                }
                .collect { _isLiked.value = it }
        }
        viewModelScope.launch {
            player.currentSong
                .distinctUntilChangedBy { it?.id }
                .collectLatest { song ->
                    if (song == null) return@collectLatest
                    // Industry-standard threshold: require at least 15s of continuous listening
                    // before recording to play_history. Rapid skips cancel this job via collectLatest
                    // so play history and Top Artists are never polluted by skipped tracks.
                    kotlinx.coroutines.delay(15_000L)
                    if (player.isPlaying.value) {
                        runSuspendCatching { libraryRepository.recordPlay(song) }
                    }
                }
        }
        viewModelScope.launch {
            player.isPlaying.collect { playing -> if (playing) consecutiveErrors = 0 }
        }
        viewModelScope.launch {
            player.playerErrors.collect { error ->
                _events.tryEmit(error)
            }
        }
        viewModelScope.launch {
            resolutionBus.resolutionFailures.collect { song ->
                _events.tryEmit("No playable source found for \"${song.title}\" — skipping")
            }
        }
    }

    fun toggleLike() {
        val song = player.currentSong.value ?: return
        _isLiked.value = !_isLiked.value // optimistic heart animation
        viewModelScope.launch {
            runSuspendCatching { libraryRepository.toggleLike(song) }
                .onFailure { _isLiked.value = !_isLiked.value } // revert on failure
        }
    }

    fun downloadCurrent() {
        val song = player.currentSong.value ?: return
        viewModelScope.launch {
            _events.tryEmit("Downloading \"${song.title}\"…")
            runSuspendCatching { downloadRepository.download(song) }
                .onSuccess { _events.tryEmit("Saved offline: ${song.title}") }
                .onFailure { _events.tryEmit(it.userMessage()) }
        }
    }

    fun playSong(song: Song) = player.playQueue(listOf(song), 0)

    fun startSleepTimer(mode: SleepMode) = player.startSleepTimer(mode)

    fun cancelSleepTimer() = player.cancelSleepTimer()

    fun setPlaybackSpeed(speed: Float) = player.setPlaybackSpeed(speed)

    fun moveQueueItem(from: Int, to: Int) = player.moveQueueItem(from, to)

    fun removeFromQueue(index: Int) = player.removeFromQueue(index)

    fun setSeekbarStyle(style: String) {
        viewModelScope.launch { settingsDataStore.saveSeekbarStyle(style) }
    }

    fun setTurntableMode(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.saveTurntableMode(enabled) }
    }
}
