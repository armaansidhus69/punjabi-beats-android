package com.punjabibeats.app.data.cipher

import com.punjabibeats.app.core.util.runSuspendCatching
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Probes candidate stream URLs before handing them to ExoPlayer, adapting the
 * zemer-cipher validation idea: a usable audio URL must answer byte-range
 * requests with HTTP 206, and a *whole-song* URL must serve bytes BEYOND the
 * free 1-MiB window the CDN grants to unattested/throttled requests.
 */
@Singleton
class StreamValidator @Inject constructor(
    okHttpClient: OkHttpClient,
) {

    enum class Verdict { USABLE, RANGE_ONLY, UNUSABLE }

    private val probeClient: OkHttpClient = okHttpClient.newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    /**
     * Two tiny range probes (1 byte at offset 0, 1 byte just past 1 MiB).
     * Range-unsupported servers answering 200 with the whole body are treated
     * as [Verdict.RANGE_ONLY] — playable, but flagged as not fully validated.
     */
    suspend fun validate(url: String): Verdict = withContext(Dispatchers.IO) {
        val head = probe(url, 0L)
        if (!head.reachable) return@withContext Verdict.UNUSABLE
        val deep = probe(url, WINDOW_BYTES + 1L)
        when {
            deep.reachable && deep.status == 206 -> Verdict.USABLE
            deep.reachable -> Verdict.RANGE_ONLY
            head.status == 206 -> Verdict.RANGE_ONLY
            else -> Verdict.USABLE // server ignores ranges but serves content
        }
    }

    private data class Probe(val reachable: Boolean, val status: Int)

    private suspend fun probe(url: String, offset: Long): Probe {
        val result = runSuspendCatching {
            val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=$offset-${offset + 1}")
                .header("User-Agent", PlayerConfigParser.DEFAULT_USER_AGENT)
                .build()
            probeClient.newCall(request).execute().use { response ->
                val ok = response.isSuccessful && response.code != 416
                Probe(ok, response.code)
            }
        }.getOrElse { Probe(false, -1) }
        return result
    }

    private companion object {
        const val WINDOW_BYTES = 1_048_576L // 1 MiB free window
    }
}
