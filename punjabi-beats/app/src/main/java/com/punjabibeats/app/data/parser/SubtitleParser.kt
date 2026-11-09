package com.punjabibeats.app.data.parser

import com.punjabibeats.app.core.util.parseTimestampToMs
import com.punjabibeats.app.domain.model.TranscriptCue

/** Parses SRT and WebVTT subtitle files into auto-scrollable transcript cues. */
object SubtitleParser {

    private val timingRegex = Regex(
        """(\d{1,3}:\d{1,2}(?::\d{1,2})?[.,]\d{1,3})\s*-->\s*(\d{1,3}:\d{1,2}(?::\d{1,2})?[.,]\d{1,3})"""
    )
    private val markupRegex = Regex("<[^>]+>")

    fun parse(raw: String): List<TranscriptCue> {
        val cues = mutableListOf<TranscriptCue>()
        val blocks = raw.replace("\r\n", "\n").split(Regex("\n{2,}"))
        for (block in blocks) {
            val lines = block.lines().filter { it.isNotBlank() }
            val timingLine = lines.firstOrNull { it.contains("-->") } ?: continue
            val match = timingRegex.find(timingLine) ?: continue
            val startMs = parseTimestampToMs(match.groupValues[1]) ?: continue
            val endMs = parseTimestampToMs(match.groupValues[2]) ?: continue
            val text = lines.dropWhile { it != timingLine }
                .drop(1)
                .joinToString(" ") { markupRegex.replace(it, "") }
                .trim()
            if (text.isNotBlank() && endMs > startMs) cues += TranscriptCue(startMs, endMs, text)
        }
        return cues.sortedBy { it.startMs }
    }
}
