package com.punjabibeats.app.core.util

import com.punjabibeats.app.domain.model.Song
import kotlinx.serialization.builtins.ListSerializer

/** Central (de)serialization point for persisting songs to JSON everywhere. */
object SongCodec {
    private val listSerializer = ListSerializer(Song.serializer())

    /**
     * Strips transient signed remote stream URLs before storing to SQLite or DataStore.
     * Prevents SQLite CursorWindow 2MB overflow when querying playlists with hundreds of songs.
     */
    fun sanitizeForStorage(song: Song): Song =
        if (song.streamUrl?.startsWith("http", ignoreCase = true) == true) song.copy(streamUrl = null) else song

    fun encode(song: Song): String = runCatching {
        PbJson.json.encodeToString(Song.serializer(), sanitizeForStorage(song))
    }.getOrDefault("")

    fun decode(json: String): Song? =
        if (json.isBlank()) null else runCatching { PbJson.json.decodeFromString(Song.serializer(), json) }.getOrNull()

    fun encodeList(songs: List<Song>): String =
        runCatching {
            PbJson.json.encodeToString(listSerializer, songs.map { sanitizeForStorage(it) })
        }.getOrDefault("[]")

    fun decodeList(json: String): List<Song> =
        if (json.isBlank()) emptyList() else runCatching { PbJson.json.decodeFromString(listSerializer, json) }.getOrDefault(emptyList())
}
