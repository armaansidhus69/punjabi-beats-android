package com.punjabibeats.app.data.remote.api

import com.punjabibeats.app.data.remote.dto.PipedSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Piped API instances act as the YouTube Music bridge for search results.
 * Instances rotate on failure (see PipedHostProvider); the base call uses
 * a fully-qualified [url].
 */
interface PipedApi {

    @GET
    suspend fun search(
        @Url url: String,
        @Query("q") query: String,
        @Query("filter") filter: String = "music_songs",
    ): PipedSearchResponse
}
