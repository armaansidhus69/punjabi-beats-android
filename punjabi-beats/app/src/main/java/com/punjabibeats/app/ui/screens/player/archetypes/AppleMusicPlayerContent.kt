package com.punjabibeats.app.ui.screens.player.archetypes

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Airplay
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.punjabibeats.app.core.util.formatDuration
import com.punjabibeats.app.domain.model.Song

/**
 * Authentic Apple Music Player Archetype (Cupertino Glass layout).
 * Features dynamic scale-on-play physics, negative remaining time scrubber,
 * bold iOS 3-button playback controls, hardware volume slider, and Cupertino action dock.
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 1. Top Header: ∨, Now Playing, ⋮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(30.dp),
                )
            }
            Text(
                text = "NOW PLAYING",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.65f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenDna) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "Song options",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // 2. Centerpiece: Floating Squircle Artwork with Apple Scale Physics
        Box(
            modifier = Modifier
                .size(290.dp)
                .scale(artworkScale),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1E1E26))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp)),
            ) {
                AsyncImage(
                    model = song?.artworkUrl,
                    contentDescription = song?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        // 3. Song Info & Favorite Star/Heart
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
                Spacer(Modifier.height(3.dp))
                Text(
                    text = song?.artist ?: "Select a song to play",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isLiked) Color(0xFFFA2D48) else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 4. Apple Scrub Bar: Minimalist with Negative Remaining Time
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
                inactiveTrackColor = Color.White.copy(alpha = 0.22f),
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
                color = Color.White.copy(alpha = 0.6f),
            )
            val remainingMs = maxOf(0L, durationMs - positionMs)
            Text(
                text = "-${formatDuration(remainingMs)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.6f),
            )
        }

        Spacer(Modifier.height(18.dp))

        // 5. Apple 3-Button Controls: Previous, Big Bold Play/Pause, Next
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onSkipPrevious,
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp),
                )
            }

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(38.dp),
                )
            }

            IconButton(
                onClick = onSkipNext,
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // 6. Apple Volume Slider with Speaker Icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.VolumeDown,
                contentDescription = "Volume down",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
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
                    inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
            Icon(
                Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = "Volume up",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        // 7. Apple Bottom Action Dock: Lyrics, AirPlay, Queue
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenLyrics) {
                Icon(
                    Icons.Rounded.Lyrics,
                    contentDescription = "Synced Lyrics",
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(24.dp),
                )
            }

            IconButton(onClick = { /* AirPlay/Audio Route */ }) {
                Icon(
                    Icons.Rounded.Airplay,
                    contentDescription = "Audio Output",
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(24.dp),
                )
            }

            IconButton(onClick = onOpenQueue) {
                Icon(
                    Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "Queue",
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
