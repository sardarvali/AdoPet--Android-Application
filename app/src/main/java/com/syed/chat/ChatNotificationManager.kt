package com.syed.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.syed.R
import com.syed.activities.ChatActivity

/**
 * Advanced Chat Notification Handler
 * Features:
 * - Message notifications with inline reply
 * - Notification grouping
 * - Custom notification sounds
 * - Message preview
 */
class ChatNotificationManager(
    private val context: Context,
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        private const val CHANNEL_ID = "chat_messages"
        private const val CHANNEL_NAME = "Chat Messages"
        private const val GROUP_KEY = "chat_group"
        const val NOTIFICATION_ID_BASE = 5000
        const val KEY_TEXT_REPLY = "key_text_reply"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel =
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                    description = "Notifications for chat messages"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show message notification with inline reply
     */
    fun showMessageNotification(
        conversationId: String,
        senderId: String,
        senderName: String,
        senderImage: String,
        message: String,
        timestamp: Long,
    ) {
        val notificationId = conversationId.hashCode()

        // Create intent to open chat
        val intent =
            Intent(context, ChatActivity::class.java).apply {
                putExtra("conversationId", conversationId)
                putExtra("userId", senderId)
                putExtra("userName", senderName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        // Create inline reply action
        val replyLabel = "Reply"
        val remoteInput =
            RemoteInput
                .Builder(KEY_TEXT_REPLY)
                .setLabel(replyLabel)
                .build()

        val replyIntent =
            Intent(context, ChatActivity::class.java).apply {
                putExtra("conversationId", conversationId)
                putExtra("userId", senderId)
            }

        val replyPendingIntent =
            PendingIntent.getActivity(
                context,
                notificationId + 1,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )

        val replyAction =
            NotificationCompat.Action
                .Builder(
                    R.drawable.ic_send,
                    replyLabel,
                    replyPendingIntent,
                ).addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build()

        // Create notification
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat)
                .setContentTitle(senderName)
                .setContentText(message)
                .setWhen(timestamp)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setGroup(GROUP_KEY)
                .addAction(replyAction)
                .setStyle(
                    NotificationCompat
                        .MessagingStyle(
                            Person
                                .Builder()
                                .setName("You")
                                .build(),
                        ).addMessage(
                            message,
                            timestamp,
                            Person
                                .Builder()
                                .setName(senderName)
                                .build(),
                        ),
                ).build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * Show typing notification
     */
    fun showTypingNotification(
        conversationId: String,
        senderName: String,
    ) {
        val notificationId = (conversationId + "_typing").hashCode()

        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat)
                .setContentTitle(senderName)
                .setContentText("is typing...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setTimeoutAfter(3000)
                .setOngoing(true)
                .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * Cancel typing notification
     */
    fun cancelTypingNotification(conversationId: String) {
        val notificationId = (conversationId + "_typing").hashCode()
        notificationManager.cancel(notificationId)
    }

    /**
     * Clear all notifications for a conversation
     */
    fun clearConversationNotifications(conversationId: String) {
        val notificationId = conversationId.hashCode()
        notificationManager.cancel(notificationId)
    }

    /**
     * Clear all chat notifications
     */
    fun clearAllNotifications() {
        notificationManager.cancelAll()
    }
}
