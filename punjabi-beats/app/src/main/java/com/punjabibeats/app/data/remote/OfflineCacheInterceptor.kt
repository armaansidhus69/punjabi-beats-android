package com.punjabibeats.app.data.remote

import com.punjabibeats.app.core.util.NetworkMonitor
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serves cached GET responses when the device is offline and puts a short TTL
 * on catalogue requests while online, so search and home carousels stay snappy.
 */
@Singleton
class OfflineCacheInterceptor @Inject constructor(
    private val networkMonitor: NetworkMonitor,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET") return chain.proceed(request)

        val online = networkMonitor.isOnline.value
        val cacheControl = if (online) {
            okhttp3.CacheControl.Builder().maxAge(2, java.util.concurrent.TimeUnit.MINUTES).build()
        } else {
            okhttp3.CacheControl.Builder().onlyIfCached().maxStale(7, java.util.concurrent.TimeUnit.DAYS).build()
        }

        val modified = request.newBuilder().header("Cache-Control", cacheControl.toString()).build()
        return try {
            chain.proceed(modified)
        } catch (offline: IOException) {
            if (!online) {
                val cacheOnly = request.newBuilder()
                    .header("Cache-Control", "only-if-cached")
                    .build()
                chain.proceed(cacheOnly)
            } else {
                throw offline
            }
        }
    }
}
