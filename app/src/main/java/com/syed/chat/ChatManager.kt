package com.syed.chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.syed.utils.FirebaseUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Advanced Real-Time Chat System
 * Supports one-on-one and group conversations with typing indicators
 */
class ChatManager {
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseUtils.auth.currentUser?.uid ?: ""

    data class ChatMessage(
        val id: String = "",
        val conversationId: String = "",
        val senderId: String = "",
        val senderName: String = "",
        val senderImage: String = "",
        val receiverId: String = "",
        val message: String = "",
        val timestamp: Timestamp = Timestamp.now(),
        val isRead: Boolean = false,
        val messageType: MessageType = MessageType.TEXT,
        val mediaUrl: String = "",
        val replyTo: String? = null,
        val metadata: Map<String, Any> = emptyMap(),
        // Advanced features
        val reactions: Map<String, List<String>> = emptyMap(), // emoji -> list of user IDs
        val isEdited: Boolean = false,
        val editedAt: Timestamp? = null,
        val isDeleted: Boolean = false,
        val deletedAt: Timestamp? = null,
        val deliveryStatus: DeliveryStatus = DeliveryStatus.SENT,
        val readBy: List<String> = emptyList(),
        val deliveredTo: List<String> = emptyList(),
        val forwardedFrom: String? = null,
        val linkPreview: LinkPreview? = null,
        val mentions: List<String> = emptyList(), // user IDs mentioned
        val duration: Long = 0, // For voice/video messages in seconds
        val thumbnailUrl: String = "", // For video/image messages
        val fileSize: Long = 0, // For document/media files
        val fileName: String = "", // For document files
        // Advanced features
        val onlineStatus: Map<String, Boolean> = emptyMap(), // userId -> online status
        val lastSeen: Map<String, Timestamp> = emptyMap(), // userId -> last seen
        val admins: List<String> = emptyList(), // For group chats
        val groupName: String? = null,
        val groupImage: String? = null,
        val groupDescription: String? = null,
        val pinnedMessages: List<String> = emptyList(), // Message IDs
        val isMuted: Map<String, Boolean> = emptyMap(), // userId -> muted status
        val wallpaper: String? = null,
        val encryptionEnabled: Boolean = false,
        val latitude: Double = 0.0, // For location messages
        val longitude: Double = 0.0, // For location messages
        val locationName: String = "", // For location messages
    )

    data class LinkPreview(
        val url: String = "",
        val title: String = "",
        val description: String = "",
        val imageUrl: String = "",
        val domain: String = "",
    )

    data class Conversation(
        val id: String = "",
        val participants: List<String> = emptyList(),
        val participantNames: Map<String, String> = emptyMap(),
        val participantImages: Map<String, String> = emptyMap(),
        val lastMessage: String = "",
        val lastMessageTime: Timestamp = Timestamp.now(),
        val lastMessageSender: String = "",
        val unreadCount: Map<String, Int> = emptyMap(),
        val typingUsers: List<String> = emptyList(),
        val isActive: Boolean = true,
        val petId: String? = null,
        val petName: String? = null,
        val conversationType: ConversationType = ConversationType.DIRECT,
        val groupName: String? = null,
        val groupImage: String? = null,
        val groupDescription: String? = null,
        val admins: List<String> = emptyList(),
        val pinnedMessages: List<String> = emptyList(),
        val isMuted: Map<String, Boolean> = emptyMap(),
        val onlineStatus: Map<String, Boolean> = emptyMap(),
        val lastSeen: Map<String, Timestamp> = emptyMap(),
    )

    enum class DeliveryStatus {
        SENDING,
        SENT,
        DELIVERED,
        READ,
        FAILED,
    }

    enum class MessageType {
        TEXT,
        IMAGE,
        VIDEO,
        AUDIO,
        VOICE_NOTE,
        DOCUMENT,
        LOCATION,
        CONTACT,
        STICKER,
        GIF,
        PET_CARD,
    }

    enum class ConversationType {
        DIRECT,
        GROUP,
        SUPPORT,
        CHANNEL,
    }

