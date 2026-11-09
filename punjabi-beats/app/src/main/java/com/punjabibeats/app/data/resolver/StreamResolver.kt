package com.punjabibeats.app.data.resolver

import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.domain.model.MusicSource
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.domain.model.StreamResolutionException
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** A single step of the playback URL fallback chain. Returns null when it cannot help. */
interface TrackResolver {
    suspend fun resolve(song: Song): Song?
}

/**
 * Priority chain from the product spec:
 * JioSaavn (320kbps) → Backend YouTube stream → InnerTube + cipher decipher → Audius → iTunes preview.
 * The source the song came from is always tried first, then the full chain.
 * Never throws anything except [StreamResolutionException] after every step fails.
 */
@Singleton
class StreamResolver @Inject constructor(
    private val zenMusicResolver: ZenMusicResolver,
    private val jioSaavnResolver: JioSaavnResolver,
    private val youTubeStreamResolver: YouTubeStreamResolver,
    private val innerTubeResolver: com.punjabibeats.app.innertube.InnerTubeResolver,
    private val audiusResolver: AudiusResolver,
    private val iTunesResolver: ITunesResolver,
) {

    suspend fun resolve(song: Song): Song {
        if (!song.streamUrl.isNullOrBlank()) return song
        for (resolver in chainFor(song.source)) {
            val resolved = runSuspendCatching {
                withTimeoutOrNull(RESOLVE_TIMEOUT_MS) { resolver.resolve(song) }
            }.getOrNull()
            if (resolved != null && !resolved.streamUrl.isNullOrBlank()) return resolved
        }
        throw StreamResolutionException(song.id)
    }

    private fun chainFor(source: MusicSource): List<TrackResolver> {
        return when (source) {
            MusicSource.YOUTUBE -> listOf(
                zenMusicResolver,
                innerTubeResolver,
                youTubeStreamResolver,
                jioSaavnResolver,
                audiusResolver,
                iTunesResolver,
            )
            MusicSource.JIOSAAVN -> listOf(
                zenMusicResolver,
                jioSaavnResolver,
                audiusResolver,
                iTunesResolver,
            )
            MusicSource.AUDIUS -> listOf(
                audiusResolver,
                zenMusicResolver,
                jioSaavnResolver,
                iTunesResolver,
            )
            MusicSource.ITUNES -> listOf(
                iTunesResolver,
                zenMusicResolver,
                jioSaavnResolver,
                audiusResolver,
            )
            MusicSource.LOCAL -> listOf(
                zenMusicResolver,
                jioSaavnResolver,
                audiusResolver,
                iTunesResolver,
            )
        }.distinct()
    }

    private companion object {
        const val RESOLVE_TIMEOUT_MS = 3_500L
    }
}
