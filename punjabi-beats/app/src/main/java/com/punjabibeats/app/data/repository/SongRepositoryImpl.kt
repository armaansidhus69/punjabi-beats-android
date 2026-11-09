package com.punjabibeats.app.data.repository

import com.punjabibeats.app.core.util.SongCodec
import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.data.local.db.HistoryDao
import com.punjabibeats.app.data.remote.api.JioSaavnApi
import com.punjabibeats.app.data.remote.api.PipedApi
import com.punjabibeats.app.data.remote.PipedHostProvider
import com.punjabibeats.app.data.resolver.StreamResolver
import com.punjabibeats.app.domain.model.AlbumCard
import com.punjabibeats.app.domain.model.ArtistCard
import com.punjabibeats.app.domain.model.CURATED_ARTISTS
import com.punjabibeats.app.domain.model.HomeFeed
import com.punjabibeats.app.domain.model.MusicSource
import com.punjabibeats.app.domain.model.SearchResults
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.domain.model.TRENDING_SEARCHES
import com.punjabibeats.app.domain.repository.SongRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class SongRepositoryImpl @Inject constructor(
    private val jioSaavnApi: JioSaavnApi,
    private val pipedApi: PipedApi,
    private val pipedHostProvider: PipedHostProvider,
    private val innerTubeClient: com.punjabibeats.app.innertube.InnerTubeClient,
    private val streamResolver: StreamResolver,
    private val historyDao: HistoryDao,
) : SongRepository {

    override suspend fun loadHomeFeed(): HomeFeed = coroutineScope {
        val hero = async { searchSongs(HERO_QUERY, limit = 8) }
        val trending = async { searchSongs(TRENDING_QUERY, limit = 12) }
        val artists = CURATED_ARTISTS.map { name ->
            async {
                val artwork = runSuspendCatching {
                    withTimeoutOrNull(ARTIST_TIMEOUT_MS) {
                        jioSaavnApi.searchArtists(name, limit = 1).data?.results
                            ?.firstOrNull()?.image?.best()?.url
                    }
                }.getOrNull()
                ArtistCard(id = name.hashCode().toString(), name = name, artworkUrl = artwork)
            }
        }.awaitAll()

        HomeFeed(
            heroSongs = hero.await(),
            curatedArtists = artists,
            trendingSongs = trending.await(),
            quickPicks = loadQuickPicks(fallback = trending.await()),
        )
    }

    override suspend fun searchAll(query: String): SearchResults = coroutineScope {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@coroutineScope SearchResults()

        val songs = async { searchSongs(trimmed, limit = 25) }
        val youtube = async { searchYouTube(trimmed) }
        val albums = async {
            runSuspendCatching {
                withTimeoutOrNull(SEARCH_TIMEOUT_MS) {
                    jioSaavnApi.searchAlbums(trimmed, limit = 12).data?.results.orEmpty().mapNotNull { entry ->
                        AlbumCard(
                            id = entry.id ?: return@mapNotNull null,
                            name = entry.name ?: return@mapNotNull null,
                            artist = entry.primaryArtists.orEmpty(),
                            artworkUrl = entry.image?.best()?.url,
                            year = entry.year?.toIntOrNull(),
                        )
                    }
                } ?: emptyList()
            }.getOrDefault(emptyList())
        }
        val artists = async {
            runSuspendCatching {
                withTimeoutOrNull(SEARCH_TIMEOUT_MS) {
                    jioSaavnApi.searchArtists(trimmed, limit = 12).data?.results.orEmpty().mapNotNull { entry ->
                        ArtistCard(
                            id = entry.id ?: return@mapNotNull null,
                            name = entry.name ?: return@mapNotNull null,
                            artworkUrl = entry.image?.best()?.url,
                        )
                    }
                } ?: emptyList()
            }.getOrDefault(emptyList())
        }

        val jioSongs = songs.await()
        SearchResults(
            songs = jioSongs + youtube.await(),
            artists = artists.await(),
            albums = albums.await(),
            podcasts = emptyList(),
        )
    }

    override suspend fun searchSongs(query: String, limit: Int): List<Song> =
        runSuspendCatching {
            withTimeoutOrNull(SEARCH_TIMEOUT_MS) {
                jioSaavnApi.searchSongs(query, limit = limit)
                    .data?.results.orEmpty()
                    .mapNotNull { it.toSong() }
            } ?: emptyList()
        }.getOrDefault(emptyList())

    override suspend fun resolveStream(song: Song): Song = streamResolver.resolve(song)

    override fun trendingSearches(): List<String> = TRENDING_SEARCHES

    override fun observeRecentlyPlayed(limit: Int): Flow<List<Song>> =
        historyDao.observeRecentPlays(limit).map { rows ->
            rows.mapNotNull { SongCodec.decode(it.songJson) }.distinctBy { it.id }
        }

    override fun observeTopArtists(limit: Int): Flow<List<String>> =
        historyDao.observeTopArtists(limit).map { rows -> rows.map { it.artist } }

    /** Quick Picks: up to three songs per top artist from the play history, else trending. */
    private suspend fun loadQuickPicks(fallback: List<Song>): List<Song> {
        val topArtists = runSuspendCatching {
            withTimeoutOrNull(2_000L) {
                historyDao.observeTopArtists(3).first().map { it.artist }
            } ?: emptyList()
        }.getOrDefault(emptyList())
        if (topArtists.isEmpty()) return fallback.take(10)
        val picks = topArtists.flatMap { artist -> searchSongs(artist, limit = 4) }
        return picks.ifEmpty { fallback.take(10) }.distinctBy { it.id }
    }

    /**
     * YouTube Music bridge. Primary: the direct InnerTube client (Metrolist's
     * `innertube` module pattern). Fallback: public Piped instances with
     * rotating hosts on failure.
     */
    private suspend fun searchYouTube(query: String): List<Song> {
        // Zen engine first: on-device YouTube Music search, no backend API.
        val zen = runSuspendCatching {
            com.zenmusic.ZenMusic.get()?.search?.songs(query)?.take(25)?.map { track ->
                val videoId = track.uri.removePrefix("zen://").substringBefore('.')
                Song(
                    id = "yt_$videoId",
                    title = track.title,
                    artist = track.artists.firstOrNull() ?: "Unknown Artist",
                    durationMs = track.durationMs,
                    source = MusicSource.YOUTUBE,
                    videoId = videoId,
                )
            }.orEmpty()
        }.getOrDefault(emptyList())
        if (zen.isNotEmpty()) return zen

        val direct = runSuspendCatching {
            withTimeoutOrNull(SEARCH_TIMEOUT_MS) {
                innerTubeClient.searchSongs(query, limit = 25).map { hit ->
                    Song(
                        id = "yt_${hit.videoId}",
                        title = hit.title,
                        artist = hit.artist,
                        artworkUrl = hit.thumbnailUrl,
                        durationMs = hit.durationMs,
                        source = MusicSource.YOUTUBE,
                        videoId = hit.videoId,
                    )
                }
            }
        }.getOrNull()
        if (!direct.isNullOrEmpty()) return direct

        repeat(PIPED_ATTEMPTS) {
            val host = pipedHostProvider.currentHost
            val result = runSuspendCatching {
                withTimeoutOrNull(SEARCH_TIMEOUT_MS) {
                    pipedApi.search("$host/search", query).items.mapNotNull { it.toSong() }
                }
            }
            result.getOrNull()?.let { return it }
            pipedHostProvider.markFailure()
        }
        return emptyList()
    }

    private companion object {
        const val ARTIST_TIMEOUT_MS = 6_000L
        const val SEARCH_TIMEOUT_MS = 8_000L
        const val HERO_QUERY = "punjabi top hits"
        const val TRENDING_QUERY = "punjabi trending songs"
        const val PIPED_ATTEMPTS = 3
    }
}
