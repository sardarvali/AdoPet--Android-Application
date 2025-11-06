package com.syed.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.syed.MainActivity
import com.syed.R

object NotificationUtils {
    private const val CHANNEL_ID_GENERAL = "pet_adoption_general"
    private const val CHANNEL_ID_REQUESTS = "pet_adoption_requests"
    private const val CHANNEL_ID_ADMIN = "pet_adoption_admin"

    private const val CHANNEL_NAME_GENERAL = "General Notifications"
    private const val CHANNEL_NAME_REQUESTS = "Request Updates"
    private const val CHANNEL_NAME_ADMIN = "Admin Notifications"

    // Notification types
    const val TYPE_NEW_REQUEST = "new_request"
    const val TYPE_REQUEST_UPDATE = "request_update"
    const val TYPE_ADMIN_MESSAGE = "admin_message"
    const val TYPE_RESCUE_UPDATE = "rescue_update"
    const val TYPE_GENERAL = "general"

    /**
     * Create all notification channels
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // General notifications channel
            val generalChannel =
                NotificationChannel(
                    CHANNEL_ID_GENERAL,
                    CHANNEL_NAME_GENERAL,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "General app notifications"
                    enableVibration(true)
                }

            // Request updates channel
            val requestsChannel =
                NotificationChannel(
                    CHANNEL_ID_REQUESTS,
                    CHANNEL_NAME_REQUESTS,
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Updates about your adoption and rescue requests"
                    enableVibration(true)
                    enableLights(true)
                }

            // Admin notifications channel
            val adminChannel =
                NotificationChannel(
                    CHANNEL_ID_ADMIN,
                    CHANNEL_NAME_ADMIN,
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Important admin notifications"
                    enableVibration(true)
                    enableLights(true)
                }

            notificationManager.createNotificationChannels(listOf(generalChannel, requestsChannel, adminChannel))
        }
    }

    /**
     * Send local notification
     */
    fun sendLocalNotification(
        context: Context,
        title: String,
        message: String,
        type: String = "general",
        data: Map<String, String> = emptyMap(),
    ) {
        val channelId =
            when (type) {
                "admin", "new_request", "admin_message" -> CHANNEL_ID_ADMIN
                "request_update", "adoption_status", "rescue_status" -> CHANNEL_ID_REQUESTS
                else -> CHANNEL_ID_GENERAL
            }

        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("notification_type", type)
                data.forEach { (key, value) ->
                    putExtra(key, value)
                }
            }

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat
                .Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Send notification to admin about new requests
     */
    fun notifyAdminNewRequest(
        context: Context,
        requestType: String,
        location: String = "",
    ) {
        val title =
            when (requestType) {
                "adoption" -> "🐾 New Adoption Request"
                "rescue" -> "🆘 New Rescue Request"
                "contact" -> "💬 New Contact Message"
                else -> "📝 New Request"
            }

        val message =
            when (requestType) {
                "adoption" -> "A new pet adoption request has been submitted"
                "rescue" -> "New rescue request from $location"
                "contact" -> "New contact message received"
                else -> "A new request requires your attention"
            }

        sendLocalNotification(context, title, message, "admin", mapOf("request_type" to requestType))

        // Also save to admin notifications collection
        saveAdminNotification(title, message, requestType)
    }

    /**
     * Send notification to user about request updates
     */
    fun notifyUserRequestUpdate(
        context: Context,
        requestType: String,
        status: String,
        adminMessage: String = "",
    ) {
        val title =
            when (status) {
                "approved" -> "✅ Request Approved!"
                "rejected" -> "❌ Request Update"
                "in_progress" -> "⏳ Request In Progress"
                "completed" -> "🎉 Request Completed!"
                else -> "📝 Request Update"
            }

        val message =
            if (adminMessage.isNotEmpty()) {
                adminMessage
            } else {
                when (requestType) {
                    "adoption" -> "Your adoption request status has been updated to: $status"
                    "rescue" -> "Your rescue request status has been updated to: $status"
                    else -> "Your request has been updated"
                }
            }

        sendLocalNotification(
            context,
            title,
            message,
            "request_update",
            mapOf(
                "request_type" to requestType,
                "status" to status,
            ),
        )
    }

    /**
     * Send notification about new pets available
     */
    fun notifyNewPetAvailable(
        context: Context,
        petName: String,
        petType: String,
    ) {
        val title = "🐾 New Pet Available!"
        val message = "Meet $petName, a lovely $petType looking for a home!"

        sendLocalNotification(
            context,
            title,
            message,
            "new_pet",
            mapOf(
                "pet_name" to petName,
                "pet_type" to petType,
            ),
        )
    }

    /**
     * Subscribe admin to admin topic
     */
    fun subscribeToAdminTopic() {
        FirebaseMessaging
            .getInstance()
            .subscribeToTopic("admin")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("NotificationUtils", "Subscribed to admin topic")
                } else {
                    android.util.Log.e("NotificationUtils", "Failed to subscribe to admin topic", task.exception)
                }
            }
    }

    /**
     * Send admin notification
     */
    fun sendAdminNotification(
        context: Context,
        title: String,
        message: String,
        type: String,
    ) {
        sendLocalNotification(context, title, message, type)
        saveAdminNotification(title, message, type)
    }

    /**
     * Notify rescue request update
     */
    fun notifyRescueRequestUpdate(
        context: Context,
        requestId: String,
        status: String,
        adminMessage: String = "",
    ) {
        notifyUserRequestUpdate(context, "rescue", status, adminMessage)
    }

    /**
     * Show notification (generic)
     */
    fun showNotification(
        context: Context,
        title: String,
        message: String,
    ) {
        sendLocalNotification(context, title, message, TYPE_GENERAL)
    }

    /**
     * Save admin notification to Firestore
     */
    private fun saveAdminNotification(
        title: String,
        message: String,
        type: String,
    ) {
        val db = FirebaseFirestore.getInstance()
        val notification =
            hashMapOf(
                "title" to title,
                "message" to message,
                "type" to type,
                "timestamp" to System.currentTimeMillis(),
                "read" to false,
            )

        db
            .collection("admin_notifications")
            .add(notification)
            .addOnSuccessListener {
                android.util.Log.d("NotificationUtils", "Admin notification saved")
            }.addOnFailureListener { e ->
                android.util.Log.e("NotificationUtils", "Failed to save admin notification", e)
            }
    }
}
