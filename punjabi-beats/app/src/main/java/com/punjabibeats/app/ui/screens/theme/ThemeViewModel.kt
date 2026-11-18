package com.punjabibeats.app.ui.screens.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punjabibeats.app.data.local.datastore.SettingsDataStore
import com.punjabibeats.app.ui.theme.PBTheme
import com.punjabibeats.app.ui.theme.pbThemeById
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val settings: SettingsDataStore,
) : ViewModel() {

    val selectedTheme: StateFlow<PBTheme> = settings.themeId()
        .map { pbThemeById(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = pbThemeById("classic"),
        )

    val seekbarStyle: StateFlow<String> = settings.seekbarStyle()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "classic_waveform")

    val isTurntableMode: StateFlow<Boolean> = settings.turntableMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private var themeJob: Job? = null
    private var seekbarJob: Job? = null
    private var turntableJob: Job? = null

    fun selectTheme(theme: PBTheme) {
        themeJob?.cancel()
        themeJob = viewModelScope.launch {
            settings.saveThemeId(theme.id)
        }
    }

    fun selectSeekbarStyle(style: String) {
        seekbarJob?.cancel()
        seekbarJob = viewModelScope.launch {
            settings.saveSeekbarStyle(style)
        }
    }

    fun toggleTurntableMode(enabled: Boolean) {
        turntableJob?.cancel()
        turntableJob = viewModelScope.launch {
            settings.saveTurntableMode(enabled)
        }
    }
}
