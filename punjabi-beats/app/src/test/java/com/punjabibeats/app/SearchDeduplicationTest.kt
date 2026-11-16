package com.punjabibeats.app

import com.punjabibeats.app.domain.model.AlbumCard
import com.punjabibeats.app.domain.model.ArtistCard
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.ui.screens.search.extractUniqueAlbums
import com.punjabibeats.app.ui.screens.search.extractUniqueArtists
import com.punjabibeats.app.ui.screens.search.removeDuplicateSongs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchDeduplicationTest {

    @Test
    fun `removeDuplicateSongs removes duplicate song IDs`() {
        val songs = listOf(
            Song(id = "jio_101", title = "Ashke", artist = "Karan Aujla"),
            Song(id = " jio_101 ", title = "Ashke", artist = "Karan Aujla"),
            Song(id = "jio_102", title = "For A Reason", artist = "Karan Aujla"),
            Song(id = "jio_101", title = "Ashke", artist = "Karan Aujla"),
            Song(id = "jio_103", title = "Ashke (Remix)", artist = "Karan Aujla"),
        )

        val unique = removeDuplicateSongs(songs)
        assertEquals(3, unique.size)
        assertEquals(listOf("jio_101", "jio_102", "jio_103"), unique.map { it.id.trim() })
    }

    @Test
    fun `extractUniqueArtists extracts artists from songs and merges with remote artists`() {
        val songs = listOf(
            Song(id = "1", title = "Song A", artist = "Otaal", artworkUrl = "https://thumb/otaal.jpg"),
            Song(id = "2", title = "Song B", artist = "Karan Aujla, Otaal & Ikky"),
            Song(id = "3", title = "Song C", artist = "Diljit Dosanjh ft. Sia"),
        )
        val remoteArtists = listOf(
            ArtistCard(id = "remote_1", name = "Diljit Dosanjh"),
        )

        val artists = extractUniqueArtists(songs, remoteArtists)
        val names = artists.map { it.name.lowercase() }

        assertTrue("Should contain Otaal", names.contains("otaal"))
        assertTrue("Should contain Karan Aujla", names.contains("karan aujla"))
        assertTrue("Should contain Ikky", names.contains("ikky"))
        assertTrue("Should contain Diljit Dosanjh", names.contains("diljit dosanjh"))
        assertTrue("Should contain Sia", names.contains("sia"))
        // Check Diljit Dosanjh is not duplicated
        assertEquals(1, names.count { it == "diljit dosanjh" })
    }

    @Test
    fun `extractUniqueAlbums extracts albums from songs and merges with remote albums`() {
        val songs = listOf(
            Song(id = "1", title = "Track 1", artist = "Otaal", albumName = "Four You", artworkUrl = "https://art/1.jpg"),
            Song(id = "2", title = "Track 2", artist = "Otaal", albumName = "Four You", artworkUrl = "https://art/1.jpg"),
            Song(id = "3", title = "Track 3", artist = "Karan Aujla", albumName = "Four You"),
            Song(id = "4", title = "Track 4", artist = "Diljit Dosanjh", albumName = "Ghost"),
        )
        val remoteAlbums = listOf(
            AlbumCard(id = "rem_1", name = "Ghost", artist = "Diljit Dosanjh"),
        )

        val albums = extractUniqueAlbums(songs, remoteAlbums)
        assertEquals(3, albums.size)
        // Check Ghost is deduplicated
        assertEquals(1, albums.count { it.name.equals("Ghost", ignoreCase = true) })
        // Check Four You by Otaal and Four You by Karan Aujla are both kept (different artists)
        assertEquals(1, albums.count { it.name.equals("Four You", ignoreCase = true) && it.artist.contains("Otaal") })
        assertEquals(1, albums.count { it.name.equals("Four You", ignoreCase = true) && it.artist.contains("Karan") })
    }
}
