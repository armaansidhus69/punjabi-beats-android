package com.punjabibeats.app.ui.screens.car

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.AirlineSeatReclineExtra
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Stitch Color Tokens
private val StitchSurface = Color(0xFF121316)
private val StitchSurfaceLow = Color(0xFF1B1B1F)
private val StitchSurfaceContainer = Color(0xFF1F1F23)
private val StitchSurfaceHigh = Color(0xFF292A2D)
private val StitchSurfaceHighest = Color(0xFF343538)
private val StitchPrimary = Color(0xFFCEBDFF)
private val StitchPrimaryContainer = Color(0xFFA78BFA)
private val StitchSecondary = Color(0xFFFFB95F) // Amber
private val StitchSecondaryContainer = Color(0xFFEE9800)
private val StitchTertiary = Color(0xFF4EDEA3) // Mint/Cyan
private val StitchOnSurface = Color(0xFFE3E2E6)
private val StitchOnSurfaceVariant = Color(0xFFCAC4D4)
private val StitchOutline = Color(0xFF948E9D)

@Composable
fun CarDspCalibrationScreen(
    onNavigateBack: () -> Unit,
    viewModel: CarDspViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchSurface)
            .statusBarsPadding(),
    ) {
        // Top Header
        DspTopBar(onNavigateBack = onNavigateBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Context Banner
            DspContextBanner()

            // 1. Bluetooth Audio Target Card
            BluetoothTargetCard(
                destinationName = state.destinationName,
                vehicleModel = state.vehicleModel,
                connectionSpec = state.connectionSpec,
                latencyMs = state.a2dpLatencyMs,
                outputLevelDb = state.outputLevelDb,
            )

            // 2. Cabin Profile Selector
            CabinProfileSection(
                selectedProfile = state.cabinProfile,
                onSelectProfile = { viewModel.selectCabinProfile(it) },
            )

            // 3. DSP Acoustic Calibration Suite
            DspCalibrationSuite(
                state = state,
                onRunSweep = { viewModel.runAcousticSweep() },
                onDelayChange = { viewModel.setDriverDelayMs(it) },
                onPhaseChange = { viewModel.setSubwooferPhase(it) },
                onBassSlamChange = { viewModel.setGaddi808SlamDb(it) },
                onHighwayNoiseToggle = { viewModel.setHighwayNoiseLeveler(it) },
            )

            // 4. Live A/B Listening Comparator
            LiveAbComparatorSection(
                isDspCalibrated = state.isDspCalibratedActive,
                onSelectMode = { viewModel.setDspCalibratedActive(it) },
            )

            // 5. Save Button
            Button(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StitchPrimaryContainer),
            ) {
                Text(
                    text = "Save DSP Vehicle Profile",
                    color = StitchSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun DspTopBar(onNavigateBack: () -> Unit) {
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
        Text(
            text = "Car DSP Calibration",
            color = StitchOnSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(StitchPrimaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.DirectionsCar,
                contentDescription = null,
                tint = StitchSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DspContextBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StitchSurfaceLow)
            .border(1.dp, StitchSurfaceHighest, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DirectionsCar,
                        contentDescription = null,
                        tint = StitchPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "ਗੱਡੀ ਆਡੀਓ ਟਿਊਨਿੰਗ",
                        color = StitchOnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "Bluetooth Master Link • Lossless DSP Calibration",
                    color = StitchOnSurfaceVariant,
                    fontSize = 11.sp,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(StitchSurfaceHighest)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(StitchTertiary),
                    )
                    Text(
                        text = "Thar_Pioneer_HD",
                        color = StitchTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun BluetoothTargetCard(
    destinationName: String,
    vehicleModel: String,
    connectionSpec: String,
    latencyMs: Int,
    outputLevelDb: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StitchSurfaceLow)
            .border(1.dp, StitchSurfaceHighest, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StitchSurfaceHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SpeakerGroup,
                            contentDescription = null,
                            tint = StitchPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "ACTIVE DESTINATION",
                            color = StitchSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                        Text(
                            text = destinationName,
                            color = StitchOnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "$vehicleModel • $connectionSpec",
                            color = StitchTertiary,
                            fontSize = 11.sp,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchSurfaceHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = "Sync",
                        tint = StitchPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // 3 Column Telemetry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TelemetryBox(
                    title = "A2DP LATENCY",
                    value = "$latencyMs",
                    unit = "ms",
                    accentColor = StitchTertiary,
                    modifier = Modifier.weight(1f),
                )
                TelemetryBox(
                    title = "OUTPUT LEVEL",
                    value = "+${outputLevelDb}",
                    unit = "dB",
                    accentColor = StitchSecondary,
                    modifier = Modifier.weight(1f),
                )
                TelemetryBox(
                    title = "STAGE HEADROOM",
                    value = "Safe 0-Clip",
                    unit = "",
                    accentColor = StitchTertiary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TelemetryBox(
    title: String,
    value: String,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(StitchSurfaceContainer)
            .padding(8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = StitchOnSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(value, color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Black)
                if (unit.isNotEmpty()) {
                    Text(unit, color = StitchOnSurfaceVariant, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun CabinProfileSection(
    selectedProfile: CabinProfile,
    onSelectProfile: (CabinProfile) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Vehicle Cabin Profile",
                color = StitchOnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "ACTIVE: ${selectedProfile.displayName}",
                color = StitchSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(CabinProfile.entries) { profile ->
                val isSelected = profile == selectedProfile
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) StitchPrimaryContainer else StitchSurfaceHigh)
                        .clickable { onSelectProfile(profile) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = profile.displayName,
                        color = if (isSelected) StitchSurface else StitchOnSurface,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }

        // Subwoofer profile card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(StitchSurfaceContainer)
                .padding(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Subwoofer System Profile",
                        color = StitchOnSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "IMPEDANCE 2Ω",
                        color = StitchTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "Dual 12-inch Sealed Trunk Subwoofers + 4 Focal Silk Dome Tweeters calibrated with custom phase crossover @ 85Hz.",
                    color = StitchOnSurfaceVariant,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun DspCalibrationSuite(
    state: CarDspState,
    onRunSweep: () -> Unit,
    onDelayChange: (Float) -> Unit,
    onPhaseChange: (Int) -> Unit,
    onBassSlamChange: (Float) -> Unit,
    onHighwayNoiseToggle: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                tint = StitchPrimary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "DSP Acoustic Calibration Suite",
                color = StitchOnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // Cabin Impulse Sweep Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(StitchSurfaceLow)
                .border(1.dp, StitchSurfaceHighest, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Cabin Impulse Sweep", color = StitchOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Phone mic listens to gaddi interior reflections", color = StitchOnSurfaceVariant, fontSize = 11.sp)
                    }
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = StitchTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Waveform Canvas (Acoustic Spectrum)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchSurfaceHighest),
                ) {
                    val w = size.width
                    val h = size.height

                    val strokePath = Path().apply {
                        moveTo(0f, h * 0.55f)
                        quadraticTo(w * 0.15f, h * 0.2f, w * 0.35f, h * 0.4f)
                        quadraticTo(w * 0.6f, h * 0.7f, w * 0.8f, h * 0.35f)
                        lineTo(w, h * 0.45f)
                    }
                    drawPath(
                        path = strokePath,
                        color = StitchTertiary,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                    // 68Hz Null Marker Circle
                    drawCircle(
                        color = StitchSecondary,
                        radius = 5.dp.toPx(),
                        center = Offset(w * 0.35f, h * 0.4f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("20Hz (Sub)", color = StitchOnSurfaceVariant, fontSize = 10.sp)
                    Text("68Hz Null Fix Applied", color = StitchSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("20kHz (Air)", color = StitchOnSurfaceVariant, fontSize = 10.sp)
                }

                Button(
                    onClick = onRunSweep,
                    enabled = !state.isAcousticSweepRunning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StitchSurfaceHigh),
                ) {
                    if (state.isAcousticSweepRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = StitchPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Measuring Cabin Reflections...", color = StitchOnSurface, fontSize = 12.sp)
                    } else {
                        Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, tint = StitchPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Run Pink Noise & Cabin Acoustic Sweep", color = StitchOnSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Driver Sweet Spot Focus Slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(StitchSurfaceLow)
                .border(1.dp, StitchSurfaceHighest, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Rounded.AirlineSeatReclineExtra, contentDescription = null, tint = StitchPrimary, modifier = Modifier.size(18.dp))
                        Text("Driver Sweet Spot Focus", color = StitchOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("${String.format("%.2f", state.driverDelayMs)} ms", color = StitchPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text("Delays front-passenger speakers to align center image at steering wheel", color = StitchOnSurfaceVariant, fontSize = 11.sp)
                Slider(
                    value = state.driverDelayMs,
                    onValueChange = onDelayChange,
                    valueRange = 0f..3.5f,
                    colors = SliderDefaults.colors(thumbColor = StitchPrimary, activeTrackColor = StitchPrimary, inactiveTrackColor = StitchSurfaceHighest),
                    modifier = Modifier.height(20.dp),
                )
            }
        }

        // Subwoofer Trunk Phase Inverter
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(StitchSurfaceLow)
                .border(1.dp, StitchSurfaceHighest, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Rounded.SwapVert, contentDescription = null, tint = StitchSecondary, modifier = Modifier.size(18.dp))
                        Text("Subwoofer Trunk Phase", color = StitchOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("180° eliminates cancellation from hatch boundary", color = StitchOnSurfaceVariant, fontSize = 11.sp)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(StitchSurfaceHighest)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PhaseButton(label = "0°", isSelected = state.subwooferPhaseDeg == 0, onClick = { onPhaseChange(0) })
                    PhaseButton(label = "180° Invert", isSelected = state.subwooferPhaseDeg == 180, onClick = { onPhaseChange(180) })
                }
            }
        }

        // Gaddi 808 Slam Compensation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(StitchSurfaceLow)
                .border(1.dp, StitchSurfaceHighest, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Rounded.GraphicEq, contentDescription = null, tint = StitchSecondary, modifier = Modifier.size(18.dp))
                        Text("Gaddi 808 Slam Compensation", color = StitchOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("+${String.format("%.1f", state.gaddi808SlamDb)} dB", color = StitchSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text("Dhol Dagga & Punjabi Drill Chassis Resonance", color = StitchOnSurfaceVariant, fontSize = 11.sp)
                Slider(
                    value = state.gaddi808SlamDb,
                    onValueChange = onBassSlamChange,
                    valueRange = 0f..12f,
                    colors = SliderDefaults.colors(thumbColor = StitchSecondary, activeTrackColor = StitchSecondary, inactiveTrackColor = StitchSurfaceHighest),
                    modifier = Modifier.height(20.dp),
                )
            }
        }

        // Highway Speed Noise Leveler
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(StitchSurfaceLow)
                .border(1.dp, StitchSurfaceHighest, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StitchSurfaceHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(imageVector = Icons.Rounded.Speed, contentDescription = null, tint = StitchTertiary, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Highway Speed Noise Leveler", color = StitchOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Auto-boosts 80–200Hz above 60 km/h cabin rumble", color = StitchOnSurfaceVariant, fontSize = 11.sp)
                    }
                }
                Switch(
                    checked = state.highwayNoiseLevelerEnabled,
                    onCheckedChange = onHighwayNoiseToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = StitchSurface, checkedTrackColor = StitchTertiary),
                )
            }
        }
    }
}

@Composable
private fun PhaseButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) StitchSecondary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = if (isSelected) StitchSurface else StitchOnSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun LiveAbComparatorSection(
    isDspCalibrated: Boolean,
    onSelectMode: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StitchSurfaceLow)
            .border(1.dp, StitchSurfaceHighest, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Rounded.Compare, contentDescription = null, tint = StitchPrimary, modifier = Modifier.size(18.dp))
                    Text("Live Cabin A/B Comparator", color = StitchOnSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Text("DSP PROCESSOR LIVE", color = StitchTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // A/B Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StitchSurfaceHighest)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!isDspCalibrated) StitchSurfaceLow else Color.Transparent)
                        .clickable { onSelectMode(false) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Stock Car BT", color = if (!isDspCalibrated) StitchOnSurface else StitchOnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Flat OEM Output", color = StitchOnSurfaceVariant, fontSize = 10.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDspCalibrated) StitchPrimaryContainer else Color.Transparent)
                        .clickable { onSelectMode(true) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Punjabi Beats DSP", color = if (isDspCalibrated) StitchSurface else StitchOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Gaddi Tuned ✨", color = if (isDspCalibrated) StitchSurface else StitchPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StitchSurfaceContainer)
                        .padding(10.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Rounded.ArrowUpward, contentDescription = null, tint = StitchTertiary, modifier = Modifier.size(14.dp))
                            Text("+42%", color = StitchTertiary, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        }
                        Text("Dholak Transient Punch", color = StitchOnSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Crisp attack, zero mud", color = StitchOnSurfaceVariant, fontSize = 10.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StitchSurfaceContainer)
                        .padding(10.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Rounded.ArrowUpward, contentDescription = null, tint = StitchPrimary, modifier = Modifier.size(14.dp))
                            Text("+65%", color = StitchPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        }
                        Text("Sub Bass Cabin Clarity", color = StitchOnSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Rattle canceled @ 42Hz", color = StitchOnSurfaceVariant, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
