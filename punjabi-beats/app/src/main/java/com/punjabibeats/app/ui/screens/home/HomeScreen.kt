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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.punjabibeats.app.ui.components.CategoryChipsRow
import com.punjabibeats.app.ui.components.NEON_MOOD_ITEMS
import com.punjabibeats.app.ui.components.NeonMoodCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.punjabibeats.app.ui.components.LocalPbSnackbar
import com.punjabibeats.app.ui.components.MoodTile
import com.punjabibeats.app.ui.components.PbLoading
import com.punjabibeats.app.ui.components.SectionHeader
import com.punjabibeats.app.ui.components.SongCard
import com.punjabibeats.app.ui.theme.LocalPBTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    onOpenSettings: (() -> Unit)? = null,
    onOpenSearch: (() -> Unit)? = null,
    onOpenAiStudio: (() -> Unit)? = null,
    onArtistClick: ((String) -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val theme = LocalPBTheme.current
    val snackbar = LocalPbSnackbar.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val currentSong by controller.currentSong.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbar.showSnackbar(message)
        }
    }

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
                onOpenSettings = onOpenSettings,
                onOpenSearch = onOpenSearch,
                onOpenAiStudio = onOpenAiStudio,
                onArtistClick = onArtistClick,
                onMoodClick = { viewModel.playMood(it) },
                onNotificationClick = {
                    scope.launch {
                        snackbar.showSnackbar("🔔 No new notifications — All Punjabi hits are up to date!")
                    }
                },
                bottomPadding = bottomPadding,
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    onOpenThemes: (() -> Unit)?,
    onMenuClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
) {
    val theme = LocalPBTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                Icons.Rounded.Menu,
                contentDescription = "Menu",
                tint = theme.text,
                modifier = Modifier.size(26.dp),
            )
        }

        // Signature Punjabi Beats Branding
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onOpenThemes?.invoke() },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👑", fontSize = 18.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Punjabi Beats",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = theme.text,
                    letterSpacing = 0.5.sp,
                )
            }
            Text(
                "YOUR MUSIC • YOUR UNIVERSE",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.muted,
                letterSpacing = 2.sp,
            )
        }

        // Right Notification Bell with unread glowing pink dot
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onNotificationClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.NotificationsNone,
                contentDescription = "Notifications",
                tint = theme.text,
                modifier = Modifier.size(24.dp),
            )
            // Unread pink dot
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(theme.accent)
                    .border(1.dp, theme.background, CircleShape),
            )
        }
    }
}

@Composable
private fun HomeSearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    onMicClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onOpenSearch: (() -> Unit)? = null,
    onOpenAiStudio: (() -> Unit)? = null,
) {
    val theme = LocalPBTheme.current
    val pillShape = RoundedCornerShape(24.dp)
    val pillBorder = theme.panel2.copy(alpha = 0.6f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(pillShape)
            .background(theme.surface.copy(alpha = 0.95f))
            .border(1.dp, pillBorder, pillShape)
            .clickable { onOpenSearch?.invoke() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = "Search",
            tint = theme.accent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Search Punjabi songs, artists, albums...",
            color = theme.muted,
            fontSize = 13.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Rounded.Mic,
            contentDescription = "Search",
            tint = theme.muted,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun HomeContent(
    feed: HomeFeed,
    controller: MusicPlayerController,
    currentSongId: String?,
    onOpenPlayer: () -> Unit,
    onOpenThemes: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenSearch: (() -> Unit)? = null,
    onOpenAiStudio: (() -> Unit)? = null,
    onArtistClick: ((String) -> Unit)? = null,
    onMoodClick: (com.punjabibeats.app.domain.model.MoodCategory) -> Unit,
    onNotificationClick: () -> Unit = {},
    bottomPadding: PaddingValues,
) {
    val theme = LocalPBTheme.current
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        // 1. Top Bar: ☰, 👑 Punjabi Beats, 🔔
        item {
            HomeTopBar(
                onOpenThemes = onOpenThemes,
                onMenuClick = { onOpenSettings?.invoke() ?: onOpenThemes?.invoke() },
                onNotificationClick = onNotificationClick,
            )
        }

        // 2. Clean Full-Width Search Pill
        item {
            HomeSearchPill(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onOpenSearch = onOpenSearch,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        // 3. Category Filter Chips (Immediately under search for quick filtering)
        item {
            CategoryChipsRow(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        // 4. Single Spotlight Hero Card (Unified, clean & proportional)
        if (feed.heroSongs.isNotEmpty()) {
            item { HeroPager(feed.heroSongs, controller, currentSongId, onOpenPlayer) }
            item { Spacer(Modifier.height(14.dp)) }
        }

        // 5. 🔥 Trending Now (Directly visible without heavy scrolling!)
        if (feed.trendingSongs.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "🔥 Trending Now",
                    actionLabel = "See All >",
                    onActionClick = {
                        controller.playQueue(feed.trendingSongs)
                        onOpenPlayer()
                    },
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(feed.trendingSongs, key = { it.id }) { song ->
                        SongCard(
                            song = song,
                            onClick = {
                                controller.playQueue(feed.trendingSongs, feed.trendingSongs.indexOf(song))
                                onOpenPlayer()
                            },
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(14.dp)) }
        }

        // 6. 👑 Curated Artists Row
        item {
            SectionHeader(
                title = "👑 Curated Artists",
                actionLabel = "See All >",
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

        item { Spacer(Modifier.height(14.dp)) }

        // 7. 🎵 Moods & Vibes
        item {
            SectionHeader(
                title = "🎵 Moods & Vibes",
                actionLabel = "See All >",
                onActionClick = {
                    val firstMood = MOOD_CATEGORIES.firstOrNull()
                    if (firstMood != null) onMoodClick(firstMood)
                },
            )
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(NEON_MOOD_ITEMS, key = { it.id }) { item ->
                    NeonMoodCard(
                        item = item,
                        onClick = {
                            val cat = MOOD_CATEGORIES.firstOrNull { it.id == item.id }
                                ?: MOOD_CATEGORIES.first()
                            onMoodClick(cat)
                        },
                    )
                }
            }
        }

        // 8. 🎧 Quick Picks
        if (feed.quickPicks.isNotEmpty()) {
            item { Spacer(Modifier.height(14.dp)) }
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

/** Hero banner: daily Punjabi hits with neon border, circular play button, and 4 dots. */
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

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp,
        ) { index ->
            val song = heroSongs[(index + daySeed) % heroSongs.size]
            val cardShape = RoundedCornerShape(20.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(cardShape)
                    .border(1.dp, theme.panel2.copy(alpha = 0.8f), cardShape)
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
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0x4408060D),
                                    Color(0xEE08060D),
                                )
                            )
                        ),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👑", fontSize = 13.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "DAILY DESI RADAR",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                        )
                    }
                    Text(
                        "FEATURED PUNJABI HIT",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.accent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        song.title + " • " + song.artist,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Clean circular Play button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(theme.accent)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        .clickable {
                            controller.playQueue(heroSongs, heroSongs.indexOf(song))
                            onOpenPlayer()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // Dot indicator row below hero banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val count = minOf(4, heroSongs.size)
            repeat(count) { i ->
                val active = (pagerState.currentPage % count) == i
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = if (active) 18.dp else 5.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(if (active) theme.accent else theme.muted.copy(alpha = 0.35f)),
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
