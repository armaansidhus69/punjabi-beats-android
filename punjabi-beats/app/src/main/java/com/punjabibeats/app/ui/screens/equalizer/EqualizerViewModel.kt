package com.punjabibeats.app.ui.screens.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punjabibeats.app.data.local.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EqualizerState())
    val uiState: StateFlow<EqualizerState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsDataStore.equalizerEnabled(),
                settingsDataStore.equalizerBands(),
                settingsDataStore.bassBoostStrength(),
                settingsDataStore.virtualizerStrength(),
            ) { enabled, bandsMilliBel, bassStrength, virtStrength ->
                val gains = if (bandsMilliBel.isNotEmpty()) {
                    bandsMilliBel.map { it.toFloat() / 100f }
                } else {
                    EqualizerPreset.GADDI_808.bandGainsDb
                }
                // Pad or clamp to 10 bands
                val fullGains = (0 until 10).map { idx ->
                    gains.getOrElse(idx) { EqualizerPreset.GADDI_808.bandGainsDb.getOrElse(idx) { 0f } }
                }
                Triple(enabled, fullGains, Pair(bassStrength, virtStrength))
            }.collect { (enabled, gains, fx) ->
                val (bass, virt) = fx
                _uiState.update { current ->
                    current.copy(
                        isEnabled = enabled,
                        bandGainsDb = gains,
                        bassBoostPercent = (bass / 10).coerceIn(0, 100),
                        virtualizerPercent = (virt / 10).coerceIn(0, 100),
                    )
                }
            }
        }
    }

    fun setMasterEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isEnabled = enabled) }
        viewModelScope.launch {
            settingsDataStore.saveEqualizerEnabled(enabled)
        }
    }

    fun setPreAmpDb(db: Float) {
        _uiState.update { it.copy(preAmpDb = db.coerceIn(-12f, 12f)) }
    }

    fun selectPreset(preset: EqualizerPreset) {
        val newGains = preset.bandGainsDb
        _uiState.update {
            it.copy(
                selectedPresetId = preset.id,
                bandGainsDb = newGains,
                bassBoostPercent = preset.defaultBassBoostPercent,
            )
        }
        viewModelScope.launch {
            val milliBels = newGains.map { (it * 100).toInt().toShort() }
            settingsDataStore.saveEqualizerBands(milliBels)
            settingsDataStore.saveBassBoostStrength(preset.defaultBassBoostPercent * 10)
        }
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in 0 until 10) return
        val currentGains = _uiState.value.bandGainsDb.toMutableList()
        currentGains[bandIndex] = gainDb.coerceIn(-12f, 12f)
        _uiState.update {
            it.copy(
                selectedPresetId = EqualizerPreset.CUSTOM.id,
                bandGainsDb = currentGains,
            )
        }
        viewModelScope.launch {
            val milliBels = currentGains.map { (it * 100).toInt().toShort() }
            settingsDataStore.saveEqualizerBands(milliBels)
        }
    }

    fun setBassBoostPercent(percent: Int) {
        val safe = percent.coerceIn(0, 100)
        _uiState.update { it.copy(bassBoostPercent = safe) }
        viewModelScope.launch {
            settingsDataStore.saveBassBoostStrength(safe * 10)
        }
    }

    fun setVirtualizerPercent(percent: Int) {
        val safe = percent.coerceIn(0, 100)
        _uiState.update { it.copy(virtualizerPercent = safe) }
        viewModelScope.launch {
            settingsDataStore.saveVirtualizerStrength(safe * 10)
        }
    }

    fun setDholTransientPercent(percent: Int) {
        val safe = percent.coerceIn(0, 100)
        _uiState.update { it.copy(dholTransientPercent = safe) }
    }

    fun resetToFlat() {
        selectPreset(EqualizerPreset.STUDIO_FLAT)
    }
}
