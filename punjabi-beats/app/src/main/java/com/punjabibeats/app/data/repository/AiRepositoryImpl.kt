package com.punjabibeats.app.data.repository

import com.punjabibeats.app.core.util.PbJson
import com.punjabibeats.app.core.util.firstObject
import com.punjabibeats.app.core.util.firstString
import com.punjabibeats.app.core.util.objectArray
import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.data.remote.api.BackendApi
import com.punjabibeats.app.domain.model.AiMessage
import com.punjabibeats.app.domain.model.AiPlaylist
import com.punjabibeats.app.domain.model.MusicSource
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.domain.repository.AiRepository
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Talks to the Gemini-backed endpoints on the Vercel backend. Responses are
 * parsed defensively: multiple alias keys are accepted because the prompt
 * schema lives server-side and can evolve independently of the app.
 */
@Singleton
class AiRepositoryImpl @Inject constructor(
    private val backend: BackendApi,
) : AiRepository {

    override suspend fun generatePlaylist(prompt: String): Result<AiPlaylist> = runSuspendCatching {
        val payload = buildJsonObject {
            put("prompt", prompt)
            put("type", "playlist")
            put("language", "punjabi")
            put("count", PLAYLIST_SIZE)
        }.toString()

        val response = backend.aiPlaylist(PbJson.requestBody(payload))
        if (!response.isSuccessful) error("AI playlist endpoint returned HTTP ${response.code()}")
        val body = response.body()?.string().orEmpty()
        val root = PbJson.json.parseToJsonElement(body) as? JsonObject
            ?: error("Malformed AI playlist response")
        val data = root.firstObject("data") ?: root

        val title = data.firstString("title", "name", "playlistName") ?: prompt.take(42)
        val description = data.firstString("description", "subtitle").orEmpty()

        val entries = data.objectArray("songs", "tracks", "items")
            .ifEmpty { root.objectArray("songs", "tracks", "items") }
        val songs = entries.mapNotNull { entry -> entry.toSong() }

        if (songs.isEmpty()) error("The AI could not find playable songs for that prompt")
        AiPlaylist(prompt = prompt, title = title, description = description, songs = songs)
    }

    override suspend fun askAssistant(history: List<AiMessage>): Result<String> = runSuspendCatching {
        val payload = buildJsonObject {
            put("prompt", history.lastOrNull()?.content.orEmpty())
            put("language", "punjabi")
            put("messages", buildJsonArray {
                history.takeLast(HISTORY_WINDOW).forEach { message ->
                    add(buildJsonObject {
                        put("role", message.role.name.lowercase())
                        put("content", message.content)
                    })
                }
            })
        }.toString()

        val response = backend.aiAssistant(PbJson.requestBody(payload))
        if (!response.isSuccessful) error("AI assistant endpoint returned HTTP ${response.code()}")
        val body = response.body()?.string().orEmpty()
        val root = PbJson.json.parseToJsonElement(body) as? JsonObject
            ?: error("Malformed assistant response")
        val data = root.firstObject("data") ?: root
        data.firstString("reply", "response", "message", "text", "answer")
            ?: root.firstString("reply", "response", "message", "text", "answer")
            ?: error("The assistant could not answer right now")
    }

    private fun JsonObject.toSong(): Song? {
        val title = firstString("title", "name", "song") ?: return null
        val videoId = firstString("videoId", "video_id", "youtubeId", "youtube_id", "id")
            ?.takeIf { VIDEO_ID_REGEX.matches(it) }
        return Song(
            id = "ai_${(title + firstString("artist").orEmpty()).hashCode()}",
            title = title,
            artist = firstString("artist", "artistName", "singer") ?: "Punjabi Beats AI",
            artworkUrl = firstString("artwork", "artworkUrl", "image", "thumbnail"),
            source = if (videoId != null) MusicSource.YOUTUBE else MusicSource.JIOSAAVN,
            videoId = videoId,
        )
    }

    private companion object {
        const val PLAYLIST_SIZE = 15
        const val HISTORY_WINDOW = 12
        val VIDEO_ID_REGEX = Regex("[A-Za-z0-9_-]{11}")
    }
}
