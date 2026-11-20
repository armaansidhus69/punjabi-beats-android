package com.punjabibeats.app.ui.components.canvas

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage

/**
 * Ambient looping motion video canvas player view.
 * Plays silent looping background videos behind Now Playing screen with smooth crossfade.
 */
@OptIn(UnstableApi::class)
@Composable
fun CanvasArtworkPlayer(
    artwork: CanvasArtwork?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    fallbackArtworkUrl: String? = null,
    overlayScrim: Color = Color.Black.copy(alpha = 0.55f),
) {
    val context = LocalContext.current
    var isVideoReady by remember(artwork?.url) { mutableStateOf(false) }

    val videoUrl = artwork?.portraitUrl ?: artwork?.url
    val isVideo = remember(videoUrl, artwork?.source) {
        artwork?.source == CanvasSource.SPOTIFY || isVideoCanvasUrl(videoUrl)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Fallback or underlying still album artwork
        if (!fallbackArtworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = fallbackArtworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Looping video canvas overlay
        if (isVideo && !videoUrl.isNullOrBlank()) {
            val exoPlayer = remember(videoUrl) {
                ExoPlayer.Builder(context).build().apply {
                    volume = 0f // Canvas motion video is always completely silent
                    repeatMode = Player.REPEAT_MODE_ALL
                    val mediaItem = MediaItem.fromUri(videoUrl)
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = isPlaying
                }
            }

            LaunchedEffect(isPlaying) {
                exoPlayer.playWhenReady = isPlaying
            }

            DisposableEffect(exoPlayer) {
                val listener = object : Player.Listener {
                    override fun onRenderedFirstFrame() {
                        isVideoReady = true
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        isVideoReady = false
                    }
                }
                exoPlayer.addListener(listener)
                onDispose {
                    exoPlayer.removeListener(listener)
                    exoPlayer.stop()
                    exoPlayer.release()
                }
            }

            AnimatedVisibility(
                visible = isVideoReady,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            val textureView = TextureView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                )
                            }
                            addView(textureView)
                            exoPlayer.setVideoTextureView(textureView)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Ambient dark gradient scrim to protect legibility of player controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.5f to overlayScrim,
                        1f to Color.Black.copy(alpha = 0.75f),
                    )
                )
        )
    }
}
