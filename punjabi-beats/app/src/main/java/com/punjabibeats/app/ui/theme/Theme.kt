package com.punjabibeats.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PbColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = Color(0xFF14100A),
    primaryContainer = Color(0xFF3D3416),
    onPrimaryContainer = AccentGoldSoft,
    secondary = AccentRed,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A1114),
    onSecondaryContainer = Color(0xFFFFB3B8),
    tertiary = AccentGoldSoft,
    background = BgBase,
    onBackground = TextPrimary,
    surface = SurfaceBase,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = BgBase,
    surfaceContainerLow = SurfaceBase,
    surfaceContainer = SurfaceElevated,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHigh,
    outline = OutlineDim,
    outlineVariant = OutlineDim,
    error = AccentRed,
    onError = Color.White,
    scrim = Color.Black,
)

private val PbShapes = androidx.compose.material3.Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun PunjabiBeatsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PbColorScheme,
        typography = PbTypography,
        shapes = PbShapes,
        content = content,
    )
}
