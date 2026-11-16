package com.punjabibeats.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.punjabibeats.app.core.util.UiState
import com.punjabibeats.app.domain.model.HomeFeed
import com.punjabibeats.app.domain.model.MOOD_CATEGORIES
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.ui.components.ArtistCard
import com.punjabibeats.app.ui.components.ErrorState
import com.punjabibeats.app.ui.components.MoodTile
import com.punjabibeats.app.ui.components.PbLoading
import com.punjabibeats.app.ui.components.SectionHeader
import com.punjabibeats.app.ui.components.SongCard
import com.punjabibeats.app.ui.theme.LocalPBTheme
import kotlinx.coroutines.delay

private val CURATED_ARTISTS_FALLBACK = listOf(
    Triple("Sidhu Moose Wala", "👳", "https://c.saavncdn.com/artists/Sidhu_Moose_Wala_004_20230610084042_500x500.jpg"),
    Triple("Karan Aujla", "🕶️", "https://c.saavncdn.com/artists/Karan_Aujla_002_20230818105742_500x500.jpg"),
    Triple("Diljit Dosanjh", "🎤", "https://c.saavncdn.com/artists/Diljit_Dosanjh_004_20221007180447_500x500.jpg"),
    Triple("AP Dhillon", "🎧", "https://c.saavncdn.com/artists/AP_Dhillon_000_20220914104245_500x500.jpg"),
    Triple("Shubh", "👑", "https://c.saavncdn.com/artists/Shubh_000_20230519124446_500x500.jpg"),
)

@Composable
fun HomeScreen(
    controller: MusicPlayerController,
    onOpenPlayer: () -> Unit,
    bottomPadding: PaddingValues,
    onOpenThemes: (() -> Unit)? = null,
    onArtistClick: ((String) -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val theme = LocalPBTheme.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val currentSong by controller.currentSong.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(theme.background).statusBarsPadding()) {
        AnimatedVisibility(visible = !isOnline) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.surface)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = theme.accent, modifier = Modifier.height(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("You're offline — cached catalog & downloads only", color = theme.text, style = MaterialTheme.typography.bodySmall)
            }
        }

        when (val feedState = uiState.feed) {
            is UiState.Loading -> PbLoading(modifier = Modifier.fillMaxSize())
            is UiState.Error -> ErrorState(
                message = feedState.message,
                onRetry = { viewModel.load() },
                modifier = Modifier.fillMaxSize(),
            )
            is UiState.Success -> HomeContent(
                feed = feedState.data,
                controller = controller,
                currentSongId = currentSong?.id,
                onOpenPlayer = onOpenPlayer,
                onOpenThemes = onOpenThemes,
                onArtistClick = onArtistClick,
                onMoodClick = { viewModel.playMood(it) },
                bottomPadding = bottomPadding,
            )
        }
    }
}

