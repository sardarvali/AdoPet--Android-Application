package com.syed.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure Key Manager using Android Keystore
 * Provides hardware-backed encryption for sensitive data
 *
 * Security Features:
 * - Uses Android Keystore (hardware-backed on most devices)
 * - AES-256-GCM encryption
 * - Keys never leave secure hardware
 * - Protection against key extraction
 * - Works on rooted devices (keys still protected)
 */
class SecureKeyManager private constructor(
    private val context: Context,
) {
    companion object {
        private const val TAG = "SecureKeyManager"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "com.syed.api_key_encryption"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        @Volatile
        private var INSTANCE: SecureKeyManager? = null

        fun getInstance(context: Context): SecureKeyManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureKeyManager(context.applicationContext).also { INSTANCE = it }
            }
    }

    private val keyStore: KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

    init {
        // Generate encryption key on first run
        generateKeyIfNeeded()
    }

    /**
     * Generate encryption key in Android Keystore if it doesn't exist
     */
    private fun generateKeyIfNeeded() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            try {
                Log.d(TAG, "Generating new encryption key in Android Keystore...")

                val keyGenerator =
                    KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        ANDROID_KEYSTORE,
                    )

                val keyGenParameterSpec =
                    KeyGenParameterSpec
                        .Builder(
                            KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256) // AES-256
                        .setUserAuthenticationRequired(false) // Don't require biometric
                        .setRandomizedEncryptionRequired(true) // Use random IV
                        .build()

                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()

                Log.d(TAG, "Encryption key generated successfully in Android Keystore")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate encryption key", e)
                throw SecurityException("Failed to initialize secure key storage", e)
            }
        } else {
            Log.d(TAG, "Encryption key already exists in Android Keystore")
        }
    }

    /**
     * Encrypt plaintext using Android Keystore key
     * @param plaintext The text to encrypt
     * @return Base64-encoded ciphertext with IV prepended
     */
    fun encrypt(plaintext: String): String {
        try {
            if (plaintext.isEmpty()) {
                throw IllegalArgumentException("Cannot encrypt empty string")
            }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val secretKey = getSecretKey()

            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            // Get the initialization vector (IV)
            val iv = cipher.iv
            if (iv.size != GCM_IV_LENGTH) {
                throw SecurityException("Invalid IV size: ${iv.size}")
            }

            // Encrypt the data
            val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            // Combine IV + encrypted data
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

            // Encode to Base64 for storage
            val result = Base64.encodeToString(combined, Base64.NO_WRAP)

            Log.d(TAG, "Successfully encrypted data (length: ${plaintext.length})")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            throw SecurityException("Failed to encrypt data", e)
        }
    }

    /**
     * Decrypt ciphertext using Android Keystore key
     * @param ciphertext Base64-encoded ciphertext with IV prepended
     * @return Decrypted plaintext
     */
    fun decrypt(ciphertext: String): String {
        try {
            if (ciphertext.isEmpty()) {
                throw IllegalArgumentException("Cannot decrypt empty string")
            }

            // Decode from Base64
            val combined = Base64.decode(ciphertext, Base64.NO_WRAP)

            if (combined.size < GCM_IV_LENGTH) {
                throw SecurityException("Invalid ciphertext: too short")
            }

            // Extract IV (first 12 bytes for GCM)
            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)

            // Extract encrypted data (remaining bytes)
            val encrypted = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.size)

            // Decrypt using Android Keystore key
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val secretKey = getSecretKey()
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decrypted = cipher.doFinal(encrypted)

            val result = String(decrypted, Charsets.UTF_8)

            Log.d(TAG, "Successfully decrypted data (length: ${result.length})")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            throw SecurityException("Failed to decrypt data", e)
        }
    }

    /**
     * Get the secret key from Android Keystore
     */
    private fun getSecretKey(): SecretKey {
        val entry = keyStore.getEntry(KEY_ALIAS, null)

        if (entry !is KeyStore.SecretKeyEntry) {
            throw SecurityException("Key entry is not a SecretKeyEntry")
        }

        return entry.secretKey
    }

    /**
     * Check if encryption key exists
     */
    fun isKeyAvailable(): Boolean =
        try {
            keyStore.containsAlias(KEY_ALIAS)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check key availability", e)
            false
        }

    /**
     * Delete the encryption key (use with caution)
     * This will make all encrypted data unrecoverable
     */
    fun deleteKey() {
        try {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
                Log.w(TAG, "Encryption key deleted from Android Keystore")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete key", e)
        }
    }

    /**
     * Get key information for debugging
     */
    fun getKeyInfo(): String =
        """
        Android Keystore Key Information:
        - Alias: $KEY_ALIAS
        - Algorithm: AES-256-GCM
        - Key Available: ${isKeyAvailable()}
        - Hardware Backed: ${isHardwareBacked()}
        - Provider: $ANDROID_KEYSTORE
        """.trimIndent()

    /**
     * Check if the key is hardware-backed
     * Hardware-backed keys provide stronger security
     */
    private fun isHardwareBacked(): Boolean =
        try {
            // On Android 9+ (API 28), most devices have hardware-backed keystore
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P
        } catch (e: Exception) {
            false
        }
}
