package com.syed.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtils {
    // Permission constants
    const val PERMISSION_REQUEST_CODE = 1001
    const val CAMERA_PERMISSION_CODE = 1002
    const val STORAGE_PERMISSION_CODE = 1003
    const val NOTIFICATION_PERMISSION_CODE = 1004

    val CAMERA_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    val STORAGE_PERMISSIONS =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        }

    val NOTIFICATION_PERMISSIONS =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

    val LOCATION_PERMISSIONS =
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

    val ALL_REQUIRED_PERMISSIONS =
        (
            CAMERA_PERMISSIONS + STORAGE_PERMISSIONS + NOTIFICATION_PERMISSIONS +
                arrayOf(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE)
        ).distinct()
            .toTypedArray()

    /**
     * Check if all permissions are granted
     */
    fun hasAllPermissions(
        context: Context,
        permissions: Array<String>,
    ): Boolean =
        permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

    /**
     * Check camera permission
     */
    fun hasCameraPermission(context: Context): Boolean = hasAllPermissions(context, CAMERA_PERMISSIONS)

    /**
     * Check storage permissions
     */
    fun hasStoragePermissions(context: Context): Boolean = hasAllPermissions(context, STORAGE_PERMISSIONS)

    /**
     * Check notification permission
     */
    fun hasNotificationPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasAllPermissions(context, NOTIFICATION_PERMISSIONS)
        } else {
            true
        }

    /**
     * Check all required permissions for the app
     */
    fun hasAllRequiredPermissions(context: Context): Boolean = hasAllPermissions(context, ALL_REQUIRED_PERMISSIONS)

    /**
     * Request storage permissions with rationale
     */
    fun requestStoragePermissions(
        activity: Activity,
        onGranted: () -> Unit = {},
    ) {
        if (hasStoragePermissions(activity)) {
            onGranted()
            return
        }

        val rationaleMessage =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                "This app needs permission to access photos from your gallery."
            } else {
                "This app needs storage permission to save and access images."
            }

        if (STORAGE_PERMISSIONS.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }) {
            AlertDialog
                .Builder(activity)
                .setTitle("Storage Permission Required")
                .setMessage(rationaleMessage)
                .setPositiveButton("Grant") { _, _ ->
                    ActivityCompat.requestPermissions(activity, STORAGE_PERMISSIONS, STORAGE_PERMISSION_CODE)
                }.setNegativeButton("Cancel", null)
                .show()
        } else {
            ActivityCompat.requestPermissions(activity, STORAGE_PERMISSIONS, STORAGE_PERMISSION_CODE)
        }
    }

    /**
     * Request image permissions (camera + storage)
     */
    fun requestImagePermissions(
        activity: Activity,
        onGranted: () -> Unit = {},
    ) {
        val neededPermissions = mutableListOf<String>()

        if (!hasCameraPermission(activity)) {
            neededPermissions.addAll(CAMERA_PERMISSIONS)
        }
        if (!hasStoragePermissions(activity)) {
            neededPermissions.addAll(STORAGE_PERMISSIONS)
        }

        if (neededPermissions.isEmpty()) {
            onGranted()
            return
        }

        AlertDialog
            .Builder(activity)
            .setTitle("Permissions Required")
            .setMessage("This app needs camera and storage permissions to handle images.")
            .setPositiveButton("Grant") { _, _ ->
                ActivityCompat.requestPermissions(activity, neededPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
            }.setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Request all required permissions at once
     */
    fun requestAllRequiredPermissions(
        activity: Activity,
        onAllGranted: () -> Unit = {},
    ) {
        val deniedPermissions =
            ALL_REQUIRED_PERMISSIONS.filter {
                ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
            }

        if (deniedPermissions.isEmpty()) {
            onAllGranted()
            return
        }

        AlertDialog
            .Builder(activity)
            .setTitle("App Permissions Required")
            .setMessage(
                "This app requires several permissions to function properly:\n\n" +
                    "• Camera: For taking pet photos\n" +
                    "• Storage: For saving and accessing images\n" +
                    "• Notifications: For important updates\n\n" +
                    "Please grant all permissions for the best experience.",
            ).setPositiveButton("Grant Permissions") { _, _ ->
                ActivityCompat.requestPermissions(activity, deniedPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
            }.setNegativeButton("Skip") { _, _ ->
                // Show what features won't work
                showLimitedFunctionalityDialog(activity)
            }.setCancelable(false)
            .show()
    }

    /**
     * Show dialog explaining limited functionality
     */
    private fun showLimitedFunctionalityDialog(activity: Activity) {
        AlertDialog
            .Builder(activity)
            .setTitle("Limited Functionality")
            .setMessage(
                "Without permissions, some features may not work:\n\n" +
                    "• Cannot take or upload photos\n" +
                    "• Won't receive important notifications\n\n" +
                    "You can enable permissions later in Settings.",
            ).setPositiveButton("Open Settings") { _, _ ->
                openAppSettings(activity)
            }.setNegativeButton("Continue Anyway", null)
            .show()
    }

    /**
     * Open app settings
     */
    fun openAppSettings(context: Context) {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        context.startActivity(intent)
    }

    /**
     * Handle permission results
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onGranted: () -> Unit = {},
        onDenied: (List<String>) -> Unit = {},
    ) {
        if (grantResults.isEmpty()) return

        val deniedPermissions = mutableListOf<String>()
        for (i in permissions.indices) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permissions[i])
            }
        }

        if (deniedPermissions.isEmpty()) {
            onGranted()
        } else {
            onDenied(deniedPermissions)
        }
    }

    /**
     * Get permission status text for UI
     */
    fun getPermissionStatusText(context: Context): String {
        val statuses = mutableListOf<String>()

        if (hasCameraPermission(context)) {
            statuses.add("✓ Camera")
        } else {
            statuses.add("✗ Camera")
        }

        if (hasStoragePermissions(context)) {
            statuses.add("✓ Storage")
        } else {
            statuses.add("✗ Storage")
        }

        if (hasNotificationPermission(context)) {
            statuses.add("✓ Notifications")
        } else {
            statuses.add("✗ Notifications")
        }

        return statuses.joinToString(", ")
    }
}
