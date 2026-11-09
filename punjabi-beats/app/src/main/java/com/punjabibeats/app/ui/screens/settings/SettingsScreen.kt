package com.punjabibeats.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punjabibeats.app.BuildConfig
import com.punjabibeats.app.ui.theme.AccentGold
import com.punjabibeats.app.ui.theme.BgBase
import com.punjabibeats.app.ui.theme.SurfaceBase
import com.punjabibeats.app.ui.theme.TextSecondary

/**
 * Playback & audio settings (the Metrolist audio-customization pattern):
 * tempo/pitch, skip silence, crossfade, equalizer, bass boost and virtualizer.
 * Values persist to DataStore; the playback service applies them live.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val speed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val pitch by viewModel.playbackPitch.collectAsStateWithLifecycle()
    val bands by viewModel.bands.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        SectionCard(title = "Playback") {
            LabeledSlider(
                label = "Tempo",
                value = speed.coerceIn(0.5f, 2f),
                valueLabel = "%.1f×".format(speed),
                valueRange = 0.5f..2f,
                steps = 14,
                onValueChangeFinished = null,
                onValueChange = { viewModel.setPlaybackSpeed((it * 10).toInt() / 10f) },
            )
            LabeledSlider(
                label = "Pitch",
                value = pitch.coerceIn(0.5f, 2f),
                valueLabel = "%.1f".format(pitch),
                valueRange = 0.5f..2f,
                steps = 14,
                onValueChangeFinished = null,
                onValueChange = { viewModel.setPlaybackPitch((it * 10).toInt() / 10f) },
            )
            ToggleRow(
                label = "Skip silence",
                subtitle = "Jump over long silent passages automatically",
                checked = uiState.skipSilence,
                onCheckedChange = viewModel::setSkipSilence,
            )
            LabeledSlider(
                label = "Crossfade",
                value = uiState.crossfadeSeconds.toFloat(),
                valueLabel = if (uiState.crossfadeSeconds == 0) "Off" else "${uiState.crossfadeSeconds}s",
                valueRange = 0f..12f,
                steps = 11,
                onValueChangeFinished = null,
                onValueChange = { viewModel.setCrossfade(it.toInt()) },
            )
        }

        SectionCard(title = "Audio effects") {
            ToggleRow(
                label = "Equalizer",
                subtitle = if (bands.isEmpty()) {
                    "Start playing a track to tune the live equalizer"
                } else {
                    "${bands.size}-band equalizer bound to the active audio session"
                },
                checked = uiState.equalizerEnabled,
                onCheckedChange = viewModel::setEqualizerEnabled,
            )
            if (uiState.equalizerEnabled) {
                bands.forEachIndexed { index, band ->
                    LabeledSlider(
                        label = band.label,
                        value = band.level.toFloat(),
                        valueLabel = "${band.level}",
                        valueRange = band.minLevel.toFloat()..band.maxLevel.toFloat().coerceAtLeast(band.minLevel.toFloat() + 1f),
                        steps = 0,
                        onValueChangeFinished = null,
                        onValueChange = { viewModel.setBandLevel(index, it.toInt()) },
                    )
                }
            }
            LabeledSlider(
                label = "Bass boost",
                value = uiState.bassBoost.toFloat(),
                valueLabel = if (uiState.bassBoost == 0) "Off" else "${(uiState.bassBoost * 100) / 1000}%",
                valueRange = 0f..1000f,
                steps = 9,
                onValueChangeFinished = null,
                onValueChange = { viewModel.setBassBoost(it.toInt()) },
            )
            LabeledSlider(
                label = "Surround (virtualizer)",
                value = uiState.virtualizer.toFloat(),
                valueLabel = if (uiState.virtualizer == 0) "Off" else "${(uiState.virtualizer * 100) / 1000}%",
                valueRange = 0f..1000f,
                steps = 9,
                onValueChangeFinished = null,
                onValueChange = { viewModel.setVirtualizer(it.toInt()) },
            )
        }

        SectionCard(title = "About") {
            Text(
                "Punjabi Beats ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Architecture inspired by Metrolist (GPL-3.0) — the innertube module, " +
                    "player connection pattern and audio customization. Stream deciphering " +
                    "inspired by zemer-cipher (GPL-3.0) — WebView cipher deobfuscation and " +
                    "self-healing player configs. Both are re-implemented from scratch for " +
                    "this project; no source code is copied.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = AccentGold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
    Surface(
        color = SurfaceBase,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) { content() }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = AccentGold),
        )
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueLabel: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)?,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.bodyMedium, color = AccentGold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = AccentGold, activeTrackColor = AccentGold),
        )
    }
}
