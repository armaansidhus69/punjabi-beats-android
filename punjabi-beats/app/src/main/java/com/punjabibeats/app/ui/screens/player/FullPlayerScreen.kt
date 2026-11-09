package com.punjabibeats.app.ui.screens.player

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import androidx.media3.common.Player
import com.punjabibeats.app.core.util.formatDuration
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.ui.components.LocalPbSnackbar
import com.punjabibeats.app.ui.components.VinylArtwork
import com.punjabibeats.app.ui.components.WaveformSeekbar
import com.punjabibeats.app.ui.theme.AccentGold
import com.punjabibeats.app.ui.theme.AccentRed
import com.punjabibeats.app.ui.theme.BgBase
import com.punjabibeats.app.ui.theme.TextSecondary
import com.punjabibeats.app.ui.screens.player.components.QueueDrawer
import com.punjabibeats.app.ui.screens.player.components.SleepTimerSheet

@Composable
fun FullPlayerScreen(
    onBack: () -> Unit,
    onOpenLyrics: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
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
    var gradientColors by remember {
        mutableStateOf(listOf(Color(0xFF3A2E10), Color(0xFF1F1608), BgBase))
    }
    LaunchedEffect(song?.artworkUrl) {
        val url = song?.artworkUrl ?: return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .size(160)
            .build()
        val bitmap = (context.imageLoader.execute(request).drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        if (bitmap != null) {
            val palette = Palette.from(bitmap).maximumColorCount(24).generate()
            val dominant = palette.getDominantColor(Color(0xFF3A2E10).toArgb())
            val vibrant = palette.getVibrantColor(dominant)
            gradientColors = listOf(
                Color(vibrant).copy(alpha = 0.85f),
                Color(dominant).copy(alpha = 0.9f),
                BgBase,
            )
        }
    }

    var showSleepSheet by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close player", tint = Color.White)
                }
                Text(
                    "NOW PLAYING",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.downloadCurrent() }) {
                    Icon(
                        Icons.Rounded.DownloadForOffline,
                        contentDescription = "Download for offline",
                        tint = Color.White.copy(alpha = 0.8f),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            VinylArtwork(artworkUrl = song?.artworkUrl, isPlaying = isPlaying, size = 300.dp)
            Spacer(Modifier.height(32.dp))

            Text(
                song?.title ?: "Nothing playing",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song?.artist ?: "Pick a track to start the party",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(20.dp))
            WaveformSeekbar(
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
                Text(formatDuration(positionMs), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Text(
                    if (isBuffering) "buffering…" else formatDuration(durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(onClick = { player.toggleShuffle() }) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) AccentGold else Color.White.copy(alpha = 0.8f),
                    )
                }
                IconButton(onClick = { player.previous() }) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AccentGold)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "playPause",
                    ) { playing ->
                        IconButton(
                            onClick = { player.playPause() },
                            modifier = Modifier.size(64.dp),
                        ) {
                            Icon(
                                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }
                }
                IconButton(onClick = { player.next() }) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White)
                }
                IconButton(onClick = { player.cycleRepeatMode() }) {
                    Icon(
                        if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repeat mode",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) AccentGold else Color.White.copy(alpha = 0.8f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(onClick = { viewModel.toggleLike() }) {
                    Icon(
                        if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) AccentRed else Color.White.copy(alpha = 0.8f),
                    )
                }
                IconButton(onClick = { showSleepSheet = true }) {
                    Icon(
                        Icons.Rounded.Bedtime,
                        contentDescription = "Sleep timer",
                        tint = if (sleepMode != null) AccentGold else Color.White.copy(alpha = 0.8f),
                    )
                }
                IconButton(onClick = { showQueue = true }) {
                    Icon(
                        Icons.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.8f),
                    )
                }
                IconButton(onClick = { viewModel.setPlaybackSpeed(if (playbackSpeed >= 2.0f) 0.5f else NEXT_SPEEDS[(NEXT_SPEEDS.indexOf(playbackSpeed) + 1).coerceAtMost(NEXT_SPEEDS.lastIndex)]) }) {
                    Icon(
                        Icons.Rounded.Speed,
                        contentDescription = "Playback speed",
                        tint = if (playbackSpeed != 1.0f) AccentGold else Color.White.copy(alpha = 0.8f),
                    )
                }
                IconButton(onClick = onOpenLyrics) {
                    Icon(
                        Icons.Rounded.Lyrics,
                        contentDescription = "Lyrics",
                        tint = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
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
    }
}

private val NEXT_SPEEDS = listOf(0.5f, 0.8f, 1.0f, 1.25f, 1.5f, 2.0f)
