package com.punjabibeats.app.innertube

import com.punjabibeats.app.core.util.PbJson
import com.punjabibeats.app.core.util.firstArray
import com.punjabibeats.app.core.util.firstObject
import com.punjabibeats.app.core.util.firstString
import com.punjabibeats.app.core.util.objectArray
import com.punjabibeats.app.core.util.parseTimestampToMs
import com.punjabibeats.app.data.cipher.InnerTubeClientSpec
import com.punjabibeats.app.data.cipher.PlayerConfigParser
import com.punjabibeats.app.data.cipher.PlayerConfigStore
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** One YouTube Music search hit (song-shaped). */
data class YtmSearchHit(
    val videoId: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val thumbnailUrl: String?,
)

/**
 * Minimal InnerTube client for music.youtube.com, following the Metrolist
 * project's `innertube` module pattern (a dedicated YouTube API layer between
 * the repositories and the resolver chain) with an original, deliberately
 * lenient implementation.
 *
 * Client descriptors and the playback signatureTimestamp come from the
 * self-healing cipher configuration ([PlayerConfigStore]); the HTTP layer is
 * plain OkHttp because requests share one endpoint shape with a JSON body.
 */
@Singleton
class InnerTubeClient @Inject constructor(
    private val configStore: PlayerConfigStore,
    okHttpClient: OkHttpClient,
) {

    private val jsonMediaType = "application/json".toMediaType()

    private val httpClient: OkHttpClient = okHttpClient.newBuilder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var cachedSpec: InnerTubeClientSpec? = null
    private val specMutex = Mutex()

    /** Player request: returns the raw `streamingData`-bearing response, or null. */
    suspend fun player(videoId: String): JsonObject? {
        val spec = currentSpec() ?: return null
        val body = buildJsonObject {
            put("context", contextJson(spec))
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
            put("playbackContext", buildJsonObject {
                put("contentPlaybackContext", buildJsonObject {
                    configStore.config().latestPlayer()?.signatureTimestamp?.let {
                        put("signatureTimestamp", it)
                    }
                    put("html5Playback", true)
                })
            })
            configStore.config().poToken.takeIf { it.isNotBlank() }?.let {
                put("serviceIntegrityDimensions", buildJsonObject { put("poToken", it) })
            }
        }
        val response = post("player", spec, body) ?: return null
        val status = response.firstObject("playabilityStatus")?.firstString("status")
        if (status != null && status != "OK") return null
        return response
    }

    /** YouTube Music search restricted to the "songs" shelf. */
    suspend fun searchSongs(query: String, limit: Int = 20): List<YtmSearchHit> {
        val spec = currentSpec() ?: return emptyList()
        val body = buildJsonObject {
            put("context", contextJson(spec))
            put("query", query)
            put("params", SONGS_FILTER_PARAMS)
        }
        val response = post("search", spec, body) ?: return emptyList()
        val shelves = mutableListOf<JsonObject>()
        val tabs = response
            .firstObject("contents")
            ?.firstObject("tabbedSearchResultsRenderer")
            ?.firstArray("tabs")
            .orEmpty()
        for (tab in tabs) {
            val content = (tab as? JsonObject)?.firstObject("tabRenderer")?.firstObject("content")
                ?: continue
            collectShelves(content, shelves)
        }
        val hits = mutableListOf<YtmSearchHit>()
        for (shelf in shelves) {
            for (item in shelf.objectArray("contents")) {
                val renderer = item.firstObject("musicResponsiveListItemRenderer") ?: continue
                parseHit(renderer)?.let { hits += it }
                if (hits.size >= limit) return hits
            }
        }
        return hits
    }

    // ------------------------------------------------------------------ internals

    private suspend fun post(endpoint: String, spec: InnerTubeClientSpec, body: JsonObject): JsonObject? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url("$MUSIC_ORIGIN/youtubei/v1/$endpoint?prettyPrint=false")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", spec.userAgent)
                    .header("X-YouTube-Client-Name", spec.clientField.toString())
                    .header("X-YouTube-Client-Version", spec.clientVersion)
                    .header("X-Origin", spec.origin)
                    .header("Referer", spec.referer)
                    .header("X-Goog-Api-Format-Version", "2")
                    .post(body.toString().toRequestBody(jsonMediaType))
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val raw = response.body?.string().orEmpty()
                    if (raw.isBlank() || raw.length > MAX_RESPONSE_BYTES) return@use null
                    PbJson.json.parseToJsonElement(raw) as? JsonObject
                }
            }.getOrNull()
        }

    private fun contextJson(spec: InnerTubeClientSpec): JsonObject = buildJsonObject {
        put("client", buildJsonObject {
            put("clientName", spec.clientName)
            put("clientVersion", spec.clientVersion)
            put("hl", "en")
            put("gl", "IN")
        })
        put("request", buildJsonObject {
            put("useSSL", true)
        })
    }

    /** Search results nest arbitrarily deep (shelves inside shelves); walk recursively. */
    private fun collectShelves(node: JsonObject, out: MutableList<JsonObject>, depth: Int = 0) {
        if (depth > 8) return
        node.firstObject("musicShelfRenderer")?.let { out += it }
        for (key in listOf("contents", "tabs", "sectionListRenderer", "itemSectionRenderer")) {
            node.firstArray(key).forEach { element ->
                (element as? JsonObject)?.let { collectShelves(it, out, depth + 1) }
            }
        }
        node.firstObject("sectionListRenderer")?.let { collectShelves(it, out, depth + 1) }
        node.firstObject("content")?.let { collectShelves(it, out, depth + 1) }
    }

    private fun parseHit(renderer: JsonObject): YtmSearchHit? {
        val videoId = renderer.firstObject("playlistItemData")?.firstString("videoId")
            ?: renderer.firstObject("overlay")
                ?.firstObject("musicItemThumbnailOverlayRenderer")
                ?.firstObject("content")
                ?.firstObject("musicPlayButtonRenderer")
                ?.firstObject("playNavigationEndpoint")
                ?.firstObject("watchEndpoint")
                ?.firstString("videoId")
            ?: return null
        if (videoId.isBlank()) return null
        val columns = renderer.firstArray("flexColumns")
            .mapNotNull { (it as? JsonObject)?.firstObject("musicResponsiveListItemFlexColumnRenderer") }
        val title = columns.getOrNull(0)
            ?.firstObject("text")
            ?.runText() ?: return null
        if (title.isBlank()) return null
        val subtitle = columns.getOrNull(1)?.firstObject("text")?.runText().orEmpty()
        val durationMs = subtitle.split("•")
            .mapNotNull { part -> parseTimestampToMs(part.trim()) }
            .firstOrNull() ?: 0L
        val artist = subtitle.split("•").firstOrNull()?.trim().orEmpty().ifBlank { "YouTube Music" }
        val thumbnail = renderer.firstObject("thumbnail")
            ?.firstObject("musicThumbnailRenderer")
            ?.firstObject("thumbnail")
            ?.firstArray("thumbnails")
            ?.lastOrNull()
            ?.let { (it as? JsonObject)?.firstString("url") }
        return YtmSearchHit(
            videoId = videoId,
            title = title,
            artist = artist,
            durationMs = durationMs,
            thumbnailUrl = thumbnail ?: "https://i.ytimg.com/vi/$videoId/mqdefault.jpg",
        )
    }

    private fun JsonObject.runText(): String =
        firstArray("runs").joinToString("") { run ->
            (run as? JsonObject)?.firstString("text").orEmpty()
        }.ifBlank { firstString("simpleText").orEmpty() }

    private suspend fun currentSpec(): InnerTubeClientSpec = specMutex.withLock {
        cachedSpec?.let { return it }
        val spec = runCatching { configStore.config().client("WEB_REMIX") }.getOrNull()
            ?: CipherSpecFallback.spec
        cachedSpec = spec
        spec
    }

    private object CipherSpecFallback {
        // Never used unless the asset config is unreadable; keeps the client alive.
        val spec = InnerTubeClientSpec(
            id = "WEB_REMIX",
            clientName = "WEB_REMIX",
            clientVersion = "1.20240403.01.00",
            clientField = 67,
            userAgent = PlayerConfigParser.DEFAULT_USER_AGENT,
            origin = "https://music.youtube.com",
            referer = "https://music.youtube.com/",
        )
    }

    private companion object {
        const val MUSIC_ORIGIN = "https://music.youtube.com"
        const val MAX_RESPONSE_BYTES = 8 * 1024 * 1024L

        /**
         * YouTube Music "songs" filter (the token documented by Invidious and
         * other public InnerTube clients). Healed remotely via clientVersion
         * rotations even if YouTube changes response shapes — parsing stays
         * lenient and Piped remains the fallback bridge.
         */
        const val SONGS_FILTER_PARAMS = "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"
    }
}
