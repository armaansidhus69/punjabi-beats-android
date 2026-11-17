package com.punjabibeats.app.data.repository

import android.content.Context
import com.punjabibeats.app.core.di.ApplicationScope
import com.punjabibeats.app.core.util.SongCodec
import com.punjabibeats.app.core.util.runSuspendCatching
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
 * - Cloud Restore & Keystore Desync Resilience: Prunes phantom DB records whose files
 *   do not exist on disk or cannot be read.
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

    init {
        // Startup sweep: purge any phantom download records whose files were excluded
        // from Google Drive cloud backup/device restore or wiped by OS storage manager
        appScope.launch(Dispatchers.IO) {
            val list = downloadDao.downloads().firstOrNull() ?: emptyList()
            for (row in list) {
                val f = File(row.filePath)
                if (!f.exists() || f.length() <= 0) {
                    downloadDao.delete(row.songId)
                }
            }
        }
    }

    override fun observeDownloads(): Flow<List<Song>> =
        downloadDao.downloads().map { rows ->
            rows.mapNotNull { row ->
                val file = File(row.filePath)
                if (file.exists() && file.length() > 0) {
                    SongCodec.decode(row.songJson)
                } else {
                    appScope.launch(Dispatchers.IO) { downloadDao.delete(row.songId) }
                    null
                }
            }
        }.flowOn(Dispatchers.IO)

    override fun observeProgress(): Flow<Map<String, Int>> = progress.asStateFlow()

    override fun observeIsDownloaded(songId: String): Flow<Boolean> =
        downloadDao.isDownloaded(songId).map { existsInDb ->
            if (!existsInDb) false
            else {
                val row = downloadDao.get(songId)
                val file = row?.filePath?.let { File(it) }
                if (file != null && file.exists() && file.length() > 0) true
                else {
                    appScope.launch(Dispatchers.IO) { downloadDao.delete(songId) }
                    false
                }
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun get(songId: String): Song? = withContext(Dispatchers.IO) {
        val row = downloadDao.get(songId) ?: return@withContext null
        val file = File(row.filePath)
        if (file.exists() && file.length() > 0) {
            SongCodec.decode(row.songJson)
        } else {
            downloadDao.delete(songId)
            null
        }
    }

    override suspend fun filePath(songId: String): String? = withContext(Dispatchers.IO) {
        val path = downloadDao.get(songId)?.filePath ?: return@withContext null
        val file = File(path)
        if (file.exists() && file.length() > 0) path else {
            downloadDao.delete(songId)
            null
        }
    }

    override suspend fun download(song: Song): Result<Unit> {
        // Fast path: already saved in DB and file exists
        val existingRow = downloadDao.get(song.id)
        if (existingRow != null) {
            val f = File(existingRow.filePath)
            if (f.exists() && f.length() > 0) return Result.success(Unit)
        }

        // Deduplication: if already in-flight, await existing task
        val existing = activeDownloads[song.id]
        if (existing != null && existing.isActive) {
            return runSuspendCatching { existing.await() }
        }

        val deferred = appScope.async(Dispatchers.IO) {
            performDownload(song)
        }
        activeDownloads[song.id] = deferred

        return runSuspendCatching {
            deferred.await()
        }.also {
            activeDownloads.remove(song.id)
        }
    }

    private suspend fun performDownload(song: Song) {
        val targetDir = context.filesDir
        val usableSpace = targetDir.usableSpace
        if (usableSpace < MIN_FREE_SPACE_BYTES) {
            val freeMb = usableSpace / (1024 * 1024)
            error("Device storage is low ($freeMb MB free). At least 50MB free space is required to download songs safely.")
        }

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

                if (totalLength > 0 && targetDir.usableSpace < totalLength + MIN_FREE_SPACE_BYTES) {
                    val neededMb = (totalLength + MIN_FREE_SPACE_BYTES) / (1024 * 1024)
                    val freeMb = targetDir.usableSpace / (1024 * 1024)
                    error("Insufficient storage space for '${song.title}' ($freeMb MB free, need $neededMb MB).")
                }

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
            val targetFile = DownloadCrypto.fileFor(context, songId)
            if (targetFile.exists()) runCatching { targetFile.delete() }

            downloadDao.delete(songId)
        }
    }

    private companion object {
        const val MIN_FREE_SPACE_BYTES = 50L * 1024 * 1024L // 50MB safety reserve
    }
}
