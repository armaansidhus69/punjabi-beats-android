package com.punjabibeats.app.domain.model

import kotlinx.serialization.Serializable

enum class AiRole { USER, ASSISTANT, SYSTEM }

data class AiMessage(
    val role: AiRole,
    val content: String,
    val timestampMs: Long = System.currentTimeMillis(),
)

data class AiPlaylist(
    val prompt: String,
    val title: String,
    val description: String = "",
    val songs: List<Song> = emptyList(),
)
