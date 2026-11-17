package com.punjabibeats.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PunjabiBeatsApp : Application() {

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
