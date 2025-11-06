package com.syed.chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Chat Analytics & Statistics Tracker
 * Tracks chat metrics, user activity, and generates insights
 */
class ChatAnalytics {
    private val db = FirebaseFirestore.getInstance()

    data class ChatStats(
        val totalMessages: Int = 0,
        val messagesThisWeek: Int = 0,
        val messagesThisMonth: Int = 0,
        val averageResponseTime: Long = 0, // in seconds
        val mostActiveHour: Int = 0,
        val mostUsedEmoji: String = "",
        val mediaMessagesCount: Int = 0,
        val voiceMessagesCount: Int = 0,
        val averageMessageLength: Int = 0,
    )

    data class ConversationInsight(
        val conversationId: String = "",
        val totalMessages: Int = 0,
        val lastActivity: Date = Date(),
        val longestGap: Long = 0, // in hours
        val sentiment: String = "neutral", // positive, neutral, negative
        val topKeywords: List<String> = emptyList(),
    )

    /**
     * Get statistics for a conversation
     */
    suspend fun getConversationStats(conversationId: String): ChatStats =
        try {
            val messages =
                db
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .get()
                    .await()

            val totalMessages = messages.size()

            val calendar = Calendar.getInstance()

            // Messages this week
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val weekAgo = Timestamp(calendar.time)
            val messagesThisWeek =
                messages.documents.count {
                    val timestamp = it.getTimestamp("timestamp")
                    timestamp != null && timestamp > weekAgo
                }

            // Messages this month
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -30)
            val monthAgo = Timestamp(calendar.time)
            val messagesThisMonth =
                messages.documents.count {
                    val timestamp = it.getTimestamp("timestamp")
                    timestamp != null && timestamp > monthAgo
                }

            // Count media and voice messages
            val mediaMessagesCount =
                messages.documents.count {
                    val type = it.getString("messageType")
                    type in listOf("IMAGE", "VIDEO", "DOCUMENT")
                }

            val voiceMessagesCount =
                messages.documents.count {
                    val type = it.getString("messageType")
                    type in listOf("VOICE_NOTE", "AUDIO")
                }

            // Calculate average message length
            val totalLength =
                messages.documents.sumOf {
                    it.getString("message")?.length ?: 0
                }
            val averageMessageLength = if (totalMessages > 0) totalLength / totalMessages else 0

            ChatStats(
                totalMessages = totalMessages,
                messagesThisWeek = messagesThisWeek,
                messagesThisMonth = messagesThisMonth,
                mediaMessagesCount = mediaMessagesCount,
                voiceMessagesCount = voiceMessagesCount,
                averageMessageLength = averageMessageLength,
            )
        } catch (e: Exception) {
            ChatStats()
        }

    /**
     * Get conversation insights
     */
    suspend fun getConversationInsight(conversationId: String): ConversationInsight =
        try {
            val messages =
                db
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .orderBy("timestamp")
                    .get()
                    .await()

            val totalMessages = messages.size()

            val lastActivity =
                messages.documents
                    .lastOrNull()
                    ?.getTimestamp("timestamp")
                    ?.toDate() ?: Date()

            // Calculate longest gap between messages
            var longestGap = 0L
            for (i in 1 until messages.documents.size) {
                val prev = messages.documents[i - 1].getTimestamp("timestamp")
                val current = messages.documents[i].getTimestamp("timestamp")

                if (prev != null && current != null) {
                    val gap = (current.seconds - prev.seconds) / 3600 // in hours
                    if (gap > longestGap) {
                        longestGap = gap
                    }
                }
            }

            ConversationInsight(
                conversationId = conversationId,
                totalMessages = totalMessages,
                lastActivity = lastActivity,
                longestGap = longestGap,
            )
        } catch (e: Exception) {
            ConversationInsight(conversationId = conversationId)
        }

    /**
     * Track message sent event
     */
    suspend fun trackMessageSent(
        conversationId: String,
        messageType: String,
        messageLength: Int,
    ) {
        try {
            val analyticsData =
                hashMapOf(
                    "conversationId" to conversationId,
                    "messageType" to messageType,
                    "messageLength" to messageLength,
                    "timestamp" to Timestamp.now(),
                )

            db
                .collection("chat_analytics")
                .add(analyticsData)
                .await()
        } catch (e: Exception) {
            // Ignore analytics errors
        }
    }

    /**
     * Get user's overall chat statistics
     */
    suspend fun getUserChatStats(userId: String): Map<String, Any> =
        try {
            val conversations =
                db
                    .collection("conversations")
                    .whereArrayContains("participants", userId)
                    .get()
                    .await()

            val totalConversations = conversations.size()
            var totalMessages = 0
            var totalUnread = 0

            conversations.documents.forEach { doc ->
                val unreadCount = (doc.get("unreadCount") as? Map<String, Long>)?.get(userId) ?: 0
                totalUnread += unreadCount.toInt()

                // Count messages in this conversation
                val messageCount =
                    db
                        .collection("conversations")
                        .document(doc.id)
                        .collection("messages")
                        .get()
                        .await()
                        .size()

                totalMessages += messageCount
            }

            mapOf(
                "totalConversations" to totalConversations,
                "totalMessages" to totalMessages,
                "totalUnread" to totalUnread,
                "averageMessagesPerConversation" to if (totalConversations > 0) totalMessages / totalConversations else 0,
            )
        } catch (e: Exception) {
            emptyMap()
        }
}
