package com.punjabibeats.app.data.repository

import com.punjabibeats.app.core.util.SongCodec
import com.punjabibeats.app.data.local.db.HistoryDao
import com.punjabibeats.app.data.local.db.LibraryDao
import com.punjabibeats.app.data.local.db.PlayHistoryEntity
import com.punjabibeats.app.data.local.db.PlaylistEntity
import com.punjabibeats.app.data.local.db.PlaylistSongEntity
import com.punjabibeats.app.data.local.db.RecentSearchEntity
import com.punjabibeats.app.data.local.db.SearchDao
import com.punjabibeats.app.domain.model.Playlist
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.domain.repository.LibraryRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val libraryDao: LibraryDao,
    private val searchDao: SearchDao,
    private val historyDao: HistoryDao,
) : LibraryRepository {

    private val likeMutex = Mutex()

    override fun observeLikedSongs(): Flow<List<Song>> =
        libraryDao.likedSongs()
            .map { rows -> rows.mapNotNull { SongCodec.decode(it.songJson) } }
            .flowOn(Dispatchers.Default)

    override fun observeIsLiked(songId: String): Flow<Boolean> = libraryDao.isLiked(songId)

    override suspend fun toggleLike(song: Song) = likeMutex.withLock {
        if (libraryDao.isLiked(song.id).first()) {
            libraryDao.unlike(song.id)
        } else {
            libraryDao.like(
                com.punjabibeats.app.data.local.db.LikedSongEntity(
                    id = song.id,
                    songJson = SongCodec.encode(song),
                )
            )
        }
    }

    override fun observePlaylists(): Flow<List<Playlist>> =
        libraryDao.playlistsWithCounts().map { rows ->
            rows.map { row ->
                Playlist(
                    id = row.id,
                    name = row.name,
                    createdAt = row.createdAt,
                    songCount = row.songCount,
                )
            }
        }

    override suspend fun createPlaylist(name: String): Playlist {
        val entity = PlaylistEntity(
            id = UUID.randomUUID().toString().replace("-", "").take(16),
            name = name.trim().ifEmpty { "New Playlist" },
        )
        libraryDao.insertPlaylist(entity)
        return Playlist(id = entity.id, name = entity.name, createdAt = entity.createdAt)
    }

    override suspend fun deletePlaylist(playlistId: String) {
        // Atomic cleanup of playlist songs and the playlist itself
        libraryDao.deletePlaylistSongs(playlistId)
        libraryDao.deletePlaylist(playlistId)
    }

    override fun observePlaylistSongs(playlistId: String): Flow<List<Song>> =
        libraryDao.playlistSongs(playlistId)
            .map { rows -> rows.mapNotNull { SongCodec.decode(it.songJson) } }
            .flowOn(Dispatchers.Default)

    override suspend fun addToPlaylist(playlistId: String, song: Song) {
        val position = libraryDao.getPlaylistSongCount(playlistId)
        libraryDao.addSongToPlaylist(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = song.id,
                songJson = SongCodec.encode(song),
                position = position,
            )
        )
    }

    override suspend fun removeFromPlaylist(playlistId: String, songId: String) {
        libraryDao.removeSongFromPlaylist(playlistId, songId)
    }

    override fun observeRecentSearches(): Flow<List<String>> =
        searchDao.recentSearches().map { rows -> rows.map { it.query } }

    override suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        searchDao.insert(RecentSearchEntity(query = trimmed))
    }

    override suspend fun clearRecentSearches() = searchDao.clear()

    override suspend fun recordPlay(song: Song) {
        historyDao.record(
            PlayHistoryEntity(
                songId = song.id,
                songJson = SongCodec.encode(song),
                artist = song.artist,
            )
        )
        // Cap play history to 500 rows to prevent unbounded database growth
        historyDao.pruneOldPlays()
    }

    private suspend fun playlistSongIds(playlistId: String): List<String> =
        libraryDao.playlistSongs(playlistId).first().map { it.songId }
}
