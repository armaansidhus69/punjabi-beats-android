package com.punjabibeats.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.punjabibeats.app.core.util.formatDuration
import com.punjabibeats.app.domain.model.MoodCategory
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.ui.theme.AccentRed
import com.punjabibeats.app.ui.theme.Grey
import com.punjabibeats.app.ui.theme.HighlightRowBg
import com.punjabibeats.app.ui.theme.LocalPBTheme
import com.punjabibeats.app.ui.theme.White
import com.punjabibeats.app.ui.theme.moodGradient

val LocalPbSnackbar = compositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val theme = LocalPBTheme.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(theme.panel)
            .border(1.dp, theme.panel2.copy(alpha = 0.6f), RoundedCornerShape(cornerRadius))
            .padding(18.dp),
        content = content,
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val theme = LocalPBTheme.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = theme.text,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
        )
        if (actionLabel != null && onActionClick != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = theme.accent,
                modifier = Modifier.clickable(onClick = onActionClick),
            )
        }
    }
}

@Composable
fun ArtworkThumb(
    url: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
) {
    val theme = LocalPBTheme.current
    val shape = RoundedCornerShape(cornerRadius)
    if (url.isNullOrBlank()) {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(theme.surface)
                .border(1.dp, theme.panel2.copy(alpha = 0.5f), shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = theme.accent, modifier = Modifier.size(size / 2))
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(theme.surface),
        )
    }
}

/** Animated 3-bar equalizer indicator for the currently playing track. */
@Composable
fun EqualizerBars(modifier: Modifier = Modifier, color: Color = Color(0xFFE2BC3F)) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.45f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "h3"
    )
    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight(h1).clip(RoundedCornerShape(1.5.dp)).background(color))
        Box(Modifier.width(3.dp).fillMaxHeight(h2).clip(RoundedCornerShape(1.5.dp)).background(color))
        Box(Modifier.width(3.dp).fillMaxHeight(h3).clip(RoundedCornerShape(1.5.dp)).background(color))
    }
}

/**
 * High-polish song row matching the showcase screenshot:
 * Gold-highlighted container when playing, artwork, title, artist • duration,
 * animated equalizer, golden circular play button, and 3-dots more menu.
 */
@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    isLiked: Boolean? = null,
    onToggleLike: (() -> Unit)? = null,
    onArtistClick: ((String) -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val theme = LocalPBTheme.current
    val rowShape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(rowShape)
            .background(if (isPlaying) theme.panel2 else theme.panel)
            .border(
                1.dp,
                if (isPlaying) theme.accent else theme.panel2.copy(alpha = 0.5f),
                rowShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkThumb(url = song.artworkUrl, size = 52.dp, cornerRadius = 12.dp)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                song.title,
                color = if (isPlaying) theme.accent else theme.text,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${song.artist} • ${formatDuration(song.durationMs)}",
                color = if (onArtistClick != null) theme.accent.copy(alpha = 0.9f) else theme.muted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (onArtistClick != null) {
                    Modifier.clickable { onArtistClick(song.artist) }
                } else Modifier,
            )
        }
        if (isPlaying) {
            EqualizerBars(modifier = Modifier.padding(end = 8.dp), color = theme.accent)
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isPlaying) theme.accent.copy(alpha = 0.2f) else Color.Transparent)
                .border(
                    1.dp,
                    if (isPlaying) theme.accent else theme.muted.copy(alpha = 0.35f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = if (isPlaying) theme.accent else theme.text,
                modifier = Modifier.size(18.dp),
            )
        }
        if (onToggleLike != null && isLiked != null) {
            IconButton(onClick = onToggleLike, modifier = Modifier.size(32.dp).padding(start = 4.dp)) {
                Icon(
                    if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isLiked) "Unlike" else "Like",
                    tint = if (isLiked) Color(0xFFFF5577) else theme.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = "More",
                tint = theme.muted,
                modifier = Modifier.padding(start = 6.dp).size(20.dp),
            )
        }
    }
}

