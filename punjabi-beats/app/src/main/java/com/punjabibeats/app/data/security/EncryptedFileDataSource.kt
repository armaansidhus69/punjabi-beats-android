package com.punjabibeats.app.data.security

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * Media3 DataSource for the `pbenc://<songId>` scheme: transparently decrypts
 * AES/GCM offline downloads while ExoPlayer reads them.
 */
@OptIn(UnstableApi::class)
class EncryptedFileDataSource(private val context: Context) : BaseDataSource(false) {

    private var stream: InputStream? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        val songId = dataSpec.uri.host
            ?: dataSpec.uri.lastPathSegment
            ?: throw FileNotFoundException("pbenc uri without id: ${dataSpec.uri}")
        val file: File = DownloadCrypto.fileFor(context, songId)
        if (!file.exists()) throw FileNotFoundException(file.absolutePath)
        uri = dataSpec.uri

        val decrypted = DownloadCrypto.openDecrypted(file)
        if (dataSpec.position > 0) {
            var bytesToSkip = dataSpec.position
            val skipBuf = ByteArray(4096)
            while (bytesToSkip > 0) {
                val skipped = decrypted.skip(bytesToSkip)
                if (skipped > 0) {
                    bytesToSkip -= skipped
                } else {
                    val toRead = minOf(bytesToSkip, skipBuf.size.toLong()).toInt()
                    val bytesRead = decrypted.read(skipBuf, 0, toRead)
                    if (bytesRead == -1) break
                    bytesToSkip -= bytesRead
                }
            }
        }
        stream = decrypted
        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length else C.LENGTH_UNSET.toLong()
        transferStarted(dataSpec)
        opened = true
        return bytesRemaining
    }

    override fun read(target: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val current = stream ?: return C.RESULT_END_OF_INPUT
        val bytesToRead = if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            minOf(length.toLong(), bytesRemaining).toInt()
        } else {
            length
        }
        val bytesRead = current.read(target, offset, bytesToRead)
        if (bytesRead == -1) return C.RESULT_END_OF_INPUT
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesRead
        }
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        if (opened) {
            opened = false
            transferEnded()
        }
        runCatching { stream?.close() }
        stream = null
    }
}
