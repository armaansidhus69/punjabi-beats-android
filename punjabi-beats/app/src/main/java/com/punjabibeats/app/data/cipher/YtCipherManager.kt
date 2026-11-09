package com.punjabibeats.app.data.cipher

import android.net.Uri
import com.punjabibeats.app.core.util.firstInt
import com.punjabibeats.app.core.util.firstLong
import com.punjabibeats.app.core.util.firstObject
import com.punjabibeats.app.core.util.firstString
import com.punjabibeats.app.core.util.objectArray
import com.punjabibeats.app.core.util.runSuspendCatching
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Facade over the zemer-cipher-style cipher pipeline:
 *
 * 1. [signatureTimestamp] — the value WEB-family clients echo in player calls
 *    (self-healed via [PlayerConfigStore]).
 * 2. [decipherSignatureCipher] — turns `signatureCipher` (`s`+`sp`+`url`) into
 *    a plain CDN URL by executing the live player JS operations.
 * 3. [prepareStreamUrl] — applies the n-parameter transform when the URL class
 *    demands it and verifies with [StreamValidator] that the URL is actually
 *    streamable past the 1-MiB free window.
 *
 * Everything degrades gracefully: any failure returns null and the resolver
 * chain simply tries the next source.
 */
@Singleton
class YtCipherManager @Inject constructor(
    private val configStore: PlayerConfigStore,
    private val deobfuscator: CipherDeobfuscator,
    private val validator: StreamValidator,
    private val poTokenProvider: PoTokenProvider,
    okHttpClient: OkHttpClient,
) {

    private val downloadClient: OkHttpClient = okHttpClient.newBuilder()
        .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val playerJsMutex = Mutex()
    private val playerJsCache = HashMap<String, String?>()

    suspend fun signatureTimestamp(): Int? =
        runSuspendCatching { configStore.config().latestPlayer()?.signatureTimestamp }.getOrNull()

    suspend fun clientSpec(id: String): InnerTubeClientSpec? =
        runSuspendCatching { configStore.config().client(id) }.getOrNull()

    suspend fun poToken(): String? = poTokenProvider.poToken()

    /**
     * Deciphers a `signatureCipher` parameter string into a direct CDN URL.
     * Returns null when the player script or its operations cannot be used.
     */
    suspend fun decipherSignatureCipher(signatureCipher: String): String? {
        val s = queryParam(signatureCipher, "s") ?: return null
        val sp = queryParam(signatureCipher, "sp") ?: "sig"
        val base = queryParam(signatureCipher, "url") ?: return null
        val playerJs = currentPlayerJs() ?: return null
        val signature = deobfuscator.decipherSignature(playerJs, s) ?: return null
        return "$base&$sp=$signature"
    }

    /**
     * Full post-processing of a candidate stream URL: n-transform (when the
     * active player spec lists this URL class), then a live validation probe.
     * Returns the URL when usable, else null.
     */
    suspend fun prepareStreamUrl(url: String): String? {
        val transformed = transformNIfNeeded(url)
        val verdict = validator.validate(transformed)
        return when (verdict) {
            StreamValidator.Verdict.USABLE -> transformed
            StreamValidator.Verdict.RANGE_ONLY -> transformed // playable; n-throttle not confirmed
            StreamValidator.Verdict.UNUSABLE -> null
        }
    }

    /** Picks the best audio-only adaptive format from a player response. */
    suspend fun bestAudioStreamUrl(playerResponse: JsonObject): String? {
        val formats = playerResponse
            .firstObject("streamingData")
            ?.objectArray("adaptiveFormats")
            .orEmpty()
        if (formats.isEmpty()) return null

        data class Candidate(val url: String?, val bitrate: Long, val needsDecipher: Boolean)

        val candidates = formats.mapNotNull { format ->
            val mime = format.firstString("mimeType") ?: return@mapNotNull null
            if (!mime.startsWith("audio")) return@mapNotNull null
            val bitrate = format.firstLong("bitrate", "averageBitrate") ?: 0L
            val direct = format.firstString("url")
            if (!direct.isNullOrBlank()) {
                Candidate(direct, bitrate, needsDecipher = false)
            } else {
                val cipher = format.firstString("signatureCipher", "cipher") ?: return@mapNotNull null
                Candidate(cipher, bitrate, needsDecipher = true)
            }
        }
        val deciphered = candidates.filter { it.needsDecipher }
            .maxByOrNull { it.bitrate }
            ?.let { candidate -> decipherSignatureCipher(candidate.url ?: return null) }
        val direct = candidates.filter { !it.needsDecipher }
            .maxByOrNull { it.bitrate }?.url
        return listOfNotNull(deciphered, direct).firstNotNullOfOrNull { candidate ->
            prepareStreamUrl(candidate)
        }
    }

    private suspend fun transformNIfNeeded(url: String): String {
        val urlClass = urlClasses().firstOrNull { classRegex ->
            runCatching { Regex(classRegex).containsMatchIn(url) }.getOrDefault(false)
        } ?: return url
        val n = queryParam(url, "n") ?: return url
        val playerJs = currentPlayerJs() ?: return url
        val transformed = deobfuscator.transformNParam(playerJs, n) ?: return url
        return runCatching {
            HttpUrlRewriter.setParameter(url, "n", transformed)
        }.getOrDefault(url)
    }

    private suspend fun urlClasses(): List<String> =
        runSuspendCatching { configStore.config().latestPlayer()?.nTransformUrlClasses }
            .getOrNull().orEmpty()

    private suspend fun currentPlayerJs(): String? = playerJsMutex.withLock {
        val playerSpec = runSuspendCatching { configStore.config().latestPlayer() }.getOrNull()
            ?: return@withLock null
        if (playerJsCache.containsKey(playerSpec.playerId)) return@withLock playerJsCache[playerSpec.playerId]
        val js = fetchPlayerJs(playerSpec.playerJsUrl)
        playerJsCache[playerSpec.playerId] = js
        js
    }

    private suspend fun fetchPlayerJs(url: String): String? = withContext(Dispatchers.IO) {
        runSuspendCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", PlayerConfigParser.DEFAULT_USER_AGENT)
                .build()
            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string()
                if (body.isNullOrEmpty() || body.length > MAX_PLAYER_JS_BYTES) null else body
            }
        }.getOrNull()
    }

    private fun queryParam(raw: String, key: String): String? =
        runCatching {
            val parseable = if (raw.contains("?")) raw else "?$raw"
            Uri.parse(parseable).getQueryParameter(key)
        }.getOrNull()?.takeIf { it.isNotBlank() }

    private companion object {
        const val MAX_PLAYER_JS_BYTES = 6 * 1024 * 1024L
    }
}

/** Small URL helper kept separate so the logic is easy to reason about. */
object HttpUrlRewriter {
    fun setParameter(url: String, key: String, value: String): String {
        val uri = Uri.parse(url)
        val builder = uri.buildUpon().clearQuery()
        for (name in uri.queryParameterNames) {
            if (name != key) {
                uri.getQueryParameters(name).firstOrNull()?.let { builder.appendQueryParameter(name, it) }
            }
        }
        builder.appendQueryParameter(key, value)
        return builder.build().toString()
    }
}
