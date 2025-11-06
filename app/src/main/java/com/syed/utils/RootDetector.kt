package com.syed.utils

import android.os.Build
import android.util.Log
import java.io.File

/**
 * Root Detection Utility
 * Detects if the Android device is rooted
 *
 * Purpose:
 * - Disable security-sensitive features on rooted devices
 * - Prevent API key extraction attempts
 * - Protect user data from malicious apps
 *
 * Note: This is not foolproof, but adds a security layer
 * Advanced root hiding tools can bypass these checks
 *
 * For Testing: Set ENABLE_ROOT_CHECK = false to disable during development
 */
object RootDetector {
    private const val TAG = "RootDetector"

    /**
     * Enable/disable root detection
     * Set to false during testing if you get false positives
     * MUST be true in production builds
     */
    private const val ENABLE_ROOT_CHECK = true // Set to false to disable for testing

    /**
     * Check if the device is rooted
     * Uses multiple detection methods for accuracy
     * Requires at least 2 indicators to reduce false positives
     */
    fun isDeviceRooted(): Boolean {
        // Allow disabling root check for testing
        if (!ENABLE_ROOT_CHECK) {
            Log.d(TAG, "⚠️ Root detection DISABLED for testing")
            return false
        }

        var rootIndicators = 0

        if (checkBuildTags()) rootIndicators++
        if (checkSuperUserApk()) rootIndicators++
        if (checkSuBinary()) rootIndicators++
        if (checkBusyBox()) rootIndicators++

        // Don't count system RW alone - it can be false positive
        // Only count it if we already have other indicators
        if (rootIndicators > 0 && checkSystemRW()) rootIndicators++

        val rooted = rootIndicators >= 2 // Need at least 2 indicators

        if (rooted) {
            Log.w(TAG, "⚠️ Root detected on this device ($rootIndicators indicators)")
        } else {
            Log.d(TAG, "✅ No root detected ($rootIndicators indicators, need 2+)")
        }

        return rooted
    }

    /**
     * Method 1: Check Build Tags
     * Test builds have "test-keys" in Build.TAGS
     */
    private fun checkBuildTags(): Boolean {
        val buildTags = Build.TAGS
        val isTestKeys = buildTags != null && buildTags.contains("test-keys")

        if (isTestKeys) {
            Log.d(TAG, "Root indicator: test-keys in build tags")
        }

        return isTestKeys
    }

    /**
     * Method 2: Check for SuperUser/SuperSU/Magisk APKs
     * These are the most common root management apps
     */
    private fun checkSuperUserApk(): Boolean {
        val rootAppPaths =
            arrayOf(
                "/system/app/Superuser.apk",
                "/system/app/SuperSU.apk",
                "/system/app/Magisk.apk",
                "/data/app/eu.chainfire.supersu",
                "/data/app/com.noshufou.android.su",
                "/data/app/com.koushikdutta.superuser",
                "/data/app/com.topjohnwu.magisk",
            )

        return rootAppPaths.any { path ->
            val exists = File(path).exists()
            if (exists) {
                Log.d(TAG, "Root indicator: Found root app at $path")
            }
            exists
        }
    }

    /**
     * Method 3: Check for 'su' binary
     * 'su' is used to gain root access
     */
    private fun checkSuBinary(): Boolean {
        val suPaths =
            arrayOf(
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su",
            )

        return suPaths.any { path ->
            val exists = File(path).exists()
            if (exists) {
                Log.d(TAG, "Root indicator: Found 'su' binary at $path")
            }
            exists
        }
    }

    /**
     * Method 4: Check for BusyBox
     * BusyBox is often installed with root
     */
    private fun checkBusyBox(): Boolean =
        try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "busybox"))
            val reader = process.inputStream.bufferedReader()
            val output = reader.readText()
            process.waitFor()

            val found = output.isNotEmpty()
            if (found) {
                Log.d(TAG, "Root indicator: BusyBox found")
            }
            found
        } catch (e: Exception) {
            false
        }

    /**
     * Method 5: Check if /system is mounted as read-write
     * Rooted devices can remount /system as RW
     * Note: This check alone can give false positives, so we don't rely on it solely
     */
    private fun checkSystemRW(): Boolean =
        try {
            val process = Runtime.getRuntime().exec("mount")
            val reader = process.inputStream.bufferedReader()
            val output = reader.readText()
            process.waitFor()

            // More strict check: look for actual rw mount at the beginning
            val lines = output.lines()
            val systemLine =
                lines.find {
                    it.contains("/system ") || it.contains(" /system ")
                }

            val isRW =
                systemLine?.let { line ->
                    // Check if mount options explicitly start with "rw,"
                    val mountOptions = line.substringAfter("(").substringBefore(")")
                    mountOptions.startsWith("rw,")
                } ?: false

            if (isRW) {
                Log.d(TAG, "Root indicator: /system mounted as read-write")
            }

            isRW
        } catch (e: Exception) {
            false
        }

    /**
     * Method 6: Check for dangerous root-related apps
     * Note: This requires QUERY_ALL_PACKAGES permission or package visibility declaration
     * Currently not implemented to avoid permission requirements
     */
    private fun checkDangerousApps(): Boolean {
        // This would require checking installed packages, which needs additional permissions
        // For now, return false to avoid adding unnecessary permissions
        // Can be implemented if app already has package query permissions
        return false
    }

    /**
     * Get a user-friendly message for rooted devices
     */
    fun getRootWarningMessage(): String =
        """
        ⚠️ Rooted Device Detected
        
        For your security, some features are disabled on rooted devices:
        - AI pet detection
        - AI chat functionality
        
        This protects your data and API keys from potential security risks.
        
        To use these features, please use a non-rooted device.
        """.trimIndent()

    /**
     * Check if device is running in emulator
     * Emulators are easier to inspect and should also be restricted
     */
    fun isEmulator(): Boolean =
        (
            Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
                "google_sdk" == Build.PRODUCT
        )

    /**
     * Comprehensive security check
     * Returns true if device is considered insecure
     */
    fun isDeviceInsecure(): Boolean {
        val rooted = isDeviceRooted()
        val emulator = isEmulator()

        if (emulator) {
            Log.w(TAG, "⚠️ Running in emulator")
        }

        return rooted || emulator
    }

    /**
     * Get detailed security status
     */
    fun getSecurityStatus(): String =
        """
        Device Security Status:
        - Rooted: ${if (isDeviceRooted()) "YES ⚠️" else "NO ✅"}
        - Emulator: ${if (isEmulator()) "YES ⚠️" else "NO ✅"}
        - Build Tags: ${Build.TAGS}
        - Manufacturer: ${Build.MANUFACTURER}
        - Model: ${Build.MODEL}
        - Device: ${Build.DEVICE}
        """.trimIndent()
}
