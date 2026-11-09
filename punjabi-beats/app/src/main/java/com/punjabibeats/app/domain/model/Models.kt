package com.punjabibeats.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MusicSource {
    JIOSAAVN, YOUTUBE, AUDIUS, ITUNES, LOCAL
}

/** A playable track. [streamUrl] is filled by the resolver chain before playback. */
@Serializable
data class Song(
    val id: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val albumName: String = "",
    val artworkUrl: String? = null,
    val durationMs: Long = 0L,
    val source: MusicSource = MusicSource.JIOSAAVN,
    val videoId: String? = null,
    val streamUrl: String? = null,
    val year: Int? = null,
    val language: String = "Punjabi",
)

@Serializable
data class ArtistCard(
    val id: String,
    val name: String,
    val artworkUrl: String? = null,
)

@Serializable
data class AlbumCard(
    val id: String,
    val name: String,
    val artist: String = "",
    val artworkUrl: String? = null,
    val year: Int? = null,
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val songCount: Int = 0,
)

data class MoodCategory(
    val id: String,
    val title: String,
    val emoji: String,
    val query: String,
)

data class HomeFeed(
    val heroSongs: List<Song> = emptyList(),
    val curatedArtists: List<ArtistCard> = emptyList(),
    val trendingSongs: List<Song> = emptyList(),
    val quickPicks: List<Song> = emptyList(),
)

data class SearchResults(
    val songs: List<Song> = emptyList(),
    val artists: List<ArtistCard> = emptyList(),
    val albums: List<AlbumCard> = emptyList(),
    val podcasts: List<PodcastChannel> = emptyList(),
) {
    val isEmpty: Boolean get() = songs.isEmpty() && artists.isEmpty() && albums.isEmpty() && podcasts.isEmpty()
}

/** Raised by [com.punjabibeats.app.data.resolver.StreamResolver] when the whole chain fails. */
class StreamResolutionException(songId: String) :
    Exception("All resolvers failed for song $songId")

/** Real Punjabi artists for the curated carousel (names come straight from the product spec). */
val CURATED_ARTISTS = listOf(
    "Sidhu Moose Wala",
    "Karan Aujla",
    "Diljit Dosanjh",
    "AP Dhillon",
    "Shubh",
    "Guru Randhawa",
    "Jassie Gill",
    "Amrit Maan",
)

val MOOD_CATEGORIES = listOf(
    MoodCategory("bhangra", "Bhangra", "💃", "punjabi bhangra hits"),
    MoodCategory("melodic", "Melodic", "🌙", "punjabi melodic songs"),
    MoodCategory("gym", "Gym / Workout", "🏋️", "punjabi workout motivation songs"),
    MoodCategory("sad", "Sad / Heartbreak", "💔", "punjabi sad songs"),
    MoodCategory("bass", "Car Bass", "🔊", "punjabi bass boosted songs"),
)

val TRENDING_SEARCHES = listOf(
    "Sidhu Moose Wala", "Karan Aujla", "Diljit Dosanjh", "AP Dhillon",
    "Shubh", "Guru Randhawa", "Punjabi Bhangra 2025", "Punjabi Love Songs",
)
