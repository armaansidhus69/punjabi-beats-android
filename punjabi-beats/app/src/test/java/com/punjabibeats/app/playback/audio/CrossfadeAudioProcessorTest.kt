package com.punjabibeats.app.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CrossfadeAudioProcessorTest {

    @Test
    fun `queueInput produces non-empty output buffer with matching bytes when crossfade is zero`() {
        val processor = CrossfadeAudioProcessor()
        val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        val input = ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder())
        for (i in 0 until 512) {
            input.putShort(1000.toShort())
        }
        input.flip()

        processor.queueInput(input)
        val output = processor.getOutput()

        assertEquals("Output buffer must have 1024 bytes remaining to play", 1024, output.remaining())
        // Since gain is 1.0, samples should match exactly
        val firstSample = output.getShort()
        assertEquals(1000.toShort(), firstSample)
    }

    @Test
    fun `queueInput applies fade in scaling when crossfade is active`() {
        val processor = CrossfadeAudioProcessor()
        processor.fadeDurationMs = 2000
        val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()
        processor.onTrackChanged(60_000L) // 60s track

        val input = ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder())
        for (i in 0 until 512) {
            input.putShort(10000.toShort())
        }
        input.flip()

        processor.queueInput(input)
        val output = processor.getOutput()

        assertEquals(1024, output.remaining())
        val firstSample = output.getShort()
        // At start of track (frame 0), gain is 0, so first sample should be near 0
        assertTrue("First sample should be attenuated near 0 during fade in", firstSample < 500)
    }
}
