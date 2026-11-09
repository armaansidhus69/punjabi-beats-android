package com.punjabibeats.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class JamParticipant(
    val userId: String,
    val name: String,
    val emoji: String = "🎧",
    val isHost: Boolean = false,
    val isMe: Boolean = false,
    val lastSeenMs: Long = 0L,
)

@Serializable
data class JamQueueEntry(
    val entryId: String,
    val song: Song,
    val addedByUserId: String,
    val addedByName: String = "",
    val played: Boolean = false,
)

data class JamReaction(
    val id: String,
    val emoji: String,
    val fromName: String,
)

/**
 * Canonical room snapshot shared by host and guests. Host anchor times are in
 * server-clock milliseconds so every client can correct for its own drift using
 * the /api/jam-clock offset.
 */
data class JamRoomSnapshot(
    val code: String,
    val hostId: String,
    val nowPlaying: Song? = null,
    val anchorPositionMs: Long = 0L,
    val anchorServerMs: Long = 0L,
    val isPlaying: Boolean = false,
    val participants: List<JamParticipant> = emptyList(),
    val queue: List<JamQueueEntry> = emptyList(),
)

enum class JamPhase { IDLE, CONNECTING, IN_ROOM, ERROR }

data class JamRoomUi(
    val phase: JamPhase = JamPhase.IDLE,
    val snapshot: JamRoomSnapshot? = null,
    val isHost: Boolean = false,
    val myId: String = "",
    val driftMs: Long = 0L,
    val transport: String = "",
    val error: String? = null,
)

val JAM_REACTIONS = listOf("🔥", "💃", "🎧", "👏", "❤️", "😂")
