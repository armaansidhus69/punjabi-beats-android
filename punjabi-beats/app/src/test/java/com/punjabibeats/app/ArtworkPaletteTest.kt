package com.punjabibeats.app

import com.punjabibeats.app.ui.theme.adaptedArtworkSaturation
import com.punjabibeats.app.ui.theme.relativeLuminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkPaletteTest {

    @Test
    fun adaptedArtworkSaturation_belowThreshold_returnsUnchanged() {
        val lowSat = 0.05f
        val adapted = adaptedArtworkSaturation(lowSat, minimum = 0.20f, maximum = 0.62f)
        assertEquals(lowSat, adapted, 0.001f)
    }

    @Test
    fun adaptedArtworkSaturation_aboveThreshold_clampsWithinRange() {
        val midSat = 0.15f
        val adapted = adaptedArtworkSaturation(midSat, minimum = 0.25f, maximum = 0.60f)
        assertEquals(0.25f, adapted, 0.001f)

        val highSat = 0.90f
        val adaptedHigh = adaptedArtworkSaturation(highSat, minimum = 0.25f, maximum = 0.60f)
        assertEquals(0.60f, adaptedHigh, 0.001f)
    }

    @Test
    fun relativeLuminance_pureBlackAndWhite() {
        val blackLuminance = relativeLuminance(0xFF000000.toInt())
        assertEquals(0f, blackLuminance, 0.001f)

        val whiteLuminance = relativeLuminance(0xFFFFFFFF.toInt())
        assertEquals(1f, whiteLuminance, 0.01f)
    }

    @Test
    fun relativeLuminance_pureRedGreenBlue() {
        val redLuminance = relativeLuminance(0xFFFF0000.toInt())
        val greenLuminance = relativeLuminance(0xFF00FF00.toInt())
        val blueLuminance = relativeLuminance(0xFF0000FF.toInt())

        // Green has highest perceived luminance in WCAG standards, followed by red, then blue
        assertTrue(greenLuminance > redLuminance)
        assertTrue(redLuminance > blueLuminance)
    }
}
