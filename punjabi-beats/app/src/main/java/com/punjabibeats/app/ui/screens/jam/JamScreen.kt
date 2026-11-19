package com.punjabibeats.app.ui.screens.jam

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.punjabibeats.app.ui.components.FeatureCard
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.punjabibeats.app.domain.model.JAM_REACTIONS
import com.punjabibeats.app.ui.components.ArtworkThumb
import com.punjabibeats.app.ui.components.GlassCard
import com.punjabibeats.app.ui.components.LocalPbSnackbar
import com.punjabibeats.app.ui.components.ReactionsOverlay
import com.punjabibeats.app.ui.components.SectionHeader
import com.punjabibeats.app.ui.components.SongRow
import com.punjabibeats.app.ui.theme.LocalPBTheme

@Composable
fun JamScreen(
    onOpenPlayer: () -> Unit,
    bottomPadding: PaddingValues,
    viewModel: JamViewModel = hiltViewModel(),
) {
    val theme = LocalPBTheme.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val inRoom = uiState.engineState.phase == com.punjabibeats.app.domain.model.JamPhase.IN_ROOM ||
        uiState.engineState.phase == com.punjabibeats.app.domain.model.JamPhase.CONNECTING

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        if (inRoom) {
            JamRoomContent(viewModel = viewModel, onOpenPlayer = onOpenPlayer, bottomPadding = bottomPadding)
        } else {
            JamLobby(viewModel = viewModel, bottomPadding = bottomPadding)
        }
        ReactionsOverlay(reactions = viewModel.reactions, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun JamLobby(viewModel: JamViewModel, bottomPadding: PaddingValues) {
    val theme = LocalPBTheme.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var codeInput by remember { mutableStateOf("") }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { scanned ->
            val code = scanned.substringAfterLast("/").takeLast(JamViewModel.ROOM_CODE_LENGTH)
            viewModel.joinRoom(code)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
    ) {
        // Sunset Campfire Hero Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.heroGradient)
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Column {
                Text(
                    "👥 Jam Rooms",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.accent,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Listen together, perfectly in sync. One person hosts, everyone hears the same millisecond.",
                    color = theme.muted,
                    fontSize = 15.sp,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // FeatureCard 1: Host a room
            FeatureCard(
                emoji = "👥",
                title = "Host a room",
                description = "You control playback; friends follow your every beat.",
            ) {
                Button(
                    onClick = { viewModel.createRoom() },
                    enabled = !uiState.connecting,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = theme.background),
                ) {
                    if (uiState.connecting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = theme.background)
                    } else {
                        Text("Create Room", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            // FeatureCard 2: Join with a code
            FeatureCard(
                emoji = "📱",
                title = "Join with a code",
                description = "Enter your friend's room code.",
            ) {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it.uppercase().take(JamViewModel.ROOM_CODE_LENGTH) },
                    placeholder = { Text("ABC123", color = theme.muted) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                scanLauncher.launch(
                                    ScanOptions()
                                        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                        .setPrompt("Scan a Jam Room QR")
                                        .setBeepEnabled(false)
                                )
                            },
                        ) {
                            Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan QR", tint = theme.accent)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.text,
                        unfocusedTextColor = theme.text,
                        focusedContainerColor = theme.surface,
                        unfocusedContainerColor = theme.surface,
                        focusedBorderColor = theme.accent,
                        unfocusedBorderColor = theme.secondary.copy(alpha = 0.5f),
                        cursorColor = theme.accent,
                    ),
                )

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = { viewModel.joinRoom(codeInput) },
                    enabled = codeInput.length == JamViewModel.ROOM_CODE_LENGTH && !uiState.connecting,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = theme.background),
                ) {
                    Text("Join Room", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            uiState.error?.let { message ->
                Text(
                    message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (uiState.recentRooms.isNotEmpty()) {
                SectionHeader("Recent rooms")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.recentRooms.size) { index ->
                        val code = uiState.recentRooms[index]
                        AssistChip(
                            onClick = { viewModel.joinRoom(code) },
                            label = { Text(code) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JamRoomContent(
    viewModel: JamViewModel,
    onOpenPlayer: () -> Unit,
    bottomPadding: PaddingValues,
) {
    val theme = LocalPBTheme.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snapshot = uiState.engineState.snapshot ?: return
    val isHost = uiState.engineState.isHost
    val drift = uiState.engineState.driftMs
    val context = LocalContext.current
    var showQr by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Room ${snapshot.code}", style = MaterialTheme.typography.headlineMedium, color = theme.accent)
                Text(
                    "${snapshot.participants.size} listening • ${uiState.engineState.transport}",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.muted,
                )
            }
            IconButton(onClick = { showQr = true }) {
                Icon(Icons.Rounded.QrCode2, contentDescription = "Show QR", tint = theme.accent)
            }
            IconButton(onClick = {
                viewModel.shareRoom { link ->
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Join my Punjabi Beats Jam Room ${snapshot.code}: $link")
                    }
                    runCatching { context.startActivity(Intent.createChooser(send, "Invite friends")) }
                }
            }) {
                Icon(Icons.Rounded.Share, contentDescription = "Share room", tint = theme.accent)
            }
            IconButton(onClick = { viewModel.leaveRoom() }) {
                Icon(Icons.Rounded.ExitToApp, contentDescription = "Leave room", tint = MaterialTheme.colorScheme.error)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text(
                        if (kotlin.math.abs(drift) < 100) "Synced ±${drift}ms" else "Drift ${drift}ms",
                        color = if (kotlin.math.abs(drift) < 100) Color(0xFF4ADE80) else theme.accent,
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = theme.surface,
                    disabledContainerColor = theme.surface,
                ),
            )
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(if (isHost) "You are the host 👑" else "Guest mode", color = theme.text) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = theme.surface,
                    disabledContainerColor = theme.surface,
                ),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            snapshot.participants.take(8).forEach { participant ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(theme.surface)
                            .border(1.5.dp, if (participant.isHost) theme.accent else theme.card, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(participant.emoji)
                    }
                    Text(
                        (if (participant.isHost) "👑 " else "") + participant.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (participant.isMe) theme.accent else theme.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(52.dp),
                    )
                }
            }
        }

        snapshot.nowPlaying?.let { song ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ArtworkThumb(url = song.artworkUrl, size = 56.dp, cornerRadius = 12.dp)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    ) {
                        Text(song.title, color = theme.text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, style = MaterialTheme.typography.bodySmall, color = theme.muted, maxLines = 1)
                    }
                    if (isHost) {
                        IconButton(onClick = { viewModel.hostTogglePlay() }) {
                            Icon(
                                if (snapshot.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = theme.accent,
                            )
                        }
                        IconButton(onClick = { viewModel.hostSkip() }) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Next track", tint = theme.text)
                        }
                    }
                    IconButton(onClick = onOpenPlayer) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Open player", tint = theme.accent)
                    }
                }
            }
        }

        SectionHeader("Shared Queue", actionLabel = "Fair turn")

        androidx.compose.foundation.text.BasicTextField(
            value = searchQuery,
            onValueChange = { value ->
                searchQuery = value
                viewModel.searchSongsForQueue(value)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = theme.text),
            decorationBox = { inner ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.surface)
                        .border(1.dp, theme.panel2, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = theme.accent)
                    Box(Modifier.padding(12.dp)) {
                        if (searchQuery.isEmpty()) Text("Add a song to the shared queue…", color = theme.muted)
                        inner()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (uiState.searchResults.isNotEmpty()) {
                items(uiState.searchResults, key = { "search_" + it.id }) { song ->
                    SongRow(
                        song = song,
                        onClick = { viewModel.queueSong(song) },
                        trailing = {
                            Icon(Icons.Rounded.Add, contentDescription = "Add to queue", tint = theme.accent)
                        },
                    )
                }
            }
            items(snapshot.queue, key = { it.entryId }) { entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isHost && !entry.played) { }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    ArtworkThumb(url = entry.song.artworkUrl, size = 44.dp, cornerRadius = 10.dp)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    ) {
                        Text(
                            entry.song.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (entry.played) theme.muted else theme.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "added by ${entry.addedByName.ifBlank { "someone" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.muted,
                        )
                    }
                    if (entry.addedByUserId == uiState.engineState.myId || isHost) {
                        IconButton(onClick = { viewModel.removeQueued(entry.entryId) }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = theme.muted)
                        }
                    }
                }
            }
            if (snapshot.queue.isEmpty() && uiState.searchResults.isEmpty()) {
                item {
                    Text(
                        "Queue is empty — add something above 🎶",
                        color = theme.muted,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            JAM_REACTIONS.forEach { emoji ->
                AssistChip(
                    onClick = { viewModel.sendReaction(emoji) },
                    label = { Text(emoji) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = theme.surface,
                    ),
                )
            }
        }
    }

    if (showQr) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showQr = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showQr = false }) { Text("Done", color = theme.accent) }
            },
            title = { Text("Room ${snapshot.code}", color = theme.text) },
            containerColor = theme.surface,
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val bitmap = remember(snapshot.code) { generateQrBitmap("punjabibeats://jam/${snapshot.code}") }
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Room QR code",
                        modifier = Modifier.size(240.dp),
                    )
                    Text(
                        "Friends scan this or type the code",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.muted,
                    )
                }
            },
        )
    }
}

private fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
    val matrix = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        size,
        size,
        mapOf(EncodeHintType.MARGIN to 1),
    )
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            pixels[y * size + x] = if (matrix.get(x, y)) 0xFF0A0A0F.toInt() else 0xFFFFFFFF.toInt()
        }
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}
