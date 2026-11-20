package com.punjabibeats.app.ui.screens.replay

data class SongHistoryItem(
    val id: String,
    val title: String,
    val artist: String,
    val genre: String = "Punjabi Beats",
    val durationListenedMs: Long = 180_000L,
    val artworkUrl: String? = null,
)

data class ArtistStat(
    val artist: String,
    val playCount: Int,
    val totalMinutes: Long,
)

data class SongStat(
    val title: String,
    val artist: String,
    val playCount: Int,
    val artworkUrl: String? = null,
)

data class GenreStat(
    val genre: String,
    val percentage: Int,
)

data class ReplaySummary(
    val year: Int = 2026,
    val totalMinutes: Long,
    val topArtists: List<ArtistStat>,
    val topSongs: List<SongStat>,
    val topGenres: List<GenreStat>,
    val personalityBadge: String,
    val personalityTitle: String,
    val personalityDescription: String,
)

fun calculateReplaySummary(history: List<SongHistoryItem>, year: Int = 2026): ReplaySummary {
    val totalMs = history.sumOf { it.durationListenedMs }
    val totalMinutes = totalMs / (1000 * 60)

    val topArtists = history.groupBy { it.artist }
        .map { (artist, songs) ->
            ArtistStat(
                artist = artist,
                playCount = songs.size,
                totalMinutes = songs.sumOf { it.durationListenedMs } / (1000 * 60),
            )
        }
        .sortedByDescending { it.playCount }

    val topSongs = history.groupBy { it.id }
        .map { (_, songs) ->
            val first = songs.first()
            SongStat(
                title = first.title,
                artist = first.artist,
                playCount = songs.size,
                artworkUrl = first.artworkUrl,
            )
        }
        .sortedByDescending { it.playCount }

    val totalGenres = history.size.coerceAtLeast(1)
    val topGenres = history.groupBy { it.genre }
        .map { (genre, songs) ->
            GenreStat(
                genre = genre,
                percentage = ((songs.size.toFloat() / totalGenres) * 100).toInt(),
            )
        }
        .sortedByDescending { it.percentage }

    val (badge, title, desc) = when {
        topArtists.firstOrNull()?.artist?.contains("Moose", ignoreCase = true) == true ->
            Triple("👑", "Royal Sidhu Disciple", "You lived and breathed the legendary rhythms and high-octane 295 anthems.")
        topArtists.firstOrNull()?.artist?.contains("Diljit", ignoreCase = true) == true ->
            Triple("✨", "Global Dil-Luminati Explorer", "From Lover to Coachella, you brought Punjabi music to every corner of your world.")
        else ->
            Triple("🥁", "Midnight Desi Beat Pioneer", "Your heart beats in synced dhol drops and vibrant Punjabi energy.")
    }

    return ReplaySummary(
        year = year,
        totalMinutes = totalMinutes,
        topArtists = topArtists,
        topSongs = topSongs,
        topGenres = topGenres,
        personalityBadge = badge,
        personalityTitle = title,
        personalityDescription = desc,
    )
}
