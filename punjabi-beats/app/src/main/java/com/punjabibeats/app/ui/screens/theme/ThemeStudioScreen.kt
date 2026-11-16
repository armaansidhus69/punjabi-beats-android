package com.punjabibeats.app.ui.screens.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punjabibeats.app.ui.components.sliders.SeekbarStyle
import com.punjabibeats.app.ui.components.sliders.SeekbarStylePickerSheet
import com.punjabibeats.app.ui.theme.LocalPBTheme
import com.punjabibeats.app.ui.theme.PBTheme
import com.punjabibeats.app.ui.theme.PBThemes

@Composable
fun ThemeStudioScreen(
    onBack: () -> Unit,
    viewModel: ThemeViewModel = hiltViewModel(),
) {
    val activeTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()
    val rawSeekbarStyle by viewModel.seekbarStyle.collectAsStateWithLifecycle()
    val currentSeekbarStyle = remember(rawSeekbarStyle) { SeekbarStyle.fromId(rawSeekbarStyle) }
    val isTurntableMode by viewModel.isTurntableMode.collectAsStateWithLifecycle()
    var showSeekbarSheet by remember { mutableStateOf(false) }
    val theme = LocalPBTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = theme.text,
                )
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text(
                    "Theme Studio",
                    color = theme.text,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Choose your music identity • 4 Premium Themes",
                    color = theme.muted,
                    fontSize = 13.sp,
                )
            }
        }

        // Active Theme Hero Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            theme.card,
                            theme.accent.copy(alpha = 0.28f),
                            theme.background,
                        )
                    )
                )
                .border(1.5.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(theme.card)
                        .border(2.dp, theme.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(activeTheme.emoji, fontSize = 28.sp)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "ACTIVE THEME",
                        color = theme.secondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        activeTheme.name,
                        color = theme.text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        activeTheme.tagline,
                        color = theme.muted,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 4 Premium Themes Selector List
        PBThemes.forEach { item ->
            val selected = item.id == activeTheme.id

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(item.surface)
                    .border(
                        if (selected) 2.dp else 1.dp,
                        if (selected) item.accent else item.card,
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { viewModel.selectTheme(item) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item.emoji, fontSize = 32.sp)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                ) {
                    Text(
                        item.name,
                        color = item.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    )
                    Text(
                        item.tagline,
                        color = item.muted,
                        fontSize = 11.sp,
                    )
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(
                            item.background,
                            item.surface,
                            item.card,
                            item.accent,
                            item.secondary,
                        ).forEach { swatchColor ->
                            Box(
                                modifier = Modifier
                                    .size(width = 25.dp, height = 8.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(0.5.dp, swatchColor.copy(alpha = 0.5f), CircleShape),
                            )
                        }
                    }
                }

                if (selected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = item.accent,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Turntable Vinyl Deck Mode Switcher Card
        Text(
            "Player Deck Style",
            color = theme.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(theme.surface)
                .border(1.dp, theme.panel2, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "📻 Turntable Vinyl Deck",
                    color = theme.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Displays rotating 12\" vinyl with animated metallic tonearm tracking the groove and 33/45 RPM speed",
                    color = theme.muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = isTurntableMode,
                onCheckedChange = { viewModel.toggleTurntableMode(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = theme.background,
                    checkedTrackColor = theme.accent,
                    uncheckedThumbColor = theme.muted,
                    uncheckedTrackColor = theme.panel2,
                ),
            )
        }

        Spacer(Modifier.height(20.dp))

        // 22 Progress Slider Collection Switcher Card
        Text(
            "Seekbar Slider Style",
            color = theme.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(theme.surface)
                .border(1.dp, theme.panel2, RoundedCornerShape(18.dp))
                .clickable { showSeekbarSheet = true }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${currentSeekbarStyle.iconEmoji} ${currentSeekbarStyle.displayName}",
                    color = theme.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    currentSeekbarStyle.description,
                    color = theme.muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.accent)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    "Change (22)",
                    color = theme.background,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Selected: ${activeTheme.name} • ${currentSeekbarStyle.displayName}",
            color = theme.accent,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
        Text(
            "Your theme and player settings are automatically saved on this device.",
            color = theme.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        if (showSeekbarSheet) {
            SeekbarStylePickerSheet(
                currentStyle = currentSeekbarStyle,
                onSelectStyle = { viewModel.selectSeekbarStyle(it.id) },
                onDismiss = { showSeekbarSheet = false },
            )
        }
    }
}
