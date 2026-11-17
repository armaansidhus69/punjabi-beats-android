package com.punjabibeats.app.ui.screens.player

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import com.punjabibeats.app.core.util.formatDuration
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.ui.components.LocalPbSnackbar
import com.punjabibeats.app.ui.components.VinylArtwork
import com.punjabibeats.app.ui.components.WaveformSeekbar
import com.punjabibeats.app.ui.components.sliders.SeekbarStyle
import com.punjabibeats.app.ui.components.sliders.SeekbarStylePickerSheet
import com.punjabibeats.app.ui.components.sliders.UniversalSeekbar
import com.punjabibeats.app.ui.components.turntable.TurntableDeck
import com.punjabibeats.app.ui.screens.player.components.QueueDrawer
import com.punjabibeats.app.ui.screens.player.components.SleepTimerSheet
import com.punjabibeats.app.ui.screens.player.components.SongDnaSheet
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.punjabibeats.app.ui.theme.LocalPBTheme
import com.punjabibeats.app.ui.theme.PBTheme
import com.punjabibeats.app.ui.theme.PlayerStyle

/**
 * Dancing vertical neon pink spectrum bars waveform equalizer matching screenshot.
 * Rendered using a single hardware-accelerated Canvas with drawRoundRect, completely
 * eliminating 32 Box composable measure/layout passes on every animation frame.
 */
@Composable
private fun FullPlayerDancingSpectrum(
    isPlaying: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spectrum")
    val anim1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse),
        label = "a1",
    )
    val anim2 by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(360, easing = LinearEasing), RepeatMode.Reverse),
        label = "a2",
    )
    val anim3 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(540, easing = LinearEasing), RepeatMode.Reverse),
        label = "a3",
    )
    val anim4 by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "a4",
    )
    val anim5 by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "a5",
    )
    val barFactors = listOf(
        anim1, anim2, anim3, anim4, anim5, anim2, anim1, anim3,
        anim5, anim4, anim3, anim2, anim1, anim4, anim5, anim3,
        anim2, anim1, anim4, anim3, anim5, anim2, anim4, anim1,
        anim3, anim5, anim4, anim2, anim1, anim3, anim5, anim2
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .padding(horizontal = 4.dp),
    ) {
        val barCount = barFactors.size
        val barWidth = 3.dp.toPx()
        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        val maxBarHeight = size.height * 0.9f
        val minBarHeight = 3.dp.toPx()
        val gap = if (barCount > 1) {
            (size.width - barCount * barWidth) / (barCount - 1)
        } else 0f
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val factor = barFactors[i]
            val barH = if (isPlaying) (maxBarHeight * factor).coerceAtLeast(minBarHeight) else minBarHeight
            val left = i * (barWidth + gap)
            val top = centerY - barH / 2f
            drawRoundRect(
                color = tint,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = cornerRadius,
            )
        }
    }
}

