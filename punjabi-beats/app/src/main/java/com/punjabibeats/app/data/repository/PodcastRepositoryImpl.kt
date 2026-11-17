package com.punjabibeats.app.data.repository

import com.punjabibeats.app.core.util.PbJson
import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.data.local.datastore.SettingsDataStore
import com.punjabibeats.app.data.parser.RssParser
import com.punjabibeats.app.data.parser.SubtitleParser
import com.punjabibeats.app.domain.model.PodcastChannel
import com.punjabibeats.app.domain.model.PodcastEpisode
import com.punjabibeats.app.domain.model.TranscriptCue
import com.punjabibeats.app.domain.repository.PodcastRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Podcasts are stored as a JSON list of channels in DataStore; episodes and
 * transcripts are fetched fresh from the RSS/XML sources on demand.
 */
@Singleton
class PodcastRepositoryImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val settings: SettingsDataStore,
) : PodcastRepository {

    override fun observeChannels(): Flow<List<PodcastChannel>> =
        settings.podcastFeedsJson().map { json -> decodeChannels(json) }

    override suspend fun addFeed(feedUrl: String): Result<PodcastChannel> = runSuspendCatching {
        val url = feedUrl.trim()
        require(url.startsWith("http")) { "Enter a valid http(s) RSS feed URL" }
        val xml = fetchText(url)
        require(xml.isNotBlank()) { "The feed returned an empty response" }
        val feed = RssParser.parse(xml)
        require(feed.title.isNotBlank()) { "Could not find a podcast in that feed" }
        val channel = PodcastChannel(
            id = "pod_${url.hashCode().toUInt()}",
            title = feed.title,
            description = feed.description,
            artworkUrl = feed.artworkUrl,
            feedUrl = url,
        )
        val existing = decodeChannels(settings.podcastFeedsJson().first()).filterNot { it.id == channel.id }
        settings.savePodcastFeedsJson(PbJson.json.encodeToString(ChannelListSerializer, existing + channel))
        channel
    }

    override suspend fun removeFeed(channelId: String) {
        val existing = decodeChannels(settings.podcastFeedsJson().first()).filterNot { it.id == channelId }
        settings.savePodcastFeedsJson(PbJson.json.encodeToString(ChannelListSerializer, existing))
    }

    override suspend fun loadEpisodes(channel: PodcastChannel): Result<List<PodcastEpisode>> = runSuspendCatching {
        val xml = fetchText(channel.feedUrl)
        val feed = RssParser.parse(xml)
        feed.episodes.mapIndexedNotNull { index, episode ->
            val streamUrl = episode.streamUrl ?: return@mapIndexedNotNull null
            PodcastEpisode(
                id = "${channel.id}_$index",
                channelTitle = feed.title.ifBlank { channel.title },
                title = episode.title,
                streamUrl = streamUrl,
                durationSec = episode.durationSec,
                pubDate = episode.pubDate,
                description = episode.description,
                transcriptUrl = episode.transcriptUrl,
            )
        }
    }

    override suspend fun searchLocalChannels(query: String): List<PodcastChannel> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        return decodeChannels(settings.podcastFeedsJson().first())
            .filter { it.title.contains(trimmed, ignoreCase = true) || it.description.contains(trimmed, ignoreCase = true) }
    }

    override suspend fun fetchTranscript(url: String): Result<List<TranscriptCue>> = runSuspendCatching {
        SubtitleParser.parse(fetchText(url))
    }

    private suspend fun fetchText(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("User-Agent", "PunjabiBeats/1.0").build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Feed request failed with HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
    }

    private fun decodeChannels(json: String): List<PodcastChannel> =
        runCatching { PbJson.json.decodeFromString(ChannelListSerializer, json) }.getOrDefault(emptyList())

    private companion object {
        val ChannelListSerializer = kotlinx.serialization.builtins.ListSerializer(PodcastChannel.serializer())
    }
}
