package com.punjabibeats.app.playback.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session-scoped audio effects (the Metrolist audio-customization pattern):
 * a platform 5-band [Equalizer] plus [BassBoost] and [Virtualizer], attached
 * to ExoPlayer's audio session id and configured from the settings DataStore.
 * Every call is guarded — many devices/ROMs throw on unsupported effects — so
 * a missing effect degrades to a no-op instead of killing playback.
 */
@Singleton
class AudioEffectsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    val bandCount: Int
        get() = equalizer?.numberOfBands?.toInt() ?: 0

    val bandRange: Short
        get() = equalizer?.bandLevelRange?.getOrNull(1) ?: 0

    fun bandFrequencies(): List<String> {
        val eq = equalizer ?: return emptyList()
        return (0 until eq.numberOfBands).map { band ->
            val centerMilliHz = eq.getCenterFreq(band.toShort())
            "${centerMilliHz / 1000} Hz"
        }
    }

    /** Creates the effect instances bound to the player's audio session. */
    fun attach(audioSessionId: Int) {
        release()
        if (audioSessionId <= 0) return
        runCatching { equalizer = Equalizer(PRIORITY, audioSessionId) }
        runCatching { bassBoost = BassBoost(PRIORITY, audioSessionId) }
        runCatching { virtualizer = Virtualizer(PRIORITY, audioSessionId) }
    }

    fun release() {
        runCatching { equalizer?.enabled = false }
        runCatching { bassBoost?.enabled = false }
        runCatching { virtualizer?.enabled = false }
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
    }

    fun setEqualizerEnabled(enabled: Boolean, bandLevelsMilliBel: List<Short>) {
        val eq = equalizer ?: return
        runCatching {
            eq.enabled = enabled
            if (enabled) {
                bandLevelsMilliBel.take(eq.numberOfBands.toInt()).forEachIndexed { band, level ->
                    eq.setBandLevel(band.toShort(), level)
                }
            }
        }
    }

    fun setBandLevel(band: Short, levelMilliBel: Short) {
        runCatching { equalizer?.setBandLevel(band, levelMilliBel) }
    }

    fun setBassBoostEnabled(enabled: Boolean, strengthPerMil: Short) {
        val bass = bassBoost ?: return
        runCatching {
            bass.enabled = enabled
            bass.setStrength(if (enabled) strengthPerMil else 0)
        }
    }

    fun setVirtualizerEnabled(enabled: Boolean, strengthPerMil: Short) {
        val virt = virtualizer ?: return
        runCatching {
            virt.enabled = enabled
            virt.setStrength(if (enabled) strengthPerMil else 0)
        }
    }

    private companion object {
        const val PRIORITY = 1
    }
}
