package com.syed.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.syed.R
import com.syed.adapters.ChatMessagesAdapter
import com.syed.chat.ChatManager
import com.syed.databinding.ActivityChatBinding
import com.syed.utils.FirebaseUtils
import kotlinx.coroutines.launch

/**
 * Original ChatActivity for admin-user interactions
 * Restored functionality for existing chat system
 */
class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var chatManager: ChatManager
    private lateinit var messagesAdapter: ChatMessagesAdapter
    private var conversationId: String = ""
    private var receiverId: String = ""
    private var receiverName: String = ""
    private var isTyping = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get conversation details from intent
        conversationId = intent.getStringExtra("CONVERSATION_ID") ?: ""
        receiverId = intent.getStringExtra("RECEIVER_ID") ?: ""
        receiverName = intent.getStringExtra("RECEIVER_NAME") ?: "Chat"

        if (conversationId.isEmpty() || receiverId.isEmpty()) {
            Toast.makeText(this, "Invalid conversation", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupChat()
        setupClickListeners()
        loadMessages()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = receiverName
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupChat() {
        chatManager = ChatManager()
        messagesAdapter =
            ChatMessagesAdapter(
                currentUserId = FirebaseUtils.getCurrentUserId() ?: "",
                onMessageLongClick = { message ->
                    showMessageOptions(message)
                },
                onReactionClick = { message, reaction ->
                    addReaction(message, reaction)
                },
                onMediaClick = { message ->
                    openMedia(message)
                },
            )

        binding.recyclerViewMessages.apply {
            layoutManager =
                LinearLayoutManager(this@ChatActivity).apply {
                    stackFromEnd = true
                }
            adapter = messagesAdapter
        }
    }

    private fun setupClickListeners() {
        binding.buttonSend.setOnClickListener {
            sendMessage()
        }

        binding.editTextMessage.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }

        // Add typing indicator
        binding.editTextMessage.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {
                    if (!isTyping && s?.isNotEmpty() == true) {
                        isTyping = true
                        lifecycleScope.launch {
                            chatManager.setTypingStatus(conversationId, true)
                        }
                    }
                }

                override fun afterTextChanged(s: android.text.Editable?) {
                    if (isTyping && s?.isEmpty() == true) {
                        isTyping = false
                        lifecycleScope.launch {
                            chatManager.setTypingStatus(conversationId, false)
                        }
                    }
                }
            },
        )
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            chatManager.getMessagesFlow(conversationId).collect { messages ->
                messagesAdapter.updateMessages(messages)
                if (messages.isNotEmpty()) {
                    binding.recyclerViewMessages.smoothScrollToPosition(messages.size - 1)
                }
            }
        }
    }

    private fun sendMessage() {
        val messageText =
            binding.editTextMessage.text
                .toString()
                .trim()
        if (messageText.isEmpty()) return

        lifecycleScope.launch {
            try {
                chatManager.sendMessage(
                    conversationId = conversationId,
                    message = messageText,
                    messageType = ChatManager.MessageType.TEXT,
                )
                binding.editTextMessage.text?.clear()
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showMessageOptions(message: ChatManager.ChatMessage) {
        // Show options like edit, delete, reply, copy, etc.
    }

    private fun addReaction(
        message: ChatManager.ChatMessage,
        reaction: String,
    ) {
        lifecycleScope.launch {
            try {
                chatManager.addReaction(message.id, reaction, reaction)
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Failed to add reaction", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openMedia(message: ChatManager.ChatMessage) {
        // Open media viewer for images, videos, documents
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Only inflate menu if it exists, otherwise skip
        try {
            menuInflater.inflate(R.menu.menu_chat, menu)
        } catch (e: Exception) {
            // Menu doesn't exist, skip
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_view_profile -> {
                // Open receiver's profile
                true
            }
            R.id.action_search -> {
                // Search in conversation
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onDestroy() {
        super.onDestroy()
        if (isTyping) {
            lifecycleScope.launch {
                chatManager.setTypingStatus(conversationId, false)
            }
        }
    }
}
