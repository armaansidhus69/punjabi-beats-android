package com.punjabibeats.app.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Stops the service when the user dismisses the playback notification. */
class NotificationDismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        context.stopService(Intent(context, MediaPlaybackService::class.java))
    }
}
