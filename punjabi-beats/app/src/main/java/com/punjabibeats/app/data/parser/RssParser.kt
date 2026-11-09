package com.punjabibeats.app.data.parser

import com.punjabibeats.app.core.util.unescapeHtml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/** Streaming RSS/itunes podcast feed parser (no third-party dependency). */
object RssParser {

    data class RssEpisode(
        val title: String,
        val streamUrl: String?,
        val durationSec: Long,
        val pubDate: String,
        val description: String,
        val transcriptUrl: String?,
    )

    data class RssFeed(
        val title: String,
        val description: String,
        val artworkUrl: String?,
        val episodes: List<RssEpisode>,
    )

    fun parse(xml: String): RssFeed {
        val parser = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = false
        }.newPullParser()
        parser.setInput(xml.reader())

        var channelTitle = ""
        var channelDescription = ""
        var channelArtwork: String? = null
        val episodes = mutableListOf<RssEpisode>()

        var inItem = false
        var inImage = false
        var currentTitle = ""
        var currentDescription = ""
        var currentStreamUrl: String? = null
        var currentDurationSec = 0L
        var currentPubDate = ""
        var currentTranscriptUrl: String? = null
        val currentText = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    currentText.setLength(0)
                    val name = parser.name.substringAfter(':')
                    when (name) {
                        "item" -> {
                            inItem = true
                            currentTitle = ""
                            currentDescription = ""
                            currentStreamUrl = null
                            currentDurationSec = 0L
                            currentPubDate = ""
                            currentTranscriptUrl = null
                        }
                        "image" -> if (!inItem) inImage = true
                        "enclosure" -> {
                            val url = parser.getAttributeValue(null, "url")
                            val type = parser.getAttributeValue(null, "type").orEmpty()
                            if (url != null && (type.contains("audio", true) || currentStreamUrl == null)) {
                                currentStreamUrl = url
                            }
                        }
                        "transcript" -> currentTranscriptUrl = parser.getAttributeValue(null, "url")
                        else -> Unit
                    }
                    if (name == "image") {
                        val href = parser.getAttributeValue(null, "href")
                        if (href != null && !inItem) channelArtwork = href
                    }
                }
                XmlPullParser.TEXT -> {
                    parser.text?.let { currentText.append(it) }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name.substringAfter(':')
                    val tagText = currentText.toString().trim()
                    when (name) {
                        "item" -> {
                            inItem = false
                            if (currentTitle.isNotBlank() && currentStreamUrl != null) {
                                episodes += RssEpisode(
                                    title = unescapeHtml(currentTitle),
                                    streamUrl = currentStreamUrl,
                                    durationSec = currentDurationSec,
                                    pubDate = currentPubDate,
                                    description = currentDescription,
                                    transcriptUrl = currentTranscriptUrl,
                                )
                            }
                        }
                        "image" -> if (!inItem) inImage = false
                        "title" -> when {
                            inItem -> currentTitle = tagText
                            inImage -> channelArtwork = tagText
                            else -> channelTitle = unescapeHtml(tagText)
                        }
                        "description" -> if (inItem) {
                            currentDescription = tagText
                        } else if (channelDescription.isBlank()) {
                            channelDescription = unescapeHtml(tagText)
                        }
                        "pubDate" -> if (inItem) currentPubDate = tagText
                        "duration" -> if (inItem) currentDurationSec = parseDurationSec(tagText)
                    }
                    currentText.setLength(0)
                }
            }
            event = parser.next()
        }
        return RssFeed(channelTitle, channelDescription, channelArtwork, episodes)
    }

    private fun parseDurationSec(raw: String): Long {
        val text = raw.trim()
        if (text.isBlank()) return 0L
        if (text.toLongOrNull() != null) return text.toLongOrNull() ?: 0L
        val parts = text.split(":").map { it.trim().toLongOrNull() ?: 0L }
        return when (parts.size) {
            3 -> parts[0] * 3600L + parts[1] * 60L + parts[2]
            2 -> parts[0] * 60L + parts[1]
            else -> 0L
        }
    }
}