@Composable
fun FullPlayerScreen(
    onBack: () -> Unit,
    onOpenLyrics: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val theme = LocalPBTheme.current
    val player = viewModel.player
    val song by player.currentSong.collectAsStateWithLifecycle()
    val isPlaying by player.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by player.isBuffering.collectAsStateWithLifecycle()
    val positionMs by player.positionMs.collectAsStateWithLifecycle()
    val durationMs by player.durationMs.collectAsStateWithLifecycle()
    val repeatMode by player.repeatMode.collectAsStateWithLifecycle()
    val shuffleEnabled by player.shuffleEnabled.collectAsStateWithLifecycle()
    val playbackSpeed by player.playbackSpeed.collectAsStateWithLifecycle()
    val sleepMode by player.sleepMode.collectAsStateWithLifecycle()
    val isLiked by viewModel.isLiked.collectAsStateWithLifecycle()

    val snackbar = LocalPbSnackbar.current
    LaunchedEffect(Unit) {
        viewModel.events.collect { message -> snackbar.showSnackbar(message) }
    }

    val context = LocalContext.current
    var gradientColors by remember(theme) {
        mutableStateOf(listOf(theme.panel2, theme.background, theme.background))
    }
    LaunchedEffect(song?.artworkUrl, theme) {
        val url = song?.artworkUrl ?: run {
            gradientColors = listOf(theme.panel2, theme.background, theme.background)
            return@LaunchedEffect
        }
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .size(160)
            .build()
        val bitmap = runCatching {
            (context.imageLoader.execute(request).drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        }.getOrNull()
        if (bitmap != null) {
            val (vibrant, dominant) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                val palette = Palette.from(bitmap).maximumColorCount(24).generate()
                val dom = palette.getDominantColor(theme.panel2.toArgb())
                val vib = palette.getVibrantColor(dom)
                vib to dom
            }
            gradientColors = listOf(
                Color(vibrant).copy(alpha = 0.85f),
                Color(dominant).copy(alpha = 0.9f),
                theme.background,
            )
        } else {
            gradientColors = listOf(theme.panel2, theme.background, theme.background)
        }
    }

    var showSleepSheet by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showDnaSheet by remember { mutableStateOf(false) }
    var showSeekbarPicker by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }

    val audioFormat by player.audioFormatInfo.collectAsStateWithLifecycle()
    val sleepTimeRemainingMs by player.sleepTimeRemainingMs.collectAsStateWithLifecycle()
    val rawSeekbarStyle by viewModel.seekbarStyle.collectAsStateWithLifecycle()
    val currentSeekbarStyle = remember(rawSeekbarStyle) { SeekbarStyle.fromId(rawSeekbarStyle) }
    val isTurntableMode by viewModel.isTurntableMode.collectAsStateWithLifecycle()

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "artworkPulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 1. Header matching screenshot: ∨, // NOW PLAYING //, ⋮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Close player",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    "//  NOW PLAYING  //",
                    fontSize = 12.sp,
                    color = theme.accent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                IconButton(onClick = { showDnaSheet = true }) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "More options",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Player View Mode Switcher: Cover Art vs Turntable
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.surface.copy(alpha = 0.85f))
                    .border(1.dp, theme.panel2, RoundedCornerShape(20.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (!isTurntableMode) theme.accent else Color.Transparent)
                        .clickable { viewModel.setTurntableMode(false) }
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                ) {
                    Text(
                        "🖼️ Cover Art",
                        fontSize = 12.sp,
                        fontWeight = if (!isTurntableMode) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isTurntableMode) Color.White else theme.muted,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isTurntableMode) theme.accent else Color.Transparent)
                        .clickable { viewModel.setTurntableMode(true) }
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                ) {
                    Text(
                        "📻 Turntable",
                        fontSize = 12.sp,
                        fontWeight = if (isTurntableMode) FontWeight.Bold else FontWeight.Medium,
                        color = if (isTurntableMode) Color.White else theme.muted,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // 2. Vinyl Centerpiece with vibrant circular neon pink halo glow
            Box(
                modifier = Modifier
                    .size(280.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Vibrant circular neon pink halo glow
                Box(
                    modifier = Modifier
                        .size(270.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    theme.accent.copy(alpha = 0.5f),
                                    theme.accent.copy(alpha = 0.2f),
                                    Color.Transparent,
                                )
                            )
                        )
                        .border(2.dp, theme.accent.copy(alpha = 0.85f), CircleShape),
                )
                if (isTurntableMode) {
                    TurntableDeck(
                        artworkUrl = song?.artworkUrl,
                        isPlaying = isPlaying,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                        size = 250.dp,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .border(2.dp, theme.accent, RoundedCornerShape(26.dp)),
                    ) {
                        VinylArtwork(
                            artworkUrl = song?.artworkUrl,
                            isPlaying = isPlaying,
                            size = 250.dp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 3. Song Info with bold title, pink artist, and solid pink heart icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song?.title ?: "Nothing playing",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        song?.artist ?: "Pick a track to start the party",
                        fontSize = 15.sp,
                        color = theme.accent,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = { viewModel.toggleLike() },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) theme.accent else theme.muted,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 🎧 Real Bitrate, Codec & Audio Quality Indicator Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.surface.copy(alpha = 0.85f))
                    .border(1.dp, if (audioFormat.isLossless) theme.secondary else theme.accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .clickable { showDnaSheet = true }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    if (audioFormat.isLossless) "💎 LOSSLESS" else "✨ HQ AUDIO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (audioFormat.isLossless) theme.secondary else theme.accent,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "• ${audioFormat.bitrateKbps} KBPS • ${audioFormat.codec} • ${audioFormat.sampleRateKhz} kHz",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = theme.muted,
                )
            }

            Spacer(Modifier.height(10.dp))

            // 4. Dancing Spectrum Bars Waveform Equalizer
            FullPlayerDancingSpectrum(
                isPlaying = isPlaying,
                tint = theme.accent,
                modifier = Modifier.padding(bottom = 6.dp),
            )

            // 22 Progress Slider Collection Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${currentSeekbarStyle.iconEmoji} ${currentSeekbarStyle.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.muted,
                    fontWeight = FontWeight.Medium,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.surface.copy(alpha = 0.85f))
                        .border(1.dp, theme.panel2, RoundedCornerShape(12.dp))
                        .clickable { showSeekbarPicker = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("🎨 22 Sliders", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.accent)
                }
            }

            UniversalSeekbar(
                style = currentSeekbarStyle,
                positionMs = positionMs,
                durationMs = durationMs,
                isPlaying = isPlaying,
                onSeek = { player.seekTo(it) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatDuration(positionMs), style = MaterialTheme.typography.labelMedium, color = theme.muted)
                Text(
                    if (isBuffering) "buffering…" else formatDuration(durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = theme.muted,
                )
            }

            Spacer(Modifier.height(8.dp))

            // 5. Controls Row with 76dp double-ring glowing Play/Pause button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { player.toggleShuffle() }) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) theme.accent else theme.muted.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp),
                    )
                }
                IconButton(onClick = { player.previous() }, modifier = Modifier.size(46.dp)) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }

                // 76dp circular Play/Pause button with double-ring glowing halo
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, theme.accent, CircleShape)
                        .padding(3.5.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, theme.accent.copy(alpha = 0.75f), CircleShape)
                        .background(theme.accent)
                        .clickable { player.playPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(34.dp),
                            color = Color.White,
                            strokeWidth = 3.dp,
                        )
                    } else {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }

                IconButton(onClick = { player.next() }, modifier = Modifier.size(46.dp)) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
                IconButton(onClick = { player.cycleRepeatMode() }) {
                    Icon(
                        when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                            else -> Icons.Rounded.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) theme.accent else theme.muted.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // 6. Bottom 4 Action Cards: 66 Lyrics, ııll Equalizer, ⏱ Timer, ≡🎵 Queue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val cardShape = RoundedCornerShape(16.dp)
                val cardBorder = theme.accent.copy(alpha = 0.35f)
                val cardBg = theme.card

                // 1. Lyrics
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clip(cardShape)
                        .background(cardBg)
                        .border(1.dp, cardBorder, cardShape)
                        .clickable(onClick = onOpenLyrics)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("66", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = theme.text)
                        Spacer(Modifier.height(4.dp))
                        Text("Lyrics", fontSize = 11.sp, color = theme.muted)
                    }
                }

                // 2. Equalizer
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clip(cardShape)
                        .background(cardBg)
                        .border(1.dp, cardBorder, cardShape)
                        .clickable { showEqualizerSheet = true }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ııll", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = theme.text)
                        Spacer(Modifier.height(4.dp))
                        Text("Equalizer", fontSize = 11.sp, color = theme.muted)
                    }
                }

                // 3. Timer
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clip(cardShape)
                        .background(cardBg)
                        .border(1.dp, cardBorder, cardShape)
                        .clickable { showSleepSheet = true }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Bedtime,
                            contentDescription = null,
                            tint = if (sleepMode != null) theme.accent else theme.text,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        val timerLabel = if (sleepTimeRemainingMs != null && sleepTimeRemainingMs!! > 0L) {
                            val totalSecs = sleepTimeRemainingMs!! / 1000L
                            val mins = totalSecs / 60L
                            val secs = totalSecs % 60L
                            "%02d:%02d".format(mins, secs)
                        } else {
                            "Timer"
                        }
                        Text(
                            timerLabel,
                            fontSize = 11.sp,
                            color = if (sleepMode != null) theme.accent else theme.muted,
                        )
                    }
                }

                // 4. Queue
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clip(cardShape)
                        .background(cardBg)
                        .border(1.dp, cardBorder, cardShape)
                        .clickable { showQueue = true }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = theme.text,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Queue", fontSize = 11.sp, color = theme.muted)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        if (showSleepSheet) {
            SleepTimerSheet(
                sleepMode = sleepMode,
                remainingMs = sleepTimeRemainingMs,
                onStart = { mode -> viewModel.startSleepTimer(mode) },
                onCancel = { viewModel.cancelSleepTimer() },
                onDismiss = { showSleepSheet = false },
            )
        }
        if (showQueue) {
            QueueDrawer(
                controller = player,
                onMove = { from, to -> viewModel.moveQueueItem(from, to) },
                onRemove = { index -> viewModel.removeFromQueue(index) },
                onDismiss = { showQueue = false },
            )
        }
        if (showDnaSheet) {
            SongDnaSheet(
                song = song,
                audioFormat = audioFormat,
                onDismiss = { showDnaSheet = false },
            )
        }
        if (showEqualizerSheet) {
            val eqEnabled by viewModel.equalizerEnabled.collectAsStateWithLifecycle()
            val eqBands by viewModel.equalizerBands.collectAsStateWithLifecycle()
            val bassBoost by viewModel.bassBoostStrength.collectAsStateWithLifecycle()
            com.punjabibeats.app.ui.screens.player.components.EqualizerSheet(
                enabled = eqEnabled,
                bands = eqBands,
                bassBoost = bassBoost,
                onEnabledChange = { viewModel.setEqualizerEnabled(it) },
                onBandChange = { index, level -> viewModel.setBandLevel(index, level) },
                onBassBoostChange = { viewModel.setBassBoost(it) },
                onReset = { viewModel.resetEqualizer() },
                onDismiss = { showEqualizerSheet = false },
            )
        }
        if (showSeekbarPicker) {
            SeekbarStylePickerSheet(
                currentStyle = currentSeekbarStyle,
                onSelectStyle = { viewModel.setSeekbarStyle(it.id) },
                onDismiss = { showSeekbarPicker = false },
            )
        }
    }
}

