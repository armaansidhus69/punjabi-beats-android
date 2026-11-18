package com.punjabibeats.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class PlayerStyle {
    SUNSET_GLASS, MOOD_STUDIO, AQUA_GLASS, CLOUD_BLUE, URBAN_NEON, MOODBEAT, ELECTRIC_AURORA,
    // Backward-compatibility aliases
    CLASSIC, ANIME, CYBER, PUNJAB, APPLE, LASTWAVE, BITCHORD, ECHO, ARCHIVE
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
        "sunset_glow" -> PlayerStyle.SUNSET_GLASS
        "ai_mood" -> PlayerStyle.MOOD_STUDIO
        "aqua_glass" -> PlayerStyle.AQUA_GLASS
        "cloud_blue" -> PlayerStyle.CLOUD_BLUE
        "urban_neon" -> PlayerStyle.URBAN_NEON
        "moodbeat" -> PlayerStyle.MOODBEAT
        "electric_aurora" -> PlayerStyle.ELECTRIC_AURORA
        // Fallbacks for legacy IDs
        "apple" -> PlayerStyle.APPLE
        "punjab" -> PlayerStyle.PUNJAB
        "cyber" -> PlayerStyle.ELECTRIC_AURORA
        "anime" -> PlayerStyle.MOOD_STUDIO
        else -> PlayerStyle.SUNSET_GLASS
    },
) {
    // Backwards-compatible aliases for app components
    val panel: Color get() = surface
    val panel2: Color get() = card
    val accent2: Color get() = secondary

    fun toLightMode(): PBTheme = this.copy(
        background = Color(0xFFF7F8FA),
        surface = Color(0xFFFFFFFF),
        card = Color(0xFFECEFF4),
        text = Color(0xFF101318),
        muted = Color(0xFF636D7E),
    )
    val heroGradient: Brush get() = when (id) {
        "sunset_glow" -> Brush.verticalGradient(
            listOf(
                Color(0xFF38151D),
                Color(0xFF26101B),
                Color(0xFF140A18),
                Color(0xFF09080E).copy(alpha = 0.5f),
                background,
            )
        )
        "ai_mood" -> Brush.verticalGradient(
            listOf(
                Color(0xFF36102B),
                Color(0xFF240C26),
                Color(0xFF150A1C),
                Color(0xFF0D0B12).copy(alpha = 0.5f),
                background,
            )
        )
        "aqua_glass" -> Brush.verticalGradient(
            listOf(
                Color(0xFF07383D),
                Color(0xFF07272D),
                Color(0xFF07191E),
                Color(0xFF08181A).copy(alpha = 0.5f),
                background,
            )
        )
        "cloud_blue" -> Brush.verticalGradient(
            listOf(
                Color(0xFF0E2E52),
                Color(0xFF0B2039),
                Color(0xFF081628),
                Color(0xFF0A131F).copy(alpha = 0.5f),
                background,
            )
        )
        "urban_neon" -> Brush.verticalGradient(
            listOf(
                Color(0xFF2B2E10),
                Color(0xFF1D200E),
                Color(0xFF12140A),
                Color(0xFF0A0A0A).copy(alpha = 0.5f),
                background,
            )
        )
        "moodbeat" -> Brush.verticalGradient(
            listOf(
                Color(0xFF2B1038),
                Color(0xFF1D0C26),
                Color(0xFF12081C),
                Color(0xFF0A0710).copy(alpha = 0.5f),
                background,
            )
        )
        "electric_aurora" -> Brush.verticalGradient(
            listOf(
                Color(0xFF2B0A42),
                Color(0xFF1A0938),
                Color(0xFF110729),
                Color(0xFF070514).copy(alpha = 0.5f),
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

/** Exactly 7 Next-Gen Glass & Glow Themes matching user design mockups. */
val PBThemes = listOf(
    PBTheme(
        id = "sunset_glow",
        name = "Sunset Glow",
        emoji = "🌅",
        background = DesignTokens.SunsetBg,
        surface = DesignTokens.SunsetSurface,
        card = DesignTokens.SunsetCard,
        accent = DesignTokens.SunsetAccent,
        secondary = DesignTokens.SunsetSecondary,
        text = DesignTokens.SunsetText,
        muted = DesignTokens.SunsetMuted,
        tagline = "Obsidian Glass • Neon Sunset Coral & AI Glow",
        style = PlayerStyle.SUNSET_GLASS,
    ),
    PBTheme(
        id = "ai_mood",
        name = "AI Mood Studio",
        emoji = "✨",
        background = DesignTokens.MoodBg,
        surface = DesignTokens.MoodSurface,
        card = DesignTokens.MoodCard,
        accent = DesignTokens.MoodAccent,
        secondary = DesignTokens.MoodSecondary,
        text = DesignTokens.MoodText,
        muted = DesignTokens.MoodMuted,
        tagline = "Mood Beat • Interactive emotional soundscape",
        style = PlayerStyle.MOOD_STUDIO,
    ),
    PBTheme(
        id = "aqua_glass",
        name = "Aqua Frost",
        emoji = "🌊",
        background = DesignTokens.AquaBg,
        surface = DesignTokens.AquaSurface,
        card = DesignTokens.AquaCard,
        accent = DesignTokens.AquaAccent,
        secondary = DesignTokens.AquaSecondary,
        text = DesignTokens.AquaText,
        muted = DesignTokens.AquaMuted,
        tagline = "Frosted Turquoise Acrylic • Liquid sound flow",
        style = PlayerStyle.AQUA_GLASS,
    ),
    PBTheme(
        id = "cloud_blue",
        name = "Cloud Blue",
        emoji = "☁️",
        background = DesignTokens.CloudBg,
        surface = DesignTokens.CloudSurface,
        card = DesignTokens.CloudCard,
        accent = DesignTokens.CloudAccent,
        secondary = DesignTokens.CloudSecondary,
        text = DesignTokens.CloudText,
        muted = DesignTokens.CloudMuted,
        tagline = "Sky Blue Glass • Neumorphic pill controls",
        style = PlayerStyle.CLOUD_BLUE,
    ),
    PBTheme(
        id = "urban_neon",
        name = "Urban Lime",
        emoji = "⚡",
        background = DesignTokens.UrbanBg,
        surface = DesignTokens.UrbanSurface,
        card = DesignTokens.UrbanCard,
        accent = DesignTokens.UrbanAccent,
        secondary = DesignTokens.UrbanSecondary,
        text = DesignTokens.UrbanText,
        muted = DesignTokens.UrbanMuted,
        tagline = "High Contrast Lime & Black • Street beat energy",
        style = PlayerStyle.URBAN_NEON,
    ),
    PBTheme(
        id = "moodbeat",
        name = "MoodBeat Vinyl",
        emoji = "🟣",
        background = DesignTokens.TurntableBg,
        surface = DesignTokens.TurntableSurface,
        card = DesignTokens.TurntableCard,
        accent = DesignTokens.TurntableAccent,
        secondary = DesignTokens.TurntableSecondary,
        text = DesignTokens.TurntableText,
        muted = DesignTokens.TurntableMuted,
        tagline = "Deep Vinyl Ring • Neon Purple & Orchid turntable",
        style = PlayerStyle.MOODBEAT,
    ),
    PBTheme(
        id = "electric_aurora",
        name = "Electric Aurora",
        emoji = "🌌",
        background = DesignTokens.AuroraBg,
        surface = DesignTokens.AuroraSurface,
        card = DesignTokens.AuroraCard,
        accent = DesignTokens.AuroraAccent,
        secondary = DesignTokens.AuroraSecondary,
        text = DesignTokens.AuroraText,
        muted = DesignTokens.AuroraMuted,
        tagline = "Midnight Aurora • Luminous wave bars & violet depth",
        style = PlayerStyle.ELECTRIC_AURORA,
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
