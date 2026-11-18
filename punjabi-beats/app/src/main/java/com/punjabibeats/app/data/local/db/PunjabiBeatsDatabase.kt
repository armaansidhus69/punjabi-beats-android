package com.punjabibeats.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 3,
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

        /**
         * Migration from version 1 to 2:
         * Adds the `lyrics_cache` table for offline cached synchronized lyrics,
         * preserving all user-liked songs, custom playlists, downloads, and history.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `lyrics_cache` (
                        `cacheKey` TEXT NOT NULL,
                        `trackName` TEXT NOT NULL,
                        `artistName` TEXT NOT NULL,
                        `lyricsJson` TEXT NOT NULL,
                        `provider` TEXT NOT NULL,
                        `isSynced` INTEGER NOT NULL,
                        `cachedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`cacheKey`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migration from version 2 to 3:
         * Adds indices on `playedAt` and `artist` in `play_history` for fast O(log N)
         * recent plays and top artist queries.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_history_playedAt` ON `play_history` (`playedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_history_artist` ON `play_history` (`artist`)")
            }
        }

        fun build(context: Context): PunjabiBeatsDatabase =
            Room.databaseBuilder(context, PunjabiBeatsDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
    }
}
