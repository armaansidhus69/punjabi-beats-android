package com.punjabibeats.app.ui.screens.jam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punjabibeats.app.data.local.db.JamDao
import com.punjabibeats.app.data.local.db.JamRoomHistoryEntity
import com.punjabibeats.app.data.local.datastore.SettingsDataStore
import com.punjabibeats.app.data.remote.api.BackendApi
import com.punjabibeats.app.domain.model.JamReaction
import com.punjabibeats.app.domain.model.JamRoomUi
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.jam.JamEngine
import com.punjabibeats.app.jam.JamEngineFactory
import com.punjabibeats.app.domain.repository.SongRepository
import com.punjabibeats.app.playback.MusicPlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class JamUiState(
    val engineState: JamRoomUi = JamRoomUi(),
    val recentRooms: List<String> = emptyList(),
    val connecting: Boolean = false,
    val error: String? = null,
    val searchResults: List<Song> = emptyList(),
)

@HiltViewModel
class JamViewModel @Inject constructor(
    private val jamEngineFactory: JamEngineFactory,
    private val jamDao: JamDao,
    private val settings: SettingsDataStore,
    private val backend: BackendApi,
    private val songRepository: SongRepository,
    val player: MusicPlayerController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JamUiState())
    val uiState: StateFlow<JamUiState> = _uiState.asStateFlow()

    private val _reactions = MutableStateFlow<JamReaction?>(null)
    val reactions: StateFlow<JamReaction?> = _reactions.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var engine: JamEngine? = null
    private var roomStateJob: kotlinx.coroutines.Job? = null
    private var reactionsJob: kotlinx.coroutines.Job? = null
    private var searchJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            jamDao.recentRooms().collect { rooms ->
                _uiState.update { it.copy(recentRooms = rooms.map { entry -> entry.code }) }
            }
        }
        viewModelScope.launch {
            settings.popPendingJamCode()?.let { code -> joinRoom(code) }
        }
    }

    val roomActive: Boolean get() = engine != null

    fun createRoom() = joinRoom(generateRoomCode(), asHost = true)

    fun joinRoom(codeInput: String, asHost: Boolean = false) {
        val code = codeInput.trim().uppercase()
        if (!ROOM_CODE_REGEX.matches(code)) {
            _uiState.update { it.copy(error = "Room codes are 6 letters/digits") }
            return
        }
        if (engine != null) {
            _uiState.update { it.copy(error = "Leave the current room first") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(connecting = true, error = null) }
            roomStateJob?.cancel()
            reactionsJob?.cancel()
            var pendingEngine: JamEngine? = null
            try {
                val newEngine = jamEngineFactory.create(code)
                pendingEngine = newEngine
                newEngine.join(asHost = asHost)
                engine = newEngine
                roomStateJob = viewModelScope.launch {
                    newEngine.roomState.collect { roomUi ->
                        _uiState.update { it.copy(engineState = roomUi) }
                    }
                }
                reactionsJob = viewModelScope.launch {
                    newEngine.reactions.collect { reaction -> _reactions.value = reaction }
                }
                jamDao.upsert(JamRoomHistoryEntity(code = code))
                settings.saveLastJamCode(code)
            } catch (failure: Exception) {
                pendingEngine?.close()
                engine?.close()
                engine = null
                roomStateJob?.cancel()
                roomStateJob = null
                reactionsJob?.cancel()
                reactionsJob = null
                _uiState.update { it.copy(connecting = false, error = failure.message) }
            } finally {
                _uiState.update { it.copy(connecting = false) }
            }
        }
    }

    fun leaveRoom() {
        roomStateJob?.cancel()
        roomStateJob = null
        reactionsJob?.cancel()
        reactionsJob = null
        searchJob?.cancel()
        searchJob = null
        engine?.close()
        engine = null
        _uiState.update {
            JamUiState(recentRooms = it.recentRooms, error = null)
        }
    }

    fun sendReaction(emoji: String) = engine?.sendReaction(emoji)

    fun queueSong(song: Song) {
        engine?.queueSong(song)
        _uiState.update { it.copy(searchResults = emptyList()) }
    }

    fun removeQueued(entryId: String) = engine?.removeQueued(entryId)

    fun hostTogglePlay() = engine?.hostTogglePlay()

    fun hostSkip() = engine?.hostSkip()

    fun searchSongsForQueue(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(250)
            val results = songRepository.searchSongs(query, limit = 12)
            _uiState.update { it.copy(searchResults = results) }
        }
    }

    /**
     * Shares the room via /api/share (server-generated card when available),
     * falling back to the plain deep link.
     */
    fun shareRoom(onResult: (String) -> Unit) {
        val code = _uiState.value.engineState.snapshot?.code ?: return
        val deepLink = "punjabibeats://jam/$code"
        viewModelScope.launch {
            val shared = runCatching {
                val payload = com.punjabibeats.app.core.util.PbJson.requestBody(
                    """{"type":"jam_room","code":"$code","link":"$deepLink"}"""
                )
                val response = backend.share(payload)
                if (!response.isSuccessful) error("share failed")
                response.body()?.string()
                    ?.let { com.punjabibeats.app.core.util.PbJson.json.parseToJsonElement(it) }
                    ?.let { element ->
                        (element as? kotlinx.serialization.json.JsonObject)
                            ?.let { it["url"] ?: it["cardUrl"] ?: it["link"] }
                            ?.let { primitive ->
                                (primitive as? kotlinx.serialization.json.JsonPrimitive)?.content
                            }
                    }
            }.getOrNull()
            _events.tryEmit("Room link ready — invite your crew!")
            onResult(shared ?: deepLink)
        }
    }

    private fun generateRoomCode(): String {
        val alphabet = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        return (1..ROOM_CODE_LENGTH).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
    }

    override fun onCleared() {
        roomStateJob?.cancel()
        reactionsJob?.cancel()
        searchJob?.cancel()
        engine?.close()
        engine = null
        super.onCleared()
    }

    companion object {
        const val ROOM_CODE_LENGTH = 6
        val ROOM_CODE_REGEX = Regex("[A-Z0-9]{6}")
    }
}
