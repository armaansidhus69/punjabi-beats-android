package com.punjabibeats.app.playback

import com.punjabibeats.app.domain.model.Song
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Relay from the background MediaPlaybackService to the UI layer when a song
 * cannot be resolved by any source in the fallback chain. The UI surfaces a
 * snackbar; playback continues with a silence placeholder, never crashes.
 */
@Singleton
class ResolutionBus @Inject constructor() {

    private val _resolutionFailures = MutableSharedFlow<Song>(extraBufferCapacity = 16)
    val resolutionFailures: SharedFlow<Song> = _resolutionFailures.asSharedFlow()

    suspend fun emitResolutionFailure(song: Song) {
        _resolutionFailures.emit(song)
    }
}
