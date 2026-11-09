package com.punjabibeats.app.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256/GCM encryption for offline downloads, keyed by an AndroidKeyStore
 * secret. File layout: [1B nonce length][nonce][GCM ciphertext].
 */
object DownloadCrypto {

    private const val KEY_ALIAS = "pb_download_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val NONCE_LENGTH = 12

    fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    /** Streams [input] encrypted into [target]; returns the file size in bytes. */
    fun encryptTo(input: InputStream, target: File): Long {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val nonce = cipher.iv
        target.parentFile?.mkdirs()
        target.outputStream().use { fileOut ->
            fileOut.write(nonce.size)
            fileOut.write(nonce)
            CipherOutputStream(fileOut, cipher).use { cipherOut ->
                input.copyTo(cipherOut, BUFFER_BYTES)
            }
        }
        return target.length()
    }

    /** Opens an encrypted download for reading through a decrypting stream. */
    fun openDecrypted(file: File): InputStream {
        if (!file.exists()) throw FileNotFoundException(file.absolutePath)
        val raw = file.inputStream().buffered()
        val nonceLength = raw.read()
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
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, nonce))
        return CipherInputStream(raw, cipher)
    }

    fun offlineDir(context: Context): File = File(context.filesDir, "offline")

    fun fileFor(context: Context, songId: String): File =
        File(offlineDir(context), "${sanitize(songId)}.pbenc")

    fun sanitize(id: String): String = id.replace(Regex("[^a-zA-Z0-9_-]"), "_")

    private const val BUFFER_BYTES = 64 * 1024
}
