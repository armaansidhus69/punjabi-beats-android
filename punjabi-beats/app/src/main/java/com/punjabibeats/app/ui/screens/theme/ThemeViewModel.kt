package com.punjabibeats.app.ui.screens.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punjabibeats.app.data.local.datastore.SettingsDataStore
import com.punjabibeats.app.ui.theme.PBTheme
import com.punjabibeats.app.ui.theme.pbThemeById
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
            initialValue = pbThemeById("punjab"),
        )

    fun selectTheme(theme: PBTheme) {
        viewModelScope.launch {
            settings.saveThemeId(theme.id)
        }
    }
}
