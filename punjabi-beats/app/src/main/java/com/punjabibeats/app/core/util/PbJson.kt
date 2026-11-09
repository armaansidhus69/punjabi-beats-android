package com.punjabibeats.app.core.util

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Single shared Json configuration. Lenient on purpose: the app talks to several
 * public APIs (Vercel backend, JioSaavn, Audius, iTunes, Piped, LRCLIB) whose
 * response shapes drift over time, so unknown keys must never crash parsing.
 */
object PbJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
        classDiscriminator = "type"
    }

    fun requestBody(payload: String): RequestBody =
        payload.toRequestBody("application/json".toMediaType())
}
