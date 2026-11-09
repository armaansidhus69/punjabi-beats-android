package com.punjabibeats.app.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
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

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(PbDataSourceFactory.defaultFor(this)))
            .setRenderersFactory(renderersFactory)
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
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                crossfadeProcessor.onTrackChanged(player.duration.coerceAtLeast(0L))
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
                crossfadeProcessor.onTrackChanged(player.duration.coerceAtLeast(0L))
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
            audioEffects.setBassBoostEnabled(bass > 0, bass.toShort())
            audioEffects.setVirtualizerEnabled(virtualizer > 0, virtualizer.toShort())
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
            serviceScope.launch {
                val safeIndex = startIndex.coerceIn(0, mediaItems.lastIndex.coerceAtLeast(0))
                val targetItem = mediaItems.getOrNull(safeIndex)
                // Resolve the song the user actually tapped right away (< 500ms)
                val resolvedTarget = targetItem?.let { resolveItemSafely(it) } ?: targetItem
                val initialList = mediaItems.toMutableList()
                if (resolvedTarget != null && safeIndex in initialList.indices) {
                    initialList[safeIndex] = resolvedTarget
                }
                // Unblock playback immediately!
                future.set(MediaSession.MediaItemsWithStartPosition(initialList, safeIndex, startPositionMs))

                // Resolve remaining items concurrently in the background so next track transitions seamlessly
                val restIndices = initialList.indices.filter { it != safeIndex }
                restIndices.map { idx ->
                    async {
                        val resolved = resolveItemSafely(initialList[idx])
                        withContext(Dispatchers.Main) {
                            if (idx < mediaSession.player.mediaItemCount) {
                                mediaSession.player.replaceMediaItem(idx, resolved)
                            }
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
            serviceScope.launch {
                // Resolve batch items concurrently in parallel rather than sequentially
                val resolved = mediaItems.map { item -> async { resolveItemSafely(item) } }.awaitAll()
                future.set(resolved)
            }
            return future
        }
    }

    private suspend fun resolveItemSafely(item: MediaItem): MediaItem {
        val song = item.toSong() ?: return item.withSilence()
        val offline = runCatching { downloadRepository.get(song.id) }.getOrNull()
        if (offline != null) {
            return item.buildUpon().setUri("pbenc://${offline.id}").build()
        }
        if (!song.streamUrl.isNullOrBlank()) {
            return item.buildUpon().setUri(song.streamUrl).build()
        }
        return try {
            val resolved = streamResolver.resolve(song)
            item.buildUpon().setUri(resolved.streamUrl).build()
        } catch (resolutionFailure: Exception) {
            resolutionBus.emitResolutionFailure(song)
            item.withSilence()
        }
    }

    private fun MediaItem.withSilence(): MediaItem =
        buildUpon()
            .setUri(SilenceAsset.DATA_URI_PREFIX + SilenceAsset.SILENT_WAV_BASE64)
            .build()
}
