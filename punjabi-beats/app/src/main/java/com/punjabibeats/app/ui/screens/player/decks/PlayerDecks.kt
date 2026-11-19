package com.punjabibeats.app.ui.screens.player.decks

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punjabibeats.app.core.util.formatDuration
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.ui.components.ArtworkThumb
import com.punjabibeats.app.ui.components.DitheredGradientBackground
import com.punjabibeats.app.ui.components.sliders.SeekbarMiniLivePreview
import com.punjabibeats.app.ui.components.sliders.SeekbarStyle
import com.punjabibeats.app.ui.components.sliders.UniversalSeekbar
import com.punjabibeats.app.ui.theme.LocalPBTheme
import kotlin.math.abs
import kotlin.math.sin

/**
 * Six Distinctive Player Deck Styles:
 * 1. Classic — Signature studio layout with turntable, gauges, and pro audio controls
 * 2. Apple Music — Full-bleed liquid animated blur backdrop with expansive controls
 * 3. LastWave — Cyberpunk telemetry visualizer-centric deck with oscillating wireframe meters
 * 4. BitChord — Chiptune / modular synth aesthetic with monospace typography and digital meters
 * 5. Echo Music — Ambient minimalist deck with concentric reverberant sound rings
 * 6. ArchiveTune — Analog vintage cassette tape deck with dual spinning reels
 */
enum class PlayerDeck(
    val id: String,
    val displayName: String,
    val iconEmoji: String,
    val description: String,
) {
    CLASSIC("classic", "Classic Studio", "🎛️", "Signature studio deck with turntable & pro gauges"),
    APPLE("apple", "Apple Music", "🍎", "Full-bleed liquid blur with expansive controls"),
    LASTWAVE("lastwave", "LastWave", "🌊", "Cyberpunk telemetry visualizer-centric deck"),
    BITCHORD("bitchord", "BitChord", "👾", "Chiptune modular synth with monospace telemetry"),
    ECHO("echo", "Echo Music", "🌌", "Ambient reverberant deck with concentric sound rings"),
    ARCHIVE("archive", "ArchiveTune", "📼", "Vintage analog cassette tape deck with rotating reels");

    companion object {
        fun fromId(id: String): PlayerDeck =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CLASSIC
    }
}

/**
 * Switcher bar embedded at the top of the player so users can instantly switch between the 6 decks.
 */
@Composable
fun PlayerDeckSwitcher(
    activeDeck: PlayerDeck,
    onSelectDeck: (PlayerDeck) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalPBTheme.current
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items(PlayerDeck.entries, key = { it.id }) { deck ->
            val isSelected = deck == activeDeck
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) theme.accent else theme.surface.copy(alpha = 0.85f))
                    .border(
                        width = 1.dp,
                        color = if (isSelected) theme.accent else theme.panel2.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable { onSelectDeck(deck) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(deck.iconEmoji, fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = deck.displayName,
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) theme.background else theme.text,
                )
            }
        }
    }
}

