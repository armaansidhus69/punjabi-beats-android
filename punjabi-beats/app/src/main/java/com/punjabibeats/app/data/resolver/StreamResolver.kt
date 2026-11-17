package com.punjabibeats.app.data.resolver

import android.util.LruCache
import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.domain.model.MusicSource
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.domain.model.StreamResolutionException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
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
 *
 * Local device tracks are validated directly without futilely scraping remote providers,
 * preventing 20-second timeout hangs in offline scenarios.
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

    private data class CachedStream(val streamUrl: String, val cachedAt: Long)
    private val streamCache = LruCache<String, CachedStream>(CACHE_MAX_ENTRIES)
    private val activeResolutions = ConcurrentHashMap<String, Deferred<Song>>()

    suspend fun resolve(song: Song): Song {
        // Fast path for local device songs: never scrape the web or timeout offline
        if (song.source == MusicSource.LOCAL) {
            if (!song.streamUrl.isNullOrBlank()) return song
            throw StreamResolutionException(song.id)
        }

        if (!song.streamUrl.isNullOrBlank()) return song

        // Fast path: In-memory LRU cache with 45-minute TTL
        val cached = streamCache.get(song.id)
        if (cached != null) {
            val age = System.currentTimeMillis() - cached.cachedAt
            if (age < CACHE_TTL_MS && cached.streamUrl.isNotBlank()) {
                return song.copy(streamUrl = cached.streamUrl)
            } else {
                streamCache.remove(song.id)
            }
        }

        // Deduplication: await existing in-flight resolution for the same songId
        val inFlight = activeResolutions[song.id]
        if (inFlight != null && inFlight.isActive) {
            return inFlight.await()
        }

        return coroutineScope {
            val deferred = activeResolutions.compute(song.id) { _, current ->
                if (current != null && current.isActive) current
                else async(Dispatchers.IO) { performResolve(song) }
            }!!

            try {
                deferred.await()
            } finally {
                activeResolutions.remove(song.id, deferred)
            }
        }
    }

    private suspend fun performResolve(song: Song): Song {
        val result = withTimeoutOrNull(TOTAL_RESOLVE_TIMEOUT_MS) {
            for (resolver in chainFor(song.source)) {
                val resolved = runSuspendCatching {
                    withTimeoutOrNull(RESOLVE_TIMEOUT_MS) { resolver.resolve(song) }
                }.getOrNull()
                if (resolved != null && !resolved.streamUrl.isNullOrBlank()) return@withTimeoutOrNull resolved
            }
            null
        }
        if (result != null && !result.streamUrl.isNullOrBlank()) {
            streamCache.put(song.id, CachedStream(result.streamUrl, System.currentTimeMillis()))
            return result
        }
        throw StreamResolutionException(song.id)
    }

    /** Evicts a stale or expired stream URL from cache. */
    fun invalidate(songId: String) {
        streamCache.remove(songId)
    }

    private fun chainFor(source: MusicSource): List<TrackResolver> {
        return when (source) {
            MusicSource.LOCAL -> emptyList()
            MusicSource.YOUTUBE -> listOf(
                zenMusicResolver,
                innerTubeResolver,
                youTubeStreamResolver,
                audiusResolver,
                iTunesResolver,
                jioSaavnResolver,
            )
            MusicSource.JIOSAAVN -> listOf(
                jioSaavnResolver,
                zenMusicResolver,
                innerTubeResolver,
                youTubeStreamResolver,
                audiusResolver,
                iTunesResolver,
            )
            MusicSource.AUDIUS -> listOf(
                audiusResolver,
                zenMusicResolver,
                innerTubeResolver,
                youTubeStreamResolver,
                iTunesResolver,
                jioSaavnResolver,
            )
            MusicSource.ITUNES -> listOf(
                iTunesResolver,
                zenMusicResolver,
                innerTubeResolver,
                youTubeStreamResolver,
                audiusResolver,
                jioSaavnResolver,
            )
        }.distinct()
    }

    private companion object {
        const val RESOLVE_TIMEOUT_MS = 8_000L
        const val TOTAL_RESOLVE_TIMEOUT_MS = 20_000L
        const val CACHE_MAX_ENTRIES = 100
        const val CACHE_TTL_MS = 45 * 60 * 1_000L // 45 minutes TTL
    }
}
