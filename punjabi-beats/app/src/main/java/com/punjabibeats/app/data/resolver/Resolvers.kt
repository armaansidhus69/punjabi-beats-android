package com.punjabibeats.app.data.resolver

import com.punjabibeats.app.BuildConfig
import com.punjabibeats.app.core.util.PbJson
import com.punjabibeats.app.core.util.firstObject
import com.punjabibeats.app.core.util.firstString
import com.punjabibeats.app.core.util.objectArray
import com.punjabibeats.app.data.remote.api.AudiusApi
import com.punjabibeats.app.data.remote.api.BackendApi
import com.punjabibeats.app.data.remote.api.ITunesApi
import com.punjabibeats.app.data.remote.api.JioSaavnApi
import com.punjabibeats.app.data.remote.AudiusHostProvider
import com.punjabibeats.app.domain.model.MusicSource
import com.punjabibeats.app.domain.model.Song
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/** JioSaavn 320kbps — the primary source for both playback and catalogue search. */
@Singleton
class JioSaavnResolver @Inject constructor(
    private val api: JioSaavnApi,
) : TrackResolver {

    override suspend fun resolve(song: Song): Song? {
        if (song.source == MusicSource.JIOSAAVN && !song.streamUrl.isNullOrBlank()) return song
        val query = "${song.title} ${song.artist}".trim()
        val best = api.searchSongs(query, limit = 5).data?.results?.firstOrNull() ?: return null
        val resolved = best.toSong() ?: return null
        val url = resolved.streamUrl ?: return null
        return song.copy(
            streamUrl = url,
            artworkUrl = song.artworkUrl ?: resolved.artworkUrl,
            durationMs = song.durationMs.takeIf { it > 0L } ?: resolved.durationMs,
        )
    }
}

/** Backend InnerTube resolver (/api/yt-stream) for tracks carrying a videoId. */
@Singleton
class YouTubeStreamResolver @Inject constructor(
    private val backend: BackendApi,
) : TrackResolver {

    override suspend fun resolve(song: Song): Song? {
        val videoId = song.videoId?.takeIf { it.isNotBlank() }
            ?: song.id.takeIf { it.startsWith("yt_") }?.removePrefix("yt_")
            ?: return null
        val body = backend.ytStream(videoId).body()?.string().orEmpty()
        if (body.isBlank()) return null
        val root = PbJson.json.parseToJsonElement(body) as? JsonObject ?: return null
        val url = root.firstString("url", "audioUrl", "bestAudioUrl", "streamUrl", "playbackUrl")
            ?: root.firstObject("data", "result")?.firstString("url", "audioUrl", "streamUrl")
            ?: root.objectArray("formats", "adaptiveFormats").firstNotNullOfOrNull { format ->
                format.firstString("url", "audioUrl")
            }
            ?: return null
        return song.copy(streamUrl = url, videoId = videoId)
    }
}

/** Audius full-length tracks; the discovery node rotates on failure. */
@Singleton
class AudiusResolver @Inject constructor(
    private val api: AudiusApi,
    private val hostProvider: AudiusHostProvider,
) : TrackResolver {

    override suspend fun resolve(song: Song): Song? {
        val query = "${song.title} ${song.artist}".trim()
        val url = "${hostProvider.host}/v1/tracks/search"
        val response = runCatching {
            api.searchTracks(url, query, BuildConfig.AUDIUS_APP_NAME, limit = 5)
        }.getOrElse {
            hostProvider.markFailure()
            return null
        }
        val track = response.data.firstOrNull() ?: return null
        val trackId = track.id ?: return null
        val streamUrl = AudiusApi.streamUrl(hostProvider.host, trackId, BuildConfig.AUDIUS_APP_NAME)
        return song.copy(
            streamUrl = streamUrl,
            artworkUrl = song.artworkUrl ?: track.artwork?.best(),
            durationMs = song.durationMs.takeIf { it > 0L } ?: track.duration * 1000L,
        )
    }
}

/** iTunes 30-second previews — the last resort so playback never dies. */
@Singleton
class ITunesResolver @Inject constructor(
    private val api: ITunesApi,
) : TrackResolver {

    override suspend fun resolve(song: Song): Song? {
        if (song.source == MusicSource.ITUNES && !song.streamUrl.isNullOrBlank()) return song
        val query = "${song.title} ${song.artist}".trim()
        val best = api.search(query, limit = 5).results.firstOrNull() ?: return null
        val preview = best.previewUrl ?: return null
        return song.copy(
            streamUrl = preview,
            artworkUrl = song.artworkUrl ?: best.artworkUrl100?.replace("100x100", "600x600"),
            durationMs = song.durationMs.takeIf { it > 0L } ?: (best.trackTimeMillis ?: 0L),
        )
    }
}
