package com.punjabibeats.app.data.remote.dto

import com.punjabibeats.app.domain.model.MusicSource
import com.punjabibeats.app.domain.model.Song
import kotlinx.serialization.Serializable

@Serializable
data class ITunesTrack(
    val trackId: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val collectionName: String? = null,
    val artworkUrl100: String? = null,
    val previewUrl: String? = null,
    val trackTimeMillis: Long? = null,
) {
    fun toSong(): Song? {
        val name = trackName ?: return null
        val artwork = artworkUrl100?.replace("100x100", "600x600")
        return Song(
            id = "itunes_${trackId ?: name.hashCode()}",
            title = name,
            artist = artistName ?: "Unknown Artist",
            albumName = collectionName ?: "",
            artworkUrl = artwork,
            durationMs = trackTimeMillis ?: 0L,
            source = MusicSource.ITUNES,
            streamUrl = previewUrl,
        )
    }
}

@Serializable
data class ITunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ITunesTrack> = emptyList(),
)
