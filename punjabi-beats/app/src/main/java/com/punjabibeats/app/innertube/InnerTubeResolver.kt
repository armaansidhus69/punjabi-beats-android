package com.punjabibeats.app.innertube

import com.punjabibeats.app.core.util.firstObject
import com.punjabibeats.app.core.util.firstString
import com.punjabibeats.app.data.cipher.YtCipherManager
import com.punjabibeats.app.data.resolver.TrackResolver
import com.punjabibeats.app.domain.model.MusicSource
import com.punjabibeats.app.domain.model.Song
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonObject

/**
 * Direct YouTube Music playback source (Metrolist-style `innertube` module):
 * a [TrackResolver] that calls the InnerTube `player` endpoint for a videoId
 * (searching YT Music first when the song has none) and lets
 * [YtCipherManager] decipher, n-transform and validate the best audio-only
 * adaptive format. Sits in the fallback chain right after the backend's own
 * /api/yt-stream step.
 */
@Singleton
class InnerTubeResolver @Inject constructor(
    private val client: InnerTubeClient,
    private val cipherManager: YtCipherManager,
) : TrackResolver {

    override suspend fun resolve(song: Song): Song? {
        val directVideoId = song.videoId?.takeIf { it.isNotBlank() }
            ?: song.id.takeIf { it.startsWith("yt_") }?.removePrefix("yt_")
        val playerResponse = directVideoId
            ?.let { client.player(it) }
            ?: searchPlayerResponse(song)
            ?: return null
        val streamUrl = cipherManager.bestAudioStreamUrl(playerResponse) ?: return null
        val videoId = directVideoId
            ?: playerResponse.firstObject("videoDetails")?.firstString("videoId")
        return song.copy(
            streamUrl = streamUrl,
            videoId = videoId ?: song.videoId,
            source = if (song.source == MusicSource.JIOSAAVN) MusicSource.YOUTUBE else song.source,
        )
    }

    /** Title/artist search against YouTube Music; the top hit is resolved. */
    private suspend fun searchPlayerResponse(song: Song): JsonObject? {
        val query = "${song.title} ${song.artist}".trim()
        val hit = client.searchSongs(query, limit = 3).firstOrNull() ?: return null
        return client.player(hit.videoId)
    }
}
