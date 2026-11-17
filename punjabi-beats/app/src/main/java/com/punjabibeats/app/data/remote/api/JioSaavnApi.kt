package com.punjabibeats.app.data.remote.api

import com.punjabibeats.app.data.remote.dto.JioSaavnLyricsResponse
import com.punjabibeats.app.data.remote.dto.JioSaavnSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Unofficial JioSaavn API (saavn.dev) — primary catalogue and 320kbps source. */
interface JioSaavnApi {

    @GET("api/search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("limit") limit: Int = 20,
        @Query("page") page: Int = 0,
    ): JioSaavnSearchResponse

    @GET("api/search/albums")
    suspend fun searchAlbums(
        @Query("query") query: String,
        @Query("limit") limit: Int = 12,
        @Query("page") page: Int = 0,
    ): JioSaavnSearchResponse

    @GET("api/search/artists")
    suspend fun searchArtists(
        @Query("query") query: String,
        @Query("limit") limit: Int = 12,
        @Query("page") page: Int = 0,
    ): JioSaavnSearchResponse

    @GET("api/songs/{id}/lyrics")
    suspend fun getLyrics(
        @Path("id") id: String,
    ): JioSaavnLyricsResponse
}