@Composable
private fun HomeContent(
    feed: HomeFeed,
    controller: MusicPlayerController,
    currentSongId: String?,
    onOpenPlayer: () -> Unit,
    onOpenThemes: (() -> Unit)? = null,
    onArtistClick: ((String) -> Unit)? = null,
    onMoodClick: (com.punjabibeats.app.domain.model.MoodCategory) -> Unit,
    bottomPadding: PaddingValues,
) {
    val theme = LocalPBTheme.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding() + 130.dp),
    ) {
        // Dynamic Theme Hero Header Banner with Theme Studio Quick Switcher
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.heroGradient)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Punjabi Beats ${theme.emoji}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.accent,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Daily hits, jam rooms & AI playlists",
                            color = theme.muted,
                            fontSize = 14.sp,
                        )
                    }
                    if (onOpenThemes != null) {
                        IconButton(
                            onClick = onOpenThemes,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(theme.panel.copy(alpha = 0.85f))
                                .border(1.5.dp, theme.accent.copy(alpha = 0.6f), CircleShape),
                        ) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = "Theme Studio",
                                tint = theme.accent,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }

        if (feed.heroSongs.isNotEmpty()) {
            item { HeroPager(feed.heroSongs, controller, currentSongId, onOpenPlayer) }
            item { Spacer(Modifier.height(16.dp)) }
        }

        // 👑 Curated Artists
        item {
            SectionHeader(
                title = "👑 Curated Artists >",
                actionLabel = "See all",
                onActionClick = {
                    controller.playQueue(feed.heroSongs.ifEmpty { feed.trendingSongs })
                },
            )
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (feed.curatedArtists.isNotEmpty()) {
                    items(feed.curatedArtists, key = { it.name }) { artist ->
                        ArtistCard(
                            name = artist.name,
                            artworkUrl = artist.artworkUrl,
                            emoji = "🎤",
                            onClick = {
                                if (onArtistClick != null) {
                                    onArtistClick(artist.name)
                                } else {
                                    controller.playQueue(feed.heroSongs.ifEmpty { feed.trendingSongs })
                                }
                            },
                        )
                    }
                } else {
                    items(CURATED_ARTISTS_FALLBACK, key = { it.first }) { (name, emoji, url) ->
                        ArtistCard(
                            name = name,
                            artworkUrl = url,
                            emoji = emoji,
                            onClick = {
                                if (onArtistClick != null) {
                                    onArtistClick(name)
                                } else {
                                    controller.playQueue(feed.heroSongs.ifEmpty { feed.trendingSongs })
                                }
                            },
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        // 🎵 Moods (2x2 grid + 1 wide card layout matching the screenshot)
        item { SectionHeader("🎵 Moods") }
        item {
            val mood1 = MOOD_CATEGORIES.getOrNull(0)
            val mood2 = MOOD_CATEGORIES.getOrNull(1)
            val mood3 = MOOD_CATEGORIES.getOrNull(2)
            val mood4 = MOOD_CATEGORIES.getOrNull(3)
            val mood5 = MOOD_CATEGORIES.getOrNull(4)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Row 1: Bhangra + Melodic
                if (mood1 != null && mood2 != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MoodTile(mood = mood1, onClick = { onMoodClick(mood1) }, modifier = Modifier.weight(1f))
                        MoodTile(mood = mood2, onClick = { onMoodClick(mood2) }, modifier = Modifier.weight(1f))
                    }
                }
                // Row 2: Gym + Sad
                if (mood3 != null && mood4 != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MoodTile(mood = mood3, onClick = { onMoodClick(mood3) }, modifier = Modifier.weight(1f))
                        MoodTile(mood = mood4, onClick = { onMoodClick(mood4) }, modifier = Modifier.weight(1f))
                    }
                }
                // Row 3: Car Bass (Full Width!)
                if (mood5 != null) {
                    MoodTile(
                        mood = mood5,
                        onClick = { onMoodClick(mood5) },
                        modifier = Modifier.fillMaxWidth(),
                        isFullWidth = true,
                    )
                }
            }
        }

        if (feed.trendingSongs.isNotEmpty()) {
            item { Spacer(Modifier.height(12.dp)) }
            item { SectionHeader("Trending Now") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(feed.trendingSongs, key = { it.id }) { song ->
                        SongCard(song = song, onClick = {
                            controller.playQueue(feed.trendingSongs, feed.trendingSongs.indexOf(song))
                            onOpenPlayer()
                        })
                    }
                }
            }
        }

        if (feed.quickPicks.isNotEmpty()) {
            item { Spacer(Modifier.height(12.dp)) }
            item { SectionHeader("Quick Picks", actionLabel = "For you") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(feed.quickPicks, key = { it.id }) { song ->
                        SongCard(song = song, onClick = {
                            controller.playQueue(feed.quickPicks, feed.quickPicks.indexOf(song))
                            onOpenPlayer()
                        })
                    }
                }
            }
        }
    }
}

/** Hero banner: daily Punjabi hits, auto-advancing every 5 seconds. */
@Composable
private fun HeroPager(
    heroSongs: List<Song>,
    controller: MusicPlayerController,
    currentSongId: String?,
    onOpenPlayer: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { heroSongs.size })
    val daySeed = rememberDayOffset()
    val theme = LocalPBTheme.current

    LaunchedEffect(heroSongs.size) {
        if (heroSongs.size > 1) {
            while (true) {
                delay(5_000)
                val next = (pagerState.currentPage + 1) % heroSongs.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 20.dp),
        pageSpacing = 12.dp,
    ) { index ->
        val song = heroSongs[(index + daySeed) % heroSongs.size]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable {
                    controller.playQueue(heroSongs, heroSongs.indexOf(song))
                    onOpenPlayer()
                },
        ) {
            AsyncImage(
                model = song.artworkUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, theme.background.copy(alpha = 0.95f)))),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            ) {
                Text("DAILY PUNJABI HITS", style = MaterialTheme.typography.labelSmall, color = theme.accent, fontWeight = FontWeight.Bold)
                Text(
                    song.title,
                    color = theme.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song.artist,
                    color = theme.muted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.accent)
                    .clickable {
                        controller.playQueue(heroSongs, heroSongs.indexOf(song))
                        onOpenPlayer()
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = theme.background,
                        modifier = Modifier.height(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Play", color = theme.background, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
            if (currentSongId == song.id) {
                Text(
                    "NOW PLAYING",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                )
            }
        }
    }
}

/** Rotates the hero set day by day so the banner feels fresh without a backend. */
@Composable
private fun rememberDayOffset(): Int {
    val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
    return dayOfYear % 7
}
