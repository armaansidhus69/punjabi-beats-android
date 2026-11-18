package com.punjabibeats.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
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
        "classic", "vice" -> PlayerStyle.CLASSIC
        "apple" -> PlayerStyle.APPLE
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
        "classic", "vice" -> Brush.verticalGradient(
            listOf(
                Color(0xFF5E124D),
                Color(0xFF3E0B38),
                Color(0xFF230728),
                Color(0xFF13061A).copy(alpha = 0.5f),
                background,
            )
        )
        "apple" -> Brush.verticalGradient(
            listOf(
                Color(0xFFFA233B).copy(alpha = 0.32f),
                Color(0xFF6E0D1B).copy(alpha = 0.18f),
                Color(0xFF1C1C1E).copy(alpha = 0.45f),
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
        name = "Neon Dusk",
        emoji = "👑",
        background = Color(0xFF08060D),
        surface = Color(0xFF130E1B),
        card = Color(0xFF1B1224),
        accent = Color(0xFFFF2E93),
        secondary = Color(0xFFFFAA00),
        text = Color.White,
        muted = Color(0xFFA592B3),
        tagline = "Vice City Neon • Hot Pink + Golden Crown",
        style = PlayerStyle.CLASSIC,
    ),
    PBTheme(
        id = "apple",
        name = "Apple Music",
        emoji = "🍎",
        background = Color(0xFF000000),
        surface = Color(0xFF1C1C1E),
        card = Color(0xFF2C2C2E),
        accent = Color(0xFFFA233B),
        secondary = Color(0xFF0A84FF),
        text = Color.White,
        muted = Color(0xFF8E8E93),
        tagline = "Cupertino Glass • Blurred mesh & authentic iOS controls",
        style = PlayerStyle.APPLE,
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

val LocalPBTheme = compositionLocalOf { PBThemes.first() }

object CurrentPBTheme {
    val theme: PBTheme
        @Composable
        @ReadOnlyComposable
        get() = LocalPBTheme.current
}
