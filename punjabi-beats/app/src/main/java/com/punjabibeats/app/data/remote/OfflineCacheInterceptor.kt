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
        val existingHeader = request.header("Cache-Control").orEmpty()
        val callerForcesFresh = existingHeader.contains("no-cache", ignoreCase = true) ||
            existingHeader.contains("no-store", ignoreCase = true)

        val cacheControl = if (online) {
            if (callerForcesFresh) {
                okhttp3.CacheControl.FORCE_NETWORK
            } else {
                okhttp3.CacheControl.Builder().maxAge(2, java.util.concurrent.TimeUnit.MINUTES).build()
            }
        } else {
            okhttp3.CacheControl.Builder().onlyIfCached().maxStale(7, java.util.concurrent.TimeUnit.DAYS).build()
        }

        val modified = request.newBuilder().header("Cache-Control", cacheControl.toString()).build()
        return try {
            chain.proceed(modified)
        } catch (offline: IOException) {
            val cacheOnly = request.newBuilder()
                .header("Cache-Control", "public, only-if-cached, max-stale=604800")
                .build()
            val cachedResponse = runCatching { chain.proceed(cacheOnly) }.getOrNull()
            if (cachedResponse != null && (cachedResponse.isSuccessful || cachedResponse.code == 304)) {
                cachedResponse
            } else {
                cachedResponse?.close()
                throw offline
            }
        }
    }
}
