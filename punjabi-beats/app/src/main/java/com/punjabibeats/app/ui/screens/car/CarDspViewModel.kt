package com.punjabibeats.app.ui.screens.car

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punjabibeats.app.data.local.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CarDspViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CarDspState())
    val uiState: StateFlow<CarDspState> = _uiState.asStateFlow()

    fun selectCabinProfile(profile: CabinProfile) {
        _uiState.update {
            it.copy(
                cabinProfile = profile,
                driverDelayMs = profile.defaultSubwooferDelayMs,
                gaddi808SlamDb = profile.acousticBoostDb,
            )
        }
    }

    fun setDriverDelayMs(delayMs: Float) {
        _uiState.update { it.copy(driverDelayMs = delayMs.coerceIn(0f, 3.5f)) }
    }

    fun setSubwooferPhase(phaseDeg: Int) {
        _uiState.update { it.copy(subwooferPhaseDeg = if (phaseDeg == 180) 180 else 0) }
    }

    fun setGaddi808SlamDb(gainDb: Float) {
        _uiState.update { it.copy(gaddi808SlamDb = gainDb.coerceIn(0f, 12f)) }
    }

    fun setHighwayNoiseLeveler(enabled: Boolean) {
        _uiState.update { it.copy(highwayNoiseLevelerEnabled = enabled) }
    }

    fun setDspCalibratedActive(active: Boolean) {
        _uiState.update { it.copy(isDspCalibratedActive = active) }
    }

    fun runAcousticSweep() {
        if (_uiState.value.isAcousticSweepRunning) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAcousticSweepRunning = true, sweepProgress = 0f) }
            for (step in 1..10) {
                delay(250)
                _uiState.update { it.copy(sweepProgress = step / 10f) }
            }
            _uiState.update { it.copy(isAcousticSweepRunning = false, sweepProgress = 1f) }
        }
    }
}
