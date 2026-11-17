package com.punjabibeats.app.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec

/**
 * Secure encryption for offline downloads with instant O(1) random-access seeking.
 *
 * Layout (v2 CTR): [1B version 0x02][16B initial IV][AES-CTR ciphertext].
 * Legacy Layout (v1 GCM): [1B nonce length 12][12B nonce][AES-GCM ciphertext].
 */
object DownloadCrypto {

    const val VERSION_CTR: Byte = 0x02
    const val CTR_IV_LENGTH: Int = 16
    const val HEADER_LENGTH_CTR: Int = 1 + CTR_IV_LENGTH // 17 bytes

    private const val KEY_ALIAS_LEGACY = "pb_download_key"
    private const val KEY_ALIAS_CTR = "pb_download_ctr_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION_CTR = "AES/CTR/NoPadding"
    private const val TRANSFORMATION_GCM = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val BUFFER_BYTES = 64 * 1024

    private val fallbackKeys = ConcurrentHashMap<String, SecretKey>()

    /** Gets or creates the 256-bit AES-CTR key in AndroidKeyStore, with safe JVM fallback. */
    fun getOrCreateCtrKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (keyStore.getEntry(KEY_ALIAS_CTR, null) as? KeyStore.SecretKeyEntry)?.secretKey
                ?: run {
                    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                    generator.init(
                        KeyGenParameterSpec.Builder(
                            KEY_ALIAS_CTR,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        )
                            .setBlockModes(KeyProperties.BLOCK_MODE_CTR)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setKeySize(256)
                            .build()
                    )
                    generator.generateKey()
                }
        } catch (e: Exception) {
            getFallbackKey(KEY_ALIAS_CTR)
        }
    }

    /** Legacy key for existing GCM downloads. */
    fun getOrCreateKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (keyStore.getEntry(KEY_ALIAS_LEGACY, null) as? KeyStore.SecretKeyEntry)?.secretKey
                ?: run {
                    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                    generator.init(
                        KeyGenParameterSpec.Builder(
                            KEY_ALIAS_LEGACY,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        )
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setKeySize(256)
                            .build()
                    )
                    generator.generateKey()
                }
        } catch (e: Exception) {
            getFallbackKey(KEY_ALIAS_LEGACY)
        }
    }

    private fun getFallbackKey(alias: String): SecretKey {
        return fallbackKeys.getOrPut(alias) {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            keyGen.generateKey()
        }
    }

    /**
     * Increments the 16-byte initial IV by [blockIndex] using 128-bit big-endian addition.
     * Allows initializing AES-CTR directly at any 16-byte block boundary for O(1) seeking.
     */
    fun calculateCounterIv(initialIv: ByteArray, blockIndex: Long): ByteArray {
        require(initialIv.size == CTR_IV_LENGTH) { "Initial IV must be 16 bytes for AES" }
        val counterIv = initialIv.copyOf()
        var carry = blockIndex
        for (i in 15 downTo 0) {
            if (carry == 0L) break
            val sum = (counterIv[i].toInt() and 0xFF) + (carry and 0xFF)
            counterIv[i] = (sum and 0xFF).toByte()
            carry = (carry ushr 8) + (sum ushr 8)
        }
        return counterIv
    }

    /**
     * Streams [input] encrypted into [target] using AES-256-CTR; returns the file size in bytes.
     * File header: 0x02 (1 byte) + 16 bytes IV.
     */
    fun encryptTo(input: InputStream, target: File): Long {
        val initialIv = ByteArray(CTR_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION_CTR)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateCtrKey(), IvParameterSpec(initialIv))
        target.parentFile?.mkdirs()
        target.outputStream().use { fileOut ->
            fileOut.write(VERSION_CTR.toInt())
            fileOut.write(initialIv)
            CipherOutputStream(fileOut, cipher).use { cipherOut ->
                input.copyTo(cipherOut, BUFFER_BYTES)
            }
        }
        return target.length()
    }

    /** Opens an encrypted download for reading, automatically detecting CTR v2 or legacy GCM. */
    fun openDecrypted(file: File): InputStream {
        if (!file.exists()) throw FileNotFoundException(file.absolutePath)
        val raw = file.inputStream().buffered()
        val firstByte = raw.read()
        if (firstByte == -1) throw IOException("Corrupt empty encrypted file: ${file.name}")

        return if (firstByte == VERSION_CTR.toInt()) {
            val initialIv = ByteArray(CTR_IV_LENGTH)
            var read = 0
            while (read < CTR_IV_LENGTH) {
                val chunk = raw.read(initialIv, read, CTR_IV_LENGTH - read)
                if (chunk == -1) throw IOException("Truncated IV in ${file.name}")
                read += chunk
            }
            val cipher = Cipher.getInstance(TRANSFORMATION_CTR)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateCtrKey(), IvParameterSpec(initialIv))
            CipherInputStream(raw, cipher)
        } else {
            // Legacy AES-GCM
            val nonceLength = firstByte
            if (nonceLength <= 0 || nonceLength > 64) {
                raw.close()
                throw IOException("Corrupt encrypted file: ${file.name}")
            }
            val nonce = ByteArray(nonceLength)
            var read = 0
            while (read < nonceLength) {
                val chunk = raw.read(nonce, read, nonceLength - read)
                if (chunk == -1) throw IOException("Truncated nonce in ${file.name}")
                read += chunk
            }
            val cipher = Cipher.getInstance(TRANSFORMATION_GCM)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, nonce))
            CipherInputStream(raw, cipher)
        }
    }

    fun offlineDir(context: Context): File = File(context.filesDir, "offline")

    fun fileFor(context: Context, songId: String): File =
        File(offlineDir(context), "${sanitize(songId)}.pbenc")

    fun sanitize(id: String): String = id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
}
