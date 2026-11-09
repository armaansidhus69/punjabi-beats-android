package com.punjabibeats.app.data.repository

import android.content.Context
import com.punjabibeats.app.core.util.SongCodec
import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.data.local.db.DownloadDao
import com.punjabibeats.app.data.local.db.DownloadedSongEntity
import com.punjabibeats.app.data.resolver.StreamResolver
import com.punjabibeats.app.data.security.DownloadCrypto
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Offline downloads: resolve the stream, fetch it over OkHttp and write it
 * AES/GCM-encrypted into app-private storage. Progress is reported as a map of
 * songId → percent, where 0 means indeterminate.
 */
@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val downloadDao: DownloadDao,
    private val streamResolver: StreamResolver,
) : DownloadRepository {

    private val progress = MutableStateFlow<Map<String, Int>>(emptyMap())

    override fun observeDownloads(): Flow<List<Song>> =
        downloadDao.downloads().map { rows -> rows.mapNotNull { SongCodec.decode(it.songJson) } }

    override fun observeProgress(): Flow<Map<String, Int>> = progress.asStateFlow()

    override fun observeIsDownloaded(songId: String): Flow<Boolean> = downloadDao.isDownloaded(songId)

    override suspend fun get(songId: String): Song? =
        downloadDao.get(songId)?.let { SongCodec.decode(it.songJson) }

    override suspend fun filePath(songId: String): String? =
        downloadDao.get(songId)?.filePath

    override suspend fun download(song: Song): Result<Unit> = runSuspendCatching {
        withContext(Dispatchers.IO) {
            if (downloadDao.get(song.id) != null) return@withContext
            progress.value = progress.value + (song.id to 0)

            val resolved = streamResolver.resolve(song)
            val url = resolved.streamUrl ?: error("Could not resolve a downloadable stream")

            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Download failed with HTTP ${response.code}")
                val body = response.body ?: error("Download returned an empty body")
                val file = DownloadCrypto.fileFor(context, song.id)
                val sizeBytes = DownloadCrypto.encryptTo(body.byteStream(), file)
                downloadDao.upsert(
                    DownloadedSongEntity(
                        songId = song.id,
                        songJson = SongCodec.encode(resolved),
                        filePath = file.absolutePath,
                        sizeBytes = sizeBytes,
                    )
                )
            }
            progress.value = progress.value + (song.id to 100)
            delay(800)
            progress.value = progress.value - song.id
        }
    }

    override suspend fun remove(songId: String) {
        withContext(Dispatchers.IO) {
            downloadDao.get(songId)?.filePath?.let { path ->
                runCatching { java.io.File(path).delete() }
            }
            downloadDao.delete(songId)
        }
    }
}