    /**
     * Get or create conversation between two users
     */
    suspend fun getOrCreateConversation(
        otherUserId: String,
        petId: String? = null,
    ): String {
        val participants = listOf(currentUserId, otherUserId).sorted()
        val conversationId = participants.joinToString("_")

        val conversationRef = db.collection("conversations").document(conversationId)
        val snapshot = conversationRef.get().await()

        if (!snapshot.exists()) {
            // Fetch user details
            val currentUser =
                db
                    .collection("users")
                    .document(currentUserId)
                    .get()
                    .await()
            val otherUser =
                db
                    .collection("users")
                    .document(otherUserId)
                    .get()
                    .await()

            val conversation =
                hashMapOf(
                    "id" to conversationId,
                    "participants" to participants,
                    "participantNames" to
                        mapOf(
                            currentUserId to (currentUser.getString("name") ?: "User"),
                            otherUserId to (otherUser.getString("name") ?: "User"),
                        ),
                    "participantImages" to
                        mapOf(
                            currentUserId to (currentUser.getString("profileImageUrl") ?: ""),
                            otherUserId to (otherUser.getString("profileImageUrl") ?: ""),
                        ),
                    "lastMessage" to "",
                    "lastMessageTime" to Timestamp.now(),
                    "unreadCount" to mapOf(currentUserId to 0, otherUserId to 0),
                    "isActive" to true,
                    "petId" to petId,
                    "conversationType" to ConversationType.DIRECT.name,
                    "createdAt" to Timestamp.now(),
                )

            conversationRef.set(conversation).await()
        }

        return conversationId
    }

    /**
     * Send a message
     */
    suspend fun sendMessage(
        conversationId: String,
        message: String,
        messageType: MessageType = MessageType.TEXT,
        mediaUrl: String = "",
        replyToId: String? = null,
    ): Boolean {
        return try {
            val conversationRef = db.collection("conversations").document(conversationId)
            val conversation = conversationRef.get().await()
            val participants = conversation.get("participants") as? List<String> ?: return false
            val receiverId = participants.firstOrNull { it != currentUserId } ?: return false

            val currentUser =
                db
                    .collection("users")
                    .document(currentUserId)
                    .get()
                    .await()
            val senderName = currentUser.getString("name") ?: "User"
            val senderImage = currentUser.getString("profileImageUrl") ?: ""

            val messageId = db.collection("conversations").document().id
            val chatMessage =
                hashMapOf(
                    "id" to messageId,
                    "conversationId" to conversationId,
                    "senderId" to currentUserId,
                    "senderName" to senderName,
                    "senderImage" to senderImage,
                    "receiverId" to receiverId,
                    "message" to message,
                    "timestamp" to Timestamp.now(),
                    "isRead" to false,
                    "messageType" to messageType.name,
                    "mediaUrl" to mediaUrl,
                    "replyTo" to replyToId,
                )

            // Add message to messages subcollection
            conversationRef
                .collection("messages")
                .document(messageId)
                .set(chatMessage)
                .await()

            // Update conversation metadata
            val unreadCount = conversation.get("unreadCount") as? Map<String, Long> ?: emptyMap()
            val updatedUnreadCount = unreadCount.toMutableMap()
            updatedUnreadCount[receiverId] = (updatedUnreadCount[receiverId] ?: 0) + 1

            conversationRef
                .update(
                    mapOf(
                        "lastMessage" to message,
                        "lastMessageTime" to Timestamp.now(),
                        "lastMessageSender" to currentUserId,
                        "unreadCount" to updatedUnreadCount,
                    ),
                ).await()

            // Send push notification to receiver
            sendMessageNotification(receiverId, senderName, message)

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get messages in real-time
     */
    fun getMessagesFlow(conversationId: String): Flow<List<ChatMessage>> =
        callbackFlow {
            val listener =
                db
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(100)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }

                        val messages =
                            snapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(ChatMessage::class.java)
                            } ?: emptyList()

                        trySend(messages)
                    }

