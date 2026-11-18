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
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
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
import kotlinx.coroutines.isActive
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

    val playerDeckStyle = settingsDataStore.playerDeckStyle()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "classic")

    val isTurntableMode = settingsDataStore.turntableMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val equalizerEnabled = settingsDataStore.equalizerEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val equalizerBands = settingsDataStore.equalizerBands()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf<Short>(0, 0, 0, 0, 0))

    val bassBoostStrength = settingsDataStore.bassBoostStrength()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private var bandUpdateJob: Job? = null
    private var bassJob: Job? = null

    fun setEqualizerEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.saveEqualizerEnabled(enabled) }
    }

    fun setBandLevel(bandIndex: Int, levelMilliBel: Short) {
        bandUpdateJob?.cancel()
        bandUpdateJob = viewModelScope.launch {
            val current = equalizerBands.value
            val next = current.toMutableList()
            while (next.size < 5) next.add(0.toShort())
            if (bandIndex in next.indices) {
                next[bandIndex] = levelMilliBel
                settingsDataStore.saveEqualizerBands(next)
            }
        }
    }

    fun resetEqualizer() {
        bandUpdateJob?.cancel()
        bassJob?.cancel()
        viewModelScope.launch {
            settingsDataStore.saveEqualizerBands(listOf<Short>(0, 0, 0, 0, 0))
            settingsDataStore.saveBassBoostStrength(0)
        }
    }

    fun setBassBoost(strength: Int) {
        bassJob?.cancel()
        bassJob = viewModelScope.launch { settingsDataStore.saveBassBoostStrength(strength) }
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
                    // Industry-standard threshold: require at least 15s of actual playback
                    // before recording to play_history. Rapid skips cancel this job via collectLatest
                    // so play history and Top Artists are never polluted by skipped tracks.
                    while (currentCoroutineContext().isActive) {
                        delay(1_000L)
                        val pos = player.positionMs.value
                        val dur = player.durationMs.value
                        val thresholdMet = pos >= 15_000L || (dur in 1..20_000L && pos >= dur / 2)
                        if (player.isPlaying.value && thresholdMet) {
                            runSuspendCatching { libraryRepository.recordPlay(song) }
                            break
                        }
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
        val currentLiked = _isLiked.value
        _isLiked.value = !currentLiked // optimistic heart animation
        viewModelScope.launch {
            runSuspendCatching { libraryRepository.toggleLike(song) }
                .onFailure {
                    if (player.currentSong.value?.id == song.id) {
                        _isLiked.value = currentLiked // revert on failure only if still same song
                    }
                }
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

    private var seekbarStyleJob: Job? = null
    fun setSeekbarStyle(style: String) {
        seekbarStyleJob?.cancel()
        seekbarStyleJob = viewModelScope.launch { settingsDataStore.saveSeekbarStyle(style) }
    }

    private var turntableModeJob: Job? = null
    fun setTurntableMode(enabled: Boolean) {
        turntableModeJob?.cancel()
        turntableModeJob = viewModelScope.launch { settingsDataStore.saveTurntableMode(enabled) }
    }

    private var playerDeckJob: Job? = null
    fun setPlayerDeckStyle(deckId: String) {
        playerDeckJob?.cancel()
        playerDeckJob = viewModelScope.launch { settingsDataStore.savePlayerDeckStyle(deckId) }
    }
}
