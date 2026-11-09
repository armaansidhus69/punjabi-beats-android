package com.punjabibeats.app.data.remote

import com.punjabibeats.app.core.util.PbJson
import com.punjabibeats.app.core.util.firstArray
import com.punjabibeats.app.core.util.runSuspendCatching
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audius requires choosing a discovery node at runtime. The official
 * https://api.audius.co endpoint returns the live host list; we pick one and
 * refresh lazily on failures.
 */
@Singleton
class AudiusHostProvider @Inject constructor(private val okHttpClient: OkHttpClient) {

    @Volatile
    var host: String = DEFAULT_HOST
        private set

    suspend fun refreshHost() {
        val result = runSuspendCatching {
            val request = okhttp3.Request.Builder()
                .url("https://api.audius.co")
                .header("User-Agent", "PunjabiBeats/1.0")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful || body.isBlank()) return@use
                val hosts = PbJson.json.parseToJsonElement(body)
                    .let { if (it is JsonObject) it.firstArray("data") else JsonArray(emptyList()) }
                    .mapNotNull { (it as? JsonPrimitive)?.content }
                if (hosts.isNotEmpty()) host = hosts.random()
            }
        }
        if (result.isFailure) host = DEFAULT_HOST
    }

    fun markFailure() {
        host = DEFAULT_HOST
    }

    private companion object {
        const val DEFAULT_HOST = "https://discoveryprovider.audius.co"
    }
}

/**
 * Public Piped instances used as the YouTube Music search bridge. They rotate
 * frequently, so we keep several candidates and advance on failure.
 */
@Singleton
class PipedHostProvider @Inject constructor() {

    private val candidates = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://api.piped.private.coffee",
        "https://pipedapi.reallyaweso.me",
    )

    @Volatile
    private var index = 0

    val currentHost: String get() = candidates[index % candidates.size]

    fun markFailure() {
        index = (index + 1) % candidates.size
    }
}
