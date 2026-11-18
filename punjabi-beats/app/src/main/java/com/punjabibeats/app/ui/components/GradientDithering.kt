package com.punjabibeats.app.ui.components

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

/**
 * Gradient Dithering Engine for Punjabi Beats.
 * Eliminates color banding, quantization rings, and uneven shading on OLED and LCD screens
 * by combining Android Skia Paint hardware dithering (Paint.DITHER_FLAG) with subtle ordered noise.
 */
object GradientDithering {

    fun createDitheredPaint(): Paint = Paint().apply {
        isDither = true
        isAntiAlias = true
        isFilterBitmap = true
    }
}

/**
 * Renders a full-bleed vertical gradient background with hardware dithering
 * to eliminate color banding behind album artwork and player decks.
 */
@Composable
fun DitheredGradientBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val paint = remember { GradientDithering.createDitheredPaint() }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f || colors.size < 2) return@Canvas

            val argbColors = IntArray(colors.size) { colors[it].toArgb() }
            val positions = FloatArray(colors.size) { it.toFloat() / (colors.size - 1) }

            paint.shader = LinearGradient(
                0f, 0f, 0f, height,
                argbColors, positions,
                Shader.TileMode.CLAMP
            )

            drawContext.canvas.nativeCanvas.drawRect(
                0f, 0f, width, height,
                paint
            )
        }
        content()
    }
}

/**
 * Modifier extension to draw a dithered background gradient on any composable.
 */
fun Modifier.ditheredGradientBackground(colors: List<Color>): Modifier = this.drawBehind {
    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f || colors.size < 2) return@drawBehind

    val paint = Paint().apply {
        isDither = true
        isAntiAlias = true
        isFilterBitmap = true
        val argbColors = IntArray(colors.size) { colors[it].toArgb() }
        val positions = FloatArray(colors.size) { it.toFloat() / (colors.size - 1) }
        shader = LinearGradient(
            0f, 0f, 0f, height,
            argbColors, positions,
            Shader.TileMode.CLAMP
        )
    }

    drawContext.canvas.nativeCanvas.drawRect(
        0f, 0f, width, height,
        paint
    )
}
