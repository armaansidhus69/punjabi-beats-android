package com.punjabibeats.app.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.punjabibeats.app.MainActivity
import com.punjabibeats.app.R
import com.punjabibeats.app.data.local.datastore.SettingsDataStore
import com.punjabibeats.app.data.resolver.SilenceAsset
import com.punjabibeats.app.data.resolver.StreamResolver
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.domain.repository.DownloadRepository
import com.punjabibeats.app.playback.audio.AudioEffectsManager
import com.punjabibeats.app.playback.audio.CrossfadeAudioProcessor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground Media3 session service. Audio focus handling (duck, pause on
 * calls, resume), pause-on-headset-disconnect and wake locks are delegated to
 * ExoPlayer flags; stream resolution runs through the fallback chain and every
 * unresolvable item degrades to a short silence asset instead of crashing.
 *
 * Metrolist-style audio customization lives here: the renderers factory plugs
 * a [CrossfadeAudioProcessor] into the audio sink, [AudioEffectsManager] binds
 * EQ/bass/virtualizer to the player's audio session, and skip-silence follows
 * the persisted setting. All of it is best-effort — device-level failures are
 * swallowed so playback itself is never affected.
 */
@AndroidEntryPoint
@UnstableApi
class MediaPlaybackService : MediaSessionService() {

    @Inject lateinit var streamResolver: StreamResolver
    @Inject lateinit var downloadRepository: DownloadRepository
    @Inject lateinit var resolutionBus: ResolutionBus
    @Inject lateinit var settings: SettingsDataStore
    @Inject lateinit var audioEffects: AudioEffectsManager