/** 3. LastWave Deck: Cyberpunk / synthwave visualizer deck with telemetry audio meters */
@Composable
fun LastWaveDeck(
    song: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    isLiked: Boolean,
    currentSeekbarStyle: SeekbarStyle,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenDna: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onBack: () -> Unit,
    onSelectSeekbarStyle: (String) -> Unit,
    onSelectDeck: (PlayerDeck) -> Unit,
) {
    val theme = LocalPBTheme.current
    val neonCyan = Color(0xFF00F5FF)
    val neonMagenta = Color(0xFFFF007F)

    val transition = rememberInfiniteTransition(label = "lastWaveAnim")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Deck Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close", tint = Color.White)
            }
            Text(
                "// LASTWAVE TELEMETRY //",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = neonCyan,
                letterSpacing = 2.sp,
            )
            IconButton(onClick = onOpenDna) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Options", tint = Color.White)
            }
        }

        PlayerDeckSwitcher(activeDeck = PlayerDeck.LASTWAVE, onSelectDeck = onSelectDeck)

        Spacer(Modifier.height(10.dp))

        // Center Cybernetic Telemetry Oscilloscope Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF080910))
                .border(1.dp, neonCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                // Neon wireframe grid lines
                for (g in 1..4) {
                    val gy = (h * g) / 5f
                    drawLine(neonCyan.copy(alpha = 0.08f), Offset(0f, gy), Offset(w, gy), 1f)
                }

                // Oscillating cyber wave
                val steps = 40
                val stepW = w / steps
                for (s in 0 until steps) {
                    val x1 = s * stepW
                    val x2 = (s + 1) * stepW
                    val v1 = if (isPlaying) sin(s * 0.4f + phase) * 35.dp.toPx() else 0f
                    val v2 = if (isPlaying) sin((s + 1) * 0.4f + phase) * 35.dp.toPx() else 0f
                    drawLine(
                        Brush.horizontalGradient(listOf(neonCyan, neonMagenta)),
                        Offset(x1, midY + v1),
                        Offset(x2, midY + v2),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }

            // Embedded Album Art floating in center
            ArtworkThumb(
                url = song?.artworkUrl,
                size = 110.dp,
                cornerRadius = 18.dp,
                modifier = Modifier
                    .shadow(16.dp, RoundedCornerShape(18.dp))
                    .border(1.5.dp, neonMagenta, RoundedCornerShape(18.dp)),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Track Info
        Text(
            text = song?.title ?: "No Track Playing",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${song?.artist ?: "Unknown Artist"} • 96kHz 24-bit FLAC",
            fontSize = 13.sp,
            color = neonCyan,
            fontWeight = FontWeight.Medium,
        )

        Spacer(Modifier.height(16.dp))

        // Active Seekbar
        UniversalSeekbar(
            style = currentSeekbarStyle,
            positionMs = positionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatDuration(positionMs), fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
            Text(formatDuration(durationMs), fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
        }

        // Live Mini Previews Carousel for all 22 seekbars
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SeekbarStyle.entries, key = { it.id }) { style ->
                SeekbarMiniLivePreview(
                    style = style,
                    isSelected = style == currentSeekbarStyle,
                    onClick = { onSelectSeekbarStyle(style.id) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Transport Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggleFavorite) {
                Icon(if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, contentDescription = "Like", tint = neonMagenta)
            }
            IconButton(onClick = onSkipPrevious) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(neonCyan)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = "Play/Pause", tint = Color.Black, modifier = Modifier.size(34.dp))
            }
            IconButton(onClick = onSkipNext) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = onOpenEqualizer) {
                Icon(Icons.Rounded.Equalizer, contentDescription = "EQ", tint = neonCyan)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** 4. BitChord Deck: Chiptune modular synthesizer aesthetic with monospace metadata */
@Composable
fun BitChordDeck(
    song: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    isLiked: Boolean,
    currentSeekbarStyle: SeekbarStyle,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenDna: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onBack: () -> Unit,
    onSelectSeekbarStyle: (String) -> Unit,
    onSelectDeck: (PlayerDeck) -> Unit,
) {
    val theme = LocalPBTheme.current
    val matrixGreen = Color(0xFF00FF66)
    val amberOrange = Color(0xFFFFB800)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close", tint = matrixGreen)
            }
            Text(
                "[BITCHORD_DECK_v2]",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = matrixGreen,
            )
            IconButton(onClick = onOpenEqualizer) {
                Icon(Icons.Rounded.Equalizer, contentDescription = "EQ", tint = amberOrange)
            }
        }

        PlayerDeckSwitcher(activeDeck = PlayerDeck.BITCHORD, onSelectDeck = onSelectDeck)

        Spacer(Modifier.height(10.dp))

        // Monospace Terminal Box with Artwork and Matrix Meter
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF050805))
                .border(1.2.dp, matrixGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                ArtworkThumb(
                    url = song?.artworkUrl,
                    size = 170.dp,
                    cornerRadius = 8.dp,
                    modifier = Modifier.border(1.dp, matrixGreen, RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "> ${song?.title ?: "SYSTEM_IDLE"}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = matrixGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "ARTIST: ${song?.artist ?: "UNKNOWN"} | DSP: ACTIVE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = amberOrange,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        UniversalSeekbar(
            style = currentSeekbarStyle,
            positionMs = positionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatDuration(positionMs), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = matrixGreen)
            Text(formatDuration(durationMs), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = matrixGreen)
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SeekbarStyle.entries, key = { it.id }) { style ->
                SeekbarMiniLivePreview(
                    style = style,
                    isSelected = style == currentSeekbarStyle,
                    onClick = { onSelectSeekbarStyle(style.id) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Terminal Style Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggleFavorite) {
                Icon(if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, contentDescription = "Like", tint = amberOrange)
            }
            IconButton(onClick = onSkipPrevious) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Prev", tint = matrixGreen, modifier = Modifier.size(32.dp))
            }
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(matrixGreen)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = "Play/Pause", tint = Color.Black, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = onSkipNext) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = matrixGreen, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = onOpenQueue) {
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Queue", tint = matrixGreen)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** 5. Echo Music Deck: Ambient minimalist deck with concentric reverberant sound rings */
@Composable
fun EchoMusicDeck(
    song: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    isLiked: Boolean,
    currentSeekbarStyle: SeekbarStyle,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenDna: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onBack: () -> Unit,
    onSelectSeekbarStyle: (String) -> Unit,
    onSelectDeck: (PlayerDeck) -> Unit,
) {
    val theme = LocalPBTheme.current
    val echoViolet = Color(0xFF9D4EDD)
    val echoAqua = Color(0xFF48CAE4)

    val transition = rememberInfiniteTransition(label = "echoRings")
    val ringPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
        label = "rings",
    )

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
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close", tint = Color.White)
            }
            Text("E C H O", fontSize = 14.sp, fontWeight = FontWeight.Light, color = echoAqua, letterSpacing = 6.sp)
            IconButton(onClick = onOpenQueue) {
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Queue", tint = Color.White)
            }
        }

        PlayerDeckSwitcher(activeDeck = PlayerDeck.ECHO, onSelectDeck = onSelectDeck)

        Spacer(Modifier.height(16.dp))

        // Concentric Echo Sound Rings with Centered Artwork
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width / 2f
                for (r in 1..3) {
                    val currentR = ((ringPhase + (r * 0.33f)) % 1f) * maxRadius
                    val alpha = (1f - (currentR / maxRadius)).coerceIn(0f, 0.7f)
                    drawCircle(
                        color = if (r % 2 == 0) echoAqua.copy(alpha = alpha) else echoViolet.copy(alpha = alpha),
                        radius = currentR,
                        center = center,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
            ArtworkThumb(
                url = song?.artworkUrl,
                size = 140.dp,
                cornerRadius = 70.dp,
                modifier = Modifier
                    .shadow(20.dp, CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = song?.title ?: "Ambient Flow",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = song?.artist ?: "Unknown Artist",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))

        UniversalSeekbar(
            style = currentSeekbarStyle,
            positionMs = positionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatDuration(positionMs), fontSize = 11.sp, color = Color.White.copy(alpha = 0.45f))
            Text(formatDuration(durationMs), fontSize = 11.sp, color = Color.White.copy(alpha = 0.45f))
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SeekbarStyle.entries, key = { it.id }) { style ->
                SeekbarMiniLivePreview(
                    style = style,
                    isSelected = style == currentSeekbarStyle,
                    onClick = { onSelectSeekbarStyle(style.id) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggleFavorite) {
                Icon(if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, contentDescription = "Like", tint = echoViolet)
            }
            IconButton(onClick = onSkipPrevious) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(1.dp, echoAqua, CircleShape)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(30.dp))
            }
            IconButton(onClick = onSkipNext) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(30.dp))
            }
            IconButton(onClick = onOpenLyrics) {
                Icon(Icons.Rounded.Lyrics, contentDescription = "Lyrics", tint = echoAqua)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** 6. ArchiveTune Deck: Analog vintage cassette tape deck with dual rotating spools */
@Composable
fun ArchiveTuneDeck(
    song: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    isLiked: Boolean,
    currentSeekbarStyle: SeekbarStyle,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenDna: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onBack: () -> Unit,
    onSelectSeekbarStyle: (String) -> Unit,
    onSelectDeck: (PlayerDeck) -> Unit,
) {
    val theme = LocalPBTheme.current
    val tapeBeige = Color(0xFFD4B483)
    val vintageGold = Color(0xFFE0A96D)

    val transition = rememberInfiniteTransition(label = "tapeReelAnim")
    val reelRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "reelRotate",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close", tint = vintageGold)
            }
            Text("ARCHIVETUNE • TAPE DECK", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tapeBeige, letterSpacing = 2.sp)
            IconButton(onClick = onOpenEqualizer) {
                Icon(Icons.Rounded.Equalizer, contentDescription = "EQ", tint = vintageGold)
            }
        }

        PlayerDeckSwitcher(activeDeck = PlayerDeck.ARCHIVE, onSelectDeck = onSelectDeck)

        Spacer(Modifier.height(10.dp))

        // Physical Cassette Tape Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF201D1A))
                .border(2.dp, tapeBeige.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .padding(14.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Cassette Label
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(tapeBeige.copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "SIDE A: ${song?.title ?: "TAPE RECORDING"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2C241E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Dual Cassette Reels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left Spool
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .rotate(if (isPlaying) reelRotation else 0f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF201D1A)))
                    }

                    // Center Tape Window
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("CrO2 90", fontSize = 10.sp, color = vintageGold, fontWeight = FontWeight.Bold)
                    }

                    // Right Spool
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .rotate(if (isPlaying) reelRotation else 0f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF201D1A)))
                    }
                }

                Text(
                    text = "ANALOG HIGH BIAS • STEREO DOLBY B-NR",
                    fontSize = 9.sp,
                    color = tapeBeige.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = song?.title ?: "Tape Inserted",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song?.artist ?: "Unknown Artist",
            fontSize = 13.sp,
            color = vintageGold,
        )

        Spacer(Modifier.height(14.dp))

        UniversalSeekbar(
            style = currentSeekbarStyle,
            positionMs = positionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatDuration(positionMs), fontSize = 11.sp, color = tapeBeige)
            Text(formatDuration(durationMs), fontSize = 11.sp, color = tapeBeige)
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SeekbarStyle.entries, key = { it.id }) { style ->
                SeekbarMiniLivePreview(
                    style = style,
                    isSelected = style == currentSeekbarStyle,
                    onClick = { onSelectSeekbarStyle(style.id) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Chrome Physical Transport Keys
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF2D2926))
                .border(1.dp, tapeBeige.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onSkipPrevious) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Rewind", tint = Color.White)
            }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(vintageGold)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = "Play", tint = Color.Black)
            }
            IconButton(onClick = onSkipNext) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Fast Forward", tint = Color.White)
            }
            IconButton(onClick = onOpenLyrics) {
                Icon(Icons.Rounded.Lyrics, contentDescription = "Lyrics", tint = tapeBeige)
            }
            IconButton(onClick = onOpenQueue) {
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Queue", tint = tapeBeige)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
