package com.punjabibeats.app.ui.screens.equalizer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Stitch Color Tokens
private val StitchSurface = Color(0xFF121316)
private val StitchSurfaceLow = Color(0xFF1B1B1F)
private val StitchSurfaceContainer = Color(0xFF1F1F23)
private val StitchSurfaceVariant = Color(0xFF343538)
private val StitchPrimary = Color(0xFFCEBDFF)
private val StitchPrimaryContainer = Color(0xFFA78BFA)
private val StitchSecondary = Color(0xFFFFB95F) // Amber Dhol
private val StitchTertiary = Color(0xFF4EDEA3) // Mint/Cyan
private val StitchOnSurface = Color(0xFFE3E2E6)
private val StitchOnSurfaceVariant = Color(0xFFCAC4D4)
private val StitchOutline = Color(0xFF948E9D)

@Composable
fun EqualizerScreen(
    onNavigateBack: () -> Unit,
    viewModel: EqualizerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchSurface)
            .statusBarsPadding(),
    ) {
        // Top App Bar
        EqualizerTopBar(onNavigateBack = onNavigateBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Diagnostics Banner & Master Toggle
            DiagnosticsBanner(
                isEnabled = state.isEnabled,
                preAmpDb = state.preAmpDb,
                onToggleMaster = { viewModel.setMasterEnabled(!state.isEnabled) },
                onPreAmpChange = { viewModel.setPreAmpDb(it) },
            )

            // 2. Tuning Presets Horizontal Shelf
            TuningPresetsSection(
                selectedPresetId = state.selectedPresetId,
                onSelectPreset = { viewModel.selectPreset(it) },
                onResetFlat = { viewModel.resetToFlat() },
            )

            // 3. Real-Time Frequency Spline Visualizer (Canvas Curve)
            SpectrumResponseSpline(
                bandGainsDb = state.bandGainsDb,
                isEnabled = state.isEnabled,
            )

            // 4. 10-Band Graphic Equalizer Console (Interactive Sliders)
            TenBandMatrixConsole(
                bandGainsDb = state.bandGainsDb,
                bandFrequencies = state.bandFrequencies,
                bandSubLabels = state.bandSubLabels,
                isEnabled = state.isEnabled,
                onBandGainChange = { index, gain -> viewModel.setBandGain(index, gain) },
            )

            // 5. Specialized Desi Acoustic Enhancers Sub-Modules
            DesiAcousticEnhancersSection(
                bassBoostPercent = state.bassBoostPercent,
                dholTransientPercent = state.dholTransientPercent,
                virtualizerPercent = state.virtualizerPercent,
                isEnabled = state.isEnabled,
                onBassBoostChange = { viewModel.setBassBoostPercent(it) },
                onDholTransientChange = { viewModel.setDholTransientPercent(it) },
                onVirtualizerChange = { viewModel.setVirtualizerPercent(it) },
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun EqualizerTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = StitchOnSurface,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "10-Band Sonic Equalizer",
                color = StitchOnSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "DSP AUDIO ENGINE • ਸੁਰ ਸੰਗ੍ਰਹਿ",
                color = StitchOnSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(StitchPrimaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = StitchSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DiagnosticsBanner(
    isEnabled: Boolean,
    preAmpDb: Float,
    onToggleMaster: () -> Unit,
    onPreAmpChange: (Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StitchSurfaceLow)
            .border(1.dp, StitchSurfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isEnabled) StitchTertiary else StitchOutline),
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "DSP Matrix v3.4",
                                color = StitchOnSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(StitchTertiary.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "LOCK 96k",
                                    color = StitchTertiary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Text(
                            text = "32-bit Float • Punjabi Desi Acoustics Engine",
                            color = StitchOnSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }
                }

                // Master Power Switch
                Box(
                    modifier = Modifier
                        .size(width = 52.dp, height = 30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (isEnabled) StitchPrimaryContainer else StitchSurfaceVariant)
                        .clickable(onClick = onToggleMaster)
                        .padding(horizontal = 4.dp),
                    contentAlignment = if (isEnabled) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(StitchSurface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PowerSettingsNew,
                            contentDescription = "Master EQ",
                            tint = if (isEnabled) StitchPrimaryContainer else StitchOutline,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            // Pre-Amp Micro HUD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(StitchSurfaceContainer)
                    .padding(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = StitchSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Pre-Amp Level",
                                color = StitchOnSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            text = "${if (preAmpDb >= 0) "+" else ""}${String.format("%.1f", preAmpDb)} dB",
                            color = StitchSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Slider(
                        value = preAmpDb,
                        onValueChange = onPreAmpChange,
                        valueRange = -12f..12f,
                        enabled = isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = StitchSecondary,
                            activeTrackColor = StitchSecondary,
                            inactiveTrackColor = StitchSurfaceVariant,
                        ),
                        modifier = Modifier.height(24.dp),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("-12dB", color = StitchOutline, fontSize = 10.sp)
                        Text("0dB (Unity)", color = StitchOutline, fontSize = 10.sp)
                        Text("+12dB", color = StitchOutline, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TuningPresetsSection(
    selectedPresetId: String,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onResetFlat: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Tuning Presets",
                    color = StitchOnSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "/ ਸੁਰ ਸੰਗ੍ਰਹਿ",
                    color = StitchOnSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
            Text(
                text = "Flat Reset",
                color = StitchPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onResetFlat),
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(EqualizerPreset.entries) { preset ->
                val isSelected = preset.id == selectedPresetId
                val icon = when (preset) {
                    EqualizerPreset.GADDI_808 -> Icons.Rounded.Speaker
                    EqualizerPreset.LIVE_DHOL_PUNCH -> Icons.Rounded.Celebration
                    EqualizerPreset.VOCAL_TUMBI -> Icons.Rounded.Mic
                    EqualizerPreset.DESI_DRILL_SKITTER -> Icons.Rounded.Speed
                    EqualizerPreset.LATE_NIGHT_SUFI -> Icons.Rounded.Nightlight
                    EqualizerPreset.STUDIO_FLAT -> Icons.Rounded.HorizontalRule
                    EqualizerPreset.CUSTOM -> Icons.Rounded.Tune
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) StitchPrimaryContainer else StitchSurfaceContainer,
                        )
                        .border(
                            1.dp,
                            if (isSelected) StitchPrimary else StitchSurfaceVariant,
                            RoundedCornerShape(20.dp),
                        )
                        .clickable { onSelectPreset(preset) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) StitchSurface else StitchOnSurface,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = preset.displayName,
                            color = if (isSelected) StitchSurface else StitchOnSurface,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpectrumResponseSpline(
    bandGainsDb: List<Float>,
    isEnabled: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0E11))
            .border(1.dp, StitchSurfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "SPECTRUM RESPONSE",
                        color = StitchPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isEnabled) StitchPrimary else StitchOutline),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("+12 dB", color = StitchOutline, fontSize = 10.sp)
                    Text("0 dB", color = StitchOutline, fontSize = 10.sp)
                    Text("-12 dB", color = StitchOutline, fontSize = 10.sp)
                }
            }

            // Real Canvas Spline
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
            ) {
                val width = size.width
                val height = size.height

                // Draw horizontal guide lines
                val centerY = height / 2f
                val plus12Y = height * 0.15f
                val minus12Y = height * 0.85f

                drawLine(
                    color = StitchSurfaceVariant,
                    start = Offset(0f, plus12Y),
                    end = Offset(width, plus12Y),
                    strokeWidth = 1f,
                )
                drawLine(
                    color = StitchOutline.copy(alpha = 0.5f),
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 1.5f,
                )
                drawLine(
                    color = StitchSurfaceVariant,
                    start = Offset(0f, minus12Y),
                    end = Offset(width, minus12Y),
                    strokeWidth = 1f,
                )

                if (bandGainsDb.isEmpty()) return@Canvas

                val count = bandGainsDb.size
                val stepX = width / (count - 1).coerceAtLeast(1)

                val points = bandGainsDb.mapIndexed { index, gain ->
                    val x = index * stepX
                    // Map +12dB to plus12Y, -12dB to minus12Y
                    val normalized = (gain.coerceIn(-12f, 12f) - (-12f)) / 24f // 0.0 to 1.0
                    val y = minus12Y - (normalized * (minus12Y - plus12Y))
                    Offset(x, y)
                }

                // Draw filled gradient area under curve
                val fillPath = Path().apply {
                    moveTo(0f, height)
                    lineTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                    lineTo(width, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            StitchPrimary.copy(alpha = if (isEnabled) 0.35f else 0.1f),
                            StitchPrimaryContainer.copy(alpha = if (isEnabled) 0.12f else 0.05f),
                            Color.Transparent,
                        ),
                    ),
                )

                // Draw the stroke curve
                val strokePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                }

                drawPath(
                    path = strokePath,
                    color = if (isEnabled) StitchPrimary else StitchOutline,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )

                // Draw peak node circles
                points.forEachIndexed { idx, point ->
                    val nodeColor = when {
                        idx < 2 -> StitchSecondary // Amber sub
                        idx < 8 -> StitchPrimary // Violet mid
                        else -> StitchTertiary // Cyan air
                    }
                    drawCircle(
                        color = if (isEnabled) nodeColor else StitchOutline,
                        radius = 4.dp.toPx(),
                        center = point,
                    )
                }
            }

            // Frequency spectrum classification
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("SUB", color = StitchSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("BASS", color = StitchSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("MID", color = StitchPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("PRESENCE", color = StitchPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("AIR", color = StitchTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TenBandMatrixConsole(
    bandGainsDb: List<Float>,
    bandFrequencies: List<String>,
    bandSubLabels: List<String>,
    isEnabled: Boolean,
    onBandGainChange: (Int, Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StitchSurfaceContainer)
            .border(1.dp, StitchSurfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = StitchPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "10-Band Direct Matrix",
                        color = StitchOnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(StitchSurfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "ISO 1/1 OCT",
                        color = StitchOnSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // 10 Vertical Sliders Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                bandGainsDb.forEachIndexed { index, gain ->
                    val freq = bandFrequencies.getOrElse(index) { "" }
                    val label = bandSubLabels.getOrElse(index) { "" }
                    val accentColor = when {
                        index < 2 -> StitchSecondary
                        index < 8 -> StitchPrimary
                        else -> StitchTertiary
                    }

                    VerticalEqualizerSlider(
                        gainDb = gain,
                        frequency = freq,
                        subLabel = label,
                        accentColor = accentColor,
                        isEnabled = isEnabled,
                        onGainChange = { onBandGainChange(index, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalEqualizerSlider(
    gainDb: Float,
    frequency: String,
    subLabel: String,
    accentColor: Color,
    isEnabled: Boolean,
    onGainChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier.width(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Gain text
        Text(
            text = "${if (gainDb >= 0) "+" else ""}${String.format("%.1f", gainDb)}",
            color = if (isEnabled) accentColor else StitchOutline,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )

        // Custom Vertical Touch Slider Rail
        Box(
            modifier = Modifier
                .width(24.dp)
                .weight(1f)
                .pointerInput(isEnabled) {
                    if (!isEnabled) return@pointerInput
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaDb = -dragAmount / 8f
                        onGainChange((gainDb + deltaDb).coerceIn(-12f, 12f))
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val railWidth = 6.dp.toPx()
                val centerX = size.width / 2f
                val height = size.height
                val centerY = height / 2f

                // Inactive Background Track
                drawRoundRect(
                    color = StitchSurfaceVariant,
                    topLeft = Offset(centerX - railWidth / 2f, 0f),
                    size = androidx.compose.ui.geometry.Size(railWidth, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(railWidth / 2f),
                )

                // Center 0dB indicator tick
                drawLine(
                    color = StitchOutline,
                    start = Offset(centerX - 8.dp.toPx(), centerY),
                    end = Offset(centerX + 8.dp.toPx(), centerY),
                    strokeWidth = 2.dp.toPx(),
                )

                // Active Level fill
                val normalizedGain = (gainDb.coerceIn(-12f, 12f) - (-12f)) / 24f // 0 to 1
                val thumbY = height * (1f - normalizedGain)

                if (gainDb > 0) {
                    drawRect(
                        color = if (isEnabled) accentColor else StitchOutline,
                        topLeft = Offset(centerX - railWidth / 2f, thumbY),
                        size = androidx.compose.ui.geometry.Size(railWidth, centerY - thumbY),
                    )
                } else if (gainDb < 0) {
                    drawRect(
                        color = if (isEnabled) accentColor else StitchOutline,
                        topLeft = Offset(centerX - railWidth / 2f, centerY),
                        size = androidx.compose.ui.geometry.Size(railWidth, thumbY - centerY),
                    )
                }

                // Thumb Knob
                drawCircle(
                    color = StitchSurface,
                    radius = 9.dp.toPx(),
                    center = Offset(centerX, thumbY),
                )
                drawCircle(
                    color = if (isEnabled) accentColor else StitchOutline,
                    radius = 5.dp.toPx(),
                    center = Offset(centerX, thumbY),
                )
            }
        }

        // Frequency Labels
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = frequency,
                color = StitchOnSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subLabel,
                color = StitchOutline,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

@Composable
private fun DesiAcousticEnhancersSection(
    bassBoostPercent: Int,
    dholTransientPercent: Int,
    virtualizerPercent: Int,
    isEnabled: Boolean,
    onBassBoostChange: (Int) -> Unit,
    onDholTransientChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Desi Acoustic Enhancers",
                color = StitchOnSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "• DSP ADDONS",
                color = StitchSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
        }

        // 1. Bass Impact (Sub-808 Synth - GADDI MODE)
        EnhancerCard(
            title = "Bass Impact (Sub-808 Synth)",
            badge = "GADDI MODE",
            subtitle = "Harmonic sub-low generator for car audio systems",
            icon = Icons.Rounded.Speaker,
            accentColor = StitchSecondary,
            value = bassBoostPercent,
            isEnabled = isEnabled,
            onValueChange = onBassBoostChange,
        )

        // 2. Transient Shaper (Dhol Snap)
        EnhancerCard(
            title = "Transient Shaper (Dhol Snap)",
            badge = null,
            subtitle = "Acoustic envelope attack booster for barrel drum punch",
            icon = Icons.Rounded.Celebration,
            accentColor = StitchPrimary,
            value = dholTransientPercent,
            isEnabled = isEnabled,
            onValueChange = onDholTransientChange,
        )

        // 3. Stereo Spatial Widener
        EnhancerCard(
            title = "Stereo Spatial Widener",
            badge = "3D SOUND",
            subtitle = "Surround stage expansion for immersive playback",
            icon = Icons.Rounded.Speed,
            accentColor = StitchTertiary,
            value = virtualizerPercent,
            isEnabled = isEnabled,
            onValueChange = onVirtualizerChange,
        )
    }
}

@Composable
private fun EnhancerCard(
    title: String,
    badge: String?,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    value: Int,
    isEnabled: Boolean,
    onValueChange: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StitchSurfaceLow)
            .border(1.dp, StitchSurfaceVariant, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StitchSurfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isEnabled) accentColor else StitchOutline,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = title,
                                color = StitchOnSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (badge != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(accentColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = badge,
                                        color = accentColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                        Text(
                            text = subtitle,
                            color = StitchOnSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    }
                }
                Text(
                    text = "$value%",
                    color = if (isEnabled) accentColor else StitchOutline,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..100f,
                enabled = isEnabled,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = StitchSurfaceVariant,
                ),
                modifier = Modifier.height(20.dp),
            )
        }
    }
}
