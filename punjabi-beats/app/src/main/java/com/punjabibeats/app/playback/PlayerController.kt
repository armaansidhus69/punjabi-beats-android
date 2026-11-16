package com.punjabibeats.app.playback

import com.punjabibeats.app.domain.model.Song
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Sleep timer configuration: either a countdown or end of the current track. */
sealed interface SleepMode {
    data class Minutes(val minutes: Int) : SleepMode
    data object EndOfTrack : SleepMode
}

/** Real-time audio stream metrics and codec information. */
data class AudioFormatInfo(
    val codec: String = "MP3",
    val bitrateKbps: Int = 320,
    val sampleRateKhz: Float = 44.1f,
    val isLossless: Boolean = false,
)

/** UI-facing contract over the Media3 session (implemented by PlayerControllerImpl). */
interface MusicPlayerController {

    val isPlaying: StateFlow<Boolean>
    val isBuffering: StateFlow<Boolean>
    val currentSong: StateFlow<Song?>
    val positionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val bufferedMs: StateFlow<Long>
    val queue: StateFlow<List<Song>>
    val currentIndex: StateFlow<Int>
    val repeatMode: StateFlow<Int>
    val shuffleEnabled: StateFlow<Boolean>
    val playbackSpeed: StateFlow<Float>
    val playbackPitch: StateFlow<Float>
    val skipSilenceEnabled: StateFlow<Boolean>
    val sleepMode: StateFlow<SleepMode?>
    val audioFormatInfo: StateFlow<AudioFormatInfo>
    val playerErrors: SharedFlow<String>

    fun playQueue(songs: List<Song>, startIndex: Int = 0)
    fun playNext(song: Song)
    fun addToQueue(songs: List<Song>)
    fun restoreQueue(songs: List<Song>, startIndex: Int)

    fun playPause()
    fun play()
    fun pause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun seekToIndex(index: Int)

    fun moveQueueItem(fromIndex: Int, toIndex: Int)
    fun removeFromQueue(index: Int)

    fun cycleRepeatMode()
    fun toggleShuffle()
    fun setPlaybackSpeed(speed: Float)
    fun setPlaybackPitch(pitch: Float)
    fun setSkipSilence(enabled: Boolean)

    fun startSleepTimer(mode: SleepMode)
    fun cancelSleepTimer()
}
