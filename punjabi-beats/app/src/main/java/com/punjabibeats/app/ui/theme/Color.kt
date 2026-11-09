package com.punjabibeats.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Strict dark palette from the product spec.
val BgBase = Color(0xFF0A0A0F)
val SurfaceBase = Color(0xFF13131A)
val SurfaceElevated = Color(0xFF1C1C26)
val SurfaceHigh = Color(0xFF26262F)
val AccentGold = Color(0xFFD4AF37)
val AccentGoldSoft = Color(0xFFF0D77B)
val AccentRed = Color(0xFFE50914)
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFF9E9EA8)
val OutlineDim = Color(0xFF2A2A35)

val GlassStrokeStart = Color.White.copy(alpha = 0.16f)
val GlassStrokeEnd = Color.White.copy(alpha = 0.03f)
val GlassFillStart = Color.White.copy(alpha = 0.09f)
val GlassFillEnd = Color.White.copy(alpha = 0.02f)

val GoldGradient = Brush.linearGradient(listOf(AccentGold, AccentGoldSoft))
val HeroGradient = Brush.verticalGradient(
    listOf(Color(0xFF3A2E10), Color(0xFF1A1207), BgBase)
)
val BrandGradient = Brush.linearGradient(listOf(AccentRed, AccentGold))

/** Per-mood gradients for the home grid, keyed by MoodCategory.id. */
val MoodGradients: Map<String, List<Color>> = mapOf(
    "bhangra" to listOf(Color(0xFFE50914), Color(0xFF7A1010)),
    "melodic" to listOf(Color(0xFF2C3E70), Color(0xFF101426)),
    "gym" to listOf(Color(0xFFB3541E), Color(0xFF3D1B0A)),
    "sad" to listOf(Color(0xFF31445A), Color(0xFF0F1420)),
    "bass" to listOf(Color(0xFF5A2A8C), Color(0xFF1A0E2B)),
)

fun moodGradient(id: String): List<Color> = MoodGradients[id] ?: listOf(SurfaceElevated, SurfaceBase)
