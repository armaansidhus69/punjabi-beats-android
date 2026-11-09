package com.punjabibeats.app.playback

import android.content.ComponentName
import android.content.Context
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.punjabibeats.app.core.util.SongCodec
import com.punjabibeats.app.data.local.datastore.SettingsDataStore
import com.punjabibeats.app.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The single source of playback truth for the UI. Connects to
 * [MediaPlaybackService] through a MediaController and mirrors its timeline
 * into StateFlows. Queue + preferences persist to DataStore so the session is
 * restored on the next app launch (app kill or reboot).
 */
@Singleton
@UnstableApi
class PlayerControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsDataStore,
) : MusicPlayerController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _mediaController = MutableStateFlow<MediaController?>(null)
    private val _isPlaying = MutableStateFlow(false)
    private val _isBuffering = MutableStateFlow(false)
    private val _currentSong = MutableStateFlow<Song?>(null)
    private val _positionMs = MutableStateFlow(0L)
    private val _durationMs = MutableStateFlow(0L)
    private val _bufferedMs = MutableStateFlow(0L)
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    private val _currentIndex = MutableStateFlow(0)
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    private val _shuffleEnabled = MutableStateFlow(false)
    private val _playbackSpeed = MutableStateFlow(1.0f)
    private val _playbackPitch = MutableStateFlow(1.0f)
    private val _skipSilenceEnabled = MutableStateFlow(false)
    private val _sleepMode = MutableStateFlow<SleepMode?>(null)
    private val _playerErrors = MutableSharedFlow<String>(extraBufferCapacity = 8)

    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    override val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()
    override val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    override val positionMs: StateFlow<Long> = _positionMs.asStateFlow()
    override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()
    override val bufferedMs: StateFlow<Long> = _bufferedMs.asStateFlow()
    override val queue: StateFlow<List<Song>> = _queue.asStateFlow()
    override val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    override val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()
    override val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()
    override val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()
    override val playbackPitch: StateFlow<Float> = _playbackPitch.asStateFlow()
    override val skipSilenceEnabled: StateFlow<Boolean> = _skipSilenceEnabled.asStateFlow()
    override val sleepMode: StateFlow<SleepMode?> = _sleepMode.asStateFlow()
    override val playerErrors: SharedFlow<String> = _playerErrors.asSharedFlow()

    private var positionJob: Job? = null
    private var sleepJob: Job? = null
    private var restored = false

    init {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MediaPlaybackService::class.java),
        )
        val future = MediaController.Builder(context, sessionToken)
            .setApplicationLooper(Looper.getMainLooper())
            .buildAsync()
        future.addListener(
            {
                val controller = runCatching { future.get() }.getOrNull()
                if (controller != null) {
                    _mediaController.value = controller
                    controller.addListener(playerListener)
                    publishTimeline()
                    publishPosition()
                    restorePersistedState()
                    observeQueueForPersistence()
                } else {
                    _playerErrors.tryEmit("Could not start the audio engine. Please reopen the app.")
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            if (playing) startTicker() else stopTicker()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _isBuffering.value = playbackState == Player.STATE_BUFFERING
            if (playbackState == Player.STATE_READY) {
                publishCurrent()
                publishPosition()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            publishTimeline()
            publishCurrent()
            publishPosition()
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            publishTimeline()
            publishCurrent()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _playbackSpeed.value = playbackParameters.speed
            _playbackPitch.value = playbackParameters.pitch
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }

        override fun onShuffleModeEnabledChanged(enabled: Boolean) {
            _shuffleEnabled.value = enabled
        }

        override fun onPlayerError(error: PlaybackException) {
            _playerErrors.tryEmit(error.message ?: "Playback error")
        }
    }

    // ------------------------------------------------------------------ queue

    override fun playQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, songs.lastIndex)
        withController { controller ->
            controller.setMediaItems(songs.map { it.toMediaItem() }, safeIndex, 0L)
            controller.prepare()
            controller.play()
        }
    }

    override fun playNext(song: Song) = withController { controller ->
        controller.addMediaItems(controller.currentMediaItemIndex + 1, listOf(song.toMediaItem()))
    }

    override fun addToQueue(songs: List<Song>) = withController { controller ->
        controller.addMediaItems(songs.map { it.toMediaItem() })
    }

    override fun restoreQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        withController { controller ->
            controller.setMediaItems(songs.map { it.toMediaItem() }, startIndex.coerceIn(0, songs.lastIndex), 0L)
        }
    }

    override fun playPause() = withController { controller ->
        if (controller.isPlaying) controller.pause() else {
            if (controller.currentMediaItem == null && _queue.value.isNotEmpty()) {
                restoreQueue(_queue.value, _currentIndex.value)
                controller.prepare()
            }
            controller.play()
        }
    }

    override fun play() = withController { it.play() }

    override fun pause() = withController { it.pause() }

    override fun next() = withController { controller ->
        if (controller.hasNextMediaItem()) controller.seekToNextMediaItem()
        else if (controller.repeatMode == Player.REPEAT_MODE_ALL) controller.seekToDefaultPosition(0)
    }

    override fun previous() = withController { controller ->
        if (controller.currentPosition > 3_000L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0L)
        }
    }

    override fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs.coerceAtLeast(0L)) }

    override fun seekToIndex(index: Int) = withController { controller ->
        if (index in 0 until controller.mediaItemCount) controller.seekToDefaultPosition(index)
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) = withController { controller ->
        if (fromIndex in 0 until controller.mediaItemCount) controller.moveMediaItem(fromIndex, toIndex)
    }

    override fun removeFromQueue(index: Int) = withController { controller ->
        if (index in 0 until controller.mediaItemCount) controller.removeMediaItem(index)
    }

    override fun cycleRepeatMode() = withController { controller ->
        controller.repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        scope.launch { settings.saveRepeatMode(controller.repeatMode) }
    }

    override fun toggleShuffle() = withController { controller ->
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
        scope.launch { settings.saveShuffle(controller.shuffleModeEnabled) }
    }

    override fun setPlaybackSpeed(speed: Float) = withController { controller ->
        controller.playbackParameters = controller.playbackParameters.withSpeed(speed)
        scope.launch { settings.savePlaybackSpeed(speed) }
    }

    override fun setPlaybackPitch(pitch: Float) = withController { controller ->
        controller.playbackParameters = androidx.media3.common.PlaybackParameters(
            controller.playbackParameters.speed,
            pitch,
        )
        scope.launch { settings.savePlaybackPitch(pitch) }
    }

    override fun setSkipSilence(enabled: Boolean) = withController { controller ->
        if (controller is androidx.media3.exoplayer.ExoPlayer) {
            controller.setSkipSilenceEnabled(enabled)
        }
        scope.launch { settings.saveSkipSilence(enabled) }
    }

    // ------------------------------------------------------------- sleep timer

    override fun startSleepTimer(mode: SleepMode) {
        cancelSleepTimer()
        _sleepMode.value = mode
        sleepJob = scope.launch {
            when (mode) {
                is SleepMode.Minutes -> delay(mode.minutes * 60_000L)
                SleepMode.EndOfTrack -> {
                    while (isActive) {
                        val controller = _mediaController.value ?: break
                        val duration = controller.duration
                        if (duration > 0 && controller.currentPosition >= duration - 300) break
                        delay(250)
                    }
                }
            }
            _mediaController.value?.pause()
            _sleepMode.value = null
        }
    }

    override fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        _sleepMode.value = null
    }

    // ------------------------------------------------------------------ internals

    private fun withController(block: (MediaController) -> Unit) {
        _mediaController.value?.let(block)
    }

    private fun startTicker() {
        if (positionJob?.isActive == true) return
        positionJob = scope.launch {
            while (isActive) {
                publishPosition()
                delay(500)
            }
        }
    }

    private fun stopTicker() {
        positionJob?.cancel()
        positionJob = null
        publishPosition()
    }

    private fun publishPosition() {
        val controller = _mediaController.value ?: return
        _positionMs.value = controller.currentPosition.coerceAtLeast(0L)
        _durationMs.value = controller.duration.takeIf { it > 0 } ?: 0L
        _bufferedMs.value = controller.bufferedPosition.coerceAtLeast(0L)
    }

    private fun publishCurrent() {
        val controller = _mediaController.value ?: return
        val item = controller.currentMediaItem
        _currentSong.value = item?.toSong() ?: _currentSong.value
        _currentIndex.value = controller.currentMediaItemIndex
    }

    private fun publishTimeline() {
        val controller = _mediaController.value ?: return
        _queue.value = (0 until controller.mediaItemCount)
            .map { controller.getMediaItemAt(it) }.toSongs()
        _currentIndex.value = controller.currentMediaItemIndex
    }

    private fun restorePersistedState() {
        if (restored) return
        restored = true
        scope.launch {
            val persisted = settings.lastQueue().first()
            val songs = SongCodec.decodeList(persisted.songsJson)
            if (songs.isNotEmpty()) {
                restoreQueue(songs, persisted.startIndex)
                publishTimeline()
                publishCurrent()
            }
            _repeatMode.value = settings.repeatMode().first()
            _shuffleEnabled.value = settings.shuffleEnabled().first()
            _playbackSpeed.value = settings.playbackSpeed().first()
            _playbackPitch.value = settings.playbackPitch().first()
            _skipSilenceEnabled.value = settings.skipSilence().first()
            withController { controller ->
                controller.repeatMode = _repeatMode.value
                controller.shuffleModeEnabled = _shuffleEnabled.value
                if (controller is androidx.media3.exoplayer.ExoPlayer) {
                    controller.setSkipSilenceEnabled(_skipSilenceEnabled.value)
                }
                controller.playbackParameters = androidx.media3.common.PlaybackParameters(
                    _playbackSpeed.value,
                    _playbackPitch.value,
                )
            }
        }
    }

    private fun observeQueueForPersistence() {
        scope.launch {
            combine(_queue, _currentIndex) { songs, index -> songs to index }
                .debounce(1_000)
                .collect { (songs, index) ->
                    if (songs.isNotEmpty()) settings.saveQueue(SongCodec.encodeList(songs), index)
                }
        }
    }
}
