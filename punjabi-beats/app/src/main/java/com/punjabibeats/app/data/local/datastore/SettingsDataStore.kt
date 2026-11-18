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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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
        val SEEKBAR_STYLE = stringPreferencesKey("seekbar_style")
        val TURNTABLE_MODE = booleanPreferencesKey("turntable_mode")
    }

    fun themeId(): Flow<String> = context.pbDataStore.data.map { it[Keys.THEME_ID] ?: "classic" }
    suspend fun saveThemeId(themeId: String) = context.pbDataStore.edit { it[Keys.THEME_ID] = themeId }

    fun seekbarStyle(): Flow<String> = context.pbDataStore.data.map { it[Keys.SEEKBAR_STYLE] ?: "classic_waveform" }
    suspend fun saveSeekbarStyle(style: String) = context.pbDataStore.edit { it[Keys.SEEKBAR_STYLE] = style }

    fun turntableMode(): Flow<Boolean> = context.pbDataStore.data.map { it[Keys.TURNTABLE_MODE] ?: false }
    suspend fun saveTurntableMode(enabled: Boolean) = context.pbDataStore.edit { it[Keys.TURNTABLE_MODE] = enabled }

    /** Jam room code arriving via a punjabibeats://jam/… deep link, consumed once. */
    suspend fun savePendingJamCode(code: String) =
        context.pbDataStore.edit { it[Keys.PENDING_JAM_CODE] = code }

    suspend fun popPendingJamCode(): String? {
        val code = context.pbDataStore.data.map { it[Keys.PENDING_JAM_CODE] }.first()
        if (!code.isNullOrBlank()) {
            context.pbDataStore.edit { it[Keys.PENDING_JAM_CODE] = "" }
        }
        return code?.takeIf { it.isNotBlank() }
    }

    /** Stable per-install identity used for Jam Room presence. */
    suspend fun ensureUserId(): String {
        val existing = context.pbDataStore.data.map { it[Keys.USER_ID] }.first()
        if (!existing.isNullOrBlank()) return existing
        val fresh = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
        context.pbDataStore.edit { it[Keys.USER_ID] = fresh }
        return fresh
    }

    fun lastQueue(): Flow<PersistedQueue> = context.pbDataStore.data.map { prefs ->
        PersistedQueue(
            songsJson = prefs[Keys.LAST_QUEUE_JSON].orEmpty(),
            startIndex = prefs[Keys.LAST_INDEX] ?: 0,
        )
    }

    suspend fun saveQueue(songsJson: String, startIndex: Int) {
        context.pbDataStore.edit { prefs ->
            prefs[Keys.LAST_QUEUE_JSON] = songsJson
            prefs[Keys.LAST_INDEX] = startIndex
        }
    }

    fun repeatMode(): Flow<Int> = context.pbDataStore.data.map { it[Keys.REPEAT_MODE] ?: 0 }
    suspend fun saveRepeatMode(mode: Int) = context.pbDataStore.edit { it[Keys.REPEAT_MODE] = mode }

    fun shuffleEnabled(): Flow<Boolean> = context.pbDataStore.data.map { it[Keys.SHUFFLE] ?: false }
    suspend fun saveShuffle(enabled: Boolean) = context.pbDataStore.edit { it[Keys.SHUFFLE] = enabled }

    fun playbackSpeed(): Flow<Float> = context.pbDataStore.data.map { it[Keys.SPEED] ?: 1.0f }
    suspend fun savePlaybackSpeed(speed: Float) = context.pbDataStore.edit { it[Keys.SPEED] = speed }

    fun playbackPitch(): Flow<Float> = context.pbDataStore.data.map { it[Keys.PITCH] ?: 1.0f }
    suspend fun savePlaybackPitch(pitch: Float) = context.pbDataStore.edit { it[Keys.PITCH] = pitch }

    fun skipSilence(): Flow<Boolean> = context.pbDataStore.data.map { it[Keys.SKIP_SILENCE] ?: false }
    suspend fun saveSkipSilence(enabled: Boolean) = context.pbDataStore.edit { it[Keys.SKIP_SILENCE] = enabled }

    fun crossfadeSeconds(): Flow<Int> = context.pbDataStore.data.map { it[Keys.CROSSFADE_SECONDS] ?: 0 }
    suspend fun saveCrossfadeSeconds(seconds: Int) =
        context.pbDataStore.edit { it[Keys.CROSSFADE_SECONDS] = seconds.coerceIn(0, 12) }

    fun equalizerEnabled(): Flow<Boolean> =
        context.pbDataStore.data.map { it[Keys.EQUALIZER_ENABLED] ?: false }

    suspend fun saveEqualizerEnabled(enabled: Boolean) =
        context.pbDataStore.edit { it[Keys.EQUALIZER_ENABLED] = enabled }

    /** Band levels stored as a JSON array of milli-bel integers, e.g. "[0,0,100,200,100]". */
    fun equalizerBands(): Flow<List<Short>> = context.pbDataStore.data.map { prefs ->
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

    suspend fun saveEqualizerBands(levels: List<Short>) {
        val json = levels.joinToString(",", "[", "]") { it.toString() }
        context.pbDataStore.edit { it[Keys.EQUALIZER_BANDS] = json }
    }

    fun bassBoostStrength(): Flow<Int> = context.pbDataStore.data.map { it[Keys.BASS_BOOST] ?: 0 }
    suspend fun saveBassBoostStrength(strength: Int) =
        context.pbDataStore.edit { it[Keys.BASS_BOOST] = strength.coerceIn(0, 1000) }

    fun virtualizerStrength(): Flow<Int> = context.pbDataStore.data.map { it[Keys.VIRTUALIZER] ?: 0 }
    suspend fun saveVirtualizerStrength(strength: Int) =
        context.pbDataStore.edit { it[Keys.VIRTUALIZER] = strength.coerceIn(0, 1000) }

    fun userName(): Flow<String> = context.pbDataStore.data.map { it[Keys.USER_NAME] ?: "Listener" }
    suspend fun saveUserName(name: String) = context.pbDataStore.edit { it[Keys.USER_NAME] = name }

    fun userEmoji(): Flow<String> = context.pbDataStore.data.map { it[Keys.USER_EMOJI] ?: "🎧" }
    suspend fun saveUserEmoji(emoji: String) = context.pbDataStore.edit { it[Keys.USER_EMOJI] = emoji }

    suspend fun lastJamCode(): String? =
        context.pbDataStore.data.map { it[Keys.LAST_JAM_CODE] }.first()

    suspend fun saveLastJamCode(code: String) = context.pbDataStore.edit { it[Keys.LAST_JAM_CODE] = code }

    fun podcastFeedsJson(): Flow<String> =
        context.pbDataStore.data.map { it[Keys.PODCAST_FEEDS_JSON] ?: "[]" }

    suspend fun savePodcastFeedsJson(json: String) =
        context.pbDataStore.edit { it[Keys.PODCAST_FEEDS_JSON] = json }
}
