package com.syed.security

/**
 * Secure Keys - Bridge to native C++ library
 * This object loads the native library and provides access to encrypted keys
 * stored in native code (harder to reverse engineer)
 */
object SecureKeys {
    init {
        try {
            System.loadLibrary("native-lib")
            android.util.Log.d("SecureKeys", "Native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("SecureKeys", "Failed to load native library", e)
        }
    }

    // Google Maps API keys decryption secrets
    external fun getGoogleMapsSecretKey(): String

    external fun getGoogleMapsIv(): String

    // Gemini AI API keys decryption secrets
    external fun getGeminiSecretKey(): String

    external fun getGeminiIv(): String

    // Additional security salt
    external fun getAppSalt(): String
}
