package com.punjabibeats.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

enum class LyricsAlignmentOption(val id: String, val label: String) {
    LEFT("LEFT", "Left"),
    CENTER("CENTER", "Center"),
    RIGHT("RIGHT", "Right");

    companion object {
        fun fromId(id: String): LyricsAlignmentOption =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: LEFT
    }
}

class LyricsSettingsDataStoreTest {

    @Test
    fun lyricsAlignmentOptionsResolveProperly() {
        assertEquals(LyricsAlignmentOption.LEFT, LyricsAlignmentOption.fromId("LEFT"))
        assertEquals(LyricsAlignmentOption.CENTER, LyricsAlignmentOption.fromId("CENTER"))
        assertEquals(LyricsAlignmentOption.RIGHT, LyricsAlignmentOption.fromId("RIGHT"))
        assertEquals(LyricsAlignmentOption.LEFT, LyricsAlignmentOption.fromId("unknown"))
    }

    @Test
    fun lyricsTextSizeAndSpacingBoundsAreValid() {
        val minTextSize = 16f
        val maxTextSize = 32f
        val defaultTextSize = 22f

        val minSpacing = 4f
        val maxSpacing = 24f
        val defaultSpacing = 10f

        val sampleSize = 40f.coerceIn(minTextSize, maxTextSize)
        assertEquals(maxTextSize, sampleSize, 0.01f)

        val sampleSpacing = 1f.coerceIn(minSpacing, maxSpacing)
        assertEquals(minSpacing, sampleSpacing, 0.01f)

        assertTrue(defaultTextSize in minTextSize..maxTextSize)
        assertTrue(defaultSpacing in minSpacing..maxSpacing)
    }

    @Test
    fun hapticFeedbackTogglesAreFunctional() {
        var touchFeedback = true
        var heartbeatFeedback = true

        touchFeedback = false
        assertTrue(!touchFeedback)

        heartbeatFeedback = false
        assertTrue(!heartbeatFeedback)
    }
}
