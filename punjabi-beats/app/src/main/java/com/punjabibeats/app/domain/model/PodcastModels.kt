package com.punjabibeats.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PodcastChannel(
    val id: String,
    val title: String,
    val description: String = "",
    val artworkUrl: String? = null,
    val feedUrl: String,
    val addedAtMs: Long = System.currentTimeMillis(),
)

data class PodcastEpisode(
    val id: String,
    val channelTitle: String,
    val title: String,
    val streamUrl: String,
    val durationSec: Long = 0L,
    val pubDate: String = "",
    val description: String = "",
    val transcriptUrl: String? = null,
)

/** Podcast playback speeds offered in the player speed switcher. */
val PLAYBACK_SPEEDS = listOf(0.5f, 0.8f, 1.0f, 1.25f, 1.5f, 2.0f)
