package com.punjabibeats.app.jam

import com.punjabibeats.app.BuildConfig
import com.punjabibeats.app.core.util.PbJson
import com.punjabibeats.app.core.util.firstArray
import com.punjabibeats.app.data.remote.api.BackendApi
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/** Pluggable realtime transport for a Jam Room. */
interface JamTransport {
    val incoming: SharedFlow<JamMessage>
    suspend fun connect(code: String)
    suspend fun send(message: JamMessage): Boolean
    fun close()

    interface Factory {
        val name: String
        fun create(): JamTransport
    }
}

/** Primary transport: OkHttp WebSocket against the backend. */
class WebSocketTransport(
    private val okHttpClient: OkHttpClient,
) : JamTransport {

    private val _incoming = MutableSharedFlow<JamMessage>(extraBufferCapacity = 64)
    override val incoming: SharedFlow<JamMessage> = _incoming.asSharedFlow()

    private var webSocket: WebSocket? = null

    override suspend fun connect(code: String) {
        val request = Request.Builder()
            .url("${BuildConfig.JAM_WS_URL}?code=$code")
            .build()
        webSocket = okHttpClient.newWebSocket(request, Listener())
    }

    override suspend fun send(message: JamMessage): Boolean {
        val socket = webSocket ?: return false
        return runCatching { socket.send(PbJsonWrapper.encode(message)) }.getOrDefault(false)
    }

    override fun close() {
        runCatching { webSocket?.close(1000, "bye") }
        webSocket = null
    }

    private inner class Listener : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            PbJsonWrapper.decode(text)?.let { message ->
                _incoming.tryEmit(message)
            }
        }
    }

    class Factory @Inject constructor(private val okHttpClient: OkHttpClient) : JamTransport.Factory {
        override val name: String = "websocket"
        override fun create(): JamTransport = WebSocketTransport(okHttpClient)
    }
}

/**
 * Fallback transport: REST polling against /api/publish. Every send is a POST
 * of the message; a background loop GETs the room's message list. Works on any
 * deployment that implements the publish contract, no WebSocket needed.
 */
class PollingTransport(
    private val backend: BackendApi,
) : JamTransport {

    private val _incoming = MutableSharedFlow<JamMessage>(extraBufferCapacity = 64)
    override val incoming: SharedFlow<JamMessage> = _incoming.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var code: String = ""
    private var pollJob: kotlinx.coroutines.Job? = null

    override suspend fun connect(code: String) {
        this.code = code
        pollJob = scope.launch {
            while (isActive) {
                runCatching { fetchOnce() }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    override suspend fun send(message: JamMessage): Boolean = runCatching {
        val payload = PbJson.json.encodeToString(JamMessage.serializer(), message)
        val body = PbJson.requestBody(payload)
        backend.publish(body).isSuccessful
    }.getOrDefault(false)

    private suspend fun fetchOnce() {
        val response = backend.publishedState(code)
        if (!response.isSuccessful) return
        val body = response.body()?.string().orEmpty()
        if (body.isBlank()) return
        val root = runCatching { PbJson.json.parseToJsonElement(body) as? JsonObject }.getOrNull() ?: return
        root.firstArray("messages", "items").forEach { element ->
            val raw = (element as? JsonPrimitive)?.content ?: return@forEach
            PbJsonWrapper.decode(raw)?.let { _incoming.tryEmit(it) }
        }
    }

    override fun close() {
        pollJob?.cancel()
        pollJob = null
    }

    class Factory @Inject constructor(private val backend: BackendApi) : JamTransport.Factory {
        override val name: String = "polling"
        override fun create(): JamTransport = PollingTransport(backend)
    }

    private companion object {
        const val POLL_INTERVAL_MS = 2_000L
    }
}

/** Thin helper so transports share one encode/decode path. */
internal object PbJsonWrapper {
    fun encode(message: JamMessage): String =
        runCatching { PbJson.json.encodeToString(JamMessage.serializer(), message) }.getOrDefault("")

    fun decode(text: String): JamMessage? =
        runCatching { PbJson.json.decodeFromString(JamMessage.serializer(), text) }.getOrNull()
}