            awaitClose { listener.remove() }
        }

    /**
     * Get all conversations for current user
     */
    fun getConversationsFlow(): Flow<List<Conversation>> =
        callbackFlow {
            val listener =
                db
                    .collection("conversations")
                    .whereArrayContains("participants", currentUserId)
                    .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }

                        val conversations =
                            snapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(Conversation::class.java)
                            } ?: emptyList()

                        trySend(conversations)
                    }

            awaitClose { listener.remove() }
        }

    /**
     * Mark messages as read
     */
    suspend fun markAsRead(conversationId: String) {
        try {
            val conversationRef = db.collection("conversations").document(conversationId)
            val messagesRef = conversationRef.collection("messages")

            // Update unread messages
            val unreadMessages =
                messagesRef
                    .whereEqualTo("receiverId", currentUserId)
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()

            db
                .runBatch { batch ->
                    unreadMessages.documents.forEach { doc ->
                        batch.update(doc.reference, "isRead", true)
                    }
                }.await()

            // Reset unread count
            val conversation = conversationRef.get().await()
            val unreadCount = conversation.get("unreadCount") as? Map<String, Long> ?: emptyMap()
            val updatedUnreadCount = unreadCount.toMutableMap()
            updatedUnreadCount[currentUserId] = 0

            conversationRef.update("unreadCount", updatedUnreadCount).await()
        } catch (e: Exception) {
            // Handle error
        }
    }

    /**
     * Set typing indicator
     */
    suspend fun setTypingStatus(
        conversationId: String,
        isTyping: Boolean,
    ) {
        try {
            val conversationRef = db.collection("conversations").document(conversationId)
            val conversation = conversationRef.get().await()
            val typingUsers = (conversation.get("typingUsers") as? List<String>)?.toMutableList() ?: mutableListOf()

            if (isTyping && !typingUsers.contains(currentUserId)) {
                typingUsers.add(currentUserId)
            } else if (!isTyping) {
                typingUsers.remove(currentUserId)
            }

            conversationRef.update("typingUsers", typingUsers).await()
        } catch (e: Exception) {
            // Handle error
        }
    }

    /**
     * Delete conversation
     */
    suspend fun deleteConversation(conversationId: String): Boolean =
        try {
            val conversationRef = db.collection("conversations").document(conversationId)

            // Delete all messages
            val messages = conversationRef.collection("messages").get().await()
            db
                .runBatch { batch ->
                    messages.documents.forEach { doc ->
                        batch.delete(doc.reference)
                    }
                }.await()

            // Delete conversation
            conversationRef.delete().await()
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Search messages
     */
    suspend fun searchMessages(
        conversationId: String,
        query: String,
    ): List<ChatMessage> =
        try {
            val messages =
                db
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .get()
                    .await()

            messages.documents
                .mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)
                }.filter { message ->
                    message.message.contains(query, ignoreCase = true)
                }
        } catch (e: Exception) {
            emptyList()
        }

    /**
     * Get unread message count
     */
    suspend fun getUnreadCount(): Int =
        try {
            val conversations =
                db
                    .collection("conversations")
                    .whereArrayContains("participants", currentUserId)
                    .get()
                    .await()

            conversations.documents.sumOf { doc ->
                val unreadCount = doc.get("unreadCount") as? Map<String, Long> ?: emptyMap()
                (unreadCount[currentUserId] ?: 0).toInt()
            }
        } catch (e: Exception) {
            0
        }

    /**
     * Send push notification for new message
     */
    private suspend fun sendMessageNotification(
        receiverId: String,
        senderName: String,
        message: String,
    ) {
        try {
            val receiverDoc =
                db
                    .collection("users")
                    .document(receiverId)
                    .get()
                    .await()
            val fcmToken = receiverDoc.getString("fcmToken") ?: return

            // Send FCM notification
            val notificationData =
                hashMapOf(
                    "token" to fcmToken,
                    "title" to "New message from $senderName",
                    "body" to message,
                    "type" to "chat",
                    "senderId" to currentUserId,
                )

            db.collection("notifications_queue").add(notificationData).await()
        } catch (e: Exception) {
            // Handle error
        }
    }

    /**
     * Block user
     */
    suspend fun blockUser(userId: String): Boolean =
        try {
            val userRef = db.collection("users").document(currentUserId)
            val blockedUsers = (userRef.get().await().get("blockedUsers") as? List<String>)?.toMutableList() ?: mutableListOf()

            if (!blockedUsers.contains(userId)) {
                blockedUsers.add(userId)
                userRef.update("blockedUsers", blockedUsers).await()
            }
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Report conversation
     */
    suspend fun reportConversation(
        conversationId: String,
        reason: String,
    ): Boolean =
        try {
            val reportData =
                hashMapOf(
                    "conversationId" to conversationId,
                    "reporterId" to currentUserId,
                    "reason" to reason,
                    "timestamp" to Timestamp.now(),
                    "status" to "pending",
                )

            db.collection("chat_reports").add(reportData).await()
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Add reaction to message
     */
    suspend fun addReaction(
        conversationId: String,
        messageId: String,
        emoji: String,
    ): Boolean =
        try {
            val messageRef =
                db
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document(messageId)

            val message = messageRef.get().await()
            val reactions = (message.get("reactions") as? Map<String, List<String>>)?.toMutableMap() ?: mutableMapOf()

            val userList = reactions[emoji]?.toMutableList() ?: mutableListOf()
            if (!userList.contains(currentUserId)) {
                userList.add(currentUserId)
                reactions[emoji] = userList
            }

            messageRef.update("reactions", reactions).await()
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Remove reaction from message
     */
    suspend fun removeReaction(
        conversationId: String,
        messageId: String,
        emoji: String,
    ): Boolean =
        try {
            val messageRef =
                db
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document(messageId)

            val message = messageRef.get().await()
            val reactions = (message.get("reactions") as? Map<String, List<String>>)?.toMutableMap() ?: mutableMapOf()

            val userList = reactions[emoji]?.toMutableList() ?: mutableListOf()
            userList.remove(currentUserId)

            if (userList.isEmpty()) {
                reactions.remove(emoji)
            } else {
                reactions[emoji] = userList
            }

            messageRef.update("reactions", reactions).await()
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Edit message
     */
    suspend fun editMessage(
        conversationId: String,
        messageId: String,
        newMessage: String,
    ): Boolean =
        try {
            val messageRef =
                db
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document(messageId)

            messageRef
                .update(
                    mapOf(
                        "message" to newMessage,
                        "isEdited" to true,
                        "editedAt" to Timestamp.now(),
                    ),
                ).await()
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Delete message
     */
    suspend fun deleteMessage(
        conversationId: String,
        messageId: String,
        deleteForEveryone: Boolean = false,
    ): Boolean =
        try {
            val messageRef =
                db
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document(messageId)

            if (deleteForEveryone) {
                messageRef
                    .update(
                        mapOf(
                            "message" to "This message was deleted",
                            "isDeleted" to true,
                            "deletedAt" to Timestamp.now(),
                            "mediaUrl" to "",
                        ),
                    ).await()
            } else {
                // Soft delete for current user only
                messageRef.delete().await()
            }
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Forward message
     */
    suspend fun forwardMessage(
        originalConversationId: String,
        originalMessageId: String,
        targetConversationId: String,
    ): Boolean =
        try {
            val originalMessageRef =
                db
                    .collection("conversations")
                    .document(originalConversationId)
                    .collection("messages")
                    .document(originalMessageId)

            val originalMessage = originalMessageRef.get().await()
            val message = originalMessage.getString("message") ?: ""
            val messageType = originalMessage.getString("messageType") ?: MessageType.TEXT.name
            val mediaUrl = originalMessage.getString("mediaUrl") ?: ""

            sendMessage(
                conversationId = targetConversationId,
                message = message,
                messageType = MessageType.valueOf(messageType),
                mediaUrl = mediaUrl,
                replyToId = null,
            )

            // Mark as forwarded
            val newMessageId = db.collection("conversations").document().id
            db
                .collection("conversations")
                .document(targetConversationId)
                .collection("messages")
                .document(newMessageId)
                .update("forwardedFrom", originalConversationId)
                .await()

            true
        } catch (e: Exception) {
            false
        }

    /**
     * Pin message
     */
    suspend fun pinMessage(
        conversationId: String,
        messageId: String,
    ): Boolean =
        try {
            val conversationRef = db.collection("conversations").document(conversationId)
            val conversation = conversationRef.get().await()
            val pinnedMessages = (conversation.get("pinnedMessages") as? List<String>)?.toMutableList() ?: mutableListOf()

            if (!pinnedMessages.contains(messageId)) {
                pinnedMessages.add(messageId)
                conversationRef.update("pinnedMessages", pinnedMessages).await()
            }
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Unpin message
     */
    suspend fun unpinMessage(
        conversationId: String,
        messageId: String,
    ): Boolean =
        try {
            val conversationRef = db.collection("conversations").document(conversationId)
            val conversation = conversationRef.get().await()
            val pinnedMessages = (conversation.get("pinnedMessages") as? List<String>)?.toMutableList() ?: mutableListOf()

            pinnedMessages.remove(messageId)
            conversationRef.update("pinnedMessages", pinnedMessages).await()
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Update online status
     */
    suspend fun updateOnlineStatus(isOnline: Boolean) {
        try {
            val userRef = db.collection("users").document(currentUserId)
            userRef
                .update(
                    mapOf(
                        "isOnline" to isOnline,
                        "lastSeen" to Timestamp.now(),
                    ),
                ).await()

            // Update all conversations
            val conversations =
                db
                    .collection("conversations")
                    .whereArrayContains("participants", currentUserId)
                    .get()
                    .await()

            db
                .runBatch { batch ->
                    conversations.documents.forEach { doc ->
                        val onlineStatus = (doc.get("onlineStatus") as? Map<String, Boolean>)?.toMutableMap() ?: mutableMapOf()
                        onlineStatus[currentUserId] = isOnline

                        val lastSeen = (doc.get("lastSeen") as? Map<String, Timestamp>)?.toMutableMap() ?: mutableMapOf()
                        lastSeen[currentUserId] = Timestamp.now()

                        batch.update(
                            doc.reference,
                            mapOf(
                                "onlineStatus" to onlineStatus,
                                "lastSeen" to lastSeen,
                            ),
                        )
                    }
                }.await()
        } catch (e: Exception) {
            // Handle error
        }
    }

    /**
     * Get online status
     */
    fun getOnlineStatusFlow(userId: String): Flow<Boolean> =
        callbackFlow {
            val listener =
                db
                    .collection("users")
                    .document(userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }

                        val isOnline = snapshot?.getBoolean("isOnline") ?: false
                        trySend(isOnline)
                    }

            awaitClose { listener.remove() }
        }

    /**
     * Mute conversation
     */
    suspend fun muteConversation(
        conversationId: String,
        isMuted: Boolean,
    ): Boolean =
        try {
            val conversationRef = db.collection("conversations").document(conversationId)
            val conversation = conversationRef.get().await()
            val mutedMap = (conversation.get("isMuted") as? Map<String, Boolean>)?.toMutableMap() ?: mutableMapOf()

            mutedMap[currentUserId] = isMuted
            conversationRef.update("isMuted", mutedMap).await()
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Create group conversation
     */
    suspend fun createGroupConversation(
        participantIds: List<String>,
        groupName: String,
        groupImage: String = "",
        groupDescription: String = "",
    ): String? =
        try {
            val allParticipants = (participantIds + currentUserId).distinct()
            val conversationId = "group_${System.currentTimeMillis()}"

            // Fetch participant details
            val participantDetails = mutableMapOf<String, String>()
            val participantImages = mutableMapOf<String, String>()

            allParticipants.forEach { userId ->
                val user =
                    db
                        .collection("users")
                        .document(userId)
                        .get()
                        .await()
                participantDetails[userId] = user.getString("name") ?: "User"
                participantImages[userId] = user.getString("profileImageUrl") ?: ""
            }

            val conversation =
                hashMapOf(
                    "id" to conversationId,
                    "participants" to allParticipants,
                    "participantNames" to participantDetails,
                    "participantImages" to participantImages,
                    "groupName" to groupName,
                    "groupImage" to groupImage,
                    "groupDescription" to groupDescription,
                    "admins" to listOf(currentUserId),
                    "lastMessage" to "",
                    "lastMessageTime" to Timestamp.now(),
                    "unreadCount" to allParticipants.associateWith { 0 },
                    "isActive" to true,
                    "conversationType" to ConversationType.GROUP.name,
                    "createdAt" to Timestamp.now(),
                    "createdBy" to currentUserId,
                )

            db
                .collection("conversations")
                .document(conversationId)
                .set(conversation)
                .await()
            conversationId
        } catch (e: Exception) {
            null
        }

    /**
     * Add participants to group
     */
    suspend fun addParticipantsToGroup(
        conversationId: String,
        participantIds: List<String>,
    ): Boolean =
        try {
            val conversationRef = db.collection("conversations").document(conversationId)
            val conversation = conversationRef.get().await()
            val currentParticipants = (conversation.get("participants") as? List<String>)?.toMutableList() ?: mutableListOf()

            participantIds.forEach { userId ->
                if (!currentParticipants.contains(userId)) {
                    currentParticipants.add(userId)

                    // Fetch user details
                    val user =
                        db
                            .collection("users")
                            .document(userId)
                            .get()
                            .await()
                    val participantNames = (conversation.get("participantNames") as? Map<String, String>)?.toMutableMap() ?: mutableMapOf()
                    val participantImages =
                        (conversation.get("participantImages") as? Map<String, String>)?.toMutableMap() ?: mutableMapOf()

                    participantNames[userId] = user.getString("name") ?: "User"
                    participantImages[userId] = user.getString("profileImageUrl") ?: ""

                    conversationRef
                        .update(
                            mapOf(
                                "participants" to currentParticipants,
                                "participantNames" to participantNames,
                                "participantImages" to participantImages,
                            ),
                        ).await()
                }
            }
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Remove participant from group
     */
    suspend fun removeParticipantFromGroup(
        conversationId: String,
        participantId: String,
    ): Boolean =
        try {
            val conversationRef = db.collection("conversations").document(conversationId)
            val conversation = conversationRef.get().await()
            val participants = (conversation.get("participants") as? List<String>)?.toMutableList() ?: mutableListOf()

            participants.remove(participantId)
            conversationRef.update("participants", participants).await()
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Leave group
     */
    suspend fun leaveGroup(conversationId: String): Boolean = removeParticipantFromGroup(conversationId, currentUserId)

    /**
     * Update delivery status
     */
    suspend fun updateDeliveryStatus(
        conversationId: String,
        messageId: String,
        status: DeliveryStatus,
    ): Boolean =
        try {
            val messageRef =
                db
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document(messageId)

            val updates = mutableMapOf<String, Any>("deliveryStatus" to status.name)

            when (status) {
                DeliveryStatus.DELIVERED -> {
                    val message = messageRef.get().await()
                    val deliveredTo = (message.get("deliveredTo") as? List<String>)?.toMutableList() ?: mutableListOf()
                    if (!deliveredTo.contains(currentUserId)) {
                        deliveredTo.add(currentUserId)
                        updates["deliveredTo"] = deliveredTo
                    }
                }
                DeliveryStatus.READ -> {
                    val message = messageRef.get().await()
                    val readBy = (message.get("readBy") as? List<String>)?.toMutableList() ?: mutableListOf()
                    if (!readBy.contains(currentUserId)) {
                        readBy.add(currentUserId)
                        updates["readBy"] = readBy
                    }
                }
                else -> {}
            }

            messageRef.update(updates).await()
            true
        } catch (e: Exception) {
            false
        }

    /**
     * Get message statistics
     */
    suspend fun getMessageStatistics(conversationId: String): Map<String, Int> =
        try {
            val messages =
                db
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .get()
                    .await()

            val totalMessages = messages.size()
            val textMessages = messages.documents.count { it.getString("messageType") == MessageType.TEXT.name }
            val mediaMessages = totalMessages - textMessages
            val deletedMessages = messages.documents.count { it.getBoolean("isDeleted") == true }

            mapOf(
                "total" to totalMessages,
                "text" to textMessages,
                "media" to mediaMessages,
                "deleted" to deletedMessages,
            )
        } catch (e: Exception) {
            emptyMap()
        }
}
