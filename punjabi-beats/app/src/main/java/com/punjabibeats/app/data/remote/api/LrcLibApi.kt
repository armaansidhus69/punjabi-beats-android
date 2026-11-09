package com.punjabibeats.app.data.remote.api

import com.punjabibeats.app.data.remote.dto.LrcLibTrack
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** LRCLIB — free synced (LRC) and plain lyrics database. */
interface LrcLibApi {

    @GET("api/get")
    suspend fun get(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("duration") durationSec: Int? = null,
    ): Response<LrcLibTrack>

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
    ): List<LrcLibTrack>
}
