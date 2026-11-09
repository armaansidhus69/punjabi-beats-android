package com.punjabibeats.app.data.cipher

import com.punjabibeats.app.core.util.PbJson
import com.punjabibeats.app.core.util.firstArray
import com.punjabibeats.app.core.util.firstInt
import com.punjabibeats.app.core.util.firstString
import com.punjabibeats.app.core.util.objectArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Descriptor for one InnerTube client used against music.youtube.com.
 * Values live in [player_configs] so a rotation can be healed remotely
 * without shipping a new APK (the zemer-cipher self-healing pattern).
 */
@Serializable
data class InnerTubeClientSpec(
    val id: String,
    val clientName: String,
    val clientVersion: String,
    val clientField: Int,
    val userAgent: String,
    val origin: String,
    val referer: String,
)

/**
 * One known player JS build. [playerId] is the hex hash of the player script,
 * [signatureTimestamp] is the `signatureTimestamp` the WEB-family clients must
 * echo in their player requests. Decipher ops are NOT stored here — they are
 * extracted from the live player JS at runtime (WebView execution), which is
 * what keeps this config small and rotation-proof.
 */
@Serializable
data class PlayerSpec(
    val playerId: String,
    val signatureTimestamp: Int,
    val playerJsUrl: String,
    val nTransformUrlClasses: List<String>,
    val requiresPoToken: Boolean,
)

/** Root of assets/player_configs.json (and of the remotely refreshed copy). */
@Serializable
data class CipherConfig(
    val version: Int,
    val remoteConfigUrl: String,
    val configTtlHours: Long,
    val clients: List<InnerTubeClientSpec>,
    val players: List<PlayerSpec>,
    /** Operator-provisioned content-binding PoToken, empty when none is set. */
    val poToken: String,
) {
    fun client(id: String): InnerTubeClientSpec? = clients.firstOrNull { it.id == id }

    fun latestPlayer(): PlayerSpec? = players.maxByOrNull { it.signatureTimestamp }

    companion object {
        fun default(configUrl: String) = CipherConfig(
            version = 0,
            remoteConfigUrl = configUrl,
            configTtlHours = 6,
            clients = listOf(
                InnerTubeClientSpec(
                    id = "WEB_REMIX",
                    clientName = "WEB_REMIX",
                    clientVersion = "1.20240403.01.00",
                    clientField = 67,
                    userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36,gzip(gfe)",
                    origin = "https://music.youtube.com",
                    referer = "https://music.youtube.com/",
                ),
            ),
            players = emptyList(),
            poToken = "",
        )
    }
}

/**
 * Lenient + regex-locked parser for the cipher configuration, following the
 * zemer-cipher `PlayerConfigParser` idea: every field must match a strict
 * pattern before it is accepted, so a poisoned remote config can never inject
 * arbitrary values into request headers or WebView scripts.
 */
object PlayerConfigParser {

    private val PLAYER_ID = Regex("^[0-9a-f]{8,64}$")
    private val TIMESTAMP = Regex("^[0-9]{9,12}$")
    private val HTTPS_URL = Regex("^https://[A-Za-z0-9._~:/?#[\\]@!$&'()*+,;=%-]+$")
    private val URL_CLASS = Regex("^[A-Za-z0-9.*?&=_:/-]{1,120}$")
    private val CLIENT_ID = Regex("^[A-Z0-9_]{3,32}$")
    private val CLIENT_VERSION = Regex("^[0-9.]{1,32}$")
    private val PO_TOKEN = Regex("^[A-Za-z0-9_-]{1,256}$")

    fun parse(raw: String, fallbackConfigUrl: String): CipherConfig? = runCatching {
        val root = PbJson.json.parseToJsonElement(raw) as? JsonObject ?: return null
        val version = root.firstInt("version") ?: 0
        val clients = root.objectArray("clients").mapNotNull { it.toClientSpec() }
        val players = root.objectArray("players").mapNotNull { it.toPlayerSpec() }
        if (clients.isEmpty()) return null
        CipherConfig(
            version = version,
            remoteConfigUrl = root.firstString("remoteConfigUrl")
                ?.takeIf { HTTPS_URL.matches(it) } ?: fallbackConfigUrl,
            configTtlHours = (root.firstInt("configTtlHours") ?: 6).coerceIn(1, 168).toLong(),
            clients = clients,
            players = players,
            poToken = root.firstString("poToken")?.takeIf { PO_TOKEN.matches(it) }.orEmpty(),
        )
    }.getOrNull()

    private fun JsonObject.toClientSpec(): InnerTubeClientSpec? {
        val id = firstString("id")?.takeIf { CLIENT_ID.matches(it) } ?: return null
        val clientName = firstString("clientName")?.takeIf { CLIENT_ID.matches(it) } ?: return null
        val clientVersion = firstString("clientVersion")?.takeIf { CLIENT_VERSION.matches(it) } ?: return null
        return InnerTubeClientSpec(
            id = id,
            clientName = clientName,
            clientVersion = clientVersion,
            clientField = firstInt("clientField") ?: 67,
            userAgent = firstString("userAgent")?.takeIf { it.length <= 300 }
                ?: DEFAULT_USER_AGENT,
            origin = firstString("origin")?.takeIf { HTTPS_URL.matches(it) }
                ?: "https://music.youtube.com",
            referer = firstString("referer")?.takeIf { HTTPS_URL.matches(it) }
                ?: "https://music.youtube.com/",
        )
    }

    private fun JsonObject.toPlayerSpec(): PlayerSpec? {
        val playerId = firstString("playerId")?.takeIf { PLAYER_ID.matches(it) } ?: return null
        val signatureTimestamp = firstString("signatureTimestamp")
            ?.takeIf { TIMESTAMP.matches(it) }?.toIntOrNull() ?: return null
        val playerJsUrl = firstString("playerJsUrl")?.takeIf { HTTPS_URL.matches(it) }
            ?: "https://www.youtube.com/s/player/$playerId/player_ias.vflset/en_US/base.js"
        return PlayerSpec(
            playerId = playerId,
            signatureTimestamp = signatureTimestamp,
            playerJsUrl = playerJsUrl,
            nTransformUrlClasses = firstArray("nTransformUrlClasses")
                .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                .filter { URL_CLASS.matches(it) },
            requiresPoToken = firstString("requiresPoToken") == "true",
        )
    }

    const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Safari/537.36"
}
