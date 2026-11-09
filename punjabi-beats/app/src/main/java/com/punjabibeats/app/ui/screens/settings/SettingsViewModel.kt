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

    /** EQ band descriptors from the live audio session; empty before first playback. */
    val bands: StateFlow<List<EqBandUi>> = settings.equalizerBands().map { saved ->
        val range = effects.bandRange
        val max = range.toInt()
        val min = -max
        if (effects.bandCount <= 0) {
            emptyList()
        } else {
            (0 until effects.bandCount).map { band ->
                EqBandUi(
                    label = effects.bandFrequencies().getOrNull(band) ?: "${band + 1}",
                    level = saved.getOrNull(band)?.toInt() ?: 0,
                    minLevel = min,
                    maxLevel = max,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setCrossfade(seconds: Int) = viewModelScope.launch { settings.saveCrossfadeSeconds(seconds) }
    fun setSkipSilence(enabled: Boolean) = player.setSkipSilence(enabled)
    fun setPlaybackSpeed(speed: Float) = player.setPlaybackSpeed(speed)
    fun setPlaybackPitch(pitch: Float) = player.setPlaybackPitch(pitch)
    fun setEqualizerEnabled(enabled: Boolean) =
        viewModelScope.launch { settings.saveEqualizerEnabled(enabled) }

    fun setBandLevel(index: Int, level: Int) = viewModelScope.launch {
        val current = settings.equalizerBands().first()
        val next = MutableList(maxOf(current.size, index + 1)) { i -> current.getOrNull(i) ?: 0.toShort() }
        next[index] = level.toShort()
        settings.saveEqualizerBands(next)
    }

    fun setBassBoost(strength: Int) =
        viewModelScope.launch { settings.saveBassBoostStrength(strength) }

    fun setVirtualizer(strength: Int) =
        viewModelScope.launch { settings.saveVirtualizerStrength(strength) }
}
