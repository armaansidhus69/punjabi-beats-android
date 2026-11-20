package com.punjabibeats.app.ui.screens.replay

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.punjabibeats.app.ui.components.glass.LiquidGlassDefaults
import com.punjabibeats.app.ui.theme.LocalPBTheme

@Composable
fun ReplayScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalPBTheme.current

    // Sample/Default replay summary representing user listening habits
    val summary = remember {
        calculateReplaySummary(
            history = listOf(
                SongHistoryItem("1", "295", "Sidhu Moose Wala", "Bhangra", 420_000L),
                SongHistoryItem("2", "The Last Ride", "Sidhu Moose Wala", "Bhangra", 380_000L),
                SongHistoryItem("3", "Softly", "Karan Aujla", "Punjabi Pop", 340_000L),
                SongHistoryItem("4", "Lover", "Diljit Dosanjh", "Punjabi Pop", 310_000L),
                SongHistoryItem("5", "GOAT", "Diljit Dosanjh", "Bhangra", 300_000L),
                SongHistoryItem("6", "Levels", "Sidhu Moose Wala", "Bhangra", 290_000L),
                SongHistoryItem("7", "Admiring You", "Karan Aujla", "Desi Hip Hop", 280_000L),
                SongHistoryItem("8", "Born to Shine", "Diljit Dosanjh", "Bhangra", 270_000L),
            ),
            year = 2026,
        )
    }

    val totalCards = 5
    var currentCardIndex by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF28103A),
                        Color(0xFF160B24),
                        Color(0xFF09060E),
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Story Progress Indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (i in 0 until totalCards) {
                    val progress = when {
                        i < currentCardIndex -> 1f
                        i == currentCardIndex -> 1f
                        else -> 0f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(if (i <= currentCardIndex) theme.accent else Color.White.copy(alpha = 0.25f))
                    )
                }
            }

            // Top Header: App Branding and Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "👑 PUNJABI REPLAY 2026",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = theme.accent,
                    letterSpacing = 1.2.sp,
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                    )
                }
            }

            // Interactive Story Content Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF1E142B).copy(alpha = 0.85f))
                    .border(LiquidGlassDefaults.EdgeWidth, Color.White.copy(alpha = 0.16f), RoundedCornerShape(32.dp))
                    .shadow(elevation = 20.dp, shape = RoundedCornerShape(32.dp), ambientColor = Color.Black, spotColor = theme.accent.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        currentCardIndex = (currentCardIndex + 1) % totalCards
                    }
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(targetState = currentCardIndex, label = "storyCard") { page ->
                    when (page) {
                        0 -> ReplayMinutesCard(summary)
                        1 -> ReplayTopArtistCard(summary)
                        2 -> ReplayTopTracksCard(summary)
                        3 -> ReplayGenresCard(summary)
                        else -> ReplayPersonalityBadgeCard(summary, onClose)
                    }
                }
            }

            // Bottom Navigation Hints
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (currentCardIndex > 0) "Tap left to go back" else "",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.clickable {
                        if (currentCardIndex > 0) currentCardIndex--
                    },
                )
                Text(
                    text = if (currentCardIndex < totalCards - 1) "Tap card to continue →" else "Swipe or close",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.accent,
                )
            }
        }
    }
}

@Composable
private fun ReplayMinutesCard(summary: ReplaySummary) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Your Year In Beats",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.75f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "${summary.totalMinutes * 142}", // illustrative annual multiplier
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFFCC00),
            letterSpacing = (-1).sp,
        )
        Text(
            text = "MINUTES LISTENED",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "You were in the top 1% of Sidhu Moose Wala and Diljit Dosanjh listeners worldwide! 👑",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun ReplayTopArtistCard(summary: ReplaySummary) {
    val topArtist = summary.topArtists.firstOrNull()?.artist ?: "Sidhu Moose Wala"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Your #1 Artist",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.70f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF9900).copy(alpha = 0.2f))
                .border(3.dp, Color(0xFFFFCC00), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "👑",
                fontSize = 58.sp,
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = topArtist,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You streamed ${topArtist}'s iconic tracks for countless late-night sessions.",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.80f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun ReplayTopTracksCard(summary: ReplaySummary) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Your Top Tracks of 2026",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(18.dp))
        summary.topSongs.take(5).forEachIndexed { index, song ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${index + 1}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFCC00),
                    modifier = Modifier.width(32.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                    )
                    Text(
                        text = song.artist,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplayGenresCard(summary: ReplaySummary) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Your Sonic Signature",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(20.dp))
        summary.topGenres.forEach { genre ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = genre.genre,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                    Text(
                        text = "${genre.percentage}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFCC00),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { genre.percentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFFFCC00),
                    trackColor = Color.White.copy(alpha = 0.12f),
                )
            }
        }
    }
}

@Composable
private fun ReplayPersonalityBadgeCard(summary: ReplaySummary, onClose: () -> Unit) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Your Listening Persona",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.70f),
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = summary.personalityBadge,
            fontSize = 64.sp,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = summary.personalityTitle,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFFFCC00),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = summary.personalityDescription,
            fontSize = 13.5.sp,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.height(26.dp))
        Button(
            onClick = {
                val shareText = "👑 My Punjabi Beats 2026 Replay:\n\n" +
                    "Persona: ${summary.personalityBadge} ${summary.personalityTitle}\n" +
                    "\"${summary.personalityDescription}\"\n\n" +
                    "Top Artist: ${summary.topArtists.firstOrNull()?.artist ?: "Sidhu Moose Wala"}\n" +
                    "Top Track: ${summary.topSongs.firstOrNull()?.title ?: "295"}\n\n" +
                    "Streamed on Punjabi Beats 👑"
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Punjabi Beats 2026 Replay")
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                try {
                    context.startActivity(Intent.createChooser(sendIntent, "Share Punjabi Beats Replay"))
                } catch (_: Exception) {
                    onClose()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Share,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Share My 2026 Replay",
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )
        }
    }
}
