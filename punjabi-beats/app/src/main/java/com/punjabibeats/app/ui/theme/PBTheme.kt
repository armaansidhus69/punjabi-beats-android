package com.punjabibeats.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class PlayerStyle {
    CLASSIC, ANIME, CYBER, PUNJAB,
    // Backward-compatibility aliases
    APPLE, LASTWAVE, BITCHORD, ECHO, ARCHIVE
}

data class PBTheme(
    val id: String,
    val name: String,
    val emoji: String,
    val background: Color,
    val surface: Color,
    val card: Color,
    val accent: Color,
    val secondary: Color,
    val text: Color,
    val muted: Color,
    val tagline: String = "",
    val style: PlayerStyle = when (id) {
        "classic" -> PlayerStyle.CLASSIC
        "anime" -> PlayerStyle.ANIME
        "cyber" -> PlayerStyle.CYBER
        "punjab" -> PlayerStyle.PUNJAB
        else -> PlayerStyle.CLASSIC
    },
) {
    // Backwards-compatible aliases for app components
    val panel: Color get() = surface
    val panel2: Color get() = card
    val accent2: Color get() = secondary
    val heroGradient: Brush get() = when (id) {
        "classic" -> Brush.verticalGradient(
            listOf(
                Color(0xFF3B165E),
                Color(0xFF6B2175),
                Color(0xFF8B2C58),
                Color(0xFF4A1A2C).copy(alpha = 0.5f),
                background,
            )
        )
        "anime" -> Brush.verticalGradient(
            listOf(
                Color(0xFF3D1047),
                Color(0xFF6D1B68),
                Color(0xFFB52E84),
                Color(0xFF50133C).copy(alpha = 0.5f),
                background,
            )
        )
        "cyber" -> Brush.verticalGradient(
            listOf(
                Color(0xFF062338),
                Color(0xFF0C3854),
                Color(0xFF165B7E),
                Color(0xFF09293D).copy(alpha = 0.5f),
                background,
            )
        )
        "punjab" -> Brush.verticalGradient(
            listOf(
                Color(0xFF3E1F0E),
                Color(0xFF642F13),
                Color(0xFF91471A),
                Color(0xFF45200E).copy(alpha = 0.5f),
                background,
            )
        )
        else -> Brush.verticalGradient(
            listOf(
                accent.copy(alpha = 0.35f),
                card,
                background,
            )
        )
    }
}

/** Exactly 4 premium themes as specified in the Punjabi Beats 4 Theme Engine. */
val PBThemes = listOf(
    PBTheme(
        id = "classic",
        name = "Classic",
        emoji = "🎵",
        background = Color(0xFF0B0A10),
        surface = Color(0xFF15141C),
        card = Color(0xFF211F2B),
        accent = Color(0xFFE2BC3F),
        secondary = Color(0xFF9B7CEB),
        text = Color.White,
        muted = Color(0xFFA5A2AF),
        tagline = "Black + Gold • Clean traditional player",
        style = PlayerStyle.CLASSIC,
    ),
    PBTheme(
        id = "anime",
        name = "Anime Beats",
        emoji = "🌸",
        background = Color(0xFF0D0718),
        surface = Color(0xFF191025),
        card = Color(0xFF29163A),
        accent = Color(0xFFFF58C8),
        secondary = Color(0xFFFFD34F),
        text = Color.White,
        muted = Color(0xFFC1AFCF),
        tagline = "Purple + Pink + Gold • Anime artwork & glowing controls",
        style = PlayerStyle.ANIME,
    ),
    PBTheme(
        id = "cyber",
        name = "Cyberpunk",
        emoji = "⚡",
        background = Color(0xFF07131D),
        surface = Color(0xFF102431),
        card = Color(0xFF173747),
        accent = Color(0xFF43E5F2),
        secondary = Color(0xFF657CFF),
        text = Color.White,
        muted = Color(0xFF9ABBC8),
        tagline = "Blue + Neon Cyan • Futuristic neon player",
        style = PlayerStyle.CYBER,
    ),
    PBTheme(
        id = "punjab",
        name = "Punjab Legend",
        emoji = "👑",
        background = Color(0xFF180D07),
        surface = Color(0xFF29180F),
        card = Color(0xFF3B2718),
        accent = Color(0xFFFFC247),
        secondary = Color(0xFFEA7546),
        text = Color(0xFFFFF6E8),
        muted = Color(0xFFCBB99E),
        tagline = "Brown + Golden • Punjabi royal music experience",
        style = PlayerStyle.PUNJAB,
    ),
)

fun pbThemeById(id: String?): PBTheme =
    PBThemes.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: PBThemes.first()

val LocalPBTheme = staticCompositionLocalOf { PBThemes.first() }

object CurrentPBTheme {
    val theme: PBTheme
        @Composable
        @ReadOnlyComposable
        get() = LocalPBTheme.current
}
