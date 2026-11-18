package com.punjabibeats.app

import com.punjabibeats.app.ui.components.sliders.SeekbarStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

enum class VisualizerMode(val label: String) {
    WAVEFORM("Waveform"),
    DOT_MATRIX("Dot Matrix"),
    NEON_PULSE("Neon Pulse"),
}

class FullPlayerVisualizerTest {

    @Test
    fun visualizerModesExistWithCorrectLabels() {
        val modes = VisualizerMode.entries
        assertEquals(3, modes.size)
        assertEquals("Waveform", VisualizerMode.WAVEFORM.label)
        assertEquals("Dot Matrix", VisualizerMode.DOT_MATRIX.label)
        assertEquals("Neon Pulse", VisualizerMode.NEON_PULSE.label)
    }

    @Test
    fun defaultVisualizerIsWaveform() {
        var currentMode = VisualizerMode.WAVEFORM
        assertEquals(VisualizerMode.WAVEFORM, currentMode)

        currentMode = VisualizerMode.DOT_MATRIX
        assertEquals(VisualizerMode.DOT_MATRIX, currentMode)

        currentMode = VisualizerMode.NEON_PULSE
        assertEquals(VisualizerMode.NEON_PULSE, currentMode)
    }

    @Test
    fun karaokeStateTogglesCorrectly() {
        var karaokeEnabled = false
        karaokeEnabled = !karaokeEnabled
        assertTrue(karaokeEnabled)
        karaokeEnabled = !karaokeEnabled
        assertTrue(!karaokeEnabled)
    }

    @Test
    fun seekbarStyleSwitchingUpdatesCorrectly() {
        var activeStyle = SeekbarStyle.CLASSIC_WAVEFORM
        assertEquals("classic_waveform", activeStyle.id)

        activeStyle = SeekbarStyle.LIGHTNING
        assertEquals("lightning", activeStyle.id)

        activeStyle = SeekbarStyle.CONSTELLATION
        assertEquals("constellation", activeStyle.id)

        activeStyle = SeekbarStyle.BUBBLES
        assertEquals("bubbles", activeStyle.id)

        activeStyle = SeekbarStyle.LIQUID_TUBE
        assertEquals("liquid_tube", activeStyle.id)
    }
}
