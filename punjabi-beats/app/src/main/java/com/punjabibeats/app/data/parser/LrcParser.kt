package com.punjabibeats.app.data.parser

import com.punjabibeats.app.core.util.parseTimestampToMs
import com.punjabibeats.app.core.util.unescapeHtml
import com.punjabibeats.app.domain.model.LyricLine
import com.punjabibeats.app.domain.model.TimedWord

/**
 * Parses LRC lyrics with "[mm:ss.xx]" (and multiple) timestamps per line.
 * Also supports the enhanced-LRC (A2) word extension "<mm:ss.xx>" for
 * word-by-word karaoke highlighting — when a line carries word tags, each
 * word gets its own [TimedWord] window.
 */
object LrcParser {

    private val tagRegex = Regex("""\[(\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?)\]""")
    private val wordTagRegex = Regex("""<(\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?)>""")
    private const val LAST_WORD_SPAN_MS = 4_000L

    fun parse(raw: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        raw.lineSequence().forEach { rawLine ->
            val tags = tagRegex.findAll(rawLine).toList()
            if (tags.isEmpty()) return@forEach
            val content = rawLine.substring(tags.last().range.last + 1)
            val wordTags = wordTagRegex.findAll(content).toList()
            if (wordTags.isEmpty()) {
                val text = content.trim().let(::unescapeHtml)
                if (text.isBlank()) return@forEach
                tags.forEach { tag ->
                    parseTimestampToMs(tag.groupValues[1])?.let { timeMs ->
                        lines += LyricLine(timeMs = timeMs, text = text)
                    }
                }
            } else {
                val text = content.replace(wordTagRegex, "").let(::unescapeHtml)
                if (text.isBlank()) return@forEach
                val words = parseWords(content, wordTags)
                tags.forEach { tag ->
                    parseTimestampToMs(tag.groupValues[1])?.let { timeMs ->
                        lines += LyricLine(timeMs = timeMs, text = text, words = words)
                    }
                }
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    /** Word text runs from right after one "<…>" tag to the start of the next. */
    private fun parseWords(content: String, wordTags: List<MatchResult>): List<TimedWord> {
        val words = mutableListOf<TimedWord>()
        wordTags.forEachIndexed { index, match ->
            val startMs = parseTimestampToMs(match.groupValues[1]) ?: return@forEachIndexed
            val nextTag = wordTags.getOrNull(index + 1)
            val rawWord = content.substring(
                startIndex = match.range.last + 1,
                endIndex = nextTag?.range?.first ?: content.length,
            ).trim()
            if (rawWord.isBlank()) return@forEachIndexed
            val endMs = nextTag
                ?.let { parseTimestampToMs(it.groupValues[1]) }
                ?: (startMs + LAST_WORD_SPAN_MS)
            words += TimedWord(text = rawWord, startMs = startMs, endMs = endMs)
        }
        return words.sortedBy { it.startMs }
    }
}
