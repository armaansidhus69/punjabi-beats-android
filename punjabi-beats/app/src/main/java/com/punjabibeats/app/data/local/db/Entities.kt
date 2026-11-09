package com.punjabibeats.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Songs are persisted with their full JSON payload ([songJson]) so catalogue
 * metadata (artwork, duration, source, resolved stream) survives restarts
 * without a brittle column-per-field mapping across API evolutions.
 */
@Entity(tableName = "liked_songs")
data class LikedSongEntity(
    @PrimaryKey val id: String,
    val songJson: String,
    val likedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(
    val playlistId: String,
    val songId: String,
    val songJson: String,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0L,
    val songId: String,
    val songJson: String,
    val artist: String,
    val playedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "downloaded_songs")
data class DownloadedSongEntity(
    @PrimaryKey val songId: String,
    val songJson: String,
    val filePath: String,
    val sizeBytes: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "jam_room_history")
data class JamRoomHistoryEntity(
    @PrimaryKey val code: String,
    val joinedAt: Long = System.currentTimeMillis(),
)
