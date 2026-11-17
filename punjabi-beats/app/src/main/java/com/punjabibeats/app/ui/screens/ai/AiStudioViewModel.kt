package com.punjabibeats.app.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punjabibeats.app.core.util.UiState
import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.core.util.userMessage
import com.punjabibeats.app.domain.model.AiMessage
import com.punjabibeats.app.domain.model.AiPlaylist
import com.punjabibeats.app.domain.model.AiRole
import com.punjabibeats.app.domain.repository.AiRepository
import com.punjabibeats.app.domain.repository.LibraryRepository
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

data class AiStudioUiState(
    val prompt: String = "",
    val playlistState: UiState<AiPlaylist>? = null,
    val chatInput: String = "",
    val messages: List<AiMessage> = emptyList(),
    val isThinking: Boolean = false,
    val savingPlaylist: Boolean = false,
)

@HiltViewModel
class AiStudioViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiStudioUiState())
    val uiState: StateFlow<AiStudioUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var generateJob: Job? = null

    fun onPromptChange(prompt: String) {
        _uiState.update { it.copy(prompt = prompt) }
    }

    fun generatePlaylist() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isBlank()) {
            _uiState.update { it.copy(playlistState = UiState.Error("Describe the vibe you want first")) }
            return
        }
        generateJob?.cancel()
        _uiState.update { it.copy(playlistState = UiState.Loading) }
        generateJob = viewModelScope.launch {
            aiRepository.generatePlaylist(prompt)
                .onSuccess { playlist ->
                    _uiState.update { it.copy(playlistState = UiState.Success(playlist)) }
                }
                .onFailure { failure ->
                    _uiState.update {
                        it.copy(playlistState = UiState.Error(failure.userMessage()))
                    }
                }
        }
    }

    fun savePlaylistToLibrary() {
        val state = _uiState.value
        if (state.savingPlaylist) return
        val playlist = (state.playlistState as? UiState.Success)?.data ?: return
        _uiState.update { it.copy(savingPlaylist = true) }
        viewModelScope.launch {
            runSuspendCatching {
                val created = libraryRepository.createPlaylist(playlist.title)
                playlist.songs.forEach { song ->
                    libraryRepository.addToPlaylist(created.id, song)
                }
            }
                .onSuccess {
                    _events.tryEmit("Saved \"${playlist.title}\" to your library")
                    _uiState.update { it.copy(savingPlaylist = false) }
                }
                .onFailure {
                    _events.tryEmit(it.userMessage())
                    _uiState.update { it.copy(savingPlaylist = false) }
                }
        }
    }

    fun onChatInputChange(input: String) {
        _uiState.update { it.copy(chatInput = input) }
    }

    fun sendChatMessage() {
        val input = _uiState.value.chatInput.trim()
        if (input.isBlank() || _uiState.value.isThinking) return
        val userMessage = AiMessage(role = AiRole.USER, content = input)
        _uiState.update { it.copy(messages = it.messages + userMessage, chatInput = "", isThinking = true) }
        viewModelScope.launch {
            aiRepository.askAssistant(_uiState.value.messages)
                .onSuccess { reply ->
                    _uiState.update {
                        it.copy(
                            messages = it.messages + AiMessage(role = AiRole.ASSISTANT, content = reply),
                            isThinking = false,
                        )
                    }
                }
                .onFailure { failure ->
                    _uiState.update {
                        it.copy(
                            messages = it.messages + AiMessage(
                                role = AiRole.ASSISTANT,
                                content = "Sorry, I hit a snag: ${failure.userMessage()}",
                            ),
                            isThinking = false,
                        )
                    }
                }
        }
    }
}
