package com.punjabibeats.app.data.resolver

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Standard 44.1kHz, 16-bit stereo PCM true-silence WAV, base64-encoded.
 *
 * Using the standard 44.1kHz stereo format prevents Android AudioTrack / AudioSink
 * from performing dynamic downsampling reconfigurations (which can cause audible hardware pops
 * or clicks when alternating between 8kHz mono placeholders and 44.1kHz audio streams).
 */
object SilenceAsset {

    const val DATA_URI_PREFIX = "data:audio/wav;base64,"

    val SILENT_WAV_BASE64: String by lazy {
        buildSilentWavBase64()
    }

    private fun buildSilentWavBase64(): String {
        val sampleRate = 44_100
        val channels = 2
        val bytesPerSample = 2
        // 0.1s of silence = 4,410 frames = 17,640 bytes of PCM zeroes
        val dataSize = (sampleRate * 0.1).toInt() * channels * bytesPerSample
        val fileSize = 36 + dataSize

        val header = ByteArray(44)
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray(Charsets.US_ASCII))
        bb.putInt(fileSize)
        bb.put("WAVE".toByteArray(Charsets.US_ASCII))
        bb.put("fmt ".toByteArray(Charsets.US_ASCII))
        bb.putInt(16) // Subchunk1Size for PCM
        bb.putShort(1) // AudioFormat 1 = PCM
        bb.putShort(channels.toShort())
        bb.putInt(sampleRate)
        bb.putInt(sampleRate * channels * bytesPerSample) // ByteRate
        bb.putShort((channels * bytesPerSample).toShort()) // BlockAlign
        bb.putShort((bytesPerSample * 8).toShort()) // BitsPerSample
        bb.put("data".toByteArray(Charsets.US_ASCII))
        bb.putInt(dataSize)

        val fullWav = ByteArray(44 + dataSize)
        System.arraycopy(header, 0, fullWav, 0, 44)
        // fullWav data bytes remain 0x00 (silence in 16-bit signed PCM)
        return android.util.Base64.encodeToString(fullWav, android.util.Base64.NO_WRAP)
    }
}
