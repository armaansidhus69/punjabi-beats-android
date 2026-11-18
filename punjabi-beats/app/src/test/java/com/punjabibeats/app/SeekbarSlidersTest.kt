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
    fun requiredNamedStylesExist() {
        assertEquals("comet", SeekbarStyle.COMET.id)
        assertEquals("lightning", SeekbarStyle.LIGHTNING.id)
        assertEquals("constellation", SeekbarStyle.CONSTELLATION.id)
        assertEquals("bubbles", SeekbarStyle.BUBBLES.id)
        assertEquals("rgb_glow", SeekbarStyle.RGB_GLOW.id)
        assertEquals("dna_helix", SeekbarStyle.DNA_HELIX.id)
        assertEquals("liquid_tube", SeekbarStyle.LIQUID_TUBE.id)
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
        // Aliases check
        assertEquals(SeekbarStyle.LIGHTNING, SeekbarStyle.fromId("neon_cyan"))
        assertEquals(SeekbarStyle.CONSTELLATION, SeekbarStyle.fromId("orbit"))
        assertEquals(SeekbarStyle.BUBBLES, SeekbarStyle.fromId("water_ripple"))
        assertEquals(SeekbarStyle.LIQUID_TUBE, SeekbarStyle.fromId("minimalist_hairline"))

        // Fallback check
        val fallback = SeekbarStyle.fromId("unknown_slider_id_123")
        assertEquals(SeekbarStyle.CLASSIC_WAVEFORM, fallback)
    }
}
