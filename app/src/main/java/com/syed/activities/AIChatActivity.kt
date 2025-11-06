package com.syed.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.syed.adapters.ChatAdapter
import com.syed.databinding.ActivityAiChatBinding
import com.syed.models.ChatMessage
import com.syed.services.AIApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Secure AI Chat Activity for AI-powered pet conversations
 * Separate from the existing admin-user chat system
 */
class AIChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAiChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var aiApiService: AIApiService
    private val messageList = mutableListOf<ChatMessage>()
    private var petBreed: String = "pet"
    private var isAiTyping = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get pet breed from intent with validation
        petBreed = intent.getStringExtra("PET_BREED")?.takeIf { it.isNotBlank() } ?: "pet"

        // Initialize secure AI service
        aiApiService = AIApiService.getInstance(this)

        setupUI()
        setupRecyclerView()
        setupClickListeners()

        // Add welcome message
        addWelcomeMessage()
    }

    private fun setupUI() {
        // Set up toolbar
        binding.toolbar.title = "Chat about $petBreed"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.chatRecyclerView.apply {
            layoutManager =
                LinearLayoutManager(this@AIChatActivity).apply {
                    stackFromEnd = true // Start from bottom
                }
            adapter = chatAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        // Send message on Enter key (optional enhancement)
        binding.etMessage.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
    }

    private fun addWelcomeMessage() {
        val welcomeMessage =
            ChatMessage(
                message = "Hello! I'm your AI pet expert. Ask me anything about your $petBreed - from care tips to behavior questions. How can I help you today?",
                isUser = false,
            )
        addMessage(welcomeMessage)
    }

    private fun sendMessage() {
        val userInput =
            binding.etMessage.text
                .toString()
                .trim()

        // Input validation
        if (userInput.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        if (userInput.length > 500) {
            Toast.makeText(this, "Message too long. Please keep it under 500 characters.", Toast.LENGTH_SHORT).show()
            return
        }

        if (isAiTyping) {
            Toast.makeText(this, "Please wait for AI to respond", Toast.LENGTH_SHORT).show()
            return
        }

        // Add user message
        val userMessage = ChatMessage(message = userInput, isUser = true)
        addMessage(userMessage)

        // Clear input
        binding.etMessage.text?.clear()

        // Send to AI
        sendToAI(userInput)
    }

    private fun sendToAI(userInput: String) {
        showTypingIndicator()

        aiApiService.sendChatMessage(
            userMessage = userInput,
            petBreed = petBreed,
            onSuccess = { aiResponse ->
                runOnUiThread {
                    hideTypingIndicator()
                    // Add small delay for better UX
                    lifecycleScope.launch {
                        delay(500) // Simulate thinking time
                        val aiMessage = ChatMessage(message = aiResponse, isUser = false)
                        addMessage(aiMessage)
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    hideTypingIndicator()
                    handleAIError(error)
                }
            },
        )
    }

    private fun addMessage(message: ChatMessage) {
        messageList.add(message)
        chatAdapter.submitList(messageList.toList()) // Create new list to trigger DiffUtil

        // Scroll to bottom with smooth animation
        lifecycleScope.launch {
            delay(100) // Small delay to ensure item is added
            binding.chatRecyclerView.smoothScrollToPosition(messageList.size - 1)
        }
    }

    private fun showTypingIndicator() {
        isAiTyping = true
        binding.typingIndicator.visibility = View.VISIBLE
        binding.btnSend.isEnabled = false

        // Scroll to show typing indicator
        lifecycleScope.launch {
            delay(100)
            binding.chatRecyclerView.smoothScrollToPosition(messageList.size)
        }
    }

    private fun hideTypingIndicator() {
        isAiTyping = false
        binding.typingIndicator.visibility = View.GONE
        binding.btnSend.isEnabled = true
    }

    private fun handleAIError(error: String) {
        // Add error message as AI response for better UX
        val errorMessage =
            when {
                error.contains("API key") -> "I'm having trouble connecting right now. Please check your internet connection and try again."
                error.contains("Too many requests") -> "I'm getting a lot of questions right now. Please wait a moment and try again."
                error.contains("Network") -> "I'm having connection issues. Please check your internet and try again."
                else -> "I'm sorry, I encountered an issue. Please try rephrasing your question."
            }

        val aiMessage = ChatMessage(message = errorMessage, isUser = false)
        addMessage(aiMessage)

        // Also show toast for technical users
        Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideTypingIndicator()
    }
}
