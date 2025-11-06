package com.syed.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real-time Connection Status Manager
 * Monitors network connectivity and Firebase connection status
 */
class ChatConnectionManager {
    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTED)
    val connectionState: Flow<ConnectionState> = _connectionState.asStateFlow()

    private val _messageQueueSize = MutableStateFlow(0)
    val messageQueueSize: Flow<Int> = _messageQueueSize.asStateFlow()

    enum class ConnectionState {
        CONNECTED,
        CONNECTING,
        DISCONNECTED,
        ERROR,
    }

    data class QueuedMessage(
        val conversationId: String,
        val message: String,
        val messageType: ChatManager.MessageType,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private val messageQueue = mutableListOf<QueuedMessage>()

    /**
     * Update connection state
     */
    fun updateConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }

    /**
     * Add message to queue (when offline)
     */
    fun queueMessage(message: QueuedMessage) {
        messageQueue.add(message)
        _messageQueueSize.value = messageQueue.size
    }

    /**
     * Get queued messages
     */
    fun getQueuedMessages(): List<QueuedMessage> = messageQueue.toList()

    /**
     * Clear message from queue
     */
    fun removeQueuedMessage(message: QueuedMessage) {
        messageQueue.remove(message)
        _messageQueueSize.value = messageQueue.size
    }

    /**
     * Clear all queued messages
     */
    fun clearQueue() {
        messageQueue.clear()
        _messageQueueSize.value = 0
    }

    /**
     * Get connection quality indicator
     */
    fun getConnectionQuality(): ConnectionQuality =
        when (_connectionState.value) {
            ConnectionState.CONNECTED -> ConnectionQuality.EXCELLENT
            ConnectionState.CONNECTING -> ConnectionQuality.POOR
            ConnectionState.DISCONNECTED -> ConnectionQuality.NO_CONNECTION
            ConnectionState.ERROR -> ConnectionQuality.NO_CONNECTION
        }

    enum class ConnectionQuality {
        EXCELLENT,
        GOOD,
        POOR,
        NO_CONNECTION,
    }
}
