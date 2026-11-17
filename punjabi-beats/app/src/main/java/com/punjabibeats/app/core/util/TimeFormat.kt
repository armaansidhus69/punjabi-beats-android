package com.punjabibeats.app.core.util

/** Formats milliseconds as m:ss (or h:mm:ss beyond an hour). */
fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

/**
 * Parses "[mm:ss.xx]" (LRC), "hh:mm:ss,xxx" (SRT) and "hh:mm:ss.mmm" (WebVTT)
 * timestamps into milliseconds. Returns null when the text is not a timestamp.
 */
fun parseTimestampToMs(raw: String): Long? {
    val cleaned = raw.trim().trim('[', ']', '(', ')')
    val normalized = cleaned.replace(',', '.')
    val parts = normalized.split(':')
    if (parts.isEmpty()) return null
    val secondsPart = parts.last()
    val seconds = secondsPart.toDoubleOrNull() ?: return null
    val minutes = parts.getOrNull(parts.size - 2)?.trim()?.toLongOrNull() ?: 0L
    val hours = parts.getOrNull(parts.size - 3)?.trim()?.toLongOrNull() ?: 0L
    return (hours * 3_600_000L + minutes * 60_000L + (seconds * 1000L).toLong()).coerceAtLeast(0L)
}
