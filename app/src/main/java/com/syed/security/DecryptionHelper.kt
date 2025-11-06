package com.syed.security

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256 Decryption Helper
 * Decrypts API keys fetched from Firebase Remote Config
 */
object DecryptionHelper {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"

    /**
     * Decrypt encrypted text using AES-256-CBC with base64-encoded keys
     * @param encryptedText Base64 encoded encrypted string
     * @param secretBase64 Base64 encoded 32-byte secret key
     * @param ivBase64 Base64 encoded 16-byte initialization vector
     * @return Decrypted string or null if decryption fails
     */
    fun decrypt(
        encryptedText: String,
        secretBase64: String,
        ivBase64: String,
    ): String? =
        try {
            // Decode the base64-encoded secret key and IV from native code
            val secretBytes = Base64.decode(secretBase64, Base64.DEFAULT)
            val ivBytes = Base64.decode(ivBase64, Base64.DEFAULT)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val secretKeySpec = SecretKeySpec(secretBytes, ALGORITHM)
            val ivParameterSpec = IvParameterSpec(ivBytes)

            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)

            val decodedValue = Base64.decode(encryptedText, Base64.DEFAULT)
            val decryptedValue = cipher.doFinal(decodedValue)

            String(decryptedValue, Charsets.UTF_8)
        } catch (e: Exception) {
            android.util.Log.e("DecryptionHelper", "Decryption failed", e)
            null
        }

    /**
     * Securely clear a string from memory
     * This is a best-effort approach in Kotlin/Java
     */
    fun clearString(str: String?) {
        str?.let {
            // In Kotlin, strings are immutable, so we can't truly clear them
            // But we can help GC by dereferencing
            @Suppress("UNUSED_VALUE")
            var temp: String? = it
            temp = null
            System.gc() // Suggest garbage collection (not guaranteed)
        }
    }
}
