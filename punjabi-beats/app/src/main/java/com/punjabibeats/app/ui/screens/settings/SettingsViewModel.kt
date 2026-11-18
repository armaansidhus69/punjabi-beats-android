package com.punjabibeats.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punjabibeats.app.data.local.datastore.SettingsDataStore
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.playback.audio.AudioEffectsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Audio-effect band state for the equalizer UI. */
data class EqBandUi(
    val label: String,
    val level: Int,
    val minLevel: Int,
    val maxLevel: Int,
)

data class SettingsUiState(
    val crossfadeSeconds: Int = 0,
    val skipSilence: Boolean = false,
    val equalizerEnabled: Boolean = false,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
)

/**
 * Settings ViewModel: exposes the persisted audio preferences (Metrolist-style
 * playback customization) and mirrors player-side values (tempo/pitch) through
 * the player controller. Effect band levels live in DataStore; the service
 * applies them to the live audio session.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsDataStore,
    val player: MusicPlayerController,
    private val effects: AudioEffectsManager,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.crossfadeSeconds(),
        settings.skipSilence(),
        settings.equalizerEnabled(),
        settings.bassBoostStrength(),
        settings.virtualizerStrength(),
    ) { crossfade, skipSilence, eqEnabled, bass, virtualizer ->
        SettingsUiState(crossfade, skipSilence, eqEnabled, bass, virtualizer)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    val playbackSpeed: StateFlow<Float> = player.playbackSpeed
    val playbackPitch: StateFlow<Float> = player.playbackPitch

    val currentThemeId: StateFlow<String> = settings.themeId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "sunset_glow")

    val themeMode: StateFlow<String> = settings.themeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "system")

    val playerDeckStyle: StateFlow<String> = settings.playerDeckStyle()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "classic")

    val dynamicIslandEnabled: StateFlow<Boolean> = settings.dynamicIslandEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val carBtAutoLaunch: StateFlow<Boolean> = settings.carBtAutoLaunch()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val streamingQuality: StateFlow<String> = settings.streamingQuality()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Hi-Res Lossless (24-bit / 96kHz)")

    val dolbyAtmos: StateFlow<Boolean> = settings.dolbyAtmos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val lyricsAlignment: StateFlow<String> = settings.lyricsAlignment()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "LEFT")

    val lyricsTextSize: StateFlow<Float> = settings.lyricsTextSize()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 22f)

    val lyricsLineSpacing: StateFlow<Float> = settings.lyricsLineSpacing()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 10f)

    val touchFeedback: StateFlow<Boolean> = settings.touchFeedback()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val heartbeatFeedback: StateFlow<Boolean> = settings.heartbeatFeedback()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setLyricsAlignment(alignment: String) = viewModelScope.launch {
        settings.saveLyricsAlignment(alignment)
    }

    fun setLyricsTextSize(size: Float) = viewModelScope.launch {
        settings.saveLyricsTextSize(size)
    }

    fun setLyricsLineSpacing(spacing: Float) = viewModelScope.launch {
        settings.saveLyricsLineSpacing(spacing)
    }

    fun setTouchFeedback(enabled: Boolean) = viewModelScope.launch {
        settings.saveTouchFeedback(enabled)
    }

    fun setHeartbeatFeedback(enabled: Boolean) = viewModelScope.launch {
        settings.saveHeartbeatFeedback(enabled)
    }

    fun selectTheme(themeId: String) {
        viewModelScope.launch { runCatching { settings.saveThemeId(themeId) } }
    }

    fun setThemeMode(mode: String) = viewModelScope.launch {
        settings.saveThemeMode(mode)
    }

    fun setPlayerDeckStyle(deckId: String) = viewModelScope.launch {
        settings.savePlayerDeckStyle(deckId)
    }

    fun setDynamicIslandEnabled(enabled: Boolean) = viewModelScope.launch {
        settings.saveDynamicIslandEnabled(enabled)
    }

    fun setCarBtAutoLaunch(enabled: Boolean) = viewModelScope.launch {
        settings.saveCarBtAutoLaunch(enabled)
    }

    fun setStreamingQuality(quality: String) = viewModelScope.launch {
        settings.saveStreamingQuality(quality)
    }

    fun setDolbyAtmos(enabled: Boolean) = viewModelScope.launch {
        settings.saveDolbyAtmos(enabled)
    }

    /** EQ band descriptors; falls back to standard 5-band configuration before first playback. */
    val bands: StateFlow<List<EqBandUi>> = settings.equalizerBands().map { saved ->
        val range = effects.bandRange
        val max = if (range > 0) range.toInt() else 1000
        val min = -max
        val count = if (effects.bandCount > 0) effects.bandCount else DEFAULT_BANDS.size
        (0 until count).map { band ->
            val label = if (effects.bandCount > 0) {
                effects.bandFrequencies().getOrNull(band) ?: "${band + 1}"
            } else {
                DEFAULT_BANDS.getOrElse(band) { "Band ${band + 1}" }
            }
            EqBandUi(
                label = label,
                level = saved.getOrNull(band)?.toInt() ?: 0,
                minLevel = min,
                maxLevel = max,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setCrossfade(seconds: Int) = viewModelScope.launch { settings.saveCrossfadeSeconds(seconds) }
    fun setSkipSilence(enabled: Boolean) = player.setSkipSilence(enabled)
    fun setPlaybackSpeed(speed: Float) = player.setPlaybackSpeed(speed)
    fun setPlaybackPitch(pitch: Float) = player.setPlaybackPitch(pitch)
    fun setEqualizerEnabled(enabled: Boolean) =
        viewModelScope.launch { settings.saveEqualizerEnabled(enabled) }

    private var bandUpdateJob: kotlinx.coroutines.Job? = null
    fun setBandLevel(index: Int, level: Int) {
        bandUpdateJob?.cancel()
        bandUpdateJob = viewModelScope.launch {
            val current = settings.equalizerBands().first()
            val next = MutableList(maxOf(current.size, index + 1)) { i -> current.getOrNull(i) ?: 0.toShort() }
            next[index] = level.toShort()
            settings.saveEqualizerBands(next)
        }
    }

    private var bassJob: kotlinx.coroutines.Job? = null
    fun setBassBoost(strength: Int) {
        bassJob?.cancel()
        bassJob = viewModelScope.launch { settings.saveBassBoostStrength(strength) }
    }

    private var virtualizerJob: kotlinx.coroutines.Job? = null
    fun setVirtualizer(strength: Int) {
        virtualizerJob?.cancel()
        virtualizerJob = viewModelScope.launch { settings.saveVirtualizerStrength(strength) }
    }

    private companion object {
        val DEFAULT_BANDS = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    }
}
