package com.punjabibeats.app

import com.punjabibeats.app.core.util.formatDuration
import com.punjabibeats.app.core.util.parseTimestampToMs
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun `formats minutes and seconds`() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:59", formatDuration(59_500))
        assertEquals("3:04", formatDuration(184_000))
        assertEquals("1:01:01", formatDuration(3_661_000))
    }

    @Test
    fun `parses LRC timestamps`() {
        assertEquals(61_240L, parseTimestampToMs("[01:01.24]"))
        assertEquals(0L, parseTimestampToMs("[00:00.00]"))
    }

    @Test
    fun `parses SRT timestamps`() {
        assertEquals(3_661_000L, parseTimestampToMs("01:01:01,000"))
    }

    @Test
    fun `rejects non timestamps`() {
        assertEquals(null, parseTimestampToMs("hello"))
    }
}
