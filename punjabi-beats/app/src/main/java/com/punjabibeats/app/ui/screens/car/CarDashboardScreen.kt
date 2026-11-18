package com.punjabibeats.app.ui.screens.car

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
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

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
fun CarDashboardScreen(
    onExitCarMode: () -> Unit,
    onOpenDspCalibration: () -> Unit,
    viewModel: CarDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchSurface)
            .statusBarsPadding(),
    ) {
        // 1. Car Mode Top Header (OBD-II, Bluetooth, Clock)
        CarModeHeader(
            a2dpCodec = state.a2dpCodec,
            batteryPercent = state.batteryPercent,
            onExit = onExitCarMode,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 2. Telemetry HUD (Speedometer & Road Location)
            TelemetryHud(
                vehicleName = state.vehicleName,
                roadName = state.roadName,
                heading = state.heading,
                speedKmh = state.speedKmh,
                speedLimitKmh = state.speedLimitKmh,
            )

            // 3. Giant Driver-Safe Now Playing Hero Deck
            GiantDriverNowPlayingDeck(
                state = state,
                onPlayPause = { viewModel.playPause() },
                onNext = { viewModel.next() },
                onPrevious = { viewModel.previous() },
                onToggleFavorite = { viewModel.toggleFavorite() },
                onSeek = { viewModel.seekTo(it) },
            )

            // 4. Gedhi Mode One-Touch Acoustic Tuning Shelf (with DSP link)
            GedhiModeTuningShelf(
                activePreset = state.activeGedhiPreset,
                onSelectPreset = { viewModel.setGedhiPreset(it) },
                onOpenDsp = onOpenDspCalibration,
            )

            // 5. Driver Fast Quick-Picks (Single-tap launch)
            FastDriverQuickPicks(
                quickPicks = state.quickPicks,
                onSelectPick = { viewModel.selectQuickPick(it) },
            )

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun CarModeHeader(
    a2dpCodec: String,
    batteryPercent: Int,
    onExit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(StitchSurfaceHigh)
                    .clickable(onClick = onExit),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Exit Car Mode",
                    tint = StitchOnSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(StitchSurfaceContainer)
                    .border(1.dp, StitchTertiary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(StitchTertiary),
                    )
                    Text(
                        text = "OBD-II LIVE",
                        color = StitchTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(StitchSurfaceContainer)
                    .border(1.dp, StitchSecondary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BluetoothConnected,
                        contentDescription = null,
                        tint = StitchSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = a2dpCodec,
                        color = StitchSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.BatteryChargingFull,
                    contentDescription = null,
                    tint = StitchOnSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "$batteryPercent%",
                    color = StitchOnSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun TelemetryHud(
    vehicleName: String,
    roadName: String,
    heading: String,
    speedKmh: Int,
    speedLimitKmh: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Vehicle Info Card
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(StitchSurfaceLow)
                .border(1.dp, StitchSurfaceHighest, RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DirectionsCar,
                        contentDescription = null,
                        tint = StitchSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = vehicleName,
                        color = StitchSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
                Text(
                    text = roadName,
                    color = StitchOnSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Explore,
                        contentDescription = null,
                        tint = StitchTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = heading,
                        color = StitchOnSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }

        // Speedometer Card
        Box(
            modifier = Modifier
                .weight(1f)
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
                Column {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "$speedKmh",
                            color = StitchOnSurface,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 32.sp,
                        )
                        Text(
                            text = "KM/H",
                            color = StitchOnSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(StitchTertiary),
                        )
                        Text(
                            text = "CRUISE OPTIMAL",
                            color = StitchTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Speed Limit Badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(StitchSurfaceHighest)
                        .border(2.dp, StitchSecondary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "LIMIT",
                            color = StitchSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "$speedLimitKmh",
                            color = StitchOnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GiantDriverNowPlayingDeck(
    state: CarDashboardState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(StitchSurfaceContainer)
            .border(1.dp, StitchSurfaceHighest, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Artwork & Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // High-Contrast Large Art Tile
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(StitchSurfaceHighest),
                ) {
                    if (state.albumArtUrl != null) {
                        AsyncImage(
                            model = state.albumArtUrl,
                            contentDescription = state.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(StitchPrimaryContainer.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "੨੯੫",
                                color = StitchPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "GADDI",
                            color = StitchPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(StitchSurfaceHigh)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "HI-RES 24-BIT",
                                color = StitchTertiary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(StitchSecondary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "GADDI DSP",
                                color = StitchSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Text(
                        text = state.title,
                        color = StitchOnSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.artist,
                        color = StitchOnSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Giant Driver Scrubber
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Slider(
                    value = state.positionMs.toFloat(),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..state.durationMs.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = StitchPrimary,
                        activeTrackColor = StitchPrimaryContainer,
                        inactiveTrackColor = StitchSurfaceHighest,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatTime(state.positionMs), color = StitchOnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("MOOSETAPE SPECIAL", color = StitchSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(formatTime(state.durationMs), color = StitchOnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Giant Transport Controls (≥64-80dp Hit Targets for Driving Safety)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Voice button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(StitchSurfaceHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Voice Prompt",
                        tint = StitchTertiary,
                        modifier = Modifier.size(28.dp),
                    )
                }

                // Previous
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(StitchSurfaceHigh)
                        .clickable(onClick = onPrevious),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = StitchOnSurface,
                        modifier = Modifier.size(34.dp),
                    )
                }

                // Massive Play / Pause Button (76dp)
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(StitchPrimaryContainer)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = StitchSurface,
                        modifier = Modifier.size(42.dp),
                    )
                }

                // Next
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(StitchSurfaceHigh)
                        .clickable(onClick = onNext),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next Track",
                        tint = StitchOnSurface,
                        modifier = Modifier.size(34.dp),
                    )
                }

                // Favorite
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(StitchSurfaceHigh)
                        .clickable(onClick = onToggleFavorite),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = "Favorite",
                        tint = if (state.isFavorite) StitchSecondary else StitchOutline,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GedhiModeTuningShelf(
    activePreset: String,
    onSelectPreset: (String) -> Unit,
    onOpenDsp: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = StitchSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Gedhi Mode Audio EQ",
                    color = StitchOnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "OPEN DSP ›",
                color = StitchTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.clickable(onClick = onOpenDsp),
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val presets = listOf(
                Triple("gaddi_808", "Gaddi 808 Slam (+8dB)", Icons.Rounded.VolumeUp),
                Triple("dholak_punch", "Live Dholak Punch", Icons.Rounded.SpeakerGroup),
                Triple("wind_cancel", "Highway Wind Cancel", Icons.Rounded.Air),
                Triple("vocal_boliyan", "Crisp Boliyan & Tumbi", Icons.Rounded.RecordVoiceOver),
            )
            items(presets) { (id, label, icon) ->
                val isSelected = activePreset == id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isSelected) StitchSecondaryContainer else StitchSurfaceHigh)
                        .clickable { onSelectPreset(id) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) StitchSurface else StitchOnSurface,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = label,
                            color = if (isSelected) StitchSurface else StitchOnSurface,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FastDriverQuickPicks(
    quickPicks: List<CarQuickPick>,
    onSelectPick: (CarQuickPick) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Fast Driver Picks",
                color = StitchOnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Tap to Play",
                color = StitchOnSurfaceVariant,
                fontSize = 12.sp,
            )
        }

        // 2x2 Grid of quick picks
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val rows = quickPicks.chunked(2)
            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (pick in row) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(StitchSurfaceLow)
                                .border(1.dp, StitchSurfaceHighest, RoundedCornerShape(12.dp))
                                .clickable { onSelectPick(pick) }
                                .padding(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StitchSurfaceHighest),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = StitchOnSurface,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pick.title,
                                        color = StitchOnSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = pick.subtitle,
                                        color = StitchOnSurfaceVariant,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}
