package com.punjabibeats.app.data.remote.api

import com.punjabibeats.app.data.remote.dto.AudiusSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Audius discovery-node API. The node host rotates (see AudiusHostProvider),
 * so every call passes a fully-qualified [url].
 */
interface AudiusApi {

    @GET
    suspend fun searchTracks(
        @Url url: String,
        @Query("query") query: String,
        @Query("app_name") appName: String,
        @Query("limit") limit: Int = 20,
    ): AudiusSearchResponse

    companion object {
        fun streamUrl(host: String, trackId: String, appName: String): String =
            "$host/v1/tracks/$trackId/stream?app_name=$appName"
    }
}
