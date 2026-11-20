package com.punjabibeats.app

import com.punjabibeats.app.ui.screens.replay.ArtistStat
import com.punjabibeats.app.ui.screens.replay.SongHistoryItem
import com.punjabibeats.app.ui.screens.replay.SongStat
import com.punjabibeats.app.ui.screens.replay.calculateReplaySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayAnalyticsTest {

    @Test
    fun calculateReplaySummary_computesTotalMinutesCorrectly() {
        val history = listOf(
            SongHistoryItem("1", "295", "Sidhu Moose Wala", "Bhangra", 240_000L),
            SongHistoryItem("2", "Softly", "Karan Aujla", "Punjabi Pop", 180_000L),
            SongHistoryItem("3", "Lover", "Diljit Dosanjh", "Bhangra", 180_000L),
        )

        val summary = calculateReplaySummary(history, year = 2026)
        assertEquals(2026, summary.year)
        // Total ms = 600,000 ms = 10 minutes
        assertEquals(10L, summary.totalMinutes)
    }

    @Test
    fun calculateReplaySummary_identifiesTopArtistAndBadge() {
        val history = listOf(
            SongHistoryItem("1", "295", "Sidhu Moose Wala", "Bhangra", 240_000L),
            SongHistoryItem("2", "The Last Ride", "Sidhu Moose Wala", "Bhangra", 300_000L),
            SongHistoryItem("3", "Softly", "Karan Aujla", "Punjabi Pop", 180_000L),
        )

        val summary = calculateReplaySummary(history, year = 2026)
        assertNotNull(summary.topArtists.firstOrNull())
        assertEquals("Sidhu Moose Wala", summary.topArtists.first().artist)
        assertEquals(2, summary.topArtists.first().playCount)
        assertTrue(summary.personalityTitle.isNotBlank())
        assertTrue(summary.personalityBadge.isNotBlank())
    }
}
