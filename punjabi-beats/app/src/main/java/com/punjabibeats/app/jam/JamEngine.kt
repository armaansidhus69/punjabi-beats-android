package com.punjabibeats.app.jam

import com.punjabibeats.app.core.util.SongCodec
import com.punjabibeats.app.domain.model.JamParticipant
import com.punjabibeats.app.domain.model.JamQueueEntry
import com.punjabibeats.app.domain.model.JamReaction
import com.punjabibeats.app.domain.model.JamRoomSnapshot
import com.punjabibeats.app.domain.model.JamRoomUi
import com.punjabibeats.app.domain.model.JamPhase
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.playback.MusicPlayerController
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Jam Room brain, one instance per joined room. The host owns playback and
 * broadcasts full [JamMessage.HostState] snapshots; guests mirror the host
 * anchor and re-seek whenever their playhead drifts more than [DRIFT_LIMIT_MS]
 * from it. Queue additions are dispatched fair-turn (round robin per member).
 */
class JamEngine(
    val code: String,
    private val me: JamParticipant,
    private val clockSync: JamClockSync,
    private val webSocketFactory: JamTransport.Factory,
    private val pollingFactory: JamTransport.Factory,
    private val playerController: MusicPlayerController,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _roomState = MutableStateFlow(
        JamRoomUi(myId = me.userId)
    )
    val roomState: StateFlow<JamRoomUi> = _roomState.asStateFlow()

    private val _reactions = MutableSharedFlow<JamReaction>(extraBufferCapacity = 32)
    val reactions: SharedFlow<JamReaction> = _reactions.asSharedFlow()

    private var transport: JamTransport? = null
    private var clockOffsetMs = 0L
    private var isHost = false
    private var jobs = mutableListOf<Job>()
    private var broadcastJob: Job? = null
    private var playedCounts = mutableMapOf<String, Int>()
    private var missedHostCycles = 0

    suspend fun join(asHost: Boolean) {
        isHost = asHost
        update { it.copy(phase = JamPhase.CONNECTING) }
        val sample = clockSync.measure()
        clockOffsetMs = sample.offsetMs

        transport = runCatching {
            webSocketFactory.create().also { it.connect(code) }
        }.getOrElse {
            pollingFactory.create().also { it.connect(code) }
        }

        jobs += scope.launch {
            transport?.incoming?.collect { message -> onMessage(message) }
        }
        jobs += scope.launch { heartbeatLoop() }
        jobs += scope.launch { hostBroadcastLoop() }
        jobs += scope.launch { hostAutoAdvanceLoop() }
        jobs += scope.launch { guestSyncLoop() }
        transport?.send(JamMessage.Join(code, me.userId, me.name, me.emoji))
        update {
            it.copy(
                phase = JamPhase.IN_ROOM,
                isHost = isHost,
                transport = transport?.let { t -> transportName(t) }.orEmpty(),
                error = null,
            )
        }
    }

    fun sendReaction(emoji: String) {
        scope.launch {
            _reactions.emit(JamReaction(UUID.randomUUID().toString(), emoji, me.name))
            transport?.send(JamMessage.Reaction(code, me.userId, me.name, emoji))
        }
    }

    fun queueSong(song: Song) {
        scope.launch {
            val entry = JamQueueEntry(
                entryId = UUID.randomUUID().toString(),
                song = song,
                addedByUserId = me.userId,
                addedByName = me.name,
            )
            transport?.send(JamMessage.QueueAdd(code, entry))
            if (isHost) applyQueueAdd(entry)
        }
    }

    fun removeQueued(entryId: String) {
        scope.launch {
            transport?.send(JamMessage.QueueRemove(code, entryId, me.userId))
            if (isHost) applyQueueRemove(entryId)
        }
    }

    fun hostTogglePlay() {
        if (!isHost) return
        playerController.playPause()
        broadcastState()
    }

    fun hostSkip() {
        if (!isHost) return
        startNextFairEntry()
    }

    fun leave() = close()

    fun close() {
        val currentTransport = transport
        transport = null
        broadcastJob?.cancel()
        broadcastJob = null
        jobs.forEach { it.cancel() }
        jobs.clear()
        scope.cancel()
        _roomState.value = _roomState.value.copy(phase = JamPhase.IDLE)
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            runCatching {
                currentTransport?.send(JamMessage.Leave(code, me.userId))
                currentTransport?.close()
            }
        }
    }

    // ----------------------------------------------------------------- inbound

    private fun onMessage(message: JamMessage) {
        when (message) {
            is JamMessage.Join -> if (isHost) onPeerJoined(message)
            is JamMessage.HostState -> if (!isHost && message.hostId != me.userId) onHostState(message)
            is JamMessage.Reaction -> _reactions.tryEmit(
                JamReaction(UUID.randomUUID().toString(), message.emoji, message.name)
            )
            is JamMessage.QueueAdd -> if (isHost) applyQueueAdd(message.entry)
            is JamMessage.QueueRemove -> if (isHost) applyQueueRemove(message.entryId)
            is JamMessage.Leave -> if (isHost) onPeerLeft(message.userId)
            is JamMessage.Ping -> Unit
        }
    }

    private fun onPeerJoined(message: JamMessage.Join) {
        if (message.userId == me.userId) return
        val now = serverNow()
        update { current ->
            val others = current.snapshot?.participants.orEmpty().filterNot { it.userId == message.userId }
            val updatedParticipants = others + JamParticipant(
                userId = message.userId,
                name = message.name.ifBlank { "Guest" },
                emoji = message.emoji,
                isHost = message.userId == current.snapshot?.hostId,
                isMe = false,
                lastSeenMs = now,
            )
            current.copy(
                snapshot = (current.snapshot ?: emptySnapshot()).copy(participants = updatedParticipants)
            )
        }
        broadcastState()
    }

    private fun onPeerLeft(userId: String) {
        update { current ->
            val participants = current.snapshot?.participants.orEmpty().filterNot { it.userId == userId }
            current.copy(snapshot = (current.snapshot ?: emptySnapshot()).copy(participants = participants))
        }
        broadcastState()
    }

    private fun onHostState(state: JamMessage.HostState) {
        missedHostCycles = 0
        val song = state.songJson?.let { SongCodec.decode(it) }
        val participants = state.participants
            .map { it.copy(isMe = it.userId == me.userId, lastSeenMs = serverNow()) }
            .let { list ->
                if (list.none { it.isMe }) {
                    list + me.copy(isHost = false, isMe = true, lastSeenMs = serverNow())
                } else list
            }
        val snapshot = JamRoomSnapshot(
            code = state.code,
            hostId = state.hostId,
            nowPlaying = song,
            anchorPositionMs = state.anchorPositionMs,
            anchorServerMs = state.anchorServerMs,
            isPlaying = state.isPlaying,
            participants = participants,
            queue = state.queue,
        )
        update { it.copy(snapshot = snapshot, phase = JamPhase.IN_ROOM, isHost = false) }
        syncPlaybackToAnchor(snapshot)
    }

    // ------------------------------------------------------------------ loops

    private suspend fun heartbeatLoop() {
        while (scope.isActive) {
            delay(HEARTBEAT_MS)
            transport?.send(JamMessage.Ping(code, me.userId))
            pruneStaleParticipants()
        }
    }

    private suspend fun hostBroadcastLoop() {
        while (scope.isActive) {
            if (isHost) broadcastState()
            delay(HOST_BROADCAST_MS)
        }
    }

    private suspend fun hostAutoAdvanceLoop() {
        var wasPlaying = false
        while (scope.isActive) {
            delay(1000)
            if (!isHost) {
                wasPlaying = false
                continue
            }
            val isPlaying = playerController.isPlaying.value
            val pos = playerController.positionMs.value
            val dur = playerController.durationMs.value
            if (isPlaying) {
                wasPlaying = true
            } else if (wasPlaying && dur > 0 && pos >= (dur - 2000L)) {
                wasPlaying = false
                startNextFairEntry()
            }
        }
    }

    private suspend fun guestSyncLoop() {
        while (scope.isActive) {
            delay(GUEST_SYNC_MS)
            val snapshot = _roomState.value.snapshot
            if (isHost || snapshot == null) continue
            missedHostCycles++
            if (missedHostCycles * GUEST_SYNC_MS > HOST_TIMEOUT_MS) {
                promoteSelfIfEldest()
                continue
            }
            val drift = measureDrift(snapshot)
            update { it.copy(driftMs = drift) }
        }
    }

    // ------------------------------------------------------------ host duties

    private fun broadcastState() {
        if (!isHost) return
        val controller = playerController
        val song = controller.currentSong.value
        val snapshot = _roomState.value.snapshot ?: emptySnapshot()
        val participants = (snapshot.participants + me.copy(
            isHost = true,
            isMe = true,
            lastSeenMs = serverNow(),
        ))
            .distinctBy { it.userId }
            .filter { serverNow() - it.lastSeenMs < PRESENCE_TTL_MS }

        val message = JamMessage.HostState(
            code = code,
            hostId = me.userId,
            songJson = song?.let { SongCodec.encode(it) },
            anchorPositionMs = controller.positionMs.value,
            anchorServerMs = serverNow(),
            isPlaying = controller.isPlaying.value,
            participants = participants,
            queue = snapshot.queue,
        )
        broadcastJob?.cancel()
        broadcastJob = scope.launch { transport?.send(message) }
        update {
            it.copy(
                snapshot = JamRoomSnapshot(
                    code = code,
                    hostId = me.userId,
                    nowPlaying = song,
                    anchorPositionMs = message.anchorPositionMs,
                    anchorServerMs = message.anchorServerMs,
                    isPlaying = message.isPlaying,
                    participants = participants,
                    queue = snapshot.queue,
                ),
                isHost = true,
                driftMs = 0L,
            )
        }
    }

    private fun applyQueueAdd(entry: JamQueueEntry) {
        update { current ->
            val snapshot = current.snapshot ?: emptySnapshot()
            if (snapshot.queue.any { it.entryId == entry.entryId }) return@update current
            val queue = snapshot.queue + entry
            val playing = snapshot.nowPlaying
            if (playing == null) {
                val markedQueue = queue.map { if (it.entryId == entry.entryId) it.copy(played = true) else it }
                current.copy(
                    snapshot = snapshot.copy(queue = markedQueue, nowPlaying = entry.song, anchorPositionMs = 0L)
                ).also { startEntry(entry) }
            } else {
                current.copy(snapshot = snapshot.copy(queue = queue))
            }
        }
        if (isHost) broadcastState()
    }

    private fun applyQueueRemove(entryId: String) {
        update { current ->
            val snapshot = current.snapshot ?: return@update current
            current.copy(snapshot = snapshot.copy(queue = snapshot.queue.filterNot { entry -> entry.entryId == entryId }))
        }
        if (isHost) broadcastState()
    }

    private fun startNextFairEntry() {
        if (!isHost) return
        val snapshot = _roomState.value.snapshot ?: return
        val remaining = snapshot.queue.filter { !it.played }
        val next = remaining.minByOrNull { entry ->
            (playedCounts[entry.addedByUserId] ?: 0) * 10_000L + snapshot.queue.indexOf(entry)
        }
        if (next == null) {
            playerController.pause()
            broadcastState()
            return
        }
        playedCounts[next.addedByUserId] = (playedCounts[next.addedByUserId] ?: 0) + 1
        update { current ->
            val snap = current.snapshot ?: return@update current
            current.copy(
                snapshot = snap.copy(
                    queue = snap.queue.map { entry ->
                        if (entry.entryId == next.entryId) entry.copy(played = true) else entry
                    }
                )
            )
        }
        startEntry(next)
        broadcastState()
    }

    private fun startEntry(entry: JamQueueEntry) {
        playerController.playQueue(listOf(entry.song), 0)
    }

    private fun promoteSelfIfEldest() {
        val snapshot = _roomState.value.snapshot ?: return
        val hostAlive = snapshot.participants.any { it.userId == snapshot.hostId }
        if (hostAlive) return
        val eldest = snapshot.participants.minByOrNull { it.userId } ?: return
        if (eldest.userId == me.userId) {
            isHost = true
            update { it.copy(isHost = true) }
            broadcastState()
        }
    }

    // ----------------------------------------------------------------- guests

    private fun syncPlaybackToAnchor(snapshot: JamRoomSnapshot) {
        val song = snapshot.nowPlaying ?: return
        val expectedMs = expectedPositionMs(snapshot)
        val playing = playerController.currentSong.value
        if (playing?.id != song.id) {
            playerController.playQueue(listOf(song), 0)
            playerController.seekTo(expectedMs)
            if (snapshot.isPlaying) playerController.play() else playerController.pause()
            update { it.copy(driftMs = 0L) }
            return
        }
        if (playerController.isPlaying.value != snapshot.isPlaying) {
            if (snapshot.isPlaying) playerController.play() else playerController.pause()
        }
        val drift = playerController.positionMs.value - expectedMs
        if (abs(drift) > DRIFT_LIMIT_MS && snapshot.isPlaying) {
            playerController.seekTo(expectedMs)
            update { it.copy(driftMs = 0L) }
        } else {
            update { it.copy(driftMs = drift) }
        }
    }

    private fun measureDrift(snapshot: JamRoomSnapshot): Long =
        playerController.positionMs.value - expectedPositionMs(snapshot)

    private fun expectedPositionMs(snapshot: JamRoomSnapshot): Long {
        if (!snapshot.isPlaying) return snapshot.anchorPositionMs
        return (serverNow() - snapshot.anchorServerMs) + snapshot.anchorPositionMs
    }

    // ------------------------------------------------------------------ utils

    private fun pruneStaleParticipants() {
        update { current ->
            val snapshot = current.snapshot ?: return@update current
            val fresh = snapshot.participants.filter { serverNow() - it.lastSeenMs < PRESENCE_TTL_MS || it.isMe }
            current.copy(snapshot = snapshot.copy(participants = fresh))
        }
    }

    private fun serverNow(): Long = clockSync.serverNow(clockOffsetMs)

    private fun emptySnapshot() = JamRoomSnapshot(code = code, hostId = me.userId)

    private fun transportName(transport: JamTransport): String = when (transport) {
        is WebSocketTransport -> "WebSocket"
        is PollingTransport -> "REST polling"
        else -> "unknown"
    }

    private inline fun update(reducer: (JamRoomUi) -> JamRoomUi) {
        _roomState.value = reducer(_roomState.value)
    }

    companion object {
        const val DRIFT_LIMIT_MS = 400L
        const val HEARTBEAT_MS = 10_000L
        const val HOST_BROADCAST_MS = 4_000L
        const val GUEST_SYNC_MS = 2_000L
        const val HOST_TIMEOUT_MS = 40_000L
        const val PRESENCE_TTL_MS = 35_000L
    }
}