/** Circular artist avatar with intense double neon glowing pink rings matching the screenshot. */
@Composable
fun ArtistCard(
    name: String,
    emoji: String = "🎤",
    artworkUrl: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalPBTheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(92.dp)
            .clickable(onClick = onClick),
    ) {
        // Double glowing neon pink rings matching screenshot
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .border(2.dp, theme.accent, CircleShape)
                .padding(3.dp)
                .clip(CircleShape)
                .border(1.5.dp, theme.accent.copy(alpha = 0.85f), CircleShape)
                .background(theme.surface),
            contentAlignment = Alignment.Center,
        ) {
            if (artworkUrl != null) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Text(emoji, fontSize = 32.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            name,
            color = theme.text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Compact square card used in horizontal carousels. */
@Composable
fun SongCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 150.dp,
) {
    val theme = LocalPBTheme.current
    Column(
        modifier = modifier
            .width(width)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box {
            ArtworkThumb(url = song.artworkUrl, size = width - 8.dp, cornerRadius = 14.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Play ${song.title}",
                    tint = theme.accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            song.title,
            color = theme.text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            song.artist,
            color = theme.muted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Rich Mood tile matching the 2x2 + 1 wide layout from the showcase screenshot.
 */
@Composable
fun MoodTile(
    mood: MoodCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFullWidth: Boolean = false,
) {
    val theme = LocalPBTheme.current
    val gradient = moodGradient(mood.id)
    Box(
        modifier = modifier
            .height(if (isFullWidth) 110.dp else 125.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(gradient))
            .border(1.dp, theme.accent.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
            Text(mood.emoji, fontSize = 30.sp)
            Text(
                mood.title,
                color = White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

data class NeonMoodItem(
    val id: String,
    val title: String,
    val emoji: String,
    val borderColor: Color,
    val bgGradient: List<Color>,
)

val NEON_MOOD_ITEMS = listOf(
    NeonMoodItem(
        id = "bhangra",
        title = "Bhangra",
        emoji = "🪘",
        borderColor = Color(0xFFFF9E0B),
        bgGradient = listOf(Color(0xFF2E1705), Color(0xFF130E1B)),
    ),
    NeonMoodItem(
        id = "chill",
        title = "Chill",
        emoji = "🌙",
        borderColor = Color(0xFF00E5FF),
        bgGradient = listOf(Color(0xFF051B2E), Color(0xFF130E1B)),
    ),
    NeonMoodItem(
        id = "drive",
        title = "Drive",
        emoji = "🏎️",
        borderColor = Color(0xFF8B5CF6),
        bgGradient = listOf(Color(0xFF1D0933), Color(0xFF130E1B)),
    ),
    NeonMoodItem(
        id = "heartbreak",
        title = "Heartbreak",
        emoji = "💔",
        borderColor = Color(0xFFEF4444),
        bgGradient = listOf(Color(0xFF330914), Color(0xFF130E1B)),
    ),
)

/**
 * Neon Mood card with individual glowing border matching screenshot 1.
 */
@Composable
fun NeonMoodCard(
    item: NeonMoodItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .width(82.dp)
            .height(96.dp)
            .clip(cardShape)
            .background(Brush.verticalGradient(item.bgGradient))
            .border(1.5.dp, item.borderColor, cardShape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.borderColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.emoji, fontSize = 24.sp)
            }
            Text(
                item.title,
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

/** Category chips row matching Vice City edition screenshot. */
@Composable
fun CategoryChipsRow(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = listOf("All" to "🎛️", "Songs" to "🎵", "Artists" to "👤", "Albums" to "💿")
    val theme = LocalPBTheme.current
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(categories) { (cat, emoji) ->
            val isSelected = cat == selectedCategory
            val pillShape = RoundedCornerShape(20.dp)
            Row(
                modifier = Modifier
                    .clip(pillShape)
                    .background(if (isSelected) theme.accent else theme.surface)
                    .border(
                        1.dp,
                        if (isSelected) theme.accent else theme.card.copy(alpha = 0.8f),
                        pillShape
                    )
                    .clickable { onCategorySelected(cat) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(emoji, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    cat,
                    color = if (isSelected) Color.White else theme.muted,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

/** Feature card with border and description matching Jam/AI screens. */
@Composable
fun FeatureCard(
    emoji: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val theme = LocalPBTheme.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = theme.surface),
        border = BorderStroke(1.dp, theme.panel2.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "$emoji  $title",
                color = theme.text,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(description, color = theme.muted, fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))
            content()
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val theme = LocalPBTheme.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = theme.accent, modifier = Modifier.size(54.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = theme.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = theme.muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val theme = LocalPBTheme.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = AccentRed, modifier = Modifier.size(40.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = theme.muted)
        if (onRetry != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = theme.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Retry", color = theme.accent, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun PbLoading(modifier: Modifier = Modifier) {
    val theme = LocalPBTheme.current
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = theme.accent)
    }
}
