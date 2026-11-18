package com.punjabibeats.app

import com.punjabibeats.app.ui.screens.equalizer.EqualizerPreset
import com.punjabibeats.app.ui.screens.equalizer.EqualizerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerViewModelTest {

    @Test
    fun presetsContainExpectedPunjabiSignatures() {
        val presets = EqualizerPreset.entries
        assertTrue("Must have at least 6 presets", presets.size >= 6)

        val presetIds = presets.map { it.id }
        assertTrue("Must contain Gaddi 808 Bass", presetIds.contains("gaddi_808"))
        assertTrue("Must contain Live Dhol Punch", presetIds.contains("dhol_punch"))
        assertTrue("Must contain Vocal & Tumbi", presetIds.contains("vocal_tumbi"))
        assertTrue("Must contain Desi Drill Skitter", presetIds.contains("desi_drill"))
        assertTrue("Must contain Late Night Sufi", presetIds.contains("late_night_sufi"))
        assertTrue("Must contain Studio Flat", presetIds.contains("studio_flat"))
    }

    @Test
    fun allPresetsHaveTenBandsWithinDbBounds() {
        for (preset in EqualizerPreset.entries) {
            assertEquals("Preset ${preset.displayName} must define exactly 10 bands", 10, preset.bandGainsDb.size)
            for (gain in preset.bandGainsDb) {
                assertTrue("Gain $gain in ${preset.displayName} must be between -12dB and +12dB", gain in -12.0f..12.0f)
            }
        }
    }

    @Test
    fun dbToMilliBelConversionIsAccurate() {
        // 1 dB = 100 milliBel
        val gainDb = 5.0f
        val milliBel = (gainDb * 100).toInt().toShort()
        assertEquals(500.toShort(), milliBel)

        val minusGain = -7.5f
        val minusMilliBel = (minusGain * 100).toInt().toShort()
        assertEquals((-750).toShort(), minusMilliBel)
    }

    @Test
    fun equalizerStateDefaultValuesMatchStitchSpec() {
        val defaultState = EqualizerState()
        assertEquals(10, defaultState.bandGainsDb.size)
        assertEquals(10, defaultState.bandFrequencies.size)
        assertEquals(10, defaultState.bandSubLabels.size)
        assertEquals("SUB", defaultState.bandSubLabels[0])
        assertEquals("AIR", defaultState.bandSubLabels[9])
    }
}
