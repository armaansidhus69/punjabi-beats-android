package com.punjabibeats.app.domain.repository

import com.punjabibeats.app.domain.model.AlbumCard
import com.punjabibeats.app.domain.model.ArtistCard
import com.punjabibeats.app.domain.model.HomeFeed
import com.punjabibeats.app.domain.model.Playlist
import com.punjabibeats.app.domain.model.PodcastChannel
import com.punjabibeats.app.domain.model.PodcastEpisode
import com.punjabibeats.app.domain.model.SearchResults
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.domain.model.SyncedLyrics
import com.punjabibeats.app.domain.model.TranscriptCue
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    suspend fun loadHomeFeed(): HomeFeed
    suspend fun searchAll(query: String): SearchResults
    suspend fun searchSongs(query: String, limit: Int = 20): List<Song>
    suspend fun resolveStream(song: Song): Song
    fun trendingSearches(): List<String>
    fun observeRecentlyPlayed(limit: Int = 20): Flow<List<Song>>
    fun observeTopArtists(limit: Int = 5): Flow<List<String>>
}

interface LibraryRepository {
    fun observeLikedSongs(): Flow<List<Song>>
    fun observeIsLiked(songId: String): Flow<Boolean>
    suspend fun toggleLike(song: Song)

    fun observePlaylists(): Flow<List<Playlist>>
    suspend fun createPlaylist(name: String): Playlist
    suspend fun deletePlaylist(playlistId: String)
    fun observePlaylistSongs(playlistId: String): Flow<List<Song>>
    suspend fun addToPlaylist(playlistId: String, song: Song)
    suspend fun removeFromPlaylist(playlistId: String, songId: String)

    fun observeRecentSearches(): Flow<List<String>>
    suspend fun addRecentSearch(query: String)
    suspend fun clearRecentSearches()

    suspend fun recordPlay(song: Song)
}

interface LyricsRepository {
    suspend fun fetchLyrics(title: String, artist: String, durationMs: Long): SyncedLyrics?
}

interface AiRepository {
    suspend fun generatePlaylist(prompt: String): Result<com.punjabibeats.app.domain.model.AiPlaylist>
    suspend fun askAssistant(history: List<com.punjabibeats.app.domain.model.AiMessage>): Result<String>
}

interface PodcastRepository {
    fun observeChannels(): Flow<List<PodcastChannel>>
    suspend fun addFeed(feedUrl: String): Result<PodcastChannel>
    suspend fun removeFeed(channelId: String)
    suspend fun loadEpisodes(channel: PodcastChannel): Result<List<PodcastEpisode>>
    suspend fun searchLocalChannels(query: String): List<PodcastChannel>
    suspend fun fetchTranscript(url: String): Result<List<TranscriptCue>>
}

interface DownloadRepository {
    fun observeDownloads(): Flow<List<Song>>
    fun observeProgress(): Flow<Map<String, Int>>
    fun observeIsDownloaded(songId: String): Flow<Boolean>
    suspend fun download(song: Song): Result<Unit>
    suspend fun remove(songId: String)
    suspend fun get(songId: String): Song?
    suspend fun filePath(songId: String): String?
}
