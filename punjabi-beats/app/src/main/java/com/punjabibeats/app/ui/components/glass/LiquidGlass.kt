package com.punjabibeats.app.ui.components.glass

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Frosted liquid glass rendering pipeline inspired by BitChord Section 7.
 * Hardware-accelerated GPU blur (API 31+) with specular highlight rims and graceful fallbacks.
 */
object LiquidGlassDefaults {
    val EdgeWidth: Dp = 0.75.dp
    val DefaultCornerRadius: Dp = 24.dp
    val SurfaceTint: Color = Color(0xFF14121A).copy(alpha = 0.78f)

    val SpecularRim: Brush = Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.38f),
        0.08f to Color.White.copy(alpha = 0.16f),
        0.5f to Color.Transparent,
        1f to Color.White.copy(alpha = 0.08f),
    )

    fun specularRim(accent: Color? = null): Brush = if (accent != null) {
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.38f),
            0.18f to accent.copy(alpha = 0.28f),
            0.60f to Color.Transparent,
            1f to Color.White.copy(alpha = 0.08f),
        )
    } else {
        SpecularRim
    }
}

/** Whether hardware GPU RenderEffect blur is supported on the current Android version. */
fun isGlassSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Hardware-accelerated GPU blur effect for background layers (API 31+).
 * Safe to apply to background image/video canvases without blurring foreground controls.
 */
fun Modifier.gpuBlur(radius: Float = 24f): Modifier = if (isGlassSupported()) {
    graphicsLayer {
        renderEffect = RenderEffect
            .createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
            .asComposeRenderEffect()
    }
} else {
    this
}

/**
 * Modifier applying liquid frosted glass effect with a specular highlight rim.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(LiquidGlassDefaults.DefaultCornerRadius),
    tint: Color = LiquidGlassDefaults.SurfaceTint,
    borderColor: Color = Color.White.copy(alpha = 0.18f),
    borderWidth: Dp = LiquidGlassDefaults.EdgeWidth,
): Modifier = this
    .clip(shape)
    .background(tint, shape)
    .border(borderWidth, borderColor, shape)

/**
 * High-vibrancy frosted glass modifier with top-down specular highlight gradient.
 */
fun Modifier.specularGlass(
    shape: Shape = RoundedCornerShape(LiquidGlassDefaults.DefaultCornerRadius),
    surfaceTint: Color = LiquidGlassDefaults.SurfaceTint,
    specularBrush: Brush = LiquidGlassDefaults.SpecularRim,
    borderWidth: Dp = LiquidGlassDefaults.EdgeWidth,
): Modifier = this
    .clip(shape)
    .background(surfaceTint, shape)
    .border(borderWidth, specularBrush, shape)

/**
 * A dedicated composable container that renders frosted liquid glass with depth shadow
 * and specular highlight rims while keeping child content (icons, text) completely sharp.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LiquidGlassDefaults.DefaultCornerRadius),
    surfaceTint: Color = LiquidGlassDefaults.SurfaceTint,
    borderBrush: Brush = LiquidGlassDefaults.SpecularRim,
    borderWidth: Dp = LiquidGlassDefaults.EdgeWidth,
    elevation: Dp = 12.dp,
    shadowColor: Color = Color.Black.copy(alpha = 0.40f),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(elevation, shape, ambientColor = shadowColor, spotColor = shadowColor)
            .clip(shape)
            .background(surfaceTint, shape)
            .border(borderWidth, borderBrush, shape),
        content = content,
    )
}

/**
 * Menu frost helper for modals, queue drawers, and bottom sheets.
 */
fun menuFrost(baseColor: Color, alpha: Float = 0.85f): Color =
    baseColor.copy(alpha = alpha)
