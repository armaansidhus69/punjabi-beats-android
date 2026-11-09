package com.punjabibeats.app.data.cipher

import android.content.Context
import com.punjabibeats.app.BuildConfig
import com.punjabibeats.app.core.util.PbJson
import com.punjabibeats.app.core.util.runSuspendCatching
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Loads, caches and self-heals the player configuration.
 *
 * Precedence: freshly downloaded remote copy → previously cached remote copy →
 * the read-only asset bundled with the APK. This mirrors the zemer-cipher
 * "remote-updatable player configuration" so YouTube-side rotations can be
 * fixed server-side without an app release.
 */
@Singleton
class PlayerConfigStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {

    private val mutex = Mutex()
    private val inMemory: MutableMap<String, CipherConfig> = mutableMapOf()

    private val cacheFile: File
        get() = File(context.filesDir, "cipher/player_configs.json")

    private val fetchClient: OkHttpClient = okHttpClient.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun config(forceRefresh: Boolean = false): CipherConfig = mutex.withLock {
        val cached = inMemory[KEY]
        if (cached != null && !forceRefresh) return cached
        val merged = loadMerged()
        inMemory[KEY] = merged
        return merged
    }

    private suspend fun loadMerged(): CipherConfig {
        val fallback = CipherConfig.default(BuildConfig.CIPHER_CONFIG_URL)
        val asset = readAsset() ?: fallback
        val remote = if (shouldRefreshRemote(asset)) fetchRemote(asset.remoteConfigUrl) else readCache()
        return when {
            remote != null && remote.version >= asset.version -> {
                writeCache(remote)
                remote
            }
            readCache() != null -> readCache() ?: asset
            else -> asset
        }
    }

    private fun shouldRefreshRemote(asset: CipherConfig): Boolean {
        if (asset.remoteConfigUrl.isBlank()) return false
        if (!cacheFile.exists()) return true
        val ageMs = System.currentTimeMillis() - cacheFile.lastModified()
        return ageMs > TimeUnit.HOURS.toMillis(asset.configTtlHours)
    }

    private suspend fun readAsset(): CipherConfig? = withContext(Dispatchers.IO) {
        runSuspendCatching {
            context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        }.map { raw -> PlayerConfigParser.parse(raw, BuildConfig.CIPHER_CONFIG_URL) }
            .getOrNull()
    }

    private suspend fun fetchRemote(url: String): CipherConfig? = withContext(Dispatchers.IO) {
        runSuspendCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PunjabiBeats/${BuildConfig.VERSION_NAME}")
                .build()
            fetchClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string().orEmpty()
                if (body.isBlank() || body.length > MAX_CONFIG_BYTES) return@use null
                PlayerConfigParser.parse(body, BuildConfig.CIPHER_CONFIG_URL)
            }
        }.getOrNull()
    }

    private fun readCache(): CipherConfig? = runCatching {
        if (!cacheFile.exists()) return null
        val raw = cacheFile.readText()
        if (raw.length > MAX_CONFIG_BYTES) return null
        PlayerConfigParser.parse(raw, BuildConfig.CIPHER_CONFIG_URL)
    }.getOrNull()

    private fun writeCache(config: CipherConfig) {
        runCatching {
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(PbJson.json.encodeToString(CipherConfig.serializer(), config))
        }
    }

    private companion object {
        const val KEY = "player_configs"
        const val ASSET_NAME = "player_configs.json"
        const val MAX_CONFIG_BYTES = 256 * 1024L
    }
}
