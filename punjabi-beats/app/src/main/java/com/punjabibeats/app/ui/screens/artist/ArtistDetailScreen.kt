package com.punjabibeats.app.ui.screens.artist

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.punjabibeats.app.domain.model.AlbumCard
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.ui.components.ArtworkThumb
import com.punjabibeats.app.ui.components.PbLoading
import com.punjabibeats.app.ui.components.SongRow
import com.punjabibeats.app.ui.theme.LocalPBTheme

@Composable
fun ArtistDetailScreen(
    onBack: () -> Unit,
    controller: MusicPlayerController,
    onOpenPlayer: () -> Unit,
    onOpenAlbum: (String, String) -> Unit,
    bottomPadding: PaddingValues,
    viewModel: ArtistViewModel = hiltViewModel(),
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
        // Top App Bar
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
                text = "Artist Profile",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = theme.text,
                modifier = Modifier.weight(1f),
            )
        }

        if (uiState.isLoading) {
            PbLoading(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding() + 130.dp),
            ) {
                // Artist Header Banner
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.heroGradient)
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Circular Avatar with glow
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape)
                                .border(3.dp, theme.accent, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!uiState.artworkUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = uiState.artworkUrl,
                                    contentDescription = uiState.artistName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = theme.accent,
                                    modifier = Modifier.size(54.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.artistName,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.text,
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Verified Artist",
                                tint = theme.accent,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${uiState.bioInfo.origin} • ${uiState.bioInfo.monthlyListeners}",
                            fontSize = 13.sp,
                            color = theme.muted,
                        )

                        Spacer(Modifier.height(12.dp))

                        // Bio card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(theme.surface.copy(alpha = 0.85f))
                                .border(1.dp, theme.panel2, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                        ) {
                            Text(
                                text = uiState.bioInfo.bio,
                                fontSize = 13.sp,
                                color = theme.text.copy(alpha = 0.9f),
                                lineHeight = 18.sp,
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Action Buttons: Play All & Shuffle
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
                                Text("Play All", fontWeight = FontWeight.Bold)
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

                // Albums / Discography
                if (uiState.albums.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(
                                text = "Albums & EPs",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.text,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(uiState.albums, key = { it.id }) { album ->
                                    ArtistAlbumCard(
                                        album = album,
                                        onClick = { onOpenAlbum(album.name, uiState.artistName) },
                                    )
                                }
                            }
                        }
                    }
                }

                // Top Songs Section
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Popular Songs (${uiState.songs.size})",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.text,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }

                itemsIndexed(uiState.songs, key = { _, s -> s.id }) { index, song ->
                    SongRow(
                        song = song,
                        isPlaying = song.id == currentSong?.id,
                        onClick = {
                            controller.playQueue(uiState.songs, index)
                            onOpenPlayer()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistAlbumCard(
    album: AlbumCard,
    onClick: () -> Unit,
) {
    val theme = LocalPBTheme.current
    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        ArtworkThumb(
            url = album.artworkUrl,
            size = 122.dp,
            cornerRadius = 14.dp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = album.name,
            color = theme.text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = album.artist.ifBlank { "Album" },
            color = theme.muted,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}
