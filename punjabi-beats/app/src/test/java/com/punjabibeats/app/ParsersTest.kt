package com.punjabibeats.app

import com.punjabibeats.app.data.parser.LrcParser
import com.punjabibeats.app.data.parser.SubtitleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParsersTest {

    @Test
    fun `parses multi tagged LRC lines and sorts them`() {
        val lrc = """
            [00:12.00]Second line
            [00:03.50][01:20.00]Repeated line
            [ar:Artist]
        """.trimIndent()
        val lines = LrcParser.parse(lrc)
        assertEquals(listOf(3_500L, 12_000L, 80_000L), lines.map { it.timeMs })
        assertTrue(lines.none { it.text.isBlank() })
    }

    @Test
    fun `parses enhanced LRC word timings into karaoke words`() {
        val lrc = "[00:10.00]<00:10.00>Ohi <00:10.40>ve <00:11.00>mainu"
        val lines = LrcParser.parse(lrc)
        assertEquals(1, lines.size)
        assertEquals("Ohi ve mainu", lines[0].text)
        assertEquals(3, lines[0].words.size)
        val words = lines[0].words
        assertEquals("Ohi", words[0].text)
        assertEquals(10_000L, words[0].startMs)
        assertEquals(10_400L, words[0].endMs)
        assertEquals(11_000L, words[1].startMs)
        assertTrue(words[2].endMs > words[2].startMs)
    }

    @Test
    fun `plain LRC lines carry no word timings`() {
        val lines = LrcParser.parse("[00:12.00]Second line")
        assertTrue(lines[0].words.isEmpty())
    }

    @Test
    fun `parses SRT blocks`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:04,000
            <i>Hello</i> world

            2
            00:00:05,000 --> 00:00:07,500
            Second cue
        """.trimIndent()
        val cues = SubtitleParser.parse(srt)
        assertEquals(2, cues.size)
        assertEquals(1_000L, cues[0].startMs)
        assertEquals("Hello world", cues[0].text)
        assertEquals(7_500L, cues[1].endMs)
    }
}
