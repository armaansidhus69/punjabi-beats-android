package com.punjabibeats.app.data.resolver

import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.domain.model.MusicSource
import com.punjabibeats.app.domain.model.Song
import com.zenmusic.ZenMusic
import com.zenmusic.core.uri.ZenUri
import com.zenmusic.datasource.ZenMediaItems
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays through the bundled Zen engine — no backend API. Resolves a Song to
 * an opaque `zen://…#zh=<handle>` stream url served by the engine's channel
 * transport (see [com.punjabibeats.app.playback.PbDataSourceFactory], which
 * routes zen:// uris into it).
 *
 * Songs from YouTube already carry a videoId; every other source gets a
 * best-effort engine search by "title artist". Null when the engine cannot
 * help — the StreamResolver chain then moves to the next resolver.
 */
@Singleton
class ZenMusicResolver @Inject constructor() : TrackResolver {

    override suspend fun resolve(song: Song): Song? = runSuspendCatching {
        val zen = ZenMusic.get() ?: return@runSuspendCatching null
        val streamUri = song.videoId?.takeIf { it.isNotBlank() }
            ?.let { ZenUri.mint(it) }
            ?: zen.search.songs("${song.title} ${song.artist}".trim())
                .firstOrNull()?.uri
            ?: return@runSuspendCatching null
        val stream = zen.resolve(streamUri)
        song.copy(
            streamUrl = ZenMediaItems.withHandle(stream),
            source = MusicSource.YOUTUBE,
            videoId = stream.uri.removePrefix("zen://").substringBefore('.'),
        )
    }.getOrNull()
}
