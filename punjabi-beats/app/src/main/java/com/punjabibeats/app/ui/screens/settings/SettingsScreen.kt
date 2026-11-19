package com.punjabibeats.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punjabibeats.app.BuildConfig
import com.punjabibeats.app.ui.theme.LocalPBTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/**
 * Playback & audio settings (the Metrolist audio-customization pattern):
 * tempo/pitch, skip silence, crossfade, equalizer, bass boost and virtualizer.
 * Values persist to DataStore; the playback service applies them live.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenThemes: (() -> Unit)? = null,
    onOpenEqualizer: (() -> Unit)? = null,
    onOpenCarMode: (() -> Unit)? = null,
    onOpenCarDsp: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val speed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val pitch by viewModel.playbackPitch.collectAsStateWithLifecycle()
    val bands by viewModel.bands.collectAsStateWithLifecycle()
    val activeThemeId by viewModel.currentThemeId.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val playerDeckStyle by viewModel.playerDeckStyle.collectAsStateWithLifecycle()
    val dynamicIslandEnabled by viewModel.dynamicIslandEnabled.collectAsStateWithLifecycle()
    val carBtAutoLaunch by viewModel.carBtAutoLaunch.collectAsStateWithLifecycle()
    val streamingQuality by viewModel.streamingQuality.collectAsStateWithLifecycle()
    val dolbyAtmos by viewModel.dolbyAtmos.collectAsStateWithLifecycle()
    val lyricsAlignment by viewModel.lyricsAlignment.collectAsStateWithLifecycle()
    val lyricsTextSize by viewModel.lyricsTextSize.collectAsStateWithLifecycle()
    val lyricsLineSpacing by viewModel.lyricsLineSpacing.collectAsStateWithLifecycle()
    val touchFeedback by viewModel.touchFeedback.collectAsStateWithLifecycle()
    val heartbeatFeedback by viewModel.heartbeatFeedback.collectAsStateWithLifecycle()
    val theme = LocalPBTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = theme.text)
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text(
                    "Settings",
                    color = theme.text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Audio, playback & appearance preferences",
                    color = theme.muted,
                    fontSize = 12.sp,
                )
            }
        }

        SectionCard(title = "Appearance") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.card)
                    .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .clickable { onOpenThemes?.invoke() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(theme.surface)
                        .border(1.5.dp, theme.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(theme.emoji, fontSize = 22.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Theme Studio",
                        color = theme.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Text(
                        "${theme.name} • ${theme.tagline}",
                        color = theme.muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.Default.Palette,
                    contentDescription = "Theme Studio",
                    tint = theme.accent,
                )
            }

            Spacer(Modifier.height(10.dp))

            // Quick Theme Switcher: Horizontally scrollable row so all 7 themes display with generous touch targets without clipping
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp),
            ) {
                items(com.punjabibeats.app.ui.theme.PBThemes.size) { index ->
                    val th = com.punjabibeats.app.ui.theme.PBThemes[index]
                    val isSelected = activeThemeId.equals(th.id, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .width(96.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) th.accent.copy(alpha = 0.22f) else theme.card)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) th.accent else theme.muted.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(14.dp),
                            )
                            .clickable { viewModel.selectTheme(th.id) }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(th.emoji, fontSize = 20.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                th.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) th.accent else theme.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("Appearance Mode", color = theme.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val modes = listOf("system" to "⚙️ System", "dark" to "🌙 Dark", "light" to "☀️ Light")
                modes.forEach { (modeKey, modeTitle) ->
                    val isSelected = themeMode.equals(modeKey, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) theme.accent.copy(alpha = 0.22f) else theme.surface)
                            .border(1.dp, if (isSelected) theme.accent else theme.panel2, RoundedCornerShape(12.dp))
                            .clickable { viewModel.setThemeMode(modeKey) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            modeTitle,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) theme.accent else theme.text,
                        )
                    }
                }
            }
        }

        SectionCard(title = "Player Decks (6 Styles)") {
            Text(
                "Select your signature playback deck with hardware-accelerated dithering",
                color = theme.muted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(10.dp))
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(com.punjabibeats.app.ui.screens.player.decks.PlayerDeck.entries) { deck ->
                    val isSelected = playerDeckStyle.equals(deck.id, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) theme.accent.copy(alpha = 0.22f) else theme.card)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) theme.accent else theme.panel2,
                                shape = RoundedCornerShape(14.dp),
                            )
                            .clickable { viewModel.setPlayerDeckStyle(deck.id) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(deck.iconEmoji, fontSize = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                deck.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) theme.accent else theme.text,
                            )
                        }
                    }
                }
            }
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
            if (onOpenEqualizer != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.panel2)
                        .clickable { onOpenEqualizer.invoke() }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "10-Band Sonic Equalizer Studio",
                            color = theme.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "DSP Matrix • Gaddi 808 & Dhol Presets",
                            color = theme.muted,
                            fontSize = 12.sp,
                        )
                    }
                    Text(
                        text = "OPEN ›",
                        color = theme.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            if (onOpenCarMode != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.panel2)
                        .clickable { onOpenCarMode.invoke() }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Car Dashboard Mode",
                            color = theme.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Driver HUD • 80dp Transport • Fast Driver Picks",
                            color = theme.muted,
                            fontSize = 12.sp,
                        )
                    }
                    Text(
                        text = "DRIVE ›",
                        color = theme.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            if (onOpenCarDsp != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.panel2)
                        .clickable { onOpenCarDsp.invoke() }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Car DSP Acoustic Calibration",
                            color = theme.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Soundstage Fader • Cabin Delays • 68Hz Null Fix",
                            color = theme.muted,
                            fontSize = 12.sp,
                        )
                    }
                    Text(
                        text = "CALIBRATE ›",
                        color = theme.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
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
                    val db = band.level / 100
                    LabeledSlider(
                        label = band.label,
                        value = band.level.toFloat(),
                        valueLabel = "${if (db > 0) "+$db" else "$db"} dB",
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

        SectionCard(title = "Audio & Streaming Engine") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Wi-Fi Audio Streaming",
                        style = MaterialTheme.typography.bodyLarge,
                        color = theme.text,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        streamingQuality,
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.accent,
                    )
                }
            }
            ToggleRow(
                label = "Dolby Atmos • Spatial Desi Sound",
                subtitle = "3D acoustic tuning for AirPods & headphones",
                checked = dolbyAtmos,
                onCheckedChange = viewModel::setDolbyAtmos,
            )
            ToggleRow(
                label = "Auto-Launch Car Mode on Bluetooth",
                subtitle = "Automatically open driver dashboard when connected to vehicle",
                checked = carBtAutoLaunch,
                onCheckedChange = viewModel::setCarBtAutoLaunch,
            )
        }

        SectionCard(title = "Lyrics & Teleprompter Typography") {
            Text(
                "Text Alignment",
                style = MaterialTheme.typography.bodyLarge,
                color = theme.text,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    Pair("LEFT", "⇤ Left"),
                    Pair("CENTER", "≣ Center"),
                    Pair("RIGHT", "⇥ Right"),
                ).forEach { (id, label) ->
                    val isSelected = lyricsAlignment == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) theme.accent.copy(alpha = 0.22f) else theme.surface)
                            .border(1.dp, if (isSelected) theme.accent else theme.panel2, RoundedCornerShape(12.dp))
                            .clickable { viewModel.setLyricsAlignment(id) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) theme.accent else theme.text,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Font Size", style = MaterialTheme.typography.bodyMedium, color = theme.text)
                Text("${lyricsTextSize.toInt()} sp", style = MaterialTheme.typography.bodyMedium, color = theme.secondary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = lyricsTextSize,
                onValueChange = viewModel::setLyricsTextSize,
                valueRange = 16f..32f,
                steps = 0,
                colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent),
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Line Spacing", style = MaterialTheme.typography.bodyMedium, color = theme.text)
                Text("${lyricsLineSpacing.toInt()} dp", style = MaterialTheme.typography.bodyMedium, color = theme.secondary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = lyricsLineSpacing,
                onValueChange = viewModel::setLyricsLineSpacing,
                valueRange = 4f..24f,
                steps = 0,
                colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent),
            )

            Spacer(Modifier.height(12.dp))

            // Live Interactive Teleprompter Preview Card
            Text(
                "Live Teleprompter Preview",
                style = MaterialTheme.typography.labelLarge,
                color = theme.muted,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            val previewTextAlign = when (lyricsAlignment) {
                "CENTER" -> TextAlign.Center
                "RIGHT" -> TextAlign.End
                else -> TextAlign.Start
            }
            var activePreviewLine by remember { mutableIntStateOf(1) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(lyricsLineSpacing.dp),
                ) {
                    val sampleLines = listOf(
                        "ਮਿੱਤਰਾਂ ਦਾ ਨਾਂ ਚੱਲਦਾ, ਜਿਵੇਂ ਚੱਲਦਾ ਸਿੱਕਾ ਬਰੈਂਡਡ",
                        "ਦਿਲ ਦਾ ਨੀ ਮਾੜਾ ਤੇਰਾ ਸਿੱਧੂ ਮੂਸੇ ਵਾਲਾ",
                        "ਸਦਾ ਚੜ੍ਹਦੀ ਕਲਾ ਦੇ ਵਿੱਚ ਰਹਿੰਦੇ ਆਂ",
                    )
                    sampleLines.forEachIndexed { idx, line ->
                        val isActive = idx == activePreviewLine
                        Text(
                            text = line,
                            fontSize = (if (isActive) lyricsTextSize else lyricsTextSize * 0.82f).sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isActive) theme.accent else theme.muted.copy(alpha = 0.7f),
                            textAlign = previewTextAlign,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activePreviewLine = idx }
                                .padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }

        SectionCard(title = "Touch & Haptic Feedback") {
            ToggleRow(
                label = "Touch Haptics on Sliders",
                subtitle = "Subtle tactile click when dragging seekbars and sliders",
                checked = touchFeedback,
                onCheckedChange = viewModel::setTouchFeedback,
            )
            ToggleRow(
                label = "Heartbeat Vibration Mode",
                subtitle = "Pulsing rhythm vibration along seekbar and bass peaks",
                checked = heartbeatFeedback,
                onCheckedChange = viewModel::setHeartbeatFeedback,
            )
        }

        SectionCard(title = "About") {
            Text(
                "Punjabi Beats ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium,
                color = theme.text,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Architecture inspired by Metrolist (GPL-3.0) — the innertube module, " +
                    "player connection pattern and audio customization. Stream deciphering " +
                    "inspired by zemer-cipher (GPL-3.0) — WebView cipher deobfuscation and " +
                    "self-healing player configs. Both are re-implemented from scratch for " +
                    "this project; no source code is copied.",
                style = MaterialTheme.typography.bodySmall,
                color = theme.muted,
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
    val theme = LocalPBTheme.current
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = theme.text,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
    Surface(
        color = theme.card,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, theme.panel2.copy(alpha = 0.7f), RoundedCornerShape(18.dp)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
    val theme = LocalPBTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = theme.text, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = theme.muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = theme.accent,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = theme.surface,
                uncheckedThumbColor = theme.muted,
            ),
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
    val theme = LocalPBTheme.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = theme.text, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.bodyMedium, color = theme.secondary, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = 0,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = theme.accent,
                activeTrackColor = theme.accent,
                inactiveTrackColor = theme.muted.copy(alpha = 0.25f),
            ),
        )
    }
}