    private val crossfadeProcessor = CrossfadeAudioProcessor()
    private val permanentlyFailedIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val errorRetryCounts = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private var activeQueueJob: kotlinx.coroutines.Job? = null
    private var activeTransitionJob: kotlinx.coroutines.Job? = null
    private var activePreFetchJob: kotlinx.coroutines.Job? = null

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(false)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf(crossfadeProcessor))
                .build()
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 1_500,
                /* bufferForPlaybackAfterRebufferMs = */ 2_500,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(PbDataSourceFactory.defaultFor(this)))
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(musicAudioAttributes(), /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
                audioEffects.attach(audioSessionId)
                applyEffectsIfEnabled()
            }
        })
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    val currentSongId = player.currentMediaItem?.toSong()?.id
                    if (currentSongId != null) {
                        errorRetryCounts.remove(currentSongId)
                    }
                    val dur = player.duration
                    if (dur > 0) {
                        crossfadeProcessor.updateDuration(dur)
                    }
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: androidx.media3.common.Player.PositionInfo,
                newPosition: androidx.media3.common.Player.PositionInfo,
                reason: Int,
            ) {
                if (reason == androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK) {
                    crossfadeProcessor.onSeek(newPosition.positionMs)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                crossfadeProcessor.onTrackChanged(player.duration.coerceAtLeast(0L))
                val currentIdx = player.currentMediaItemIndex
                if (currentIdx in 0 until player.mediaItemCount) {
                    val currentItem = player.getMediaItemAt(currentIdx)
                    val currentUri = currentItem.localConfiguration?.uri?.toString().orEmpty()
                    if (currentUri.isEmpty() || currentUri.startsWith(SilenceAsset.DATA_URI_PREFIX)) {
                        val songId = currentItem.toSong()?.id.orEmpty()
                        if (songId.isNotEmpty() && permanentlyFailedIds.contains(songId)) {
                            advanceOrPause(player, shouldPlay = player.playWhenReady)
                        } else {
                            // CRITICAL FIX P1.1: Pause immediately so ExoPlayer does NOT play out
                            // the short silence placeholder WAV and auto-skip before network resolution completes!
                            val wasPlayWhenReady = player.playWhenReady
                            player.pause()
                            activeTransitionJob?.cancel()
                            activeTransitionJob = serviceScope.launch {
                                val resolved = resolveItemSafely(currentItem)
                                withContext(Dispatchers.Main) {
                                    if (currentIdx == player.currentMediaItemIndex && currentIdx < player.mediaItemCount) {
                                        val isStillSilence = resolved.localConfiguration?.uri?.toString()
                                            ?.startsWith(SilenceAsset.DATA_URI_PREFIX) == true
                                        if (!isStillSilence) {
                                            player.replaceMediaItem(currentIdx, resolved)
                                            player.seekToDefaultPosition(currentIdx)
                                            player.playWhenReady = true
                                            player.prepare()
                                            player.play()
                                        } else {
                                            if (songId.isNotEmpty()) permanentlyFailedIds.add(songId)
                                            advanceOrPause(player, shouldPlay = wasPlayWhenReady)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Pre-resolve the real next track in queue (accounts for shuffle mode & repeat all)
                val timeline = player.currentTimeline
                val nextIdx = if (!timeline.isEmpty) {
                    timeline.getNextWindowIndex(currentIdx, player.repeatMode, player.shuffleModeEnabled)
                } else {
                    currentIdx + 1
                }
                if (nextIdx != androidx.media3.common.C.INDEX_UNSET && nextIdx in 0 until player.mediaItemCount) {
                    val nextItem = player.getMediaItemAt(nextIdx)
                    val nextUri = nextItem.localConfiguration?.uri?.toString().orEmpty()
                    if (nextUri.isEmpty() || nextUri.startsWith(SilenceAsset.DATA_URI_PREFIX)) {
                        activePreFetchJob?.cancel()
                        activePreFetchJob = serviceScope.launch {
                            val resolved = resolveItemSafely(nextItem)
                            withContext(Dispatchers.Main) {
                                if (nextIdx < player.mediaItemCount) {
                                    player.replaceMediaItem(nextIdx, resolved)
                                }
                            }
                        }
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val currentIdx = player.currentMediaItemIndex
                if (currentIdx !in 0 until player.mediaItemCount) return
                val currentItem = player.getMediaItemAt(currentIdx)
                val song = currentItem.toSong()
                if (song == null) {
                    advanceOrPause(player)
                    return
                }

                android.util.Log.w("MediaPlaybackService", "onPlayerError on ${song.id} (${song.title}): ${error.errorCodeName} - ${error.message}")
                streamResolver.invalidate(song.id)
                val retries = errorRetryCounts.getOrDefault(song.id, 0)
                if (retries < 1) {
                    // Capture current position before retrying so playback resumes exactly where it stalled!
                    val resumePositionMs = player.currentPosition.coerceAtLeast(0L)
                    errorRetryCounts[song.id] = retries + 1
                    activeTransitionJob?.cancel()
                    activeTransitionJob = serviceScope.launch {
                        val freshSong = song.copy(streamUrl = null)
                        val resolved = resolveItemSafely(freshSong.toMediaItem())
                        withContext(Dispatchers.Main) {
                            if (currentIdx == player.currentMediaItemIndex && currentIdx < player.mediaItemCount) {
                                val isSilence = resolved.localConfiguration?.uri?.toString()
                                    ?.startsWith(SilenceAsset.DATA_URI_PREFIX) == true
                                if (!isSilence) {
                                    player.replaceMediaItem(currentIdx, resolved)
                                    player.seekTo(currentIdx, resumePositionMs)
                                    player.playWhenReady = true
                                    player.prepare()
                                    player.play()
                                    return@withContext
                                }
                            }
                            permanentlyFailedIds.add(song.id)
                            streamResolver.invalidate(song.id)
                            advanceOrPause(player)
                        }
                    }
                } else {
                    permanentlyFailedIds.add(song.id)
                    streamResolver.invalidate(song.id)
                    advanceOrPause(player)
                }
            }
        })

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .setCallback(SessionCallback())
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply { setSmallIcon(R.drawable.ic_notification) }
        )
        observeAudioSettings(player)
    }

    @OptIn(UnstableApi::class)
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        audioEffects.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        serviceScope.cancel()
        mainScope.cancel()
        super.onDestroy()
    }

    /**
     * Mirrors the audio-related settings into the live player + effects.
     * Settings are persisted by the Settings screen; this side only reads.
     */
    private fun observeAudioSettings(player: ExoPlayer) {
        mainScope.launch {
            settings.crossfadeSeconds().collect { seconds ->
                crossfadeProcessor.fadeDurationMs = seconds * 1_000
            }
        }
        mainScope.launch {
            settings.skipSilence().collect { enabled ->
                runCatching { player.skipSilenceEnabled = enabled }
            }
        }
        serviceScope.launch {
            settings.equalizerEnabled().collect { applyEffectsIfEnabled() }
        }
        serviceScope.launch {
            settings.equalizerBands().collect { applyEffectsIfEnabled() }
        }
        serviceScope.launch {
            settings.bassBoostStrength().collect { applyEffectsIfEnabled() }
        }
        serviceScope.launch {
            settings.virtualizerStrength().collect { applyEffectsIfEnabled() }
        }
    }

    private fun applyEffectsIfEnabled() {
        serviceScope.launch {
            val eqEnabled = settings.equalizerEnabled().first()
            val bands = settings.equalizerBands().first()
            val bass = settings.bassBoostStrength().first()
            val virtualizer = settings.virtualizerStrength().first()
            audioEffects.setEqualizerEnabled(eqEnabled, bands)
            audioEffects.setBassBoostEnabled(eqEnabled && bass > 0, bass.toShort())
            audioEffects.setVirtualizerEnabled(eqEnabled && virtualizer > 0, virtualizer.toShort())
        }
    }

    private fun advanceOrPause(player: androidx.media3.common.Player, shouldPlay: Boolean = true) {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            if (shouldPlay) {
                player.prepare()
                player.play()
            }
        } else {
            player.pause()
        }
    }

    private fun musicAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    private inner class SessionCallback : MediaSession.Callback {

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            permanentlyFailedIds.clear()
            errorRetryCounts.clear()
            activeQueueJob?.cancel()
            activeTransitionJob?.cancel()
            activePreFetchJob?.cancel()
            activeQueueJob = serviceScope.launch {
                val safeIndex = startIndex.coerceIn(0, mediaItems.lastIndex.coerceAtLeast(0))
                val targetItem = mediaItems.getOrNull(safeIndex)
                // Resolve the song the user actually tapped right away (< 500ms)
                val resolvedTarget = targetItem?.let { resolveItemSafely(it) } ?: targetItem
                // In ExoPlayer, every MediaItem in a playlist must have a valid non-null URI
                // (localConfiguration != null). Items not yet resolved get a silence placeholder.
                val initialList = mediaItems.mapIndexed { idx, item ->
                    if (idx == safeIndex) (resolvedTarget ?: item.withSilence())
                    else if (item.localConfiguration?.uri != null) item
                    else item.withSilence()
                }.toMutableList()

                // Unblock playback immediately!
                future.set(MediaSession.MediaItemsWithStartPosition(initialList, safeIndex, startPositionMs))

                // Pre-resolve the immediate next track first so its transition is seamless (accounts for shuffle)
                val timeline = mediaSession.player.currentTimeline
                val nextIdx = if (!timeline.isEmpty) {
                    timeline.getNextWindowIndex(safeIndex, mediaSession.player.repeatMode, mediaSession.player.shuffleModeEnabled)
                } else {
                    safeIndex + 1
                }
                if (nextIdx != androidx.media3.common.C.INDEX_UNSET && nextIdx in initialList.indices) {
                    val resolvedNext = resolveItemSafely(mediaItems[nextIdx])
                    withContext(Dispatchers.Main) {
                        if (nextIdx < mediaSession.player.mediaItemCount) {
                            mediaSession.player.replaceMediaItem(nextIdx, resolvedNext)
                        }
                    }
                }
            }
            return future
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            val future: SettableFuture<List<MediaItem>> = SettableFuture.create()
            // Non-blocking queue addition: items with URIs keep them; unresolved get silence placeholders
            // and resolve lazily when playback transitions near them.
            val initial = mediaItems.map { item ->
                if (item.localConfiguration?.uri != null) item else item.withSilence()
            }
            future.set(initial)
            return future
        }
    }

    private suspend fun resolveItemSafely(item: MediaItem): MediaItem {
        val song = item.toSong() ?: return item.withSilence()
        if (permanentlyFailedIds.contains(song.id)) {
            return item.withSilence()
        }
        val offline = runCatching { downloadRepository.get(song.id) }.getOrNull()
        if (offline != null && com.punjabibeats.app.data.security.DownloadCrypto.fileFor(this, offline.id).exists()) {
            return item.buildUpon().setUri("pbenc://${offline.id}").build()
        }
        if (!song.streamUrl.isNullOrBlank()) {
            return item.buildUpon().setUri(song.streamUrl).build()
        }
        return try {
            val resolved = streamResolver.resolve(song)
            if (resolved.streamUrl.isNullOrBlank()) {
                permanentlyFailedIds.add(song.id)
                resolutionBus.emitResolutionFailure(song)
                item.withSilence()
            } else {
                item.buildUpon().setUri(resolved.streamUrl).build()
            }
        } catch (resolutionFailure: Exception) {
            permanentlyFailedIds.add(song.id)
            resolutionBus.emitResolutionFailure(song)
            item.withSilence()
        }
    }

    private fun MediaItem.withSilence(): MediaItem =
        buildUpon()
            .setUri(SilenceAsset.DATA_URI_PREFIX + SilenceAsset.SILENT_WAV_BASE64)
            .build()
}
