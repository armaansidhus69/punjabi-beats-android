package com.punjabibeats.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.punjabibeats.app.core.util.PbJson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.pbDataStore: DataStore<Preferences> by preferencesDataStore(name = "pb_settings")

data class PersistedQueue(
    val songsJson: String = "",
    val startIndex: Int = 0,
)

/** Small key-value settings: player prefs, last queue for session restore, identity. */
@Singleton
class SettingsDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val LAST_QUEUE_JSON = stringPreferencesKey("last_queue_json")
        val LAST_INDEX = intPreferencesKey("last_index")
        val REPEAT_MODE = intPreferencesKey("repeat_mode")
        val SHUFFLE = booleanPreferencesKey("shuffle")
        val SPEED = floatPreferencesKey("speed")
        val PITCH = floatPreferencesKey("pitch")
        val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        val CROSSFADE_SECONDS = intPreferencesKey("crossfade_seconds")
        val EQUALIZER_ENABLED = booleanPreferencesKey("equalizer_enabled")
        val EQUALIZER_BANDS = stringPreferencesKey("equalizer_bands_json")
        val BASS_BOOST = intPreferencesKey("bass_boost_strength")
        val VIRTUALIZER = intPreferencesKey("virtualizer_strength")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMOJI = stringPreferencesKey("user_emoji")
        val USER_ID = stringPreferencesKey("user_id")
        val LAST_JAM_CODE = stringPreferencesKey("last_jam_code")
        val PENDING_JAM_CODE = stringPreferencesKey("pending_jam_code")
        val PODCAST_FEEDS_JSON = stringPreferencesKey("podcast_feeds_json")
        val THEME_ID = stringPreferencesKey("theme_id")
        val THEME_VERSION = intPreferencesKey("theme_version")
        val SEEKBAR_STYLE = stringPreferencesKey("seekbar_style")
        val TURNTABLE_MODE = booleanPreferencesKey("turntable_mode")
        val DYNAMIC_ISLAND_ENABLED = booleanPreferencesKey("dynamic_island_enabled")
        val CAR_BT_AUTO_LAUNCH = booleanPreferencesKey("car_bt_auto_launch")
        val STREAMING_QUALITY = stringPreferencesKey("streaming_quality")
        val DOLBY_ATMOS = booleanPreferencesKey("dolby_atmos")
        val LYRICS_ALIGNMENT = stringPreferencesKey("lyrics_alignment")
        val LYRICS_TEXT_SIZE = floatPreferencesKey("lyrics_text_size")
        val LYRICS_LINE_SPACING = floatPreferencesKey("lyrics_line_spacing")
        val TOUCH_FEEDBACK = booleanPreferencesKey("touch_feedback")
        val HEARTBEAT_FEEDBACK = booleanPreferencesKey("heartbeat_feedback")
    }

    private val safeData: Flow<Preferences> = context.pbDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }

    fun themeId(): Flow<String> = safeData.map { prefs ->
        val version = prefs[Keys.THEME_VERSION] ?: 1
        val saved = prefs[Keys.THEME_ID]
        // Automatic upgrade: upgrade legacy theme IDs to Sunset Glow ("sunset_glow")
        if (version < 3) {
            "sunset_glow"
        } else {
            saved ?: "sunset_glow"
        }
    }
    suspend fun saveThemeId(themeId: String) = runCatching {
        context.pbDataStore.edit {
            it[Keys.THEME_ID] = themeId
            it[Keys.THEME_VERSION] = 3
        }
    }.isSuccess

    fun seekbarStyle(): Flow<String> = safeData.map { it[Keys.SEEKBAR_STYLE] ?: "classic_waveform" }
    suspend fun saveSeekbarStyle(style: String) = runCatching {
        context.pbDataStore.edit { it[Keys.SEEKBAR_STYLE] = style }
    }.isSuccess

    fun turntableMode(): Flow<Boolean> = safeData.map { it[Keys.TURNTABLE_MODE] ?: true }
    suspend fun saveTurntableMode(enabled: Boolean) = runCatching {
        context.pbDataStore.edit { it[Keys.TURNTABLE_MODE] = enabled }
    }.isSuccess

    fun dynamicIslandEnabled(): Flow<Boolean> = safeData.map { it[Keys.DYNAMIC_ISLAND_ENABLED] ?: true }
    suspend fun saveDynamicIslandEnabled(enabled: Boolean) = runCatching {
        context.pbDataStore.edit { it[Keys.DYNAMIC_ISLAND_ENABLED] = enabled }
    }.isSuccess

    fun carBtAutoLaunch(): Flow<Boolean> = safeData.map { it[Keys.CAR_BT_AUTO_LAUNCH] ?: false }
    suspend fun saveCarBtAutoLaunch(enabled: Boolean) = runCatching {
        context.pbDataStore.edit { it[Keys.CAR_BT_AUTO_LAUNCH] = enabled }
    }.isSuccess

    fun streamingQuality(): Flow<String> = safeData.map { it[Keys.STREAMING_QUALITY] ?: "Hi-Res Lossless (24-bit / 96kHz)" }
    suspend fun saveStreamingQuality(quality: String) = runCatching {
        context.pbDataStore.edit { it[Keys.STREAMING_QUALITY] = quality }
    }.isSuccess

    fun dolbyAtmos(): Flow<Boolean> = safeData.map { it[Keys.DOLBY_ATMOS] ?: true }
    suspend fun saveDolbyAtmos(enabled: Boolean) = runCatching {
        context.pbDataStore.edit { it[Keys.DOLBY_ATMOS] = enabled }
    }.isSuccess

    fun lyricsAlignment(): Flow<String> = safeData.map { it[Keys.LYRICS_ALIGNMENT] ?: "LEFT" }
    suspend fun saveLyricsAlignment(alignment: String) = runCatching {
        context.pbDataStore.edit { it[Keys.LYRICS_ALIGNMENT] = alignment }
    }.isSuccess

    fun lyricsTextSize(): Flow<Float> = safeData.map { it[Keys.LYRICS_TEXT_SIZE] ?: 22f }
    suspend fun saveLyricsTextSize(size: Float) = runCatching {
        context.pbDataStore.edit { it[Keys.LYRICS_TEXT_SIZE] = size.coerceIn(16f, 32f) }
    }.isSuccess

    fun lyricsLineSpacing(): Flow<Float> = safeData.map { it[Keys.LYRICS_LINE_SPACING] ?: 10f }
    suspend fun saveLyricsLineSpacing(spacing: Float) = runCatching {
        context.pbDataStore.edit { it[Keys.LYRICS_LINE_SPACING] = spacing.coerceIn(4f, 24f) }
    }.isSuccess

    fun touchFeedback(): Flow<Boolean> = safeData.map { it[Keys.TOUCH_FEEDBACK] ?: true }
    suspend fun saveTouchFeedback(enabled: Boolean) = runCatching {
        context.pbDataStore.edit { it[Keys.TOUCH_FEEDBACK] = enabled }
    }.isSuccess

    fun heartbeatFeedback(): Flow<Boolean> = safeData.map { it[Keys.HEARTBEAT_FEEDBACK] ?: true }
    suspend fun saveHeartbeatFeedback(enabled: Boolean) = runCatching {
        context.pbDataStore.edit { it[Keys.HEARTBEAT_FEEDBACK] = enabled }
    }.isSuccess

    /** Jam room code arriving via a punjabibeats://jam/… deep link, consumed once. */
    suspend fun savePendingJamCode(code: String) = runCatching {
        context.pbDataStore.edit { it[Keys.PENDING_JAM_CODE] = code }
    }.isSuccess

    suspend fun popPendingJamCode(): String? {
        val code = runCatching { safeData.map { it[Keys.PENDING_JAM_CODE] }.first() }.getOrNull()
        if (!code.isNullOrBlank()) {
            runCatching { context.pbDataStore.edit { it[Keys.PENDING_JAM_CODE] = "" } }
        }
        return code?.takeIf { it.isNotBlank() }
    }

    /** Stable per-install identity used for Jam Room presence. */
    suspend fun ensureUserId(): String {
        val existing = runCatching { safeData.map { it[Keys.USER_ID] }.first() }.getOrNull()
        if (!existing.isNullOrBlank()) return existing
        val fresh = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
        runCatching { context.pbDataStore.edit { it[Keys.USER_ID] = fresh } }
        return fresh
    }

    fun lastQueue(): Flow<PersistedQueue> = safeData.map { prefs ->
        PersistedQueue(
            songsJson = prefs[Keys.LAST_QUEUE_JSON].orEmpty(),
            startIndex = prefs[Keys.LAST_INDEX] ?: 0,
        )
    }

    suspend fun saveQueue(songsJson: String, startIndex: Int) = runCatching {
        context.pbDataStore.edit { prefs ->
            prefs[Keys.LAST_QUEUE_JSON] = songsJson
            prefs[Keys.LAST_INDEX] = startIndex
        }
    }.isSuccess

    fun repeatMode(): Flow<Int> = safeData.map { it[Keys.REPEAT_MODE] ?: 0 }
    suspend fun saveRepeatMode(mode: Int) = runCatching {
        context.pbDataStore.edit { it[Keys.REPEAT_MODE] = mode }
    }.isSuccess

    fun shuffleEnabled(): Flow<Boolean> = safeData.map { it[Keys.SHUFFLE] ?: false }
    suspend fun saveShuffle(enabled: Boolean) = runCatching {
        context.pbDataStore.edit { it[Keys.SHUFFLE] = enabled }
    }.isSuccess

    fun playbackSpeed(): Flow<Float> = safeData.map { it[Keys.SPEED] ?: 1.0f }
    suspend fun savePlaybackSpeed(speed: Float) = runCatching {
        context.pbDataStore.edit { it[Keys.SPEED] = speed }
    }.isSuccess

    fun playbackPitch(): Flow<Float> = safeData.map { it[Keys.PITCH] ?: 1.0f }
    suspend fun savePlaybackPitch(pitch: Float) = runCatching {
        context.pbDataStore.edit { it[Keys.PITCH] = pitch }
    }.isSuccess

    fun skipSilence(): Flow<Boolean> = safeData.map { it[Keys.SKIP_SILENCE] ?: false }
    suspend fun saveSkipSilence(enabled: Boolean) = runCatching {
        context.pbDataStore.edit { it[Keys.SKIP_SILENCE] = enabled }
    }.isSuccess

    fun crossfadeSeconds(): Flow<Int> = safeData.map { it[Keys.CROSSFADE_SECONDS] ?: 0 }
    suspend fun saveCrossfadeSeconds(seconds: Int) = runCatching {
        context.pbDataStore.edit { it[Keys.CROSSFADE_SECONDS] = seconds.coerceIn(0, 12) }
    }.isSuccess

    fun equalizerEnabled(): Flow<Boolean> =
        safeData.map { it[Keys.EQUALIZER_ENABLED] ?: false }

    suspend fun saveEqualizerEnabled(enabled: Boolean) = runCatching {
        context.pbDataStore.edit { it[Keys.EQUALIZER_ENABLED] = enabled }
    }.isSuccess

    /** Band levels stored as a JSON array of milli-bel integers, e.g. "[0,0,100,200,100]". */
    fun equalizerBands(): Flow<List<Short>> = safeData.map { prefs ->
        val parsed = runCatching {
            PbJson.json.parseToJsonElement(prefs[Keys.EQUALIZER_BANDS] ?: "[]")
                .let { el ->
                    when (el) {
                        is kotlinx.serialization.json.JsonArray -> el
                        else -> kotlinx.serialization.json.JsonArray(emptyList())
                    }
                }
                .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() }
                .map { it.toShort() }
        }.getOrDefault(emptyList())
        if (parsed.size >= 5) parsed.take(5) else (parsed + List(5 - parsed.size) { 0.toShort() })
    }

    suspend fun saveEqualizerBands(levels: List<Short>) = runCatching {
        val json = levels.joinToString(",", "[", "]") { it.toString() }
        context.pbDataStore.edit { it[Keys.EQUALIZER_BANDS] = json }
    }.isSuccess

    fun bassBoostStrength(): Flow<Int> = safeData.map { it[Keys.BASS_BOOST] ?: 0 }
    suspend fun saveBassBoostStrength(strength: Int) = runCatching {
        context.pbDataStore.edit { it[Keys.BASS_BOOST] = strength.coerceIn(0, 1000) }
    }.isSuccess

    fun virtualizerStrength(): Flow<Int> = safeData.map { it[Keys.VIRTUALIZER] ?: 0 }
    suspend fun saveVirtualizerStrength(strength: Int) = runCatching {
        context.pbDataStore.edit { it[Keys.VIRTUALIZER] = strength.coerceIn(0, 1000) }
    }.isSuccess

    fun userName(): Flow<String> = safeData.map { it[Keys.USER_NAME] ?: "Listener" }
    suspend fun saveUserName(name: String) = runCatching {
        context.pbDataStore.edit { it[Keys.USER_NAME] = name }
    }.isSuccess

    fun userEmoji(): Flow<String> = safeData.map { it[Keys.USER_EMOJI] ?: "🎧" }
    suspend fun saveUserEmoji(emoji: String) = runCatching {
        context.pbDataStore.edit { it[Keys.USER_EMOJI] = emoji }
    }.isSuccess

    suspend fun lastJamCode(): String? = runCatching {
        safeData.map { it[Keys.LAST_JAM_CODE] }.first()
    }.getOrNull()

    suspend fun saveLastJamCode(code: String) = runCatching {
        context.pbDataStore.edit { it[Keys.LAST_JAM_CODE] = code }
    }.isSuccess

    fun podcastFeedsJson(): Flow<String> =
        safeData.map { it[Keys.PODCAST_FEEDS_JSON] ?: "[]" }

    suspend fun savePodcastFeedsJson(json: String) = runCatching {
        context.pbDataStore.edit { it[Keys.PODCAST_FEEDS_JSON] = json }
    }.isSuccess
}
