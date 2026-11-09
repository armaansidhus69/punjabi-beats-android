package com.punjabibeats.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.punjabibeats.app.core.util.UiState
import com.punjabibeats.app.domain.model.HomeFeed
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.ui.components.ErrorState
import com.punjabibeats.app.ui.components.MoodTile
import com.punjabibeats.app.ui.components.PbLoading
import com.punjabibeats.app.ui.components.SectionHeader
import com.punjabibeats.app.ui.components.SongCard
import com.punjabibeats.app.ui.theme.AccentGold
import com.punjabibeats.app.ui.theme.BgBase
import com.punjabibeats.app.ui.theme.TextSecondary
import com.punjabibeats.app.ui.theme.moodGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    controller: MusicPlayerController,
    onOpenPlayer: () -> Unit,
    bottomPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val currentSong by controller.currentSong.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        AnimatedVisibility(visible = !isOnline) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = AccentGold, modifier = Modifier.height(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("You're offline — cached catalog & downloads only", style = MaterialTheme.typography.bodySmall)
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
    onMoodClick: (com.punjabibeats.app.domain.model.MoodCategory) -> Unit,
    bottomPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding() + 130.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("Punjabi Beats", style = MaterialTheme.typography.displaySmall, color = AccentGold)
                Text("Daily hits, jam rooms & AI playlists", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
        if (feed.heroSongs.isNotEmpty()) {
            item { HeroPager(feed.heroSongs, controller, currentSongId) }
        }
        if (feed.curatedArtists.isNotEmpty()) {
            item { SectionHeader("Curated Artists") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(feed.curatedArtists, key = { it.name }) { artist ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(84.dp)
                                .clickable { controller.playQueue(feed.heroSongs.ifEmpty { feed.trendingSongs }) },
                        ) {
                            if (artist.artworkUrl != null) {
                                AsyncImage(
                                    model = artist.artworkUrl,
                                    contentDescription = artist.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .height(84.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(42.dp)),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .height(84.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(42.dp))
                                        .background(Brush.linearGradient(moodGradient(artist.name.hashCode().toString()))),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        artist.name.take(2).uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = AccentGold,
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                artist.name,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        item { SectionHeader("Moods") }
        val moodRows = com.punjabibeats.app.domain.model.MOOD_CATEGORIES.chunked(2)
        items(moodRows.size) { rowIndex ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                moodRows[rowIndex].forEach { mood ->
                    MoodTile(
                        mood = mood,
                        onClick = { onMoodClick(mood) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (moodRows[rowIndex].size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (feed.trendingSongs.isNotEmpty()) {
            item { SectionHeader("Trending Now") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
            item { SectionHeader("Quick Picks", actionLabel = "For you") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
) {
    val pagerState = rememberPagerState(pageCount = { heroSongs.size })
    val daySeed = rememberDayOffset()

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
                .height(220.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable {
                    controller.playQueue(heroSongs, heroSongs.indexOf(song))
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
                    .background(Brush.verticalGradient(listOf(Color.Transparent, BgBase.copy(alpha = 0.95f)))),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            ) {
                Text("DAILY PUNJABI HITS", style = MaterialTheme.typography.labelSmall, color = AccentGold)
                Text(
                    song.title,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AccentGold)
                    .clickable {
                        controller.playQueue(heroSongs, heroSongs.indexOf(song))
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.Black,
                        modifier = Modifier.height(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Play", color = Color.Black, style = MaterialTheme.typography.labelLarge)
                }
            }
            if (currentSongId == song.id) {
                Text(
                    "NOW PLAYING",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGold,
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
