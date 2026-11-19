package com.punjabibeats.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punjabibeats.app.domain.model.AlbumCard
import com.punjabibeats.app.domain.model.ArtistCard
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.ui.components.ArtworkThumb
import com.punjabibeats.app.ui.components.PbLoading
import com.punjabibeats.app.ui.components.SongRow
import com.punjabibeats.app.ui.theme.LocalPBTheme

private data class SearchTabItem(val tab: SearchTab, val label: String, val icon: ImageVector)

private val SEARCH_TABS = listOf(
    SearchTabItem(SearchTab.ALL, "All", Icons.Rounded.SelectAll),
    SearchTabItem(SearchTab.SONGS, "Songs", Icons.Rounded.MusicNote),
    SearchTabItem(SearchTab.ARTISTS, "Artists", Icons.Rounded.Person),
    SearchTabItem(SearchTab.ALBUMS, "Albums", Icons.Rounded.Album),
    SearchTabItem(SearchTab.PODCASTS, "Podcasts", Icons.Rounded.Podcasts),
)

@Composable
fun SearchScreen(
    controller: MusicPlayerController,
    onOpenPlayer: () -> Unit,
    bottomPadding: PaddingValues,
    onOpenArtist: ((String) -> Unit)? = null,
    onOpenAlbum: ((String, String) -> Unit)? = null,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val theme = LocalPBTheme.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSong by controller.currentSong.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        // Themed Skyline Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.heroGradient)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Column {
                Text(
                    "Search",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.text,
                )
            }
        }

        // Pill Search Bar
        SearchField(
            query = uiState.query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = viewModel::submitSearch,
        )

        Spacer(Modifier.height(10.dp))

        // Horizontal Filter Chips Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SEARCH_TABS, key = { it.tab }) { item ->
                val isSelected = uiState.tab == item.tab
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) theme.accent.copy(alpha = 0.22f) else theme.surface)
                        .border(1.dp, if (isSelected) theme.accent else theme.panel2, RoundedCornerShape(20.dp))
                        .clickable { viewModel.setTab(item.tab) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) theme.accent else theme.muted,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        item.label,
                        color = if (isSelected) theme.accent else theme.text,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        if (uiState.query.isBlank()) {
            SuggestionsSection(
                recent = uiState.recentSearches,
                trending = uiState.trendingSearches,
                onPick = viewModel::onSuggestionClick,
                onClearRecent = viewModel::clearRecentSearches,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.height(20.dp))
            return@Column
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
                onSelectArtist = { artistName ->
                    if (onOpenArtist != null) {
                        onOpenArtist(artistName)
                    } else {
                        viewModel.onQueryChange(artistName)
                        viewModel.setTab(SearchTab.SONGS)
                        viewModel.submitSearch()
                    }
                },
                onSelectAlbum = { albumName ->
                    if (onOpenAlbum != null) {
                        onOpenAlbum(albumName, "")
                    } else {
                        viewModel.onQueryChange(albumName)
                        viewModel.setTab(SearchTab.SONGS)
                        viewModel.submitSearch()
                    }
                },
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
    val theme = LocalPBTheme.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        placeholder = { Text("Search songs or artists", color = theme.muted, fontSize = 15.sp) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = theme.accent)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = theme.muted)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = theme.text,
            unfocusedTextColor = theme.text,
            focusedContainerColor = theme.surface,
            unfocusedContainerColor = theme.surface,
            focusedBorderColor = theme.accent,
            unfocusedBorderColor = theme.panel2,
            cursorColor = theme.accent
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
    )
}

