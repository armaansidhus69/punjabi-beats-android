package com.punjabibeats.app.data.remote.dto

import com.punjabibeats.app.domain.model.MusicSource
import com.punjabibeats.app.domain.model.Song
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AudiusUser(
    val name: String? = null,
    val handle: String? = null,
)

@Serializable
data class AudiusArtwork(
    @SerialName("150x150") val small: String? = null,
    @SerialName("480x480") val medium: String? = null,
    @SerialName("1000x1000") val large: String? = null,
) {
    fun best(): String? = large ?: medium ?: small
}

@Serializable
data class AudiusTrack(
    val id: String? = null,
    val title: String? = null,
    val duration: Long = 0L,
    val user: AudiusUser? = null,
    val artwork: AudiusArtwork? = null,
    val genre: String? = null,
) {
    fun toSong(): Song? {
        val id = id ?: return null
        val title = title ?: return null
        return Song(
            id = "audius_$id",
            title = title,
            artist = user?.name ?: user?.handle ?: "Unknown Artist",
            artworkUrl = artwork?.best(),
            durationMs = duration * 1000L,
            source = MusicSource.AUDIUS,
        )
    }
}

@Serializable
data class AudiusSearchResponse(
    val data: List<AudiusTrack> = emptyList(),
)
