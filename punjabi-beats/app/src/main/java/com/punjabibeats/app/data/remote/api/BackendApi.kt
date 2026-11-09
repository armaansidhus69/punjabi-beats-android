package com.punjabibeats.app.data.remote.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * The project's own Vercel backend. Responses are parsed leniently as raw
 * bodies because Gemini/Youtube-resolver payloads vary by deployment; callers
 * check [Response.isSuccessful] themselves.
 */
interface BackendApi {

    @GET("api/health")
    suspend fun health(): Response<ResponseBody>

    @GET("api/yt-stream")
    suspend fun ytStream(@Query("id") videoId: String): Response<ResponseBody>

    @POST("api/ai-playlist")
    suspend fun aiPlaylist(@Body body: RequestBody): Response<ResponseBody>

    @POST("api/ai-assistant")
    suspend fun aiAssistant(@Body body: RequestBody): Response<ResponseBody>

    @GET("api/jam-clock")
    suspend fun jamClock(): Response<ResponseBody>

    @POST("api/publish")
    suspend fun publish(@Body body: RequestBody): Response<ResponseBody>

    @GET("api/publish")
    suspend fun publishedState(@Query("code") code: String): Response<ResponseBody>

    @POST("api/share")
    suspend fun share(@Body body: RequestBody): Response<ResponseBody>
}
