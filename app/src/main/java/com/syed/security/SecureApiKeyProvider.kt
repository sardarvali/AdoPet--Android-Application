package com.syed.security

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.tasks.await

/**
 * Secure API Key Provider
 * Fetches encrypted keys from Firebase Remote Config and decrypts them
 * Keys are never stored - they're decrypted on-demand and cleared from memory after use
 */
object SecureApiKeyProvider {
    private var remoteConfig: FirebaseRemoteConfig? = null
    private var isInitialized = false

    // Remote Config parameter names
    private const val ENCRYPTED_GOOGLE_MAPS_KEY = "encrypted_google_maps_key"
    private const val ENCRYPTED_GEMINI_KEY = "encrypted_gemini_api_key"

    // Cache control
    private const val FETCH_INTERVAL_SECONDS = 3600L // 1 hour

    /**
     * Initialize Remote Config
     * Call this once in Application class or MainActivity
     */
    suspend fun initialize() {
        if (isInitialized) return

        try {
            remoteConfig = Firebase.remoteConfig
            val configSettings =
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = FETCH_INTERVAL_SECONDS
                }
            remoteConfig?.setConfigSettingsAsync(configSettings)

            // Set default values (empty strings for security)
            val defaults =
                mapOf(
                    ENCRYPTED_GOOGLE_MAPS_KEY to "",
                    ENCRYPTED_GEMINI_KEY to "",
                )
            remoteConfig?.setDefaultsAsync(defaults)

            // Initial fetch
            remoteConfig?.fetchAndActivate()?.await()
            isInitialized = true

            android.util.Log.d("SecureApiKeyProvider", "Remote Config initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("SecureApiKeyProvider", "Failed to initialize Remote Config", e)
        }
    }

    /**
     * Get Google Maps API Key
     * Fetches from Remote Config, decrypts, and returns
     * Key is automatically cleared from memory after use
     */
    suspend fun getGoogleMapsApiKey(): String? =
        getDecryptedKey(
            remoteConfigKey = ENCRYPTED_GOOGLE_MAPS_KEY,
            secretKeyProvider = { SecureKeys.getGoogleMapsSecretKey() },
            ivProvider = { SecureKeys.getGoogleMapsIv() },
        )

    /**
     * Get Gemini AI API Key
     * Fetches from Remote Config, decrypts, and returns
     * Key is automatically cleared from memory after use
     */
    suspend fun getGeminiApiKey(): String? =
        getDecryptedKey(
            remoteConfigKey = ENCRYPTED_GEMINI_KEY,
            secretKeyProvider = { SecureKeys.getGeminiSecretKey() },
            ivProvider = { SecureKeys.getGeminiIv() },
        )

    /**
     * Generic method to fetch and decrypt any encrypted key
     */
    private suspend fun getDecryptedKey(
        remoteConfigKey: String,
        secretKeyProvider: () -> String,
        ivProvider: () -> String,
    ): String? {
        try {
            // Ensure initialized
            if (!isInitialized) {
                initialize()
            }

            // Fetch encrypted key from Remote Config
            val encryptedKey = remoteConfig?.getString(remoteConfigKey)

            if (encryptedKey.isNullOrEmpty()) {
                android.util.Log.w("SecureApiKeyProvider", "Encrypted key not found: $remoteConfigKey")
                return null
            }

            // Get decryption secrets from native code
            val secret = secretKeyProvider()
            val iv = ivProvider()

            // Decrypt
            val decryptedKey = DecryptionHelper.decrypt(encryptedKey, secret, iv)

            if (decryptedKey == null) {
                android.util.Log.e("SecureApiKeyProvider", "Failed to decrypt key: $remoteConfigKey")
            }

            return decryptedKey
        } catch (e: Exception) {
            android.util.Log.e("SecureApiKeyProvider", "Error getting decrypted key", e)
            return null
        }
    }

    /**
     * Force refresh Remote Config
     * Use this sparingly - only when you update keys
     */
    suspend fun forceRefresh() {
        try {
            remoteConfig?.fetchAndActivate()?.await()
            android.util.Log.d("SecureApiKeyProvider", "Remote Config force refreshed")
        } catch (e: Exception) {
            android.util.Log.e("SecureApiKeyProvider", "Failed to force refresh", e)
        }
    }
}
