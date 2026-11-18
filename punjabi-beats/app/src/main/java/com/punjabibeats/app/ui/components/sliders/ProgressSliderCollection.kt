package com.punjabibeats.app.ui.components.sliders

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punjabibeats.app.ui.components.WaveformSeekbar
import com.punjabibeats.app.ui.theme.LocalPBTheme
import com.punjabibeats.app.ui.theme.PBTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Universal interactive seekbar that renders any of the 22 Seekbar Styles.
 * Supports smooth tap-to-seek, horizontal scrubbing drag, and real-time playback animations.
 */
@Composable
fun UniversalSeekbar(
    style: SeekbarStyle,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val theme = LocalPBTheme.current

    if (style == SeekbarStyle.CLASSIC_WAVEFORM) {
        WaveformSeekbar(
            positionMs = positionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            onSeek = onSeek,
            modifier = modifier,
            enabled = enabled,
            accentColor = theme.accent,
        )
        return
    }

    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 200),
        label = "universalSeekFraction",
    )
    var dragFraction by remember { mutableFloatStateOf(Float.NaN) }
    val activeFraction = if (dragFraction.isNaN()) animatedFraction else dragFraction

    val infiniteTransition = rememberInfiniteTransition(label = "sliderAnim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "phaseAnim",
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseAnim",
    )

    BoxWithConstraints(modifier = modifier.height(54.dp).padding(horizontal = 4.dp)) {
        val widthPx = constraints.maxWidth.toFloat()
        fun commitSeek(x: Float) {
            if (durationMs > 0 && widthPx > 0f) {
                val targetFraction = (x.coerceIn(0f, widthPx) / widthPx).coerceIn(0f, 1f)
                onSeek((targetFraction * durationMs).toLong())
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(durationMs, enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset -> commitSeek(offset.x) }
                }
                .pointerInput(durationMs, enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            if (widthPx > 0f) dragFraction = (offset.x / widthPx).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            if (!dragFraction.isNaN()) commitSeek(dragFraction * widthPx)
                            dragFraction = Float.NaN
                        },
                        onDragCancel = { dragFraction = Float.NaN },
                    ) { change, _ ->
                        change.consume()
                        if (widthPx > 0f) {
                            dragFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                        }
                    }
                },
        ) {
            val h = size.height
            val w = size.width
            val centerY = h / 2f
            val playedX = w * activeFraction
            val primary = theme.accent
            val secondary = theme.secondary
            val trackMuted = theme.panel2.copy(alpha = 0.6f)

            when (style) {
                SeekbarStyle.CLASSIC_WAVEFORM -> { /* handled above */ }

                SeekbarStyle.WAVY -> {
                    // Base inactive line
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                    // Animated sinusoidal wave up to playedX
                    if (playedX > 0f) {
                        val wavePath = Path()
                        val wavePoints = (playedX / 4f).toInt().coerceAtLeast(1)
                        wavePath.moveTo(0f, centerY)
                        for (i in 0..wavePoints) {
                            val px = minOf(i * 4f, playedX)
                            val waveOffset = sin(px * 0.045f + if (isPlaying) phase * 2f else 0f) * 10.dp.toPx()
                            wavePath.lineTo(px, centerY + waveOffset)
                        }
                        drawPath(wavePath, primary, style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round))
                    }
                    drawCircle(primary, radius = 9.dp.toPx(), center = Offset(playedX, centerY))
                    drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(playedX, centerY))
                }

                SeekbarStyle.RGB_GLOW -> {
                    // Inactive track
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
                    // Rainbow Chroma Brush
                    val chromaColors = listOf(
                        Color(0xFFFF0055),
                        Color(0xFFFFAA00),
                        Color(0xFF00FF66),
                        Color(0xFF00EEFF),
                        Color(0xFF9900FF),
                        Color(0xFFFF0055),
                    )
                    val rainbowBrush = Brush.horizontalGradient(chromaColors, startX = 0f, endX = w)
                    drawLine(rainbowBrush, Offset(0f, centerY), Offset(playedX, centerY), strokeWidth = 8.dp.toPx() * pulse, cap = StrokeCap.Round)
                    drawCircle(Color.White, radius = 10.dp.toPx(), center = Offset(playedX, centerY))
                    drawCircle(Color(0xFF00EEFF), radius = 5.dp.toPx(), center = Offset(playedX, centerY))
                }

                SeekbarStyle.PILL_CAPSULE -> {
                    // Inactive capsule background
                    drawRoundRect(trackMuted, Offset(0f, centerY - 6.dp.toPx()), Size(w, 12.dp.toPx()), CornerRadius(6.dp.toPx(), 6.dp.toPx()))
                    // Active filled capsule
                    drawRoundRect(
                        Brush.horizontalGradient(listOf(secondary, primary)),
                        Offset(0f, centerY - 6.dp.toPx()),
                        Size(playedX, 12.dp.toPx()),
                        CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    )
                    // Large pill thumb
                    drawRoundRect(
                        primary,
                        Offset(playedX - 14.dp.toPx(), centerY - 12.dp.toPx()),
                        Size(28.dp.toPx(), 24.dp.toPx()),
                        CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                    )
                    drawRoundRect(
                        Color.White,
                        Offset(playedX - 4.dp.toPx(), centerY - 6.dp.toPx()),
                        Size(8.dp.toPx(), 12.dp.toPx()),
                        CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                }

                SeekbarStyle.SEGMENTS -> {
                    val count = 28
                    val gap = 4.dp.toPx()
                    val blockWidth = (w - (count - 1) * gap) / count
                    for (i in 0 until count) {
                        val startX = i * (blockWidth + gap)
                        val isFilled = startX <= playedX
                        val blockColor = if (isFilled) primary else trackMuted
                        drawRoundRect(
                            color = blockColor,
                            topLeft = Offset(startX, centerY - 8.dp.toPx()),
                            size = Size(blockWidth, 16.dp.toPx()),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                        )
                    }
                }

                SeekbarStyle.EQUALIZER -> {
                    val barCount = 36
                    val barWidth = 4.dp.toPx()
                    val gap = (w - barCount * barWidth) / (barCount - 1)
                    for (i in 0 until barCount) {
                        val bx = i * (barWidth + gap)
                        val isPlayed = bx <= playedX
                        val animH = if (isPlaying) abs(sin(i * 0.4f + phase * 2f)) * 14.dp.toPx() + 6.dp.toPx() else 8.dp.toPx()
                        val col = if (isPlayed) primary else trackMuted
                        drawRoundRect(col, Offset(bx, centerY - animH / 2f), Size(barWidth, animH), CornerRadius(2.dp.toPx(), 2.dp.toPx()))
                    }
                }

                SeekbarStyle.LIGHTNING -> {
                    val lightningCyan = Color(0xFF00F5FF)
                    val lightningVolt = Color(0xFFE0FFFF)
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 3.dp.toPx())
                    val steps = 24
                    val stepW = playedX / steps.coerceAtLeast(1)
                    for (s in 0 until steps) {
                        val x1 = s * stepW
                        val x2 = (s + 1) * stepW
                        val jitter = if (s % 2 == 0) sin(s * 1.7f + phase) * 4.dp.toPx() else -sin(s * 1.3f + phase) * 4.dp.toPx()
                        drawLine(lightningCyan, Offset(x1, centerY), Offset(x2, centerY + jitter), strokeWidth = 2.5.dp.toPx())
                    }
                    drawLine(lightningCyan.copy(alpha = 0.45f * pulse), Offset(0f, centerY), Offset(playedX, centerY), strokeWidth = 10.dp.toPx())
                    drawCircle(lightningVolt, radius = 7.dp.toPx(), center = Offset(playedX, centerY))
                }

                SeekbarStyle.CONSTELLATION -> {
                    val starColor = Color(0xFFCEBDFF)
                    val glowColor = Color(0xFF9D7BFF)
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 2.dp.toPx())
                    drawLine(glowColor, Offset(0f, centerY), Offset(playedX, centerY), strokeWidth = 3.dp.toPx())
                    val nodeCount = 6
                    for (n in 1..nodeCount) {
                        val nx = (playedX * n) / (nodeCount + 1)
                        val ny = centerY + sin(n * 2.2f + phase) * 5.dp.toPx()
                        drawCircle(glowColor.copy(alpha = 0.4f), radius = 6.dp.toPx(), center = Offset(nx, ny))
                        drawCircle(starColor, radius = 3.dp.toPx(), center = Offset(nx, ny))
                    }
                    drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(playedX, centerY))
                    drawCircle(glowColor.copy(alpha = 0.5f * pulse), radius = 13.dp.toPx(), center = Offset(playedX, centerY))
                }

                SeekbarStyle.HEARTBEAT -> {
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 3.dp.toPx())
                    val ecgPath = Path()
                    ecgPath.moveTo(0f, centerY)
                    val step = 6.dp.toPx()
                    var cx = 0f
                    var idx = 0
                    while (cx < playedX) {
                        idx++
                        val isSpike = (idx % 7 == 0)
                        if (isSpike) {
                            ecgPath.lineTo(cx + step * 0.25f, centerY - 14.dp.toPx())
                            ecgPath.lineTo(cx + step * 0.5f, centerY + 14.dp.toPx())
                            ecgPath.lineTo(cx + step * 0.75f, centerY - 6.dp.toPx())
                        }
                        cx += step
                        ecgPath.lineTo(cx.coerceAtMost(playedX), centerY)
                    }
                    drawPath(ecgPath, Color(0xFFFF2A6D), style = Stroke(width = 3.5.dp.toPx()))
                    drawCircle(Color(0xFFFF2A6D), radius = 8.dp.toPx() * pulse, center = Offset(playedX, centerY))
                }

                SeekbarStyle.DNA_HELIX -> {
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 2.dp.toPx())
                    val strand1 = Path()
                    val strand2 = Path()
                    strand1.moveTo(0f, centerY)
                    strand2.moveTo(0f, centerY)
                    val points = (playedX / 4f).toInt().coerceAtLeast(1)
                    for (i in 0..points) {
                        val px = i * 4f
                        val wave = sin(px * 0.06f + if (isPlaying) phase else 0f) * 10.dp.toPx()
                        strand1.lineTo(px, centerY + wave)
                        strand2.lineTo(px, centerY - wave)
                    }
                    drawPath(strand1, primary, style = Stroke(width = 3.dp.toPx()))
                    drawPath(strand2, secondary, style = Stroke(width = 3.dp.toPx()))
                    drawCircle(primary, radius = 7.dp.toPx(), center = Offset(playedX, centerY))
                }

                SeekbarStyle.COMET -> {
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 3.dp.toPx())
                    val cometBrush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, primary.copy(alpha = 0.3f), primary),
                        startX = (playedX - 80.dp.toPx()).coerceAtLeast(0f),
                        endX = playedX,
                    )
                    drawLine(cometBrush, Offset((playedX - 80.dp.toPx()).coerceAtLeast(0f), centerY), Offset(playedX, centerY), strokeWidth = 6.dp.toPx())
                    // Sparkling head
                    drawCircle(Color.White, radius = 7.dp.toPx() * pulse, center = Offset(playedX, centerY))
                    drawCircle(primary, radius = 10.dp.toPx(), center = Offset(playedX, centerY), style = Stroke(2.dp.toPx()))
                }

                SeekbarStyle.VINYL_GROOVE -> {
                    // Concentric groove lines
                    for (offsetY in listOf(-6.dp.toPx(), -2.dp.toPx(), 2.dp.toPx(), 6.dp.toPx())) {
                        drawLine(trackMuted.copy(alpha = 0.35f), Offset(0f, centerY + offsetY), Offset(w, centerY + offsetY), strokeWidth = 1.dp.toPx())
                        drawLine(primary.copy(alpha = 0.7f), Offset(0f, centerY + offsetY), Offset(playedX, centerY + offsetY), strokeWidth = 1.dp.toPx())
                    }
                    // Vinyl stylus needle indicator
                    drawCircle(primary, radius = 8.dp.toPx(), center = Offset(playedX, centerY))
                    drawCircle(Color.Black, radius = 3.dp.toPx(), center = Offset(playedX, centerY))
                }

                SeekbarStyle.RETRO_TAPE -> {
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 10.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(Color(0xFF8B4513), Offset(0f, centerY), Offset(playedX, centerY), strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
                    // Left reel
                    drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(12.dp.toPx(), centerY), style = Stroke(2.dp.toPx()))
                    // Right reel
                    drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(w - 12.dp.toPx(), centerY), style = Stroke(2.dp.toPx()))
                    drawCircle(primary, radius = 9.dp.toPx(), center = Offset(playedX, centerY))
                }

                SeekbarStyle.FIRE_EMBER -> {
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 4.dp.toPx())
                    val flameBrush = Brush.horizontalGradient(listOf(Color(0xFFFF3D00), Color(0xFFFF9100), Color(0xFFFFD600)))
                    drawLine(flameBrush, Offset(0f, centerY), Offset(playedX, centerY), strokeWidth = 7.dp.toPx() * pulse, cap = StrokeCap.Round)
                    drawCircle(Color(0xFFFFD600), radius = 9.dp.toPx(), center = Offset(playedX, centerY))
                    drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(playedX, centerY))
                }

                SeekbarStyle.BUBBLES -> {
                    val bubbleColor = Color(0xFF38BDF8)
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(bubbleColor, Offset(0f, centerY), Offset(playedX, centerY), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                    for (b in 0..6) {
                        val bx = (playedX * (b + 1)) / 8f
                        val by = centerY - (abs(sin(phase + b * 0.9f)) * 14.dp.toPx())
                        val bradius = (3f + (b % 4) * 1.5f).dp.toPx()
                        drawCircle(bubbleColor.copy(alpha = 0.6f), radius = bradius, center = Offset(bx, by), style = Stroke(1.5.dp.toPx()))
                        drawCircle(Color.White.copy(alpha = 0.7f), radius = bradius * 0.35f, center = Offset(bx - bradius * 0.3f, by - bradius * 0.3f))
                    }
                    drawCircle(bubbleColor, radius = 7.dp.toPx(), center = Offset(playedX, centerY))
                }

                SeekbarStyle.DOT_MATRIX -> {
                    val dotCount = 38
                    val gap = w / dotCount
                    for (i in 0 until dotCount) {
                        val dx = i * gap + gap / 2f
                        val isLit = dx <= playedX
                        val col = if (isLit) primary else trackMuted.copy(alpha = 0.4f)
                        drawCircle(col, radius = 3.dp.toPx(), center = Offset(dx, centerY))
                    }
                    drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(playedX, centerY))
                }

                SeekbarStyle.AUDIO_SPECTRUM -> {
                    val segments = 40
                    val segW = w / segments
                    for (i in 0 until segments) {
                        val sx = i * segW
                        val isPassed = sx <= playedX
                        val barH = if (isPlaying) abs(cos(i * 0.35f + phase)) * 18.dp.toPx() + 4.dp.toPx() else 6.dp.toPx()
                        val col = if (isPassed) primary else trackMuted
                        drawRect(col, Offset(sx + 1.dp.toPx(), centerY - barH / 2f), Size(segW - 2.dp.toPx(), barH))
                    }
                }

                SeekbarStyle.LASER_BEAM -> {
                    val laserColor = Color(0xFF00FF99)
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 2.dp.toPx())
                    drawLine(laserColor.copy(alpha = 0.35f), Offset(0f, centerY), Offset(playedX, centerY), strokeWidth = 10.dp.toPx())
                    drawLine(laserColor, Offset(0f, centerY), Offset(playedX, centerY), strokeWidth = 3.5.dp.toPx())
                    drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(playedX, centerY))
                }

                SeekbarStyle.LIQUID_TUBE -> {
                    val tubeColor = Color(0xFF10B981)
                    val tubeHeight = 12.dp.toPx()
                    drawRoundRect(
                        color = trackMuted.copy(alpha = 0.4f),
                        topLeft = Offset(0f, centerY - tubeHeight / 2),
                        size = Size(w, tubeHeight),
                        cornerRadius = CornerRadius(tubeHeight / 2, tubeHeight / 2),
                    )
                    if (playedX > 0f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(listOf(tubeColor.copy(alpha = 0.7f), Color(0xFF34D399))),
                            topLeft = Offset(0f, centerY - tubeHeight / 2 + 1.dp.toPx()),
                            size = Size(playedX, tubeHeight - 2.dp.toPx()),
                            cornerRadius = CornerRadius(tubeHeight / 2, tubeHeight / 2),
                        )
                        drawCircle(Color.White, radius = (tubeHeight / 2) - 2.dp.toPx(), center = Offset(playedX - tubeHeight / 4, centerY))
                    }
                }

                SeekbarStyle.PUNJABI_RHYTHM -> {
                    val saffron = Color(0xFFFF7700)
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 3.dp.toPx())
                    val dholPath = Path()
                    dholPath.moveTo(0f, centerY)
                    val step = 8.dp.toPx()
                    var curX = 0f
                    var beat = 0
                    while (curX < playedX) {
                        beat++
                        val dholSpike = when (beat % 4) {
                            0 -> 16.dp.toPx()
                            2 -> 10.dp.toPx()
                            else -> 4.dp.toPx()
                        }
                        dholPath.lineTo(curX + step * 0.5f, centerY - dholSpike)
                        dholPath.lineTo(curX + step, centerY + dholSpike * 0.5f)
                        curX += step
                    }
                    drawPath(dholPath, saffron, style = Stroke(width = 3.dp.toPx()))
                    drawCircle(primary, radius = 8.dp.toPx(), center = Offset(playedX, centerY))
                }

                SeekbarStyle.GOLDEN_SPARKLE -> {
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 3.dp.toPx())
                    val goldBrush = Brush.horizontalGradient(listOf(Color(0xFFE2BC3F), Color(0xFFFFDF70), Color(0xFFE2BC3F)))
                    drawLine(goldBrush, Offset(0f, centerY), Offset(playedX, centerY), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
                    // Golden star sparkles trailing
                    for (offset in listOf(15.dp.toPx(), 30.dp.toPx(), 45.dp.toPx())) {
                        val spX = (playedX - offset).coerceAtLeast(0f)
                        drawCircle(Color(0xFFFFDF70), radius = 2.5.dp.toPx(), center = Offset(spX, centerY - 5.dp.toPx()))
                    }
                    drawCircle(Color(0xFFFFDF70), radius = 9.dp.toPx() * pulse, center = Offset(playedX, centerY))
                    drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(playedX, centerY))
                }

                SeekbarStyle.ELASTIC_STRING -> {
                    // Physics-based vibrating elastic resonant string with harmonic tension
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    if (playedX > 0f) {
                        val stringPath = Path()
                        val steps = (playedX / 6f).toInt().coerceAtLeast(2)
                        stringPath.moveTo(0f, centerY)
                        for (i in 1..steps) {
                            val px = (i.toFloat() / steps) * playedX
                            val harmonicTension = sin((px / playedX) * Math.PI.toFloat())
                            val vibration = if (isPlaying) sin(phase * 4f + i * 0.4f) * 7.dp.toPx() * harmonicTension else 0f
                            stringPath.lineTo(px, centerY + vibration)
                        }
                        drawPath(stringPath, Brush.horizontalGradient(listOf(primary, secondary)), style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))
                    }
                    drawCircle(secondary.copy(alpha = 0.4f), radius = 13.dp.toPx() * pulse, center = Offset(playedX, centerY))
                    drawCircle(primary, radius = 9.dp.toPx(), center = Offset(playedX, centerY))
                    drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(playedX, centerY))
                }
            }
        }
    }
}

