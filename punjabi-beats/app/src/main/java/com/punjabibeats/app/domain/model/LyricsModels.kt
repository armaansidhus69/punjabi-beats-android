package com.punjabibeats.app.domain.model

/** One timed word inside a lyric line (enhanced-LRC / karaoke mode). */
data class TimedWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

/** One timed line of a synced lyric track. */
data class LyricLine(
    val timeMs: Long,
    val text: String,
    val romanized: String? = null,
    val words: List<TimedWord> = emptyList(),
)

data class SyncedLyrics(
    val trackName: String,
    val artistName: String,
    val lines: List<LyricLine>,
    val provider: String,
    val isSynced: Boolean,
)

/** Transcript cue for podcast episodes (SRT / WebVTT). */
data class TranscriptCue(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

enum class ScriptMode { GURMUKHI, ROMAN, BOTH }
