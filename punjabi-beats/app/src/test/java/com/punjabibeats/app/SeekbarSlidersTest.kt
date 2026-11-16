package com.punjabibeats.app

import com.punjabibeats.app.ui.components.sliders.SeekbarStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekbarSlidersTest {

    @Test
    fun exactlyTwentyTwoSeekbarStylesExist() {
        val styles = SeekbarStyle.entries
        assertEquals("Must have exactly 22 progress slider styles", 22, styles.size)
    }

    @Test
    fun allSeekbarStylesHaveUniqueIdsAndMetadata() {
        val ids = mutableSetOf<String>()
        for (style in SeekbarStyle.entries) {
            assertTrue("Style ID must not be blank: ${style.name}", style.id.isNotBlank())
            assertTrue("Display name must not be blank: ${style.name}", style.displayName.isNotBlank())
            assertTrue("Icon emoji must not be blank: ${style.name}", style.iconEmoji.isNotBlank())
            assertTrue("Description must not be blank: ${style.name}", style.description.isNotBlank())
            assertTrue("Duplicate style ID detected: ${style.id}", ids.add(style.id))
        }
    }

    @Test
    fun seekbarStyleFromIdResolvesCorrectly() {
        for (style in SeekbarStyle.entries) {
            val resolved = SeekbarStyle.fromId(style.id)
            assertEquals("Failed to resolve style from id ${style.id}", style, resolved)
        }
        // Fallback check
        val fallback = SeekbarStyle.fromId("unknown_slider_id_123")
        assertEquals(SeekbarStyle.CLASSIC_WAVEFORM, fallback)
    }
}
