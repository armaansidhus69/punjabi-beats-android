package com.punjabibeats.app.data.remote.dto

import com.punjabibeats.app.domain.model.Song
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class MediaUrl(val quality: String, val url: String)

/**
 * JioSaavn returns media (images / download urls) either as an array of
 * {quality, link|url} objects, a bare string, or — on some endpoints — an
 * object. This wrapper normalises all three shapes.
 */
@Serializable(with = MediaUrlsSerializer::class)
data class MediaUrls(val values: List<MediaUrl>) {

    fun best(): MediaUrl? = values.filter { it.url.isNotBlank() }.maxByOrNull { rank(it.quality) }

    fun byQuality(quality: String): MediaUrl? =
        values.firstOrNull { it.quality.equals(quality, ignoreCase = true) && it.url.isNotBlank() }

    /** First url whose quality label contains [prefix], e.g. "500" for "500x500". */
    fun byQualityPrefix(prefix: String): MediaUrl? =
        values.firstOrNull { it.quality.contains(prefix) && it.url.isNotBlank() }

    private fun rank(quality: String): Int = quality.filter { it.isDigit() }.toIntOrNull() ?: 0
}

object MediaUrlsSerializer : KSerializer<MediaUrls> {
    override val descriptor: SerialDescriptor = ListSerializer(String.serializer()).descriptor

    override fun deserialize(decoder: Decoder): MediaUrls {
        val element = decoder.decodeSerializableValue(JsonElement.serializer())
        val urls = when (element) {
            is JsonArray -> element.mapNotNull { child -> child.toMediaUrl() }
            is JsonObject -> listOfNotNull(element.toMediaUrl())
            is JsonPrimitive -> if (element is JsonNull) emptyList() else listOf(MediaUrl("", element.content))
            else -> emptyList()
        }
        return MediaUrls(urls)
    }

    override fun serialize(encoder: Encoder, value: MediaUrls) {
        encoder.encodeSerializableValue(
            JsonElement.serializer(),
            buildJsonArray {
                value.values.forEach { url ->
                    add(buildJsonObject {
                        put("quality", url.quality)
                        put("link", url.url)
                    })
                }
            }
        )
    }

    private fun JsonElement.toMediaUrl(): MediaUrl? = when (this) {
        is JsonObject -> MediaUrl(
            quality = (this["quality"] as? JsonPrimitive)?.contentOrNull ?: "",
            url = (this["link"] as? JsonPrimitive)?.contentOrNull
                ?: (this["url"] as? JsonPrimitive)?.contentOrNull
                ?: ""
        )
        is JsonPrimitive -> if (this is JsonNull) null else MediaUrl("", content)
        else -> null
    }
}

/** JioSaavn artist/album reference: {"id": "...", "name": "..."} */
@Serializable
data class JioSaavnRef(
    val id: String? = null,
    val name: String? = null,
)

@Serializable
data class JioSaavnSong(
    val id: String? = null,
    val name: String? = null,
    val title: String? = null,
    val primaryArtists: String? = null,
    val featuredArtists: String? = null,
    val singers: String? = null,
    val album: JioSaavnRef? = null,
    val year: String? = null,
    val duration: String? = null,
    val image: MediaUrls? = null,
    @SerialName("downloadUrl") val downloadUrls: MediaUrls? = null,
    val type: String? = null,
) {
    fun toSong(): Song? {
        val name = name ?: title ?: return null
        return Song(
            id = "jio_${id ?: name.hashCode()}",
            title = name,
            artist = primaryArtists ?: featuredArtists ?: singers ?: "Unknown Artist",
            albumName = album?.name ?: "",
            artworkUrl = image?.byQualityPrefix("500")?.url ?: image?.best()?.url,
            durationMs = (duration?.toLongOrNull() ?: 0L) * 1000L,
            source = com.punjabibeats.app.domain.model.MusicSource.JIOSAAVN,
            streamUrl = downloadUrls?.byQuality("320kbps")?.url ?: downloadUrls?.best()?.url,
            year = year?.toIntOrNull(),
        )
    }
}

@Serializable
data class JioSaavnSearchData(
    val total: Int? = null,
    val start: Int? = null,
    val results: List<JioSaavnSong> = emptyList(),
)

@Serializable
data class JioSaavnSearchResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val data: JioSaavnSearchData? = null,
)

@Serializable
data class JioSaavnLyricsData(
    val lyrics: String? = null,
    val snippet: String? = null,
    val copyright: String? = null,
)

@Serializable
data class JioSaavnLyricsResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val data: JioSaavnLyricsData? = null,
)
