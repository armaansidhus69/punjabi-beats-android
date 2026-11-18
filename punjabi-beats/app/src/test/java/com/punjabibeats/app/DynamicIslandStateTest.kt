package com.punjabibeats.app

import com.punjabibeats.app.ui.components.dynamicisland.DynamicIslandMode
import com.punjabibeats.app.ui.components.dynamicisland.DynamicIslandState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicIslandStateTest {

    @Test
    fun islandHasThreeDistinctDisplayModes() {
        val modes = DynamicIslandMode.entries
        assertEquals(3, modes.size)
        assertTrue(modes.contains(DynamicIslandMode.COMPACT_PILL))
        assertTrue(modes.contains(DynamicIslandMode.EXPANDED_CARD))
        assertTrue(modes.contains(DynamicIslandMode.DISMISSED))
    }

    @Test
    fun defaultIslandStateIsCompactPill() {
        val state = DynamicIslandState()
        assertEquals(DynamicIslandMode.COMPACT_PILL, state.mode)
        assertTrue(state.isVisible)
        assertTrue(state.isEqualizerAnimating)
    }

    @Test
    fun expandingAndCollapsingIslandTransitionsCorrectly() {
        val state = DynamicIslandState()
        val expanded = state.copy(mode = DynamicIslandMode.EXPANDED_CARD)
        assertEquals(DynamicIslandMode.EXPANDED_CARD, expanded.mode)
        assertTrue(expanded.isVisible)

        val collapsed = expanded.copy(mode = DynamicIslandMode.COMPACT_PILL)
        assertEquals(DynamicIslandMode.COMPACT_PILL, collapsed.mode)
        assertTrue(collapsed.isVisible)

        val dismissed = state.copy(mode = DynamicIslandMode.DISMISSED)
        assertEquals(DynamicIslandMode.DISMISSED, dismissed.mode)
        assertFalse(dismissed.isVisible)
    }
}
