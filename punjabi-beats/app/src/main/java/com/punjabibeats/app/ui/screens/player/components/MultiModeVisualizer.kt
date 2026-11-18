package com.punjabibeats.app.ui.screens.player.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class VisualizerMode(val label: String) {
    WAVEFORM("Waveform"),
    DOT_MATRIX("Dot Matrix"),
    NEON_PULSE("Neon Pulse"),
}

@Composable
fun VisualizerSelectorChips(
    currentMode: VisualizerMode,
    onModeSelect: (VisualizerMode) -> Unit,
    modifier: Modifier = Modifier,
    bpm: Int = 142,
    accentColor: Color = Color(0xFFA78BFA),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "HUD:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFCAC4D4),
                letterSpacing = 1.sp,
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0D0E11))
                    .border(1.dp, Color(0xFF292A2D), RoundedCornerShape(20.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                VisualizerMode.entries.forEach { mode ->
                    val isSelected = mode == currentMode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.25f) else Color.Transparent,
                            )
                            .clickable { onModeSelect(mode) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = mode.label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) accentColor else Color(0xFF948E9D),
                        )
                    }
                }
            }
        }

        // Live BPM Indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4EDEA3)),
            )
            Text(
                text = "$bpm BPM",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF4EDEA3),
            )
        }
    }
}

@Composable
fun MultiModeVisualizer(
    mode: VisualizerMode,
    isPlaying: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "multiSpectrum")
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
        anim3, anim5, anim4, anim2, anim1, anim3, anim5, anim2,
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0D0E11))
            .border(1.dp, Color(0xFF1F1F23), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        val barCount = barFactors.size
        val barWidth = 3.5.dp.toPx()
        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        val maxBarHeight = size.height * 0.92f
        val minBarHeight = 3.dp.toPx()
        val gap = if (barCount > 1) {
            (size.width - barCount * barWidth) / (barCount - 1)
        } else 0f
        val centerY = size.height / 2f

        when (mode) {
            VisualizerMode.WAVEFORM -> {
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

            VisualizerMode.DOT_MATRIX -> {
                val dotsPerColumn = 6
                val dotSize = 3.dp.toPx()
                val dotGap = 2.dp.toPx()
                for (i in 0 until barCount) {
                    val factor = barFactors[i]
                    val activeDots = if (isPlaying) (factor * dotsPerColumn).toInt().coerceIn(1, dotsPerColumn) else 1
                    val left = i * (barWidth + gap)

                    for (d in 0 until dotsPerColumn) {
                        val dotBottom = size.height - d * (dotSize + dotGap)
                        val dotTop = dotBottom - dotSize
                        val isLit = d < activeDots
                        drawCircle(
                            color = if (isLit) tint else Color(0xFF292A2D),
                            radius = dotSize / 2f,
                            center = Offset(left + barWidth / 2f, dotTop + dotSize / 2f),
                        )
                    }
                }
            }

            VisualizerMode.NEON_PULSE -> {
                // Neon glow: draw wider blurred background pass, then sharp intense core
                for (i in 0 until barCount) {
                    val factor = barFactors[i]
                    val barH = if (isPlaying) (maxBarHeight * factor).coerceAtLeast(minBarHeight) else minBarHeight
                    val left = i * (barWidth + gap)
                    val top = centerY - barH / 2f

                    // Outer halo
                    drawRoundRect(
                        color = tint.copy(alpha = 0.25f),
                        topLeft = Offset(left - 2.dp.toPx(), top - 2.dp.toPx()),
                        size = Size(barWidth + 4.dp.toPx(), barH + 4.dp.toPx()),
                        cornerRadius = cornerRadius,
                    )
                    // Inner bright core
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barH),
                        cornerRadius = cornerRadius,
                    )
                }
            }
        }
    }
}
