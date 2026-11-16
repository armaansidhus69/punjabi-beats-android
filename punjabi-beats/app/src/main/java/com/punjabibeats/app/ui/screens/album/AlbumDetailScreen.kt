package com.punjabibeats.app.ui.screens.album

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.punjabibeats.app.core.util.formatDuration
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.ui.components.EmptyState
import com.punjabibeats.app.ui.components.EqualizerBars
import com.punjabibeats.app.ui.components.PbLoading
import com.punjabibeats.app.ui.components.SongRow
import com.punjabibeats.app.ui.theme.LocalPBTheme

@Composable
fun AlbumDetailScreen(
    onBack: () -> Unit,
    controller: MusicPlayerController,
    onOpenPlayer: () -> Unit,
    onOpenArtist: (String) -> Unit,
    bottomPadding: PaddingValues,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val theme = LocalPBTheme.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSong by controller.currentSong.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .statusBarsPadding(),
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = theme.accent)
            }
            Text(
                text = "Album Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = theme.text,
                modifier = Modifier.weight(1f),
            )
        }

        if (uiState.isLoading) {
            PbLoading(modifier = Modifier.fillMaxSize())
        } else if (uiState.songs.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Album,
                title = "No tracks found",
                subtitle = "Could not load tracklist for this album.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding() + 130.dp),
            ) {
                // Album Banner Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.heroGradient)
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Album Artwork with theme border
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(2.dp, theme.accent, RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!uiState.artworkUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = uiState.artworkUrl,
                                    contentDescription = uiState.albumName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    Icons.Default.Album,
                                    contentDescription = null,
                                    tint = theme.accent,
                                    modifier = Modifier.size(64.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = uiState.albumName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.text,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = uiState.artistName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.accent,
                            modifier = Modifier.clickable { onOpenArtist(uiState.artistName) },
                        )

                        Spacer(Modifier.height(4.dp))

                        val meta = buildString {
                            if (uiState.year != null) append("${uiState.year} • ")
                            append("${uiState.songs.size} tracks")
                            if (uiState.totalDurationMs > 0) {
                                append(" • ${formatDuration(uiState.totalDurationMs)}")
                            }
                        }
                        Text(
                            text = meta,
                            fontSize = 13.sp,
                            color = theme.muted,
                        )

                        Spacer(Modifier.height(18.dp))

                        // Play & Shuffle buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Button(
                                onClick = {
                                    if (uiState.songs.isNotEmpty()) {
                                        controller.playQueue(uiState.songs, 0)
                                        onOpenPlayer()
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = theme.accent,
                                    contentColor = theme.background,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = theme.background)
                                Spacer(Modifier.width(6.dp))
                                Text("Play Album", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (uiState.songs.isNotEmpty()) {
                                        controller.playQueue(uiState.songs.shuffled(), 0)
                                        onOpenPlayer()
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, theme.accent),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                            ) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = theme.accent)
                                Spacer(Modifier.width(6.dp))
                                Text("Shuffle", color = theme.accent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Tracklist",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.text,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }

                itemsIndexed(uiState.songs, key = { _, s -> s.id }) { index, song ->
                    val isPlaying = song.id == currentSong?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                controller.playQueue(uiState.songs, index)
                                onOpenPlayer()
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Track number or playing equalizer
                        Box(
                            modifier = Modifier.width(32.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (isPlaying) {
                                EqualizerBars(color = theme.accent)
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    color = theme.muted,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = if (isPlaying) theme.accent else theme.text,
                                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = song.artist,
                                color = theme.muted,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = formatDuration(song.durationMs),
                            color = theme.muted,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}
