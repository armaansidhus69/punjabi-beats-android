package com.punjabibeats.app.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/**
 * Linear crossfade between consecutive tracks, implemented as a Media3
 * [AudioProcessor] (the Metrolist approach to crossfade without a second
 * player instance): the output gain ramps down over the last
 * [fadeDurationMs] of a track and ramps up over the first [fadeDurationMs]
 * of the next one. The processor counts PCM frames itself, so it needs no
 * coupling to the player's position thread.
 *
 * Position comes from frame counting; [trackDurationUs] is pushed from the
 * service whenever the current media item changes. A duration of
 * [C.TIME_UNSET] (unknown) disables the fade-out path.
 */
@UnstableApi
class CrossfadeAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var fadeDurationMs: Int = 0

    @Volatile
    var trackDurationUs: Long = C.TIME_UNSET

    private var frameCount = 0L
    private var sampleRate = 44_100

    /** Called by the service when the active media item changes. */
    fun onTrackChanged(durationMs: Long) {
        frameCount = 0L
        trackDurationUs = if (durationMs > 0) durationMs * 1_000L else C.TIME_UNSET
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        // Passthrough is fine for non-16-bit encodings; the gain math below
        // only handles PCM 16-bit samples.
        return inputAudioFormat
    }

    override fun queueInput(input: ByteBuffer) {
        val remaining = input.remaining()
        if (remaining == 0) {
            replaceOutputBuffer(0)
            return
        }
        val output = replaceOutputBuffer(remaining)
        val gain = currentGain()
        val isPcm16 = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT
        if (gain >= 0.999f || !isPcm16) {
            output.put(input)
        } else {
            // Process whole samples only; a trailing odd byte (never expected
            // for PCM 16-bit, but cheap to guard) is copied untouched.
            val evenEnd = input.position() + remaining - (remaining % 2)
            while (input.position() < evenEnd) {
                val sample = input.getShort().toInt()
                val faded = (sample * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                output.putShort(faded.toShort())
            }
            if (input.hasRemaining()) output.put(input)
        }
        val framesAdvanced = remaining.toLong() / maxOf(1, bytesPerFrame())
        frameCount += framesAdvanced
    }

    private fun currentGain(): Float {
        val fadeMs = fadeDurationMs
        if (fadeMs <= 0) return 1f
        val positionUs = frameCount * 1_000_000L / sampleRate
        val fadeUs = fadeMs * 1_000L
        val fadeIn = positionUs.coerceAtMost(fadeUs)
        val fadeOut = trackDurationUs.takeIf { it != C.TIME_UNSET }
            ?.let { duration -> (duration - positionUs).coerceAtLeast(0).coerceAtMost(fadeUs) }
            ?: fadeUs
        val limiting = minOf(fadeIn, fadeOut)
        return (limiting.toFloat() / fadeUs).coerceIn(0f, 1f)
    }

    private fun bytesPerFrame(): Int =
        if (inputAudioFormat.bytesPerFrame > 0) inputAudioFormat.bytesPerFrame
        else 2 * inputAudioFormat.channelCount
}
