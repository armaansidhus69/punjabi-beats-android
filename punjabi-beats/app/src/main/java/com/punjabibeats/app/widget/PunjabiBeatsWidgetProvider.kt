package com.punjabibeats.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.punjabibeats.app.MainActivity
import com.punjabibeats.app.R

abstract class BasePunjabiBeatsWidgetProvider(private val isWide: Boolean) : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId, isWide, null, null, false)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PLAY_PAUSE, ACTION_NEXT, ACTION_PREV -> {
                // Forward playback control action to MediaPlaybackService directly
                val serviceIntent = Intent(context, com.punjabibeats.app.playback.MediaPlaybackService::class.java).apply {
                    action = com.punjabibeats.app.playback.MediaPlaybackService.ACTION_PLAYBACK_COMMAND
                    putExtra(com.punjabibeats.app.playback.MediaPlaybackService.EXTRA_COMMAND, intent.action)
                }
                try {
                    context.startService(serviceIntent)
                } catch (_: Exception) {}
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.punjabibeats.app.ACTION_WIDGET_PLAY_PAUSE"
        const val ACTION_NEXT = "com.punjabibeats.app.ACTION_WIDGET_NEXT"
        const val ACTION_PREV = "com.punjabibeats.app.ACTION_WIDGET_PREV"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            isWide: Boolean,
            title: String?,
            artist: String?,
            isPlaying: Boolean,
        ) {
            val layoutId = if (isWide) R.layout.widget_punjabi_beats_wide else R.layout.widget_punjabi_beats_square
            val views = RemoteViews(context.packageName, layoutId)

            val displayTitle = title?.ifBlank { "Punjabi Beats" } ?: "Punjabi Beats"
            val displayArtist = artist?.ifBlank { "Desi Music Experience" } ?: "Desi Music Experience"

            views.setTextViewText(R.id.widget_title, displayTitle)
            views.setTextViewText(R.id.widget_artist, displayArtist)
            views.setImageViewResource(
                R.id.widget_play_pause,
                if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            )

            // Click root to open app
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val appPendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(if (isWide) R.id.widget_root_wide else R.id.widget_root_square, appPendingIntent)

            // Play/Pause intent
            val playPauseIntent = Intent(context, if (isWide) PunjabiBeatsWideWidgetProvider::class.java else PunjabiBeatsSquareWidgetProvider::class.java).apply {
                action = ACTION_PLAY_PAUSE
            }
            val playPausePendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_play_pause, playPausePendingIntent)

            if (isWide) {
                // Next intent
                val nextIntent = Intent(context, PunjabiBeatsWideWidgetProvider::class.java).apply {
                    action = ACTION_NEXT
                }
                val nextPendingIntent = PendingIntent.getBroadcast(
                    context,
                    2,
                    nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widget_next, nextPendingIntent)

                // Previous intent
                val prevIntent = Intent(context, PunjabiBeatsWideWidgetProvider::class.java).apply {
                    action = ACTION_PREV
                }
                val prevPendingIntent = PendingIntent.getBroadcast(
                    context,
                    3,
                    prevIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widget_prev, prevPendingIntent)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        fun updateAllWidgets(
            context: Context,
            title: String?,
            artist: String?,
            isPlaying: Boolean,
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return

            val squareIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, PunjabiBeatsSquareWidgetProvider::class.java)
            )
            for (id in squareIds) {
                updateWidget(context, appWidgetManager, id, false, title, artist, isPlaying)
            }

            val wideIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, PunjabiBeatsWideWidgetProvider::class.java)
            )
            for (id in wideIds) {
                updateWidget(context, appWidgetManager, id, true, title, artist, isPlaying)
            }
        }
    }
}

class PunjabiBeatsSquareWidgetProvider : BasePunjabiBeatsWidgetProvider(isWide = false)

class PunjabiBeatsWideWidgetProvider : BasePunjabiBeatsWidgetProvider(isWide = true)
