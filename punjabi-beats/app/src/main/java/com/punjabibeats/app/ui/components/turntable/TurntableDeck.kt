package com.punjabibeats.app.ui.components.turntable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.punjabibeats.app.ui.theme.LocalPBTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-End Audiophile Turntable & Vinyl Deck.
 * Features realistic aluminum platter with strobe dots, continuous spinning vinyl record,
 * authentic sweeping light reflections, interactive 33/45 RPM switcher, and an animated
 * tonearm that cues down onto the groove and follows song progress.
 */
@Composable
fun TurntableDeck(
    artworkUrl: String?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onSpeedChange: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
) {
    val theme = LocalPBTheme.current
    var is45Rpm by remember { mutableStateOf(false) }

    val rotation = remember { Animatable(0f) }
    val rpmSpeedMs = if (is45Rpm) 1333 else 1800 // 45 RPM vs 33.3 RPM rotation period

    LaunchedEffect(isPlaying, is45Rpm) {
        if (isPlaying) {
            while (true) {
                val normalized = rotation.value % 360f
                rotation.snapTo(normalized)
                rotation.animateTo(
                    targetValue = normalized + 360f,
                    animationSpec = tween(durationMillis = rpmSpeedMs, easing = LinearEasing),
                )
            }
        }
    }

    // Tonearm angle: Parked at -32° when stopped. When playing, cues down to -12° and tracks across the record to +10°.
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val targetArmAngle = if (isPlaying) -12f + (progress * 22f) else -34f
    val animatedArmAngle by animateFloatAsState(
        targetValue = targetArmAngle,
        animationSpec = tween(durationMillis = 800),
        label = "tonearmAngle",
    )

    Column(
        modifier = modifier
            .width(size + 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF1E1C24),
                        Color(0xFF131218),
                        Color(0xFF0F0E14),
                    )
                )
            )
            .border(1.5.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Turntable Plinth Header with Power Indicator and RPM Switcher
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) Color(0xFF00FF66) else Color(0xFFFF4444)),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isPlaying) "MOTOR: ACTIVE" else "MOTOR: STOP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) Color(0xFF00FF66) else theme.muted,
                )
            }

            // Interactive RPM speed toggle (33 ⅓ vs 45 RPM)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (is45Rpm) theme.accent else theme.surface)
                    .border(1.dp, if (is45Rpm) theme.accent else theme.panel2, RoundedCornerShape(8.dp))
                    .clickable {
                        is45Rpm = !is45Rpm
                        onSpeedChange?.invoke(if (is45Rpm) 1.25f else 1.0f)
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = if (is45Rpm) "45 RPM (FAST)" else "33 ⅓ RPM",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (is45Rpm) theme.background else theme.accent,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Center Deck: Platter, Vinyl, Center Spindle, and Tonearm
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF0A090D)),
            contentAlignment = Alignment.Center,
        ) {
            // Rotating Aluminum Platter with Rim Strobe Dots
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = rotation.value },
            ) {
                val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                val outerRadius = size.toPx() / 2f - 2.dp.toPx()
                // Brushed platter rim
                drawCircle(
                    color = Color(0xFF2C2A36),
                    radius = outerRadius,
                    center = center,
                    style = Stroke(width = 8.dp.toPx()),
                )
                // Strobe dot markers along platter rim
                val strobeCount = 36
                for (i in 0 until strobeCount) {
                    val angle = (i * (360f / strobeCount)) * (PI.toFloat() / 180f)
                    val dotRadius = outerRadius - 4.dp.toPx()
                    val dx = center.x + cos(angle) * dotRadius
                    val dy = center.y + sin(angle) * dotRadius
                    drawCircle(Color(0xFF757185), radius = 1.8.dp.toPx(), center = Offset(dx, dy))
                }
            }

            // 12-Inch Vinyl Record Disc
            Box(
                modifier = Modifier
                    .size(size - 18.dp)
                    .graphicsLayer { rotationZ = rotation.value }
                    .clip(CircleShape)
                    .background(Color(0xFF100F14))
                    .border(1.5.dp, Color(0xFF2E2C38), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                // Microgroove track lines & sweeping light reflections
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                    val r = (size - 18.dp).toPx() / 2f
                    // Concentric vinyl grooves
                    val grooveRadii = listOf(0.42f, 0.50f, 0.58f, 0.66f, 0.74f, 0.82f, 0.90f)
                    for (frac in grooveRadii) {
                        drawCircle(
                            color = Color(0xFF1D1B22),
                            radius = r * frac,
                            center = center,
                            style = Stroke(width = 1.2.dp.toPx()),
                        )
                    }
                    // Sweeping light reflections
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent,
                            ),
                            center = center,
                        ),
                        radius = r - 2.dp.toPx(),
                        center = center,
                    )
                }

                // Center Album Artwork Label
                Box(
                    modifier = Modifier
                        .size(size * 0.38f)
                        .clip(CircleShape)
                        .border(2.dp, theme.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Spindle Hole
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0A090D))
                            .border(1.5.dp, Color(0xFFE2BC3F), CircleShape),
                    )
                }
            }

            // Realistic Metallic Tonearm Overlay
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
            ) {
                val w = size.toPx()
                val pivotX = w * 0.88f
                val pivotY = w * 0.12f
                val pivotCenter = Offset(pivotX, pivotY)

                // Pivot Base
                drawCircle(Color(0xFF1E1C24), radius = 18.dp.toPx(), center = pivotCenter)
                drawCircle(Color(0xFF5A5766), radius = 13.dp.toPx(), center = pivotCenter)
                drawCircle(theme.accent, radius = 5.dp.toPx(), center = pivotCenter)

                // Rotate the arm from the pivot center based on animatedArmAngle
                rotate(degrees = animatedArmAngle, pivot = pivotCenter) {
                    val armLength = w * 0.68f
                    val armPath = Path()
                    armPath.moveTo(pivotX, pivotY)
                    // S-curved classic tonearm shape
                    armPath.cubicTo(
                        pivotX - armLength * 0.35f, pivotY + armLength * 0.25f,
                        pivotX - armLength * 0.55f, pivotY + armLength * 0.65f,
                        pivotX - armLength * 0.70f, pivotY + armLength * 0.95f,
                    )
                    // Arm tube shadow
                    drawPath(armPath, Color.Black.copy(alpha = 0.4f), style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
                    // Arm tube (polished chrome)
                    drawPath(
                        armPath,
                        Brush.linearGradient(listOf(Color(0xFFD6D4DE), Color(0xFF8E8B99), Color(0xFFFFFFFF))),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    )

                    // Headshell & Needle Cartridge at end of arm
                    val needleTip = Offset(pivotX - armLength * 0.70f, pivotY + armLength * 0.95f)
                    drawRoundRect(
                        color = Color(0xFF111016),
                        topLeft = Offset(needleTip.x - 7.dp.toPx(), needleTip.y - 4.dp.toPx()),
                        size = Size(14.dp.toPx(), 18.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    )
                    // Golden cartridge indicator
                    drawCircle(theme.accent, radius = 2.5.dp.toPx(), center = needleTip)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Tonearm Status readout
        Text(
            text = if (isPlaying) "TONEARM: TRACKING GROOVE (${(progress * 100).toInt()}%)" else "TONEARM: CUE REST",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isPlaying) theme.accent else theme.muted,
        )
    }
}
