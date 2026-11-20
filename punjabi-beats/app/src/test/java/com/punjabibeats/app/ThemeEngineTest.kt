package com.punjabibeats.app

import androidx.compose.ui.graphics.Color
import com.punjabibeats.app.ui.theme.DesignTokens
import com.punjabibeats.app.ui.theme.PBThemes
import com.punjabibeats.app.ui.theme.PlayerStyle
import com.punjabibeats.app.ui.theme.pbThemeById
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ThemeEngineTest {

    @Test
    fun defaultThemeMatchesPunjabiRoyalePalette() {
        val defaultTheme = pbThemeById(null)
        assertNotNull(defaultTheme)
        assertEquals("Punjabi Royale", defaultTheme.name)
        assertEquals("👑", defaultTheme.emoji)
        assertEquals(DesignTokens.SunsetAccent, defaultTheme.accent)
        assertEquals(DesignTokens.SunsetBg, defaultTheme.background)
        assertEquals(DesignTokens.SunsetSurface, defaultTheme.surface)
        assertEquals(DesignTokens.SunsetCard, defaultTheme.card)
        assertEquals(DesignTokens.SunsetSecondary, defaultTheme.secondary)
        assertEquals(PlayerStyle.SUNSET_GLASS, defaultTheme.style)
    }

    @Test
    fun punjabiRoyaleThemeResolvableByLegacyId() {
        val theme = pbThemeById("sunset_glow")
        assertEquals("Punjabi Royale", theme.name)
        assertEquals(DesignTokens.SunsetAccent, theme.accent)
    }

    @Test
    fun aiMoodThemeResolvableById() {
        val theme = pbThemeById("ai_mood")
        assertNotNull(theme)
        assertEquals("AI Mood Studio", theme.name)
        assertEquals("✨", theme.emoji)
        assertEquals(DesignTokens.MoodAccent, theme.accent)
        assertEquals(PlayerStyle.MOOD_STUDIO, theme.style)
    }

    @Test
    fun aquaGlassThemeResolvableById() {
        val theme = pbThemeById("aqua_glass")
        assertNotNull(theme)
        assertEquals("Aqua Frost", theme.name)
        assertEquals("🌊", theme.emoji)
        assertEquals(DesignTokens.AquaAccent, theme.accent)
        assertEquals(PlayerStyle.AQUA_GLASS, theme.style)
    }

    @Test
    fun moodbeatThemeResolvableById() {
        val theme = pbThemeById("moodbeat")
        assertNotNull(theme)
        assertEquals("MoodBeat Vinyl", theme.name)
        assertEquals("🟣", theme.emoji)
        assertEquals(DesignTokens.TurntableAccent, theme.accent)
        assertEquals(PlayerStyle.MOODBEAT, theme.style)
    }

    @Test
    fun pbThemesContainsSevenModernThemes() {
        assertEquals(7, PBThemes.size)
        val ids = PBThemes.map { it.id }
        assertEquals(
            listOf("sunset_glow", "ai_mood", "aqua_glass", "cloud_blue", "urban_neon", "moodbeat", "electric_aurora"),
            ids
        )
    }

    @Test
    fun legacyFallbackResolvesSafely() {
        val fallback = pbThemeById("classic")
        assertNotNull(fallback)
        assertEquals("sunset_glow", fallback.id)
    }
}
