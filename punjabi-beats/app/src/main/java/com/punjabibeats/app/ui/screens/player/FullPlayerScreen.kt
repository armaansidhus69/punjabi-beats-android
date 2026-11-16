package com.punjabibeats.app.ui.screens.player

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import com.punjabibeats.app.ui.theme.LocalPBTheme
import com.punjabibeats.app.ui.theme.PBTheme
import com.punjabibeats.app.ui.theme.PlayerStyle

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
        val bitmap = (context.imageLoader.execute(request).drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        if (bitmap != null) {
            val palette = Palette.from(bitmap).maximumColorCount(24).generate()
            val dominant = palette.getDominantColor(theme.panel2.toArgb())
            val vibrant = palette.getVibrantColor(dominant)
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

    val audioFormat by player.audioFormatInfo.collectAsStateWithLifecycle()
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Close player",
                        tint = theme.text,
                    )
                }
                Text(
                    "NOW PLAYING",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.muted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                IconButton(onClick = { viewModel.downloadCurrent() }) {
                    Icon(
                        Icons.Rounded.DownloadForOffline,
                        contentDescription = "Download for offline",
                        tint = theme.muted,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

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
                        color = if (!isTurntableMode) theme.background else theme.muted,
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
                        color = if (isTurntableMode) theme.background else theme.muted,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Artwork or Audiophile Turntable Deck
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
                        .size(260.dp)
                        .scale(if (isPlaying && (theme.id == "anime" || theme.id == "cyber")) pulse else 1f)
                        .clip(
                            RoundedCornerShape(
                                when (theme.id) {
                                    "classic" -> 18.dp
                                    "anime" -> 36.dp
                                    "cyber" -> 12.dp
                                    else -> 28.dp
                                }
                            )
                        )
                ) {
                    VinylArtwork(
                        artworkUrl = song?.artworkUrl,
                        isPlaying = isPlaying,
                        size = 260.dp,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                song?.title ?: "Nothing playing",
                style = MaterialTheme.typography.headlineMedium,
                color = theme.text,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song?.artist ?: "Pick a track to start the party",
                style = MaterialTheme.typography.bodyLarge,
                color = theme.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(10.dp))

            // 🎧 Real Bitrate, Codec & Audio Quality Indicator Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.surface.copy(alpha = 0.85f))
                    .border(1.dp, if (audioFormat.isLossless) theme.secondary else theme.accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .clickable { showDnaSheet = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
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

            Spacer(Modifier.height(14.dp))

            if (theme.id == "anime" || theme.id == "cyber") {
                Text(
                    if (isPlaying) "♪ 〰 ♫ 〰 ♪ 〰 ♫"
                    else "PAUSED",
                    color = theme.accent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            // 22 Progress Slider Collection Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatDuration(positionMs), style = MaterialTheme.typography.labelMedium, color = theme.muted)
                Text(
                    if (isBuffering) "buffering…" else formatDuration(durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = theme.muted,
                )
            }

            Spacer(Modifier.height(16.dp))

            // 8-Theme Dynamic Player Controls
            ThemePlayerControls(
                theme = theme,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                repeatMode = repeatMode,
                shuffleEnabled = shuffleEnabled,
                onPlayPause = { player.playPause() },
                onPrevious = { player.previous() },
                onNext = { player.next() },
                onToggleShuffle = { player.toggleShuffle() },
                onCycleRepeat = { player.cycleRepeatMode() },
            )

            Spacer(Modifier.height(20.dp))

            // Bottom Secondary Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(onClick = { viewModel.toggleLike() }) {
                    Icon(
                        if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFFFF5577) else theme.muted,
                    )
                }
                IconButton(onClick = { showSleepSheet = true }) {
                    Icon(
                        Icons.Rounded.Bedtime,
                        contentDescription = "Sleep timer",
                        tint = if (sleepMode != null) theme.accent else theme.muted,
                    )
                }
                IconButton(onClick = { showQueue = true }) {
                    Icon(
                        Icons.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = theme.muted,
                    )
                }
                IconButton(onClick = { viewModel.setPlaybackSpeed(nextSpeed(playbackSpeed)) }) {
                    Icon(
                        Icons.Rounded.Speed,
                        contentDescription = "Playback speed",
                        tint = if (playbackSpeed != 1.0f) theme.accent else theme.muted,
                    )
                }
                IconButton(onClick = { showDnaSheet = true }) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = "Song DNA & Info",
                        tint = theme.accent,
                    )
                }
                IconButton(onClick = onOpenLyrics) {
                    Icon(
                        Icons.Rounded.Lyrics,
                        contentDescription = "Lyrics",
                        tint = theme.muted,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        if (showSleepSheet) {
            SleepTimerSheet(
                sleepMode = sleepMode,
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
