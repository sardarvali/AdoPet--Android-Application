package com.syed.models

import java.util.Date

/**
 * Conversation model for managing chat conversations between users
 */
data class Conversation(
    var id: String = "",
    val participants: List<String> = emptyList(), // List of user IDs
    val participantNames: Map<String, String> = emptyMap(), // Map of userId to name
    val participantPhotos: Map<String, String> = emptyMap(), // Map of userId to photo URL
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageTime: Date = Date(),
    val unreadCount: Map<String, Int> = emptyMap(), // Map of userId to unread count
    val conversationType: String = "direct", // direct, adoption, shelter, support
    val relatedEntityId: String = "", // Pet ID or Shelter ID if applicable
    val relatedEntityType: String = "", // pet, shelter, etc.
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
) {
    /**
     * Get the other participant's ID (for direct conversations)
     */
    fun getOtherParticipantId(currentUserId: String?): String = participants.firstOrNull { it != currentUserId } ?: ""

    /**
     * Get the other participant's name
     */
    fun getOtherParticipantName(currentUserId: String?): String {
        val otherUserId = getOtherParticipantId(currentUserId)
        return participantNames[otherUserId] ?: "Unknown User"
    }

    /**
     * Get the other participant's photo URL
     */
    fun getOtherParticipantPhoto(currentUserId: String?): String {
        val otherUserId = getOtherParticipantId(currentUserId)
        return participantPhotos[otherUserId] ?: ""
    }

    /**
     * Get unread count for current user
     */
    fun getUnreadCount(currentUserId: String?): Int = unreadCount[currentUserId] ?: 0

    /**
     * Get display name for the conversation
     */
    val recipientName: String
        get() = participantNames.values.firstOrNull() ?: "Chat"
}
