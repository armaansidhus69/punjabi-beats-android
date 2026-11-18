package com.punjabibeats.app.ui.screens.player.archetypes

import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Airplay
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.punjabibeats.app.core.util.formatDuration
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.ui.screens.player.decks.PlayerDeck
import com.punjabibeats.app.ui.screens.player.decks.PlayerDeckSwitcher

/**
 * Authentic Apple Music Player Archetype (Cupertino Glass layout).
 * Features signature artwork-blurred liquid mesh background, dynamic scale-on-play physics,
 * negative remaining time scrubber, frameless pure white iOS 3-button playback controls,
 * hardware volume slider, favorite star, and Cupertino bottom action dock.
 */
@Composable
fun AppleMusicPlayerContent(
    song: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    isLiked: Boolean,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenDna: () -> Unit,
    onBack: () -> Unit,
    onSelectDeck: ((PlayerDeck) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    var currentVolumeFraction by remember(audioManager) {
        mutableFloatStateOf(
            audioManager?.let {
                val max = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                if (max > 0) it.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max.toFloat() else 0.7f
            } ?: 0.7f
        )
    }

    // Signature Apple Music physics: artwork expands to 1.0x when playing, shrinks to 0.85x on pause
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "appleArtworkScale",
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Apple Music Dynamic Liquid Backdrop (Blurred Album Artwork + Dark Scrim)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
        ) {
            if (!song?.artworkUrl.isNullOrBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(song?.artworkUrl)
                        .crossfade(500)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(72.dp),
                )
            } else {
                // Fallback elegant Cupertino gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF3B0D18),
                                    Color(0xFF1C0A10),
                                    Color(0xFF000000),
                                )
                            )
                        )
                )
            }

            // Apple dark glass scrim overlay so text & controls pop with crystal-clear contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.38f),
                                Color.Black.copy(alpha = 0.52f),
                                Color.Black.copy(alpha = 0.85f),
                            )
                        )
                    )
            )
        }

        // 2. Main Apple Music Player Controls & Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Apple Top Bar: Chevron down, centered grabber pill, and glass more-options button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(32.dp),
                    )
                }

                // Apple Grabber Handle
                Box(
                    modifier = Modifier
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 38.dp, height = 5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.35f))
                    )
                }

                // Apple Glass Action Button (...)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable(onClick = onOpenDna),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.MoreHoriz,
                        contentDescription = "Song options",
                        tint = Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (onSelectDeck != null) {
                PlayerDeckSwitcher(
                    activeDeck = PlayerDeck.APPLE,
                    onSelectDeck = onSelectDeck,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            // 3. Centerpiece: Floating Squircle Artwork with Apple Scale Physics & Deep Ambient Shadow
            Box(
                modifier = Modifier
                    .size(296.dp)
                    .scale(artworkScale),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            elevation = 28.dp,
                            shape = RoundedCornerShape(16.dp),
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = 0.6f),
                            spotColor = Color.Black.copy(alpha = 0.8f),
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1C1C1E)),
                ) {
                    AsyncImage(
                        model = song?.artworkUrl,
                        contentDescription = song?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            // 4. Song Info & Apple Favorite Star
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song?.title ?: "No Song Selected",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = song?.artist ?: "Select a song to play",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Apple Favorite Star (Red #FA233B when favorited)
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isLiked) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = if (isLiked) "Favorited" else "Favorite",
                        tint = if (isLiked) Color(0xFFFA233B) else Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // 5. Apple Scrub Bar: Minimalist with Negative Remaining Time
            var draggingPosition by remember { mutableStateOf<Float?>(null) }
            val currentProgress = when {
                draggingPosition != null -> draggingPosition!!
                durationMs > 0L -> (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                else -> 0f
            }

            Slider(
                value = currentProgress,
                onValueChange = { draggingPosition = it },
                onValueChangeFinished = {
                    val targetMs = ((draggingPosition ?: 0f) * durationMs.toFloat()).toLong()
                    onSeek(targetMs)
                    draggingPosition = null
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White.copy(alpha = 0.95f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.20f),
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatDuration(positionMs),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.55f),
                )
                val remainingMs = maxOf(0L, durationMs - positionMs)
                Text(
                    text = "-${formatDuration(remainingMs)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }

            Spacer(Modifier.height(20.dp))

            // 6. Apple 3-Button Controls: Frameless pure white Previous, Play/Pause, Next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier.size(54.dp),
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }

                // Apple Frameless Play/Pause (Pure white iconography with soft touch highlight)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(46.dp),
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(54.dp),
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 7. Apple Volume Slider with Speaker Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.VolumeDown,
                    contentDescription = "Volume down",
                    tint = Color.White.copy(alpha = 0.50f),
                    modifier = Modifier.size(19.dp),
                )
                Slider(
                    value = currentVolumeFraction,
                    onValueChange = { fraction ->
                        currentVolumeFraction = fraction
                        audioManager?.let { am ->
                            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val target = (fraction * max).toInt().coerceIn(0, max)
                            am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White.copy(alpha = 0.85f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.20f),
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                )
                Icon(
                    Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = "Volume up",
                    tint = Color.White.copy(alpha = 0.50f),
                    modifier = Modifier.size(19.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            // 8. Apple Bottom Action Dock: Lyrics, AirPlay, Queue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 26.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onOpenLyrics) {
                    Icon(
                        Icons.Rounded.Lyrics,
                        contentDescription = "Synced Lyrics",
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(24.dp),
                    )
                }

                IconButton(onClick = { /* AirPlay/Audio Route */ }) {
                    Icon(
                        Icons.Rounded.Airplay,
                        contentDescription = "Audio Output",
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(24.dp),
                    )
                }

                IconButton(onClick = onOpenQueue) {
                    Icon(
                        Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
