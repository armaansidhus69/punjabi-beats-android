package com.punjabibeats.app.playback

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.punjabibeats.app.core.util.SongCodec
import com.punjabibeats.app.domain.model.Song

/** Bridges domain [Song]s and Media3 [MediaItem]s via metadata extras. */
const val EXTRA_SONG_JSON = "pb.song.json"

fun Song.toMediaItem(uri: String? = streamUrl): MediaItem {
    val extras = Bundle().apply { putString(EXTRA_SONG_JSON, SongCodec.encode(this@toMediaItem)) }
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(albumName)
        .setArtworkUri(artworkUrl?.toUri())
        .setExtras(extras)
        .build()
    val builder = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(metadata)
    uri?.let { builder.setUri(it) }
    return builder.build()
}

fun MediaItem.toSong(): Song? {
    val restored = mediaMetadata.extras?.getString(EXTRA_SONG_JSON)?.let { SongCodec.decode(it) }
    if (restored != null) return restored
    if (mediaId.isBlank()) return null
    return Song(
        id = mediaId,
        title = mediaMetadata.title?.toString().orEmpty(),
        artist = mediaMetadata.artist?.toString() ?: "Unknown Artist",
        albumName = mediaMetadata.albumTitle?.toString().orEmpty(),
        artworkUrl = mediaMetadata.artworkUri?.toString(),
    )
}

fun List<MediaItem>.toSongs(): List<Song> = mapNotNull { it.toSong() }
