package com.punjabibeats.app

import androidx.compose.ui.graphics.Color
import com.punjabibeats.app.ui.theme.PBThemes
import com.punjabibeats.app.ui.theme.pbThemeById
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ThemeEngineTest {

    @Test
    fun defaultThemeMatchesViceCityNeonDuskPalette() {
        val defaultTheme = pbThemeById(null)
        assertNotNull(defaultTheme)
        
        // Exact Vice City Neon Dusk Palette from screenshots
        assertEquals("Default theme accent must be Neon Pink (#FF2E93)", Color(0xFFFF2E93), defaultTheme.accent)
        assertEquals("Default theme background must be Obsidian (#08060D)", Color(0xFF08060D), defaultTheme.background)
        assertEquals("Default theme surface must be Dark Glass (#130E1B)", Color(0xFF130E1B), defaultTheme.surface)
        assertEquals("Default theme card must be #1B1224", Color(0xFF1B1224), defaultTheme.card)
        assertEquals("Default theme secondary must be Golden Crown (#FFAA00)", Color(0xFFFFAA00), defaultTheme.secondary)
    }

    @Test
    fun viceCityThemeResolvableById() {
        val viceTheme = pbThemeById("classic")
        assertEquals(Color(0xFFFF2E93), viceTheme.accent)
    }
}
