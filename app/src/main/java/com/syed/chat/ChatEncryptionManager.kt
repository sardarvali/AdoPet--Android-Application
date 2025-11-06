package com.syed.chat

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-End Encryption Manager for Chat
 * Uses AES-256 encryption for message content
 * Note: This is a simplified implementation. For production,
 * use a proper E2EE protocol like Signal Protocol.
 */
class ChatEncryptionManager {
    companion object {
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val KEY_SIZE = 256
    }

    /**
     * Generate a new encryption key
     */
    fun generateKey(): String {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
        keyGenerator.init(KEY_SIZE, SecureRandom())
        val secretKey = keyGenerator.generateKey()
        return Base64.encodeToString(secretKey.encoded, Base64.DEFAULT)
    }

    /**
     * Encrypt message
     */
    fun encryptMessage(
        message: String,
        keyString: String,
    ): EncryptedData? =
        try {
            val key = SecretKeySpec(Base64.decode(keyString, Base64.DEFAULT), ALGORITHM)
            val cipher = Cipher.getInstance(TRANSFORMATION)

            // Generate random IV
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
            val encrypted = cipher.doFinal(message.toByteArray())

            EncryptedData(
                encryptedText = Base64.encodeToString(encrypted, Base64.DEFAULT),
                iv = Base64.encodeToString(iv, Base64.DEFAULT),
            )
        } catch (e: Exception) {
            null
        }

    /**
     * Decrypt message
     */
    fun decryptMessage(
        encryptedData: EncryptedData,
        keyString: String,
    ): String? =
        try {
            val key = SecretKeySpec(Base64.decode(keyString, Base64.DEFAULT), ALGORITHM)
            val cipher = Cipher.getInstance(TRANSFORMATION)

            val iv = Base64.decode(encryptedData.iv, Base64.DEFAULT)
            val ivSpec = IvParameterSpec(iv)

            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
            val decrypted = cipher.doFinal(Base64.decode(encryptedData.encryptedText, Base64.DEFAULT))

            String(decrypted)
        } catch (e: Exception) {
            null
        }

    data class EncryptedData(
        val encryptedText: String,
        val iv: String,
    )
}
