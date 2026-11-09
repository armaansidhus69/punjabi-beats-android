@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.punjabibeats.app.ui.screens.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punjabibeats.app.core.util.UiState
import com.punjabibeats.app.domain.model.ScriptMode
import com.punjabibeats.app.domain.model.TimedWord
import com.punjabibeats.app.ui.components.EmptyState
import com.punjabibeats.app.ui.components.LocalPbSnackbar
import com.punjabibeats.app.ui.components.PbLoading
import com.punjabibeats.app.ui.theme.AccentGold
import com.punjabibeats.app.ui.theme.BgBase
import com.punjabibeats.app.ui.theme.TextSecondary
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Karaoke-style synced lyrics with Gurmukhi/Roman toggle and lyric-card sharing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    onBack: () -> Unit,
    viewModel: LyricsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val positionMs by viewModel.positionMs.collectAsStateWithLifecycle()
    val song by viewModel.player.currentSong.collectAsStateWithLifecycle()
    val snackbar = LocalPbSnackbar.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { message -> snackbar.showSnackbar(message) }
    }

    val lyrics = (uiState.lyrics as? UiState.Success)?.data
    val lines = lyrics?.lines.orEmpty()
    val isSynced = lyrics?.isSynced == true

    val activeIndex = remember(lines, isSynced) {
        derivedStateOf {
            if (!isSynced) -1 else lines.indexOfLast { it.timeMs <= positionMs }
        }
    }
    val currentLineIndex by activeIndex

    val listState = rememberLazyListState()
    var lastUserScrollMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling -> if (scrolling) lastUserScrollMs = System.currentTimeMillis() }
    }
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0 && System.currentTimeMillis() - lastUserScrollMs > 2_500) {
            listState.animateScrollToItem(index = (currentLineIndex - 2).coerceAtLeast(0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song?.title ?: "Lyrics",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    song?.artist ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                )
            }
            IconButton(onClick = { viewModel.cycleScriptMode() }) {
                Icon(
                    Icons.Rounded.Translate,
                    contentDescription = "Script toggle",
                    tint = if (uiState.scriptMode != ScriptMode.BOTH) AccentGold else TextSecondary,
                )
            }
            IconButton(onClick = { viewModel.toggleSelectMode() }) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Select lines",
                    tint = if (uiState.selectMode) AccentGold else TextSecondary,
                )
            }
            IconButton(
                enabled = uiState.selectMode && uiState.selectedIndices.isNotEmpty() || song != null,
                onClick = {
                    val selectedLines = if (uiState.selectMode && uiState.selectedIndices.isNotEmpty()) {
                        uiState.selectedIndices.sorted().mapNotNull { lines.getOrNull(it) }
                    } else if (currentLineIndex >= 0) {
                        lines.drop(currentLineIndex).take(3)
                    } else {
                        emptyList()
                    }
                    val currentSong = song ?: return@IconButton
                    if (selectedLines.isEmpty()) {
                        scope.launch { snackbar.showSnackbar("Select lyric lines first") }
                        return@IconButton
                    }
                    scope.launch {
                    val bitmap = withContext(Dispatchers.Default) {
                        LyricCardRenderer.render(
                            context,
                            currentSong,
                            selectedLines.map { line -> uiState.scriptTextFor(line) },
                        )
                    }
                        LyricCardRenderer.share(context, bitmap)
                        viewModel.notifyShared(selectedLines.size)
                    }
                },
            ) {
                Icon(Icons.Rounded.IosShare, contentDescription = "Share lyric card", tint = AccentGold)
            }
        }
        Text(
            when (uiState.scriptMode) {
                ScriptMode.BOTH -> "Gurmukhi + Roman"
                ScriptMode.ROMAN -> "Roman phonetic"
                ScriptMode.GURMUKHI -> "Gurmukhi"
            } + "  •  ${if (lyrics?.isSynced == true) "synced by " + lyrics.provider else "unsynced"}",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        when (val lyricsState = uiState.lyrics) {
            is UiState.Loading -> PbLoading(modifier = Modifier.fillMaxSize())
            is UiState.Error -> EmptyState(
                icon = Icons.Rounded.MusicNote,
                title = "No lyrics playing",
                subtitle = lyricsState.message,
            )
            is UiState.Success -> {
                if (lyricsState.data == null || lines.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.MusicNote,
                        title = "No lyrics found",
                        subtitle = "LRCLIB had no match for this track",
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            top = 40.dp,
                            bottom = 160.dp,
                            start = 20.dp,
                            end = 20.dp,
                        ),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(lines, key = { index, line -> "${line.timeMs}_$index" }) { index, line ->
                            val isActive = index == currentLineIndex
                            val isSelected = index in uiState.selectedIndices
                            var fillFraction = 0f
                            if (isActive && lyricsState.data.isSynced && line.words.isEmpty()) {
                                val nextTime = lines.getOrNull(index + 1)?.timeMs ?: (line.timeMs + 6_000)
                                val span = (nextTime - line.timeMs).coerceAtLeast(1)
                                fillFraction = ((positionMs - line.timeMs).toFloat() / span).coerceIn(0f, 1f)
                            }
                            LyricLineItem(
                                text = uiState.scriptTextFor(line),
                                roman = if (uiState.scriptMode == ScriptMode.BOTH) line.romanized else null,
                                isActive = isActive,
                                fillFraction = fillFraction,
                                isSelected = isSelected,
                                selectMode = uiState.selectMode,
                                words = if (isActive && uiState.scriptMode != ScriptMode.ROMAN) line.words else emptyList(),
                                positionMs = positionMs,
                                onClick = {
                                    if (uiState.selectMode) viewModel.toggleSelected(index)
                                    else viewModel.player.seekTo(line.timeMs)
                                },
                                onLongClick = { viewModel.toggleSelectMode() },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun com.punjabibeats.app.ui.screens.lyrics.LyricsUiState.scriptTextFor(
    line: com.punjabibeats.app.domain.model.LyricLine,
): String = when (scriptMode) {
    ScriptMode.GURMUKHI -> line.text
    ScriptMode.ROMAN -> line.romanized ?: line.text
    ScriptMode.BOTH -> line.text
}

@Composable
private fun LyricLineItem(
    text: String,
    roman: String?,
    isActive: Boolean,
    fillFraction: Float,
    isSelected: Boolean,
    selectMode: Boolean,
    words: List<TimedWord>,
    positionMs: Long,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> AccentGold.copy(alpha = 0.18f)
                    isActive -> Color.White.copy(alpha = 0.04f)
                    else -> Color.Transparent
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 6.dp, horizontal = 10.dp),
    ) {
        when {
            isActive && words.isNotEmpty() -> WordKaraokeLine(words, positionMs)
            isActive && fillFraction > 0f -> {
                Box {
                    Text(
                        text,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Start,
                    )
                    Text(
                        text,
                        style = MaterialTheme.typography.titleLarge,
                        color = AccentGold,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth(fillFraction)
                            .clipToBounds(),
                    )
                }
            }
            else -> Text(
                text,
                style = MaterialTheme.typography.titleLarge,
                color = if (isActive) AccentGold else TextSecondary,
                textAlign = TextAlign.Start,
            )
        }
        if (roman != null) {
            Text(
                roman,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary.copy(alpha = 0.8f),
                textAlign = TextAlign.Start,
            )
        }
        if (selectMode) {
            Text(
                if (isSelected) "✓ selected — tap to unselect" else "tap to select for share card",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) AccentGold else TextSecondary.copy(alpha = 0.6f),
            )
        }
    }
}

/**
 * Word-by-word karaoke line (Better-Lyrics pattern): sung words turn gold,
 * upcoming ones stay dim.
 */
@Composable
private fun WordKaraokeLine(
    words: List<TimedWord>,
    positionMs: Long,
) {
    val baseStyle = SpanStyle(color = TextSecondary)
    val sungStyle = SpanStyle(color = AccentGold)
    val annotated = buildAnnotatedString {
        words.forEachIndexed { index, word ->
            withStyle(if (positionMs >= word.startMs) sungStyle else baseStyle) {
                append(word.text)
                if (index < words.lastIndex) append(" ")
            }
        }
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Start,
    )
}