/**
 * Bottom Sheet allowing user to choose any of the 22 seekbar styles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeekbarStylePickerSheet(
    currentStyle: SeekbarStyle,
    onSelectStyle: (SeekbarStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    val theme = LocalPBTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = theme.surface,
        contentColor = theme.text,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Progress Slider Collection",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.accent,
                    )
                    Text(
                        text = "Choose from 22 live interactive progress seekbars",
                        fontSize = 13.sp,
                        color = theme.muted,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = theme.muted)
                }
            }

            Spacer(Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().height(420.dp),
            ) {
                items(SeekbarStyle.entries, key = { it.id }) { style ->
                    val isSelected = style == currentStyle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) theme.accent.copy(alpha = 0.15f) else theme.card)
                            .border(
                                1.5.dp,
                                if (isSelected) theme.accent else theme.panel2,
                                RoundedCornerShape(16.dp),
                            )
                            .clickable {
                                onSelectStyle(style)
                                onDismiss()
                            }
                            .padding(12.dp),
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${style.iconEmoji} ${style.displayName}",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) theme.accent else theme.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = theme.accent,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = style.description,
                                fontSize = 11.5.sp,
                                color = theme.muted,
                                maxLines = 2,
                                lineHeight = 15.sp,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}


/**
 * Lightweight, interactive live mini preview of any of the 22 seekbar styles.
 * Renders a miniature dynamic canvas with fluid playback animation.
 */
