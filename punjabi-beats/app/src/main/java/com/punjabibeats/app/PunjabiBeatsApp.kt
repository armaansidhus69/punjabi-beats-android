package com.punjabibeats.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.punjabibeats.app.data.local.db.LyricsCacheDao
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class PunjabiBeatsApp : Application(), ImageLoaderFactory {

    @Inject lateinit var okHttpClient: Lazy<OkHttpClient>
    @Inject lateinit var lyricsCacheDao: Lazy<LyricsCacheDao>

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        // Protect SQLite queries from CursorWindow 2MB limitation on large playlists
        try {
            val field = android.database.CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
            field.isAccessible = true
            field.set(null, 10 * 1024 * 1024) // 10MB
        } catch (_: Throwable) {}

        // Zen engine: warms the whole playback chain in the background
        // (session visitor, PO-token WebView, cipher) — search and playback
        // then run entirely on-device with no backend API.
        runCatching { com.zenmusic.ZenMusic.initialize(applicationContext) }

        // Periodic cache hygiene: prune lyrics cached older than 30 days so the database does not accumulate unbounded blobs
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L)
                lyricsCacheDao.get().pruneOld(thirtyDaysAgo)
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient { okHttpClient.get() }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024L * 1024L)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_PLAYBACK,
            getString(R.string.notification_channel_playback),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_playback_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_PLAYBACK = "pb_playback"
    }
}
