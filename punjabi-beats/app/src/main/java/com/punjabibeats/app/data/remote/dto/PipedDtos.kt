package com.punjabibeats.app.data.remote.dto

import com.punjabibeats.app.domain.model.MusicSource
import com.punjabibeats.app.domain.model.Song
import kotlinx.serialization.Serializable

@Serializable
data class PipedItem(
    val url: String? = null,
    val type: String? = null,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val uploaderName: String? = null,
    val duration: Long = 0L,
) {
    fun videoId(): String? {
        val raw = url ?: return null
        val afterV = raw.substringAfter("v=", "")
        if (afterV.isEmpty()) return null
        return afterV.takeWhile { it != '&' }.takeIf { it.isNotBlank() }
    }

    fun toSong(): Song? {
        val id = videoId() ?: return null
        val name = title ?: return null
        if (type != null && type != "stream") return null
        return Song(
            id = "yt_$id",
            title = name,
            artist = uploaderName ?: "YouTube",
            artworkUrl = thumbnailUrl ?: "https://i.ytimg.com/vi/$id/mqdefault.jpg",
            durationMs = duration * 1000L,
            source = MusicSource.YOUTUBE,
            videoId = id,
        )
    }
}

@Serializable
data class PipedSearchResponse(
    val items: List<PipedItem> = emptyList(),
)
