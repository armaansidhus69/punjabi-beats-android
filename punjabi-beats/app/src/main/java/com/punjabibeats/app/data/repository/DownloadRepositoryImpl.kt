package com.punjabibeats.app.data.repository

import android.content.Context
import com.punjabibeats.app.core.di.ApplicationScope
import com.punjabibeats.app.core.util.SongCodec
import com.punjabibeats.app.data.local.db.DownloadDao
import com.punjabibeats.app.data.local.db.DownloadedSongEntity
import com.punjabibeats.app.data.resolver.StreamResolver
import com.punjabibeats.app.data.security.DownloadCrypto
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Offline downloads: resolve the stream, fetch it over OkHttp and write it
 * AES/CTR-encrypted into app-private storage.
 *
 * Resilient implementation:
 * - Runs in @ApplicationScope so navigation away from screen does NOT abort the download.
 * - Writes to atomic temporary file (.tmp) first; never leaves corrupt partial .pbenc files.
 * - Deduplicates concurrent download requests for the same songId.
 * - Emits real 1..100% progress updates.
 */
@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val downloadDao: DownloadDao,
    private val streamResolver: StreamResolver,
    @ApplicationScope private val appScope: CoroutineScope,
) : DownloadRepository {

    private val progress = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val activeDownloads = ConcurrentHashMap<String, Deferred<Unit>>()

    override fun observeDownloads(): Flow<List<Song>> =
        downloadDao.downloads().map { rows -> rows.mapNotNull { SongCodec.decode(it.songJson) } }

    override fun observeProgress(): Flow<Map<String, Int>> = progress.asStateFlow()

    override fun observeIsDownloaded(songId: String): Flow<Boolean> = downloadDao.isDownloaded(songId)

    override suspend fun get(songId: String): Song? =
        downloadDao.get(songId)?.let { SongCodec.decode(it.songJson) }

    override suspend fun filePath(songId: String): String? =
        downloadDao.get(songId)?.filePath

    override suspend fun download(song: Song): Result<Unit> {
        // Fast path: already saved in DB
        if (downloadDao.get(song.id) != null) return Result.success(Unit)

        // Deduplication: if already in-flight, await existing task
        val existing = activeDownloads[song.id]
        if (existing != null && existing.isActive) {
            return runCatching { existing.await() }
        }

        val deferred = appScope.async(Dispatchers.IO) {
            performDownload(song)
        }
        activeDownloads[song.id] = deferred

        return runCatching {
            deferred.await()
        }.also {
            activeDownloads.remove(song.id)
        }
    }

    private suspend fun performDownload(song: Song) {
        val targetFile = DownloadCrypto.fileFor(context, song.id)
        val tempFile = File(context.cacheDir, "dl_${DownloadCrypto.sanitize(song.id)}_${System.currentTimeMillis()}.tmp")

        try {
            progress.update { it + (song.id to 1) }

            val resolved = streamResolver.resolve(song)
            val url = resolved.streamUrl ?: error("Could not resolve a downloadable stream for '${song.title}'")

            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Download failed with HTTP ${response.code}")
                val body = response.body ?: error("Download returned an empty body")
                val totalLength = body.contentLength()

                val sizeBytes = DownloadCrypto.encryptTo(
                    input = body.byteStream(),
                    target = tempFile,
                    totalBytes = totalLength,
                    onProgress = { pct ->
                        progress.update { it + (song.id to pct) }
                    },
                )

                if (tempFile.exists() && tempFile.length() > 0) {
                    targetFile.parentFile?.mkdirs()
                    if (targetFile.exists()) targetFile.delete()
                    if (!tempFile.renameTo(targetFile)) {
                        tempFile.copyTo(targetFile, overwrite = true)
                        tempFile.delete()
                    }

                    downloadDao.upsert(
                        DownloadedSongEntity(
                            songId = song.id,
                            songJson = SongCodec.encode(resolved),
                            filePath = targetFile.absolutePath,
                            sizeBytes = sizeBytes,
                        )
                    )
                } else {
                    error("Downloaded temp file was empty or missing")
                }
            }

            progress.update { it + (song.id to 100) }
            delay(500)
        } catch (t: Throwable) {
            if (tempFile.exists()) tempFile.delete()
            throw t
        } finally {
            progress.update { it - song.id }
        }
    }

    override suspend fun remove(songId: String) {
        withContext(Dispatchers.IO) {
            activeDownloads.remove(songId)?.cancel()
            progress.update { it - songId }
            downloadDao.get(songId)?.filePath?.let { path ->
                runCatching { File(path).delete() }
            }
            // Clean up any matching target file directly
            val targetFile = DownloadCrypto.fileFor(context, songId)
            if (targetFile.exists()) runCatching { targetFile.delete() }

            downloadDao.delete(songId)
        }
    }
}
