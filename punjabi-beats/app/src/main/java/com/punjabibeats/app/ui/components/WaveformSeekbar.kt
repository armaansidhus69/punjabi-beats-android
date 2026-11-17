package com.punjabibeats.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.punjabibeats.app.ui.theme.LocalPBTheme
import com.punjabibeats.app.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Animated waveform seekbar: deterministic bar envelope, dynamic theme accent fill for the
 * played part (adapts to Vice City pink, Cyber cyan, Punjab gold), shimmer while playing, tap + horizontal drag to seek.
 */
@Composable
fun WaveformSeekbar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = LocalPBTheme.current.accent,
) {
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 220),
        label = "waveProgress",
    )
    val phase by rememberInfiniteTransition(label = "wavePhase").animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val bars = remember {
        List(72) { i ->
            (abs(sin(i * 0.55)) * 0.55 + abs(sin(i * 0.17)) * 0.45)
                .coerceIn(0.12, 1.0).toFloat()
        }
    }
    var dragFraction by remember { mutableFloatStateOf(Float.NaN) }

    BoxWithConstraints(modifier = modifier.height(56.dp)) {
        val widthPx = constraints.maxWidth.toFloat()
        val duration = durationMs

        fun commitSeek(x: Float) {
            if (duration > 0 && widthPx > 0f) {
                val targetFraction = (x.coerceIn(0f, widthPx) / widthPx).coerceIn(0f, 1f)
                onSeek((targetFraction * duration).toLong())
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
                .pointerInput(duration, enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset -> commitSeek(offset.x) }
                }
                .pointerInput(duration, enabled) {
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
            val progress = if (dragFraction.isNaN()) animatedFraction else dragFraction
            val barCount = bars.size
            val effectiveBarWidth = (size.width / (barCount * 1.5f)).coerceIn(2f, 4.dp.toPx())
            val gap = if (barCount > 1) ((size.width - barCount * effectiveBarWidth) / (barCount - 1)).coerceAtLeast(0f) else 0f
            val centerY = size.height / 2f
            bars.forEachIndexed { index, base ->
                val played = (index + 0.5f) / bars.size <= progress
                val shimmer = if (isPlaying) 0.78f + 0.22f * sin(phase + index * 0.35f) else 1f
                val height = size.height * 0.82f * base * (if (played) shimmer else 0.6f)
                drawRoundRect(
                    color = if (played) accentColor else TextSecondary.copy(alpha = 0.35f),
                    topLeft = Offset(index * (effectiveBarWidth + gap), centerY - height / 2f),
                    size = Size(effectiveBarWidth, height),
                    cornerRadius = CornerRadius(effectiveBarWidth / 2f),
                )
            }
        }
    }
}
