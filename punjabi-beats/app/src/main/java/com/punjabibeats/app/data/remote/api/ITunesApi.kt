package com.punjabibeats.app.data.remote.api

import com.punjabibeats.app.data.remote.dto.ITunesSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

/** iTunes Search API — 30-second previews as the final fallback resolver. */
interface ITunesApi {

    @GET("search")
    suspend fun search(
        @Query("term") term: String,
        @Query("media") media: String = "music",
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 15,
    ): ITunesSearchResponse
}
