package com.punjabibeats.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LrcLibTrack(
    val id: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
)
