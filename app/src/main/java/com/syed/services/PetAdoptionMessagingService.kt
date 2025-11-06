package com.syed.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.syed.MainActivity
import com.syed.R
import com.syed.utils.FirebaseUtils

class PetAdoptionMessagingService : FirebaseMessagingService() {
    companion object {
        private const val CHANNEL_ID = "pet_adoption_notifications"
        private const val CHANNEL_NAME = "Pet Adoption Notifications"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Save token to Firestore for current user
        val currentUser = FirebaseUtils.auth.currentUser
        currentUser?.let { user ->
            FirebaseUtils.firestore
                .collection(FirebaseUtils.USERS_COLLECTION)
                .document(user.uid)
                .update("fcmToken", token)
                .addOnFailureListener { e ->
                    android.util.Log.e("FCM", "Failed to save token", e)
                }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Extract notification data
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Pet Adoption"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["message"] ?: ""
        val type = remoteMessage.data["type"] ?: "general"

        // Show notification
        showNotification(title, body, type)

        // Save notification to Firestore for persistence
        saveNotificationToFirestore(title, body, type, remoteMessage.data)
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String,
    ) {
        val intent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("notification_type", type)
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun saveNotificationToFirestore(
        title: String,
        body: String,
        type: String,
        data: Map<String, String>,
    ) {
        val currentUser = FirebaseUtils.auth.currentUser ?: return

        val notification =
            hashMapOf(
                "userId" to currentUser.uid,
                "title" to title,
                "message" to body,
                "type" to type,
                "data" to data,
                "timestamp" to System.currentTimeMillis(),
                "read" to false,
                "received" to true,
            )

        FirebaseUtils.firestore
            .collection("user_notifications")
            .add(notification)
            .addOnFailureListener { e ->
                android.util.Log.e("FCM", "Failed to save notification", e)
            }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Notifications for pet adoption updates"
                    enableVibration(true)
                    enableLights(true)
                }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
