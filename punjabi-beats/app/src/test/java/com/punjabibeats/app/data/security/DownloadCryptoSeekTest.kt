package com.punjabibeats.app.data.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class DownloadCryptoSeekTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testAesCtrRandomAccessSeeking() {
        val keyBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val initialIv = ByteArray(16).also { SecureRandom().nextBytes(it) }

        // Create 64 KB of arbitrary audio-like sample data
        val originalData = ByteArray(64 * 1024) { (it % 251).toByte() }

        // Encrypt the entire stream with standard AES/CTR/NoPadding
        val encryptCipher = Cipher.getInstance("AES/CTR/NoPadding")
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(initialIv))
        val ciphertext = encryptCipher.doFinal(originalData)

        // Test seeking to multiple arbitrary positions
        val seekPositions = listOf(0, 1, 15, 16, 17, 31, 32, 100, 1024, 12345, 45000, 65530)
        for (pos in seekPositions) {
            val blockIndex = (pos / 16).toLong()
            val intraBlockOffset = pos % 16
            val counterIv = DownloadCrypto.calculateCounterIv(initialIv, blockIndex)

            val decryptCipher = Cipher.getInstance("AES/CTR/NoPadding")
            decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(counterIv))

            // Ciphertext starting at the block boundary
            val blockCiphertextOffset = (blockIndex * 16).toInt()
            val lengthToRead = minOf(512, originalData.size - pos)
            val ciphertextSliceLength = (lengthToRead + intraBlockOffset + 15) / 16 * 16

            val decryptedBlock = decryptCipher.update(
                ciphertext,
                blockCiphertextOffset,
                minOf(ciphertextSliceLength, ciphertext.size - blockCiphertextOffset)
            )

            val decryptedSlice = ByteArray(lengthToRead)
            System.arraycopy(decryptedBlock, intraBlockOffset, decryptedSlice, 0, lengthToRead)

            val expectedSlice = ByteArray(lengthToRead)
            System.arraycopy(originalData, pos, expectedSlice, 0, lengthToRead)

            assertArrayEquals("Mismatch at seek position $pos", expectedSlice, decryptedSlice)
        }
    }

    @Test
    fun testDownloadCryptoEncryptAndOpenDecrypted() {
        val originalData = ByteArray(32 * 1024) { (it * 7 % 255).toByte() }
        val targetFile = File(tempFolder.root, "test_song.pbenc")

        // Encrypt using DownloadCrypto
        val sizeWritten = DownloadCrypto.encryptTo(ByteArrayInputStream(originalData), targetFile)
        assertTrue("Target file must exist", targetFile.exists())
        assertEquals("Target file size must equal header + data", DownloadCrypto.HEADER_LENGTH_CTR.toLong() + originalData.size, sizeWritten)

        // Read back completely using openDecrypted
        val decryptedBytes = DownloadCrypto.openDecrypted(targetFile).use { it.readBytes() }
        assertArrayEquals("Decrypted bytes must match original exactly", originalData, decryptedBytes)
    }

    @Test
    fun testFileChannelSeekDecryptionMatchesOriginal() {
        val originalData = ByteArray(48 * 1024) { (it * 13 % 253).toByte() }
        val targetFile = File(tempFolder.root, "test_seek_channel.pbenc")

        DownloadCrypto.encryptTo(ByteArrayInputStream(originalData), targetFile)

        // Read header
        val fis = FileInputStream(targetFile)
        val version = fis.read()
        assertEquals(DownloadCrypto.VERSION_CTR.toInt(), version)

        val initialIv = ByteArray(DownloadCrypto.CTR_IV_LENGTH)
        fis.read(initialIv)

        // Test seeking via file channel + counter IV
        val testPositions = listOf(0L, 5L, 16L, 17L, 35L, 512L, 4096L, 16385L, 40000L)
        for (pos in testPositions) {
            val seekFis = FileInputStream(targetFile)
            seekFis.skip(1L + DownloadCrypto.CTR_IV_LENGTH) // skip header

            val blockIndex = pos / 16L
            val intraBlockOffset = (pos % 16L).toInt()
            val ciphertextOffset = DownloadCrypto.HEADER_LENGTH_CTR.toLong() + (blockIndex * 16L)

            seekFis.channel.position(ciphertextOffset)

            val counterIv = DownloadCrypto.calculateCounterIv(initialIv, blockIndex)
            val cipher = Cipher.getInstance("AES/CTR/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, DownloadCrypto.getOrCreateCtrKey(), IvParameterSpec(counterIv))

            val cipherIn = CipherInputStream(seekFis, cipher)

            // Discard intra-block offset
            if (intraBlockOffset > 0) {
                val discard = ByteArray(intraBlockOffset)
                cipherIn.read(discard)
            }

            val readLen = minOf(256, (originalData.size - pos).toInt())
            val buffer = ByteArray(readLen)
            var totalRead = 0
            while (totalRead < readLen) {
                val r = cipherIn.read(buffer, totalRead, readLen - totalRead)
                if (r == -1) break
                totalRead += r
            }

            val expected = ByteArray(readLen)
            System.arraycopy(originalData, pos.toInt(), expected, 0, readLen)

            assertArrayEquals("Mismatch at position $pos", expected, buffer)
            cipherIn.close()
        }
        fis.close()
    }
}
