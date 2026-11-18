package com.punjabibeats.app.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KaraokeLivePreviewCard(
    onOpenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFA78BFA),
    amberColor: Color = Color(0xFFFFB95F),
    songTitle: String? = null,
) {
    var karaokeEnabled by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1B1B1F).copy(alpha = 0.9f))
            .border(1.dp, Color(0xFF292A2D), RoundedCornerShape(22.dp))
            .padding(14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Lyrics Icon + Title + Gurmukhi Tag + Karaoke Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = "Lyrics",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Lyrics",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE3E2E6),
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF292A2D))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "Gurmukhi & English",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = amberColor,
                        )
                    }
                }

                // Interactive Karaoke On / Off Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (karaokeEnabled) accentColor.copy(alpha = 0.18f) else Color(0xFF292A2D),
                        )
                        .border(
                            1.dp,
                            if (karaokeEnabled) accentColor.copy(alpha = 0.5f) else Color.Transparent,
                            RoundedCornerShape(14.dp),
                        )
                        .clickable { karaokeEnabled = !karaokeEnabled }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (karaokeEnabled) accentColor else Color(0xFF948E9D)),
                        )
                        Text(
                            text = if (karaokeEnabled) "Karaoke On" else "Karaoke Off",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (karaokeEnabled) accentColor else Color(0xFF948E9D),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active Verse Teleprompter Snippet
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF292A2D).copy(alpha = 0.6f))
                    .clickable(onClick = onOpenLyrics)
                    .padding(12.dp),
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4EDEA3)),
                        )
                        Text(
                            text = if (karaokeEnabled) "PITCH & SYNC • ACTIVE TELEPROMPTER" else "SYNCED VERSE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFF4EDEA3),
                        )
                    }

                    Text(
                        text = "ਕੁੱਝ ਤਾਂ ਜ਼ਮੀਰ ਤੇ ਯਕੀਨ ਰੱਖ ਯਾਰਾਂ, ਗੱਲ 295 ਦੀ ਤੇ ਖਰੇ ਸੱਚ ਦੀ!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE3E2E6),
                        lineHeight = 20.sp,
                    )
                    Text(
                        text = "Kujh taan zameer te yakeen rakh yaaran • Pure truth & raw courage",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clickable(onClick = onOpenLyrics)
                        .padding(vertical = 2.dp, horizontal = 4.dp),
                ) {
                    Text(
                        text = "Fullscreen Lyrics",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                    )
                    Icon(
                        imageVector = Icons.Rounded.OpenInFull,
                        contentDescription = "Expand Lyrics",
                        tint = accentColor,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}
