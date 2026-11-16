package com.punjabibeats.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Strict dark palette matching the showcase mockup
val BgBase = Color(0xFF0B0A10)
val Background = Color(0xFF0B0A10)
val SurfaceBase = Color(0xFF15141C)
val SurfaceDark = Color(0xFF1C1B25)
val SurfaceElevated = Color(0xFF1C1B25)
val SurfaceHigh = Color(0xFF262433)
val SurfaceCard = Color(0xFF1C1B25)

val AccentGold = Color(0xFFE2BC3F)
val Gold = Color(0xFFE2BC3F)
val AccentGoldSoft = Color(0xFFF5D66C)
val AccentPurple = Color(0xFF922BDE)
val Purple = Color(0xFF922BDE)
val NeonPurpleGlow = Color(0x66922BDE)
val AccentRed = Color(0xFFE50914)

val TextPrimary = Color(0xFFF5F3FA)
val White = Color(0xFFF5F3FA)
val TextSecondary = Color(0xFFA5A2AF)
val Grey = Color(0xFFA5A2AF)
val OutlineDim = Color(0xFF2E263D)
val HighlightRowBg = Color(0xFF30251A)

val GlassStrokeStart = Color.White.copy(alpha = 0.16f)
val GlassStrokeEnd = Color.White.copy(alpha = 0.03f)
val GlassFillStart = Color.White.copy(alpha = 0.09f)
val GlassFillEnd = Color.White.copy(alpha = 0.02f)

val GoldGradient = Brush.linearGradient(listOf(AccentGold, AccentGoldSoft))
val HeroGradient = Brush.verticalGradient(
    listOf(Color(0xFF3A2E10), Color(0xFF1A1207), BgBase)
)
val SunsetSkyGradient = Brush.verticalGradient(
    listOf(Color(0xFF3B165E), Color(0xFF6B2175), Color(0xFF9E3852), Color(0xFF5A2234).copy(alpha = 0.4f), Color(0xFF0B0A10))
)
val BrandGradient = Brush.linearGradient(listOf(AccentPurple, AccentGold))

/** Per-mood gradients for the home grid, keyed by MoodCategory.id matching mockup. */
val MoodGradients: Map<String, List<Color>> = mapOf(
    "bhangra" to listOf(Color(0xFFB31325), Color(0xFF630B15)),
    "melodic" to listOf(Color(0xFF24345E), Color(0xFF111A30)),
    "gym" to listOf(Color(0xFF854016), Color(0xFF48230D)),
    "sad" to listOf(Color(0xFF25374A), Color(0xFF131D26)),
    "bass" to listOf(Color(0xFF542477), Color(0xFF28113A)),
)

fun moodGradient(id: String): List<Color> = MoodGradients[id] ?: listOf(SurfaceElevated, SurfaceBase)
