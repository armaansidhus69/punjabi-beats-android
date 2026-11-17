package com.punjabibeats.app.ui.screens.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.core.util.userMessage
import com.punjabibeats.app.domain.model.PodcastChannel
import com.punjabibeats.app.domain.model.PodcastEpisode
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.domain.model.TranscriptCue
import com.punjabibeats.app.domain.repository.PodcastRepository
import com.punjabibeats.app.playback.MusicPlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PodcastUiState(
    val channels: List<PodcastChannel> = emptyList(),
    val selectedChannel: PodcastChannel? = null,
    val episodes: List<PodcastEpisode> = emptyList(),
    val loadingEpisodes: Boolean = false,
    val error: String? = null,
    val speed: Float = 1.0f,
    val transcript: List<TranscriptCue>? = null,
    val transcriptLoading: Boolean = false,
)

@HiltViewModel
class PodcastViewModel @Inject constructor(
    private val podcastRepository: PodcastRepository,
    private val player: MusicPlayerController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PodcastUiState())
    val uiState: StateFlow<PodcastUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var loadEpisodesJob: Job? = null
    private var transcriptJob: Job? = null

    val positionMs: StateFlow<Long> = player.positionMs

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    init {
        viewModelScope.launch {
            podcastRepository.observeChannels().collect { channels ->
                _uiState.update { it.copy(channels = channels) }
            }
        }
        viewModelScope.launch {
            player.playbackSpeed.collect { speed ->
                _uiState.update { it.copy(speed = speed) }
            }
        }
    }

    fun addFeed(url: String) {
        viewModelScope.launch {
            _events.tryEmit("Fetching feed…")
            podcastRepository.addFeed(url)
                .onSuccess { _events.tryEmit("Added \"${it.title}\"") }
                .onFailure { _uiState.update { state -> state.copy(error = it.userMessage()) } }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun removeFeed(channelId: String) {
        viewModelScope.launch {
            runSuspendCatching { podcastRepository.removeFeed(channelId) }
                .onSuccess {
                    if (_uiState.value.selectedChannel?.id == channelId) {
                        _uiState.update { it.copy(selectedChannel = null, episodes = emptyList()) }
                    }
                    _events.tryEmit("Channel removed")
                }
                .onFailure { failure ->
                    _events.tryEmit(failure.userMessage())
                }
        }
    }

    fun selectChannel(channel: PodcastChannel) {
        loadEpisodesJob?.cancel()
        _uiState.update { it.copy(selectedChannel = channel, episodes = emptyList(), loadingEpisodes = true) }
        loadEpisodesJob = viewModelScope.launch {
            podcastRepository.loadEpisodes(channel)
                .onSuccess { episodes ->
                    _uiState.update { it.copy(episodes = episodes, loadingEpisodes = false) }
                }
                .onFailure { failure ->
                    _uiState.update {
                        it.copy(loadingEpisodes = false, error = failure.userMessage())
                    }
                }
        }
    }

    fun closeChannel() {
        loadEpisodesJob?.cancel()
        _uiState.update { it.copy(selectedChannel = null, episodes = emptyList()) }
    }

    fun playEpisode(episode: PodcastEpisode) {
        val channel = _uiState.value.selectedChannel ?: return
        val song = Song(
            id = "pod_${episode.id}",
            title = episode.title,
            artist = episode.channelTitle,
            albumName = channel.title,
            artworkUrl = channel.artworkUrl,
            durationMs = episode.durationSec * 1000L,
            streamUrl = episode.streamUrl,
        )
        player.playQueue(listOf(song), 0)
        _events.tryEmit("Playing at ${_uiState.value.speed}x")
    }

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    fun openTranscript(episode: PodcastEpisode) {
        val url = episode.transcriptUrl ?: run {
            _events.tryEmit("This episode has no transcript in its feed")
            return
        }
        transcriptJob?.cancel()
        _uiState.update { it.copy(transcriptLoading = true, transcript = null) }
        transcriptJob = viewModelScope.launch {
            podcastRepository.fetchTranscript(url)
                .onSuccess { cues ->
                    _uiState.update {
                        it.copy(
                            transcript = cues,
                            transcriptLoading = false,
                            error = if (cues.isEmpty()) "Transcript file was empty" else null,
                        )
                    }
                }
                .onFailure { failure ->
                    _uiState.update {
                        it.copy(transcriptLoading = false, error = failure.userMessage())
                    }
                }
        }
    }

    fun closeTranscript() {
        transcriptJob?.cancel()
        _uiState.update { it.copy(transcript = null, transcriptLoading = false) }
    }
}
