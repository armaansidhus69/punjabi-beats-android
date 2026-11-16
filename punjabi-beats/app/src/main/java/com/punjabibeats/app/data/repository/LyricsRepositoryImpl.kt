package com.punjabibeats.app.data.repository

import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.data.local.db.LyricsCacheDao
import com.punjabibeats.app.data.local.db.LyricsCacheEntity
import com.punjabibeats.app.data.parser.LrcParser
import com.punjabibeats.app.data.remote.api.LrcLibApi
import com.punjabibeats.app.domain.model.LyricLine
import com.punjabibeats.app.domain.model.SyncedLyrics
import com.punjabibeats.app.domain.repository.LyricsRepository
import com.punjabibeats.app.core.util.transliterateGurmukhi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lyrics source: LRCLIB synced LRC first, cached offline in Room DB so subsequent
 * plays load instantly without any network call. Gurmukhi lines get roman transliteration.
 */
@Singleton
class LyricsRepositoryImpl @Inject constructor(
    private val lrcLibApi: LrcLibApi,
    private val lyricsCacheDao: LyricsCacheDao,
) : LyricsRepository {

    override suspend fun fetchLyrics(title: String, artist: String, durationMs: Long): SyncedLyrics? {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return null
        val cacheKey = "${cleanTitle.lowercase()}_${artist.trim().lowercase()}"

        // 1. Check offline Room DB cache first
        val cached = runSuspendCatching { lyricsCacheDao.getLyrics(cacheKey) }.getOrNull()
        if (cached != null) {
            val lines = decodeLines(cached.lyricsJson)
            if (lines.isNotEmpty()) {
                return SyncedLyrics(
                    trackName = cached.trackName,
                    artistName = cached.artistName,
                    lines = lines,
                    provider = "${cached.provider} (Offline Cache)",
                    isSynced = cached.isSynced,
                )
            }
        }

        val durationSec = (durationMs / 1000L).toInt().takeIf { it > 0 }

        // 2. Fetch from LRCLIB
        val exact = runSuspendCatching {
            lrcLibApi.get(cleanTitle, artist.trim(), durationSec).body()
        }.getOrNull()
        val synced = exact?.toSyncedLyrics(cleanTitle, artist)
            ?: runSuspendCatching {
                lrcLibApi.search("$artist $cleanTitle", limit = 3).firstOrNull()
            }.getOrNull()?.toSyncedLyrics(cleanTitle, artist)

        // 3. Cache result for offline access
        if (synced != null) {
            runSuspendCatching {
                lyricsCacheDao.insert(
                    LyricsCacheEntity(
                        cacheKey = cacheKey,
                        trackName = synced.trackName,
                        artistName = synced.artistName,
                        lyricsJson = encodeLines(synced.lines),
                        provider = synced.provider,
                        isSynced = synced.isSynced,
                    )
                )
            }
        }

        return synced
    }

    private fun encodeLines(lines: List<LyricLine>): String =
        lines.joinToString("\n") { "${it.timeMs}|${it.text}|${it.romanized.orEmpty()}" }

    private fun decodeLines(raw: String): List<LyricLine> =
        raw.lineSequence().filter { it.isNotBlank() }.mapNotNull { line ->
            val parts = line.split("|", limit = 3)
            if (parts.size >= 2) {
                val time = parts[0].toLongOrNull() ?: 0L
                val text = parts[1]
                val roman = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
                LyricLine(timeMs = time, text = text, romanized = roman)
            } else null
        }.toList()

    private fun com.punjabibeats.app.data.remote.dto.LrcLibTrack.toSyncedLyrics(
        title: String,
        artist: String,
    ): SyncedLyrics? {
        val synced = syncedLyrics?.takeIf { it.isNotBlank() }
        if (synced != null) {
            val lines = LrcParser.parse(synced)
            if (lines.isNotEmpty()) {
                return SyncedLyrics(
                    trackName = trackName ?: title,
                    artistName = artistName ?: artist,
                    lines = lines.map { it.withTransliteration() },
                    provider = "LRCLIB",
                    isSynced = true,
                )
            }
        }
        val plain = plainLyrics?.takeIf { it.isNotBlank() } ?: return null
        val lines = plain.lineSequence()
            .filter { it.isNotBlank() }
            .mapIndexed { index, text -> LyricLine(timeMs = index * UNSYNCED_STEP_MS, text = text.trim()) }
            .toList()
        if (lines.isEmpty()) return null
        return SyncedLyrics(
            trackName = trackName ?: title,
            artistName = artistName ?: artist,
            lines = lines.map { it.withTransliteration() },
            provider = "LRCLIB",
            isSynced = false,
        )
    }

    private fun LyricLine.withTransliteration(): LyricLine =
        copy(romanized = transliterateGurmukhi(text).takeIf { it != text })
}

private const val UNSYNCED_STEP_MS = 3_000L
