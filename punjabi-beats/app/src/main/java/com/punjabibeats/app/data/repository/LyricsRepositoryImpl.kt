package com.punjabibeats.app.data.repository

import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.data.parser.LrcParser
import com.punjabibeats.app.data.remote.api.LrcLibApi
import com.punjabibeats.app.domain.model.LyricLine
import com.punjabibeats.app.domain.model.SyncedLyrics
import com.punjabibeats.app.domain.repository.LyricsRepository
import com.punjabibeats.app.core.util.transliterateGurmukhi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lyrics source: LRCLIB synced LRC first, then plain lyrics rendered as one
 * unsynced block. Gurmukhi lines get a roman transliteration attached upfront.
 */
@Singleton
class LyricsRepositoryImpl @Inject constructor(
    private val lrcLibApi: LrcLibApi,
) : LyricsRepository {

    override suspend fun fetchLyrics(title: String, artist: String, durationMs: Long): SyncedLyrics? {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return null
        val durationSec = (durationMs / 1000L).toInt().takeIf { it > 0 }

        val exact = runSuspendCatching {
            lrcLibApi.get(cleanTitle, artist.trim(), durationSec).body()
        }.getOrNull()
        exact?.let { return it.toSyncedLyrics(cleanTitle, artist) }

        val fuzzy = runSuspendCatching {
            lrcLibApi.search("$artist $cleanTitle", limit = 3).firstOrNull()
        }.getOrNull()
        return fuzzy?.toSyncedLyrics(cleanTitle, artist)
    }

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
