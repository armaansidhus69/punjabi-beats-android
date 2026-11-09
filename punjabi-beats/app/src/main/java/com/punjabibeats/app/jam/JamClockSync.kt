package com.punjabibeats.app.jam

import com.punjabibeats.app.core.util.PbJson
import com.punjabibeats.app.core.util.firstLong
import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.data.remote.api.BackendApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject

/**
 * NTP-style millisecond clock sync against /api/jam-clock. Multiple samples are
 * taken and the one with the lowest round-trip wins, because it has the least
 * queueing noise. Offset = serverTime - (t0 + rtt/2).
 */
@Singleton
class JamClockSync @Inject constructor(private val backend: BackendApi) {

    data class ClockSample(val offsetMs: Long, val rttMs: Long)

    suspend fun measure(samples: Int = 3): ClockSample {
        var best: ClockSample? = null
        repeat(samples) {
            val sample = measureOnce()
            if (sample != null && (best == null || sample.rttMs < best!!.rttMs)) best = sample
            delay(120)
        }
        return best ?: ClockSample(offsetMs = 0L, rttMs = 0L)
    }

    private suspend fun measureOnce(): ClockSample? = runSuspendCatching {
        val sentAt = System.currentTimeMillis()
        val response = backend.jamClock()
        if (!response.isSuccessful) return@runSuspendCatching null
        val body = response.body()?.string().orEmpty()
        val receivedAt = System.currentTimeMillis()
        val root = PbJson.json.parseToJsonElement(body) as? JsonObject ?: return@runSuspendCatching null
        val serverTimeMs = root.firstLong(
            "serverTimeMs", "serverTime", "time", "now", "clock", "epochMs", "timestamp",
        ) ?: return@runSuspendCatching null
        val rtt = receivedAt - sentAt
        ClockSample(offsetMs = serverTimeMs - (sentAt + rtt / 2), rttMs = rtt)
    }.getOrNull()

    fun serverNow(offsetMs: Long): Long = System.currentTimeMillis() + offsetMs
}
