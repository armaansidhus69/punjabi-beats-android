package com.punjabibeats.app.data.security

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec

/**
 * Media3 DataSource for the `pbenc://<songId>` scheme:
 * Transparently decrypts offline downloads with instant O(1) random-access seeking.
 *
 * For AES-CTR (v2): Direct kernel lseek to the 16-byte block boundary + IvParameterSpec(counterIv).
 * Discards at most 15 intra-block bytes, completely eliminating multi-megabyte sequential skip latency.
 *
 * For Legacy AES-GCM (v1): Fallback sequential skip ensures existing downloaded files remain playable.
 *
 * Cloud Restore & Keystore Desync Resilience: Catches keystore invalidation or corrupted files and
 * surfaces an actionable IOException so ExoPlayer triggers seamless stream recovery without crashing.
 */
@OptIn(UnstableApi::class)
class EncryptedFileDataSource(private val context: Context) : BaseDataSource(false) {

    private var stream: InputStream? = null
    private var fileIn: FileInputStream? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        val songId = dataSpec.uri.host
            ?: dataSpec.uri.lastPathSegment
            ?: throw FileNotFoundException("pbenc uri without id: ${dataSpec.uri}")
        val file: File = DownloadCrypto.fileFor(context, songId)
        if (!file.exists()) throw FileNotFoundException("Offline file not found for '$songId': ${file.absolutePath}")
        uri = dataSpec.uri

        val fis = file.inputStream()
        fileIn = fis

        try {
            val firstByte = fis.read()
            if (firstByte == -1) {
                fis.close()
                throw IOException("Corrupt empty encrypted file: ${file.name}")
            }

            val decryptedStream: InputStream = if (firstByte == DownloadCrypto.VERSION_CTR.toInt()) {
                // Modern AES-CTR: Instant O(1) random-access seek
                val initialIv = ByteArray(DownloadCrypto.CTR_IV_LENGTH)
                var ivRead = 0
                while (ivRead < initialIv.size) {
                    val count = fis.read(initialIv, ivRead, initialIv.size - ivRead)
                    if (count == -1) {
                        fis.close()
                        throw IOException("Truncated IV in ${file.name}")
                    }
                    ivRead += count
                }

                val position = dataSpec.position
                val blockIndex = position / 16L
                val intraBlockOffset = (position % 16L).toInt()
                val ciphertextOffset = DownloadCrypto.HEADER_LENGTH_CTR.toLong() + (blockIndex * 16L)

                // Instant kernel seek directly to the block boundary
                fis.channel.position(ciphertextOffset)

                val counterIv = DownloadCrypto.calculateCounterIv(initialIv, blockIndex)
                val cipher = Cipher.getInstance("AES/CTR/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, DownloadCrypto.getOrCreateCtrKey(), IvParameterSpec(counterIv))

                val cipherIn = CipherInputStream(fis, cipher)

                // Discard intra-block offset (0..15 bytes max)
                if (intraBlockOffset > 0) {
                    val discardBuf = ByteArray(intraBlockOffset)
                    var discarded = 0
                    while (discarded < intraBlockOffset) {
                        val count = cipherIn.read(discardBuf, discarded, intraBlockOffset - discarded)
                        if (count == -1) break
                        discarded += count
                    }
                }
                cipherIn
            } else {
                // Legacy AES-GCM fallback: Read nonce and sequential skip
                val nonceLength = firstByte
                if (nonceLength <= 0 || nonceLength > 64) {
                    fis.close()
                    throw IOException("Corrupt encrypted file: ${file.name}")
                }
                val nonce = ByteArray(nonceLength)
                var read = 0
                while (read < nonceLength) {
                    val chunk = fis.read(nonce, read, nonceLength - read)
                    if (chunk == -1) {
                        fis.close()
                        throw IOException("Truncated nonce in ${file.name}")
                    }
                    read += chunk
                }
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, DownloadCrypto.getOrCreateKey(), GCMParameterSpec(128, nonce))
                val cipherIn = CipherInputStream(fis, cipher)

                if (dataSpec.position > 0) {
                    var bytesToSkip = dataSpec.position
                    val skipBuf = ByteArray(4096)
                    while (bytesToSkip > 0) {
                        val skipped = cipherIn.skip(bytesToSkip)
                        if (skipped > 0) {
                            bytesToSkip -= skipped
                        } else {
                            val toRead = minOf(bytesToSkip, skipBuf.size.toLong()).toInt()
                            val bytesRead = cipherIn.read(skipBuf, 0, toRead)
                            if (bytesRead == -1) break
                            bytesToSkip -= bytesRead
                        }
                    }
                }
                cipherIn
            }

            stream = decryptedStream

            bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                if (firstByte == DownloadCrypto.VERSION_CTR.toInt()) {
                    (file.length() - DownloadCrypto.HEADER_LENGTH_CTR.toLong() - dataSpec.position).coerceAtLeast(0L)
                } else {
                    C.LENGTH_UNSET.toLong()
                }
            }

            transferStarted(dataSpec)
            opened = true
            return bytesRemaining
        } catch (e: Exception) {
            runCatching { fis.close() }
            fileIn = null
            stream = null
            if (e is IOException) throw e
            throw IOException("Crypto error reading offline file for '$songId': ${e.message}", e)
        }
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
        runCatching { fileIn?.close() }
        stream = null
        fileIn = null
    }
}
