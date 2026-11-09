package com.punjabibeats.app.core.util

import com.punjabibeats.app.domain.model.Song
import kotlinx.serialization.builtins.ListSerializer

/** Central (de)serialization point for persisting songs to JSON everywhere. */
object SongCodec {
    private val listSerializer = ListSerializer(Song.serializer())

    fun encode(song: Song): String = runCatching { PbJson.json.encodeToString(Song.serializer(), song) }
        .getOrDefault("")

    fun decode(json: String): Song? = runCatching { PbJson.json.decodeFromString(Song.serializer(), json) }
        .getOrNull()

    fun encodeList(songs: List<Song>): String =
        runCatching { PbJson.json.encodeToString(listSerializer, songs) }.getOrDefault("[]")

    fun decodeList(json: String): List<Song> =
        runCatching { PbJson.json.decodeFromString(listSerializer, json) }.getOrDefault(emptyList())
}
