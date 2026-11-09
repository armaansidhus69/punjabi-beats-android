package com.punjabibeats.app.jam

import android.content.Context
import com.punjabibeats.app.data.local.datastore.SettingsDataStore
import com.punjabibeats.app.domain.model.JamParticipant
import com.punjabibeats.app.playback.MusicPlayerController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Builds a fresh [JamEngine] per joined room with the user's persisted identity. */
@Singleton
class JamEngineFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clockSync: JamClockSync,
    private val webSocketFactory: WebSocketTransport.Factory,
    private val pollingFactory: PollingTransport.Factory,
    private val playerController: MusicPlayerController,
    private val settings: SettingsDataStore,
) {

    suspend fun create(code: String): JamEngine {
        val userId = settings.ensureUserId()
        val name = settings.userName().first()
        val emoji = settings.userEmoji().first()
        return JamEngine(
            code = code.uppercase(),
            me = JamParticipant(userId = userId, name = name, emoji = emoji),
            clockSync = clockSync,
            webSocketFactory = webSocketFactory,
            pollingFactory = pollingFactory,
            playerController = playerController,
        )
    }

    suspend fun myIdentity(): JamParticipant {
        val userId = settings.ensureUserId()
        val name = settings.userName().first()
        val emoji = settings.userEmoji().first()
        return JamParticipant(userId = userId, name = name, emoji = emoji)
    }
}
