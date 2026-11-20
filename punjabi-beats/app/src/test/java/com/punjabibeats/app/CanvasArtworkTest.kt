package com.punjabibeats.app

import com.punjabibeats.app.ui.components.canvas.CanvasArtwork
import com.punjabibeats.app.ui.components.canvas.CanvasSource
import com.punjabibeats.app.ui.components.canvas.isVideoCanvasUrl
import com.punjabibeats.app.ui.components.canvas.normalizeForMatch
import com.punjabibeats.app.ui.components.canvas.splitArtists
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasArtworkTest {

    @Test
    fun normalizeForMatch_removesDiacriticsPunctuationAndExtraSpaces() {
        val input = "Sidhu Moose Wala - 295 (feat. The Kidd)!!"
        val normalized = input.normalizeForMatch()
        assertEquals("sidhu moose wala 295 feat the kidd", normalized)
    }

    @Test
    fun splitArtists_handlesVariousSeparators() {
        val artists = splitArtists("Diljit Dosanjh, Sia & Greg Kurstin feat. Arijit Singh x Karan Aujla")
        assertTrue(artists.contains("diljit dosanjh"))
        assertTrue(artists.contains("sia"))
        assertTrue(artists.contains("greg kurstin"))
        assertTrue(artists.contains("arijit singh"))
        assertTrue(artists.contains("karan aujla"))
    }

    @Test
    fun isVideoCanvasUrl_identifiesVideoFormats() {
        assertTrue(isVideoCanvasUrl("https://example.com/art/canvas.mp4"))
        assertTrue(isVideoCanvasUrl("https://example.com/stream.webm"))
        assertTrue(isVideoCanvasUrl("https://example.com/hls/index.m3u8"))
        assertTrue(isVideoCanvasUrl("https://rr1---sn-example.googlevideo.com/videoplayback?id=123"))
        assertFalse(isVideoCanvasUrl("https://example.com/audio.mp3"))
        assertFalse(isVideoCanvasUrl(null))
        assertFalse(isVideoCanvasUrl(""))
    }

    @Test
    fun canvasArtwork_matches_exactAndPartialMatches() {
        val canvas = CanvasArtwork(
            url = "https://example.com/canvas.mp4",
            title = "Softly",
            artist = "Karan Aujla, Ikky",
            source = CanvasSource.SPOTIFY,
        )

        // Exact match
        assertTrue(canvas.matches(wantTitle = "Softly", wantArtist = "Karan Aujla & Ikky", wantAlbum = null))

        // Normalized match with extra punctuation
        assertTrue(canvas.matches(wantTitle = "Softly!", wantArtist = "Karan Aujla, Ikky", wantAlbum = null))

        // Mismatched title
        assertFalse(canvas.matches(wantTitle = "White Brown Black", wantArtist = "Karan Aujla", wantAlbum = null))

        // Mismatched artist
        assertFalse(canvas.matches(wantTitle = "Softly", wantArtist = "AP Dhillon", wantAlbum = null))
    }
}
