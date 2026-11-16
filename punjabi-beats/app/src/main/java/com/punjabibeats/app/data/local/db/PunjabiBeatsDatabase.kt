package com.punjabibeats.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LikedSongEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        RecentSearchEntity::class,
        PlayHistoryEntity::class,
        DownloadedSongEntity::class,
        JamRoomHistoryEntity::class,
        LyricsCacheEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class PunjabiBeatsDatabase : RoomDatabase() {

    abstract fun libraryDao(): LibraryDao
    abstract fun searchDao(): SearchDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun jamDao(): JamDao
    abstract fun lyricsCacheDao(): LyricsCacheDao

    companion object {
        const val NAME = "punjabi_beats.db"

        fun build(context: Context): PunjabiBeatsDatabase =
            Room.databaseBuilder(context, PunjabiBeatsDatabase::class.java, NAME)
                .fallbackToDestructiveMigration()
                .build()
    }
}
