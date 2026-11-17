package com.punjabibeats.app.data.local.db

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class DatabaseMigrationTest {

    @Test
    fun testMigration1To2CreatesLyricsCacheTable() {
        val executedQueries = mutableListOf<String>()

        val fakeDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedQueries.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        PunjabiBeatsDatabase.MIGRATION_1_2.migrate(fakeDb)

        assertTrue(
            "Expected CREATE TABLE IF NOT EXISTS lyrics_cache in executed SQL",
            executedQueries.any { it.contains("CREATE TABLE IF NOT EXISTS `lyrics_cache`") },
        )
    }

    @Test
    fun testMigration2To3CreatesPlayHistoryIndices() {
        val executedQueries = mutableListOf<String>()

        val fakeDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedQueries.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        PunjabiBeatsDatabase.MIGRATION_2_3.migrate(fakeDb)

        assertTrue(
            "Expected index_play_history_playedAt in executed SQL",
            executedQueries.any { it.contains("index_play_history_playedAt") },
        )
        assertTrue(
            "Expected index_play_history_artist in executed SQL",
            executedQueries.any { it.contains("index_play_history_artist") },
        )
    }
}
