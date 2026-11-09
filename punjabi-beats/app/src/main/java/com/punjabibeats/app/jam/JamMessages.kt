package com.punjabibeats.app.jam

import com.punjabibeats.app.domain.model.JamParticipant
import com.punjabibeats.app.domain.model.JamQueueEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire protocol for Jam Rooms. All messages are JSON with a "type"
 * discriminator; the host is authoritative and broadcasts full state
 * snapshots, while guests emit lightweight intents (join, reaction, queue ops).
 */
@Serializable
sealed class JamMessage {

    @Serializable
    @SerialName("join")
    data class Join(
        val code: String,
        val userId: String,
        val name: String,
        val emoji: String = "🎧",
    ) : JamMessage()

    @Serializable
    @SerialName("state")
    data class HostState(
        val code: String,
        val hostId: String,
        val songJson: String? = null,
        val anchorPositionMs: Long = 0L,
        val anchorServerMs: Long = 0L,
        val isPlaying: Boolean = false,
        val participants: List<JamParticipant> = emptyList(),
        val queue: List<JamQueueEntry> = emptyList(),
    ) : JamMessage()

    @Serializable
    @SerialName("reaction")
    data class Reaction(
        val code: String,
        val userId: String,
        val name: String = "",
        val emoji: String = "🔥",
    ) : JamMessage()

    @Serializable
    @SerialName("queue_add")
    data class QueueAdd(
        val code: String,
        val entry: JamQueueEntry,
    ) : JamMessage()

    @Serializable
    @SerialName("queue_remove")
    data class QueueRemove(
        val code: String,
        val entryId: String,
        val userId: String,
    ) : JamMessage()

    @Serializable
    @SerialName("leave")
    data class Leave(
        val code: String,
        val userId: String,
    ) : JamMessage()

    @Serializable
    @SerialName("ping")
    data class Ping(
        val code: String,
        val userId: String,
    ) : JamMessage()
}