@Composable
private fun ThemePlayerControls(
    theme: PBTheme,
    isPlaying: Boolean,
    isBuffering: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    val infinite = rememberInfiniteTransition(label = "buttonPulse")
    val buttonScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "buttonScale",
    )

    val playButton: @Composable (Modifier) -> Unit = { modifier ->
        FilledIconButton(
            onClick = onPlayPause,
            modifier = modifier
                .size(if (theme.id == "anime") 88.dp else 72.dp)
                .scale(if (isPlaying && theme.id == "anime") buttonScale else 1f),
            shape = when (theme.id) {
                "classic" -> CircleShape
                "anime" -> RoundedCornerShape(28.dp)
                "cyber" -> RoundedCornerShape(12.dp)
                "punjab" -> RoundedCornerShape(50.dp)
                else -> CircleShape
            },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = theme.accent,
                contentColor = theme.background,
            ),
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = theme.background,
                    strokeWidth = 2.5.dp,
                )
            } else {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = theme.background,
                    modifier = Modifier.size(38.dp),
                )
            }
        }
    }

    when (theme.id) {
        "anime" -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(35.dp))
                    .background(theme.card)
                    .border(
                        2.dp,
                        Brush.horizontalGradient(listOf(theme.accent, theme.secondary)),
                        RoundedCornerShape(35.dp),
                    )
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle", tint = if (shuffleEnabled) theme.accent else theme.text, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = theme.text, modifier = Modifier.size(26.dp))
                }
                Box(
                    modifier = Modifier
                        .border(2.dp, theme.secondary, CircleShape)
                        .padding(4.dp),
                ) {
                    playButton(Modifier)
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = theme.text, modifier = Modifier.size(26.dp))
                }
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) theme.accent else theme.text,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        "cyber" -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.card)
                    .border(1.dp, theme.accent, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle", tint = if (shuffleEnabled) theme.accent else theme.text, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = theme.text, modifier = Modifier.size(26.dp))
                }
                playButton(Modifier)
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = theme.text, modifier = Modifier.size(26.dp))
                }
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) theme.accent else theme.text,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        "punjab" -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(40.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(theme.card, theme.accent.copy(alpha = 0.22f), theme.card)
                        )
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle", tint = if (shuffleEnabled) theme.accent else theme.text, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = theme.text, modifier = Modifier.size(26.dp))
                }
                playButton(Modifier)
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = theme.text, modifier = Modifier.size(26.dp))
                }
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) theme.accent else theme.text,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        else -> { // "classic" and fallbacks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle", tint = if (shuffleEnabled) theme.accent else theme.text, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = theme.text, modifier = Modifier.size(26.dp))
                }
                playButton(Modifier)
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = theme.text, modifier = Modifier.size(26.dp))
                }
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) theme.accent else theme.text,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

private val NEXT_SPEEDS = listOf(0.5f, 0.8f, 1.0f, 1.25f, 1.5f, 2.0f)

private fun nextSpeed(current: Float): Float {
    return NEXT_SPEEDS.firstOrNull { it > current + 0.01f } ?: NEXT_SPEEDS.first()
}