@Composable
fun SeekbarMiniLivePreview(
    style: SeekbarStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalPBTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "miniSeekbarPhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "miniPhase",
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "miniPulse",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(88.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) theme.accent.copy(alpha = 0.18f) else theme.surface)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) theme.accent else theme.panel2,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
    ) {
        Text(style.iconEmoji, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = style.displayName,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) theme.accent else theme.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.3f)),
        ) {
            val w = size.width
            val h = size.height
            val centerY = h / 2f
            val playedX = w * 0.65f
            val primary = theme.accent
            val trackMuted = Color.White.copy(alpha = 0.2f)

            when (style) {
                SeekbarStyle.LIGHTNING -> {
                    val cyan = Color(0xFF00F5FF)
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 2f)
                    val steps = 6
                    val stepW = playedX / steps
                    for (s in 0 until steps) {
                        val jitter = if (s % 2 == 0) sin(s * 1.5f + phase) * 3f else -sin(s * 1.5f + phase) * 3f
                        drawLine(cyan, Offset(s * stepW, centerY), Offset((s + 1) * stepW, centerY + jitter), strokeWidth = 2f)
                    }
                    drawCircle(Color.White, radius = 3f, center = Offset(playedX, centerY))
                }
                SeekbarStyle.CONSTELLATION -> {
                    val star = Color(0xFFCEBDFF)
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 1.5f)
                    drawLine(primary, Offset(0f, centerY), Offset(playedX, centerY), strokeWidth = 2f)
                    for (n in 1..3) {
                        val nx = (playedX * n) / 4f
                        val ny = centerY + sin(n * 2f + phase) * 3f
                        drawCircle(star, radius = 2f, center = Offset(nx, ny))
                    }
                    drawCircle(Color.White, radius = 3.5f, center = Offset(playedX, centerY))
                }
                SeekbarStyle.BUBBLES -> {
                    val bubbleColor = Color(0xFF38BDF8)
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 2f)
                    drawLine(bubbleColor, Offset(0f, centerY), Offset(playedX, centerY), strokeWidth = 2f)
                    for (b in 1..3) {
                        val bx = (playedX * b) / 4f
                        val by = centerY - (abs(sin(phase + b)) * 4f)
                        drawCircle(bubbleColor, radius = 2f, center = Offset(bx, by), style = Stroke(1f))
                    }
                    drawCircle(bubbleColor, radius = 3.5f, center = Offset(playedX, centerY))
                }
                SeekbarStyle.LIQUID_TUBE -> {
                    val tubeColor = Color(0xFF10B981)
                    drawRoundRect(trackMuted, topLeft = Offset(0f, centerY - 4f), size = Size(w, 8f), cornerRadius = CornerRadius(4f, 4f))
                    drawRoundRect(tubeColor, topLeft = Offset(0f, centerY - 4f), size = Size(playedX, 8f), cornerRadius = CornerRadius(4f, 4f))
                }
                SeekbarStyle.RGB_GLOW -> {
                    val rainbow = Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Magenta))
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 2f)
                    drawLine(rainbow, Offset(0f, centerY), Offset(playedX, centerY), strokeWidth = 3f)
                    drawCircle(Color.White, radius = 3.5f, center = Offset(playedX, centerY))
                }
                SeekbarStyle.DNA_HELIX -> {
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 1f)
                    val steps = 14
                    for (i in 0 until steps) {
                        val x = (playedX * i) / steps
                        val y1 = centerY + sin(i * 0.8f + phase) * 4f
                        val y2 = centerY - sin(i * 0.8f + phase) * 4f
                        drawCircle(primary, radius = 1.5f, center = Offset(x, y1))
                        drawCircle(theme.secondary, radius = 1.5f, center = Offset(x, y2))
                    }
                }
                SeekbarStyle.COMET -> {
                    val cometBrush = Brush.horizontalGradient(listOf(Color.Transparent, primary.copy(alpha = 0.5f), Color.White))
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 1.5f)
                    drawLine(cometBrush, Offset(0f, centerY), Offset(playedX, centerY), strokeWidth = 3f)
                    drawCircle(Color.White, radius = 4f * pulse, center = Offset(playedX, centerY))
                }
                SeekbarStyle.HEARTBEAT -> {
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 1.5f)
                    val ecgColor = Color(0xFFFF2D55)
                    val peakX = playedX * 0.7f
                    drawLine(ecgColor, Offset(0f, centerY), Offset(peakX - 6f, centerY), strokeWidth = 2f)
                    drawLine(ecgColor, Offset(peakX - 6f, centerY), Offset(peakX - 2f, centerY - 6f * pulse), strokeWidth = 2f)
                    drawLine(ecgColor, Offset(peakX - 2f, centerY - 6f * pulse), Offset(peakX + 2f, centerY + 5f), strokeWidth = 2f)
                    drawLine(ecgColor, Offset(peakX + 2f, centerY + 5f), Offset(peakX + 5f, centerY), strokeWidth = 2f)
                    drawLine(ecgColor, Offset(peakX + 5f, centerY), Offset(playedX, centerY), strokeWidth = 2f)
                    drawCircle(Color.White, radius = 3.5f, center = Offset(playedX, centerY))
                }
                SeekbarStyle.EQUALIZER, SeekbarStyle.AUDIO_SPECTRUM -> {
                    val barCount = 10
                    val barW = (w / barCount) - 2f
                    for (b in 0 until barCount) {
                        val bx = b * (barW + 2f)
                        val isPlayed = bx <= playedX
                        val barH = (3f + abs(sin(phase + b * 0.9f)) * 10f)
                        val barColor = if (isPlayed) primary else trackMuted
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(bx, centerY - (barH / 2f)),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(1.5f, 1.5f),
                        )
                    }
                }
                SeekbarStyle.WAVY -> {
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 1.5f)
                    val steps = 18
                    for (i in 0 until steps) {
                        val x = (playedX * i) / steps
                        val y = centerY + sin(i * 0.5f + phase) * 3f
                        drawCircle(primary, radius = 1.5f, center = Offset(x, y))
                    }
                    drawCircle(Color.White, radius = 3.5f, center = Offset(playedX, centerY))
                }
                SeekbarStyle.FIRE_EMBER -> {
                    val ember = Brush.horizontalGradient(listOf(Color(0xFFFF3B30), Color(0xFFFF9500), Color(0xFFFFCC00)))
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 1.5f)
                    drawLine(ember, Offset(0f, centerY), Offset(playedX, centerY), strokeWidth = 3f)
                    drawCircle(Color(0xFFFFCC00), radius = 4f * pulse, center = Offset(playedX, centerY))
                }
                SeekbarStyle.PUNJABI_RHYTHM -> {
                    val saffron = Color(0xFFFF9933)
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 1.5f)
                    drawLine(saffron, Offset(0f, centerY), Offset(playedX, centerY), strokeWidth = 2.5f)
                    drawCircle(saffron, radius = 4f * pulse, center = Offset(playedX, centerY))
                }
                SeekbarStyle.GOLDEN_SPARKLE -> {
                    val gold = Color(0xFFFFD700)
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 1.5f)
                    drawLine(gold, Offset(0f, centerY), Offset(playedX, centerY), strokeWidth = 2.5f)
                    drawCircle(gold, radius = 3.5f * pulse, center = Offset(playedX, centerY))
                }
                SeekbarStyle.ELASTIC_STRING -> {
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 1.5f)
                    val steps = 12
                    for (i in 0 until steps) {
                        val x = (playedX * i) / steps
                        val tension = sin((i.toFloat() / steps) * Math.PI.toFloat())
                        val y = centerY + sin(i * 0.7f + phase * 2f) * 4f * tension
                        drawLine(primary, Offset(x, y), Offset(minOf(x + (playedX / steps), playedX), centerY), strokeWidth = 2f)
                    }
                    drawCircle(primary, radius = 3.5f * pulse, center = Offset(playedX, centerY))
                }
                else -> {
                    drawLine(trackMuted, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 2f)
                    drawLine(primary, Offset(0f, centerY), Offset(playedX, centerY), strokeWidth = 2.5f)
                    drawCircle(primary, radius = 3.5f, center = Offset(playedX, centerY))
                }
            }
        }
    }
}
