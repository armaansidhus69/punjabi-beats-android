package com.punjabibeats.app.ui.screens.search

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.ui.components.ArtworkThumb
import com.punjabibeats.app.ui.components.PbLoading
import com.punjabibeats.app.ui.components.SongCard
import com.punjabibeats.app.ui.components.SongRow
import com.punjabibeats.app.ui.theme.AccentGold
import com.punjabibeats.app.ui.theme.SurfaceElevated
import com.punjabibeats.app.ui.theme.TextSecondary

private val SEARCH_TABS = listOf("All", "Songs", "Artists", "Albums", "Podcasts")

@Composable
fun SearchScreen(
    controller: MusicPlayerController,
    onOpenPlayer: () -> Unit,
    bottomPadding: PaddingValues,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSong by controller.currentSong.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Text(
            "Search",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )

        SearchField(
            query = uiState.query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = viewModel::submitSearch,
        )

        if (uiState.query.isBlank()) {
            SuggestionsSection(
                recent = uiState.recentSearches,
                trending = uiState.trendingSearches,
                onPick = viewModel::onSuggestionClick,
                onClearRecent = viewModel::clearRecentSearches,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.height(bottomPadding.calculateBottomPadding() + 120.dp))
            return@Column
        }

        TabRow(
            selectedTabIndex = uiState.tab.ordinal,
            containerColor = Color.Transparent,
            contentColor = AccentGold,
        ) {
            SEARCH_TABS.forEachIndexed { index, label ->
                Tab(
                    selected = uiState.tab.ordinal == index,
                    onClick = { viewModel.setTab(SearchTab.entries[index]) },
                    text = { Text(label) },
                )
            }
        }

        when {
            uiState.isSearching -> PbLoading(modifier = Modifier.weight(1f))
            uiState.error != null -> Text(
                uiState.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(20.dp),
            )
            else -> ResultsSection(
                state = uiState,
                controller = controller,
                currentSongId = currentSong?.id,
                onOpenPlayer = onOpenPlayer,
                bottomPadding = bottomPadding,
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary)
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { inner ->
                Box(Modifier.padding(12.dp)) {
                    if (query.isEmpty()) {
                        Text("Songs, artists, albums…", color = TextSecondary)
                    }
                    inner()
                }
            },
            modifier = Modifier.weight(1f),
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = TextSecondary)
            }
        }
    }
}

@Composable
private fun SuggestionsSection(
    recent: List<String>,
    trending: List<String>,
    onPick: (String) -> Unit,
    onClearRecent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
    ) {
        if (recent.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Recent", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Clear",
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentGold,
                    modifier = Modifier.clickable(onClick = onClearRecent),
                )
            }
            recent.forEach { query ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(query) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = TextSecondary)
                    Spacer(Modifier.width(12.dp))
                    Text(query, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Icon(Icons.Rounded.TrendingUp, contentDescription = null, tint = AccentGold)
            Spacer(Modifier.width(8.dp))
            Text("Trending searches", style = MaterialTheme.typography.titleMedium)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(trending, key = { it }) { query ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceElevated)
                        .clickable { onPick(query) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(query, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ResultsSection(
    state: SearchUiState,
    controller: MusicPlayerController,
    currentSongId: String?,
    onOpenPlayer: () -> Unit,
    bottomPadding: PaddingValues,
) {
    val results = state.results
    if (results == null || results.isEmpty) {
        Text(
            if (results == null) "Type to search the catalogue" else "No results for \"${state.query}\"",
            color = TextSecondary,
            modifier = Modifier.padding(20.dp),
        )
        return
    }
    val songs = results.songs
    val artists = results.artists
    val albums = results.albums
    val podcasts = results.podcasts

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomPadding.calculateBottomPadding() + 130.dp),
    ) {
        val showAll = state.tab == SearchTab.ALL
        if ((showAll || state.tab == SearchTab.SONGS) && songs.isNotEmpty()) {
            SectionLabel(if (showAll) "Songs" else null)
            songs.forEachIndexed { index, song ->
                SongRow(
                    song = song,
                    isPlaying = song.id == currentSongId,
                    onClick = {
                        controller.playQueue(songs, index)
                        onOpenPlayer()
                    },
                )
            }
        }
        if ((showAll || state.tab == SearchTab.ARTISTS) && artists.isNotEmpty()) {
            SectionLabel(if (showAll) "Artists" else null)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(artists, key = { it.id }) { artist ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(92.dp),
                    ) {
                        ArtworkThumb(url = artist.artworkUrl, size = 92.dp, cornerRadius = 46.dp)
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
        if ((showAll || state.tab == SearchTab.ALBUMS) && albums.isNotEmpty()) {
            SectionLabel(if (showAll) "Albums" else null)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(albums, key = { it.id }) { album ->
                    Column(modifier = Modifier.width(140.dp)) {
                        ArtworkThumb(url = album.artworkUrl, size = 140.dp, cornerRadius = 14.dp)
                        Spacer(Modifier.height(6.dp))
                        Text(album.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(album.artist, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
                    }
                }
            }
        }
        if ((showAll || state.tab == SearchTab.PODCASTS) && podcasts.isNotEmpty()) {
            SectionLabel(if (showAll) "Podcasts" else null)
            podcasts.forEach { channel ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtworkThumb(url = channel.artworkUrl, size = 52.dp, cornerRadius = 10.dp)
                    Column(Modifier.padding(horizontal = 12.dp)) {
                        Text(channel.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Text("Podcast • your library", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String?) {
    if (title != null) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}
