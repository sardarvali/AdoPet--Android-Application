package com.syed.models

/**
 * Data class representing a chat message in the AI conversation
 * @param message The text content of the message
 * @param isUser True if the message is from the user, false if from AI
 * @param timestamp The timestamp when the message was created
 */
data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)
