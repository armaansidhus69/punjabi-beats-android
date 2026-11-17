package com.punjabibeats.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Query("SELECT * FROM liked_songs ORDER BY likedAt DESC")
    fun likedSongs(): Flow<List<LikedSongEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE id = :id)")
    fun isLiked(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun like(entity: LikedSongEntity)

    @Query("DELETE FROM liked_songs WHERE id = :id")
    suspend fun unlike(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(entity: PlaylistEntity)

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun playlists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun playlist(id: String): PlaylistEntity?

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deletePlaylistSongs(playlistId: String)

    @Query("""
        SELECT p.id, p.name, p.createdAt, COUNT(ps.songId) AS songCount
        FROM playlists p
        LEFT JOIN playlist_songs ps ON p.id = ps.playlistId
        GROUP BY p.id
        ORDER BY p.createdAt DESC
    """)
    fun playlistsWithCounts(): Flow<List<PlaylistWithCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(entity: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position, addedAt")
    fun playlistSongs(playlistId: String): Flow<List<PlaylistSongEntity>>

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    fun playlistSize(playlistId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getPlaylistSongCount(playlistId: String): Int
}

@Dao
interface SearchDao {

    @Query("SELECT * FROM recent_searches ORDER BY searchedAt DESC LIMIT :limit")
    fun recentSearches(limit: Int = 12): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE query = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clear()
}

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun record(entity: PlayHistoryEntity)

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT :limit")
    suspend fun recentPlays(limit: Int): List<PlayHistoryEntity>

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT :limit")
    fun observeRecentPlays(limit: Int): Flow<List<PlayHistoryEntity>>

    @Query("SELECT artist AS artist, COUNT(*) AS plays FROM play_history GROUP BY artist ORDER BY plays DESC LIMIT :limit")
    fun observeTopArtists(limit: Int): Flow<List<ArtistCount>>

    @Query("DELETE FROM play_history WHERE rowId NOT IN (SELECT rowId FROM play_history ORDER BY playedAt DESC LIMIT :keepLimit)")
    suspend fun pruneOldPlays(keepLimit: Int = 500)

    @Query("DELETE FROM play_history")
    suspend fun clear()
}

/** Lightweight projection for GROUP BY artist queries. */
data class ArtistCount(val artist: String, val plays: Int)

/** Lightweight projection for playlists with counted songs. */
data class PlaylistWithCount(
    val id: String,
    val name: String,
    val createdAt: Long,
    val songCount: Int,
)

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")
    fun downloads(): Flow<List<DownloadedSongEntity>>

    @Query("SELECT * FROM downloaded_songs WHERE songId = :songId")
    suspend fun get(songId: String): DownloadedSongEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_songs WHERE songId = :songId)")
    fun isDownloaded(songId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadedSongEntity)

    @Query("DELETE FROM downloaded_songs WHERE songId = :songId")
    suspend fun delete(songId: String)
}

@Dao
interface JamDao {

    @Query("SELECT * FROM jam_room_history ORDER BY joinedAt DESC LIMIT :limit")
    fun recentRooms(limit: Int = 6): Flow<List<JamRoomHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: JamRoomHistoryEntity)

    @Query("DELETE FROM jam_room_history WHERE code = :code")
    suspend fun delete(code: String)
}

@Dao
interface LyricsCacheDao {

    @Query("SELECT * FROM lyrics_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun getLyrics(key: String): LyricsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LyricsCacheEntity)

    @Query("DELETE FROM lyrics_cache WHERE cachedAt < :olderThanMs")
    suspend fun pruneOld(olderThanMs: Long)
}