@Composable
private fun SuggestionsSection(
    recent: List<String>,
    trending: List<String>,
    onPick: (String) -> Unit,
    onClearRecent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalPBTheme.current
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
                Text("Recent Searches", color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Clear",
                    style = MaterialTheme.typography.labelLarge,
                    color = theme.accent,
                    modifier = Modifier.clickable(onClick = onClearRecent),
                )
            }
            Spacer(Modifier.height(8.dp))
            recent.forEach { query ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(query) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = theme.muted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(query, color = theme.text, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null, tint = theme.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Trending Searches", color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(trending, key = { it }) { query ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(theme.card)
                        .border(1.dp, theme.panel2, RoundedCornerShape(20.dp))
                        .clickable { onPick(query) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(query, color = theme.text, fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Curated Vibes & Genres from Stitch Screen 01
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Text("Curated Vibes & Genres", color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        val genres = listOf(
            "Bhangra Hits",
            "Desi Hip-Hop",
            "Mandi Beats",
            "Sufi & Qawwali",
            "Gym & Dhol",
            "Old School PB",
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(genres, key = { it }) { genre ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(theme.surface)
                        .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .clickable { onPick(genre) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(genre, color = theme.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

fun removeDuplicateSongs(songs: List<Song>): List<Song> {
    return songs.distinctBy { it.id.trim() }
}

fun extractUniqueArtists(songs: List<Song>, remoteArtists: List<ArtistCard>): List<ArtistCard> {
    val artistSplitRegex = Regex("[,&/•]|\\b(?:feat|ft|featuring)\\b\\.?", RegexOption.IGNORE_CASE)
    val extracted = songs.flatMap { song ->
        song.artist.split(artistSplitRegex)
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("Unknown Artist", ignoreCase = true) }
            .map { artistName ->
                ArtistCard(
                    id = "art_${artistName.lowercase().trim().replace(" ", "_")}",
                    name = artistName,
                    artworkUrl = song.artworkUrl,
                )
            }
    }
    return (remoteArtists + extracted).distinctBy { it.name.trim().lowercase() }
}

fun extractUniqueAlbums(songs: List<Song>, remoteAlbums: List<AlbumCard>): List<AlbumCard> {
    val extracted = songs
        .filter { it.albumName.isNotBlank() }
        .map { song ->
            AlbumCard(
                id = "alb_${song.albumName.trim().lowercase().replace(" ", "_")}_${song.artist.trim().lowercase().replace(" ", "_")}",
                name = song.albumName.trim(),
                artist = song.artist,
                artworkUrl = song.artworkUrl,
                year = song.year,
            )
        }
    return (remoteAlbums + extracted).distinctBy {
        (it.name.trim().lowercase() + "_" + it.artist.trim().lowercase())
    }
}

@Composable
private fun ResultsSection(
    state: SearchUiState,
    controller: MusicPlayerController,
    currentSongId: String?,
    onOpenPlayer: () -> Unit,
    onSelectArtist: (String) -> Unit,
    onSelectAlbum: (String) -> Unit,
    bottomPadding: PaddingValues,
) {
    val theme = LocalPBTheme.current
    val results = state.results
    val query = state.query.trim()

    val rawSongs = results?.songs.orEmpty()
    val uniqueSongs = remember(rawSongs) {
        removeDuplicateSongs(rawSongs)
    }

    val filteredSongs = remember(uniqueSongs, query) {
        if (query.isBlank()) uniqueSongs
        else {
            val matched = uniqueSongs.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.albumName.contains(query, ignoreCase = true)
            }
            matched.ifEmpty { uniqueSongs }
        }
    }

    val rawArtists = results?.artists.orEmpty()
    val uniqueArtists = remember(rawArtists, uniqueSongs, query) {
        val allArtists = extractUniqueArtists(uniqueSongs, rawArtists)
        if (query.isBlank()) allArtists
        else {
            val matched = allArtists.filter { it.name.contains(query, ignoreCase = true) }
            matched.ifEmpty { allArtists }
        }
    }

    val rawAlbums = results?.albums.orEmpty()
    val uniqueAlbums = remember(rawAlbums, uniqueSongs, query) {
        val allAlbums = extractUniqueAlbums(uniqueSongs, rawAlbums)
        if (query.isBlank()) allAlbums
        else {
            val matched = allAlbums.filter {
                it.name.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
            }
            matched.ifEmpty { allAlbums }
        }
    }

    val podcasts = results?.podcasts.orEmpty()

    val isEverythingEmpty = filteredSongs.isEmpty() && uniqueArtists.isEmpty() && uniqueAlbums.isEmpty() && podcasts.isEmpty()
    if (isEverythingEmpty) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(30.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (results == null) "Type to search songs, artists or albums" else "No results for \"${state.query}\"",
                color = theme.muted,
                fontSize = 16.sp,
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
    ) {
        val showAll = state.tab == SearchTab.ALL

        // ===================== SONGS =====================
        if (showAll || state.tab == SearchTab.SONGS) {
            if (filteredSongs.isNotEmpty()) {
                SectionLabel("🎵 Songs (${filteredSongs.size})")
                val displaySongs = if (showAll) filteredSongs.take(8) else filteredSongs
                displaySongs.forEachIndexed { index, song ->
                    SongRow(
                        song = song,
                        isPlaying = song.id == currentSongId || (currentSongId == null && index == 0 && showAll),
                        onArtistClick = onSelectArtist,
                        onClick = {
                            controller.playQueue(filteredSongs, index)
                            onOpenPlayer()
                        },
                    )
                }
            } else if (state.tab == SearchTab.SONGS) {
                EmptyTabMessage("No songs found for \"${state.query}\"")
            }
        }

        // ===================== ARTISTS =====================
        if (showAll || state.tab == SearchTab.ARTISTS) {
            if (uniqueArtists.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionLabel("👑 Artists (${uniqueArtists.size})")
                if (showAll) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(uniqueArtists, key = { it.id }) { artist ->
                            ArtistChipCard(
                                artist = artist,
                                onClick = { onSelectArtist(artist.name) },
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        uniqueArtists.forEach { artist ->
                            ArtistListRow(
                                artist = artist,
                                onClick = { onSelectArtist(artist.name) },
                            )
                        }
                    }
                }
            } else if (state.tab == SearchTab.ARTISTS) {
                EmptyTabMessage("No artists found for \"${state.query}\"")
            }
        }

        // ===================== ALBUMS =====================
        if (showAll || state.tab == SearchTab.ALBUMS) {
            if (uniqueAlbums.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionLabel("💿 Albums (${uniqueAlbums.size})")
                if (showAll) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(uniqueAlbums, key = { it.id }) { album ->
                            AlbumCardItem(
                                album = album,
                                onClick = { onSelectAlbum(album.name) },
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        uniqueAlbums.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                pair.forEach { album ->
                                    Box(Modifier.weight(1f)) {
                                        AlbumCardItem(
                                            album = album,
                                            onClick = { onSelectAlbum(album.name) },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else if (state.tab == SearchTab.ALBUMS) {
                EmptyTabMessage("No albums found for \"${state.query}\"")
            }
        }

        // ===================== PODCASTS =====================
        if (showAll || state.tab == SearchTab.PODCASTS) {
            if (podcasts.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionLabel("🎙️ Podcasts (${podcasts.size})")
                podcasts.forEach { channel ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAlbum(channel.title) }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ArtworkThumb(url = channel.artworkUrl, size = 52.dp, cornerRadius = 12.dp)
                        Column(Modifier.padding(horizontal = 12.dp)) {
                            Text(channel.title, color = theme.text, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                            Text("Podcast • your library", style = MaterialTheme.typography.bodySmall, color = theme.muted)
                        }
                    }
                }
            } else if (state.tab == SearchTab.PODCASTS) {
                EmptyTabMessage("No podcasts found for \"${state.query}\"")
            }
        }
    }
}

@Composable
private fun ArtistChipCard(
    artist: ArtistCard,
    onClick: () -> Unit,
) {
    val theme = LocalPBTheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .border(1.5.dp, theme.accent.copy(alpha = 0.6f), CircleShape),
        ) {
            ArtworkThumb(url = artist.artworkUrl, size = 80.dp, cornerRadius = 40.dp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            artist.name,
            color = theme.text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ArtistListRow(
    artist: ArtistCard,
    onClick: () -> Unit,
) {
    val theme = LocalPBTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.panel)
            .border(1.dp, theme.panel2.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .border(1.5.dp, theme.accent.copy(alpha = 0.7f), CircleShape),
        ) {
            ArtworkThumb(url = artist.artworkUrl, size = 58.dp, cornerRadius = 29.dp)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                artist.name,
                color = theme.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Artist • Tap to view songs",
                color = theme.accent,
                fontSize = 13.sp,
            )
        }
        Icon(
            Icons.Rounded.PlayArrow,
            contentDescription = "Explore",
            tint = theme.accent,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun AlbumCardItem(
    album: AlbumCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalPBTheme.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(theme.panel.copy(alpha = 0.6f))
            .border(1.dp, theme.panel2.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        ArtworkThumb(
            url = album.artworkUrl,
            size = 140.dp,
            cornerRadius = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            album.name,
            color = theme.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (album.artist.isNotBlank()) {
            Text(
                album.artist,
                style = MaterialTheme.typography.bodySmall,
                color = theme.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (album.year != null && album.year > 0) {
            Text(
                "${album.year}",
                fontSize = 11.sp,
                color = theme.accent,
            )
        }
    }
}

@Composable
private fun EmptyTabMessage(message: String) {
    val theme = LocalPBTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message,
            color = theme.muted,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun SectionLabel(title: String?) {
    val theme = LocalPBTheme.current
    if (title != null) {
        Text(
            title,
            color = theme.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}
