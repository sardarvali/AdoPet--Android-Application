package com.syed.activities.admin

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.syed.R
import com.syed.adapters.ContactMessagesAdapter
import com.syed.databinding.ActivityContactMessagesBinding
import com.syed.models.ContactMessage
import com.syed.utils.FirebaseUtils

class ContactMessagesActivity : AdminBaseActivity() {
    private lateinit var binding: ActivityContactMessagesBinding
    private lateinit var adapter: ContactMessagesAdapter
    private val messages = mutableListOf<ContactMessage>()

    override fun onAdminVerified() {
        binding = ActivityContactMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadMessages()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Contact Messages"
    }

    private fun setupRecyclerView() {
        adapter =
            ContactMessagesAdapter(messages) { message, action ->
                when (action) {
                    "reply" -> replyToMessage(message)
                    "mark_read" -> markAsRead(message)
                    "view" -> viewMessageDetails(message)
                }
            }

        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter
    }

    private fun loadMessages() {
        binding.progressBar.visibility = View.VISIBLE

        FirebaseUtils.firestore
            .collection(FirebaseUtils.CONTACT_MESSAGES_COLLECTION)
            .orderBy("sentDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE

                if (error != null) {
                    android.util.Log.e("ContactMessages", "Error loading messages", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    messages.clear()
                    for (document in snapshot.documents) {
                        try {
                            val message = document.toObject(ContactMessage::class.java)
                            message?.let {
                                it.id = document.id
                                messages.add(it)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ContactMessages", "Error parsing message", e)
                        }
                    }
                    adapter.notifyDataSetChanged()

                    binding.tvEmptyState.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
                }
            }
    }

    private fun replyToMessage(message: ContactMessage) {
        // Show dialog to get admin reply
        val builder =
            androidx.appcompat.app.AlertDialog
                .Builder(this)
        val input = android.widget.EditText(this)
        input.hint = "Enter your reply"
        input.setPadding(50, 30, 50, 30)

        builder
            .setTitle("Reply to ${message.userName}")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val replyText = input.text.toString().trim()
                if (replyText.isNotEmpty()) {
                    sendReply(message, replyText)
                } else {
                    android.widget.Toast
                        .makeText(this, "Reply cannot be empty", android.widget.Toast.LENGTH_SHORT)
                        .show()
                }
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendReply(
        message: ContactMessage,
        replyText: String,
    ) {
        val updates =
            hashMapOf<String, Any>(
                "adminReply" to replyText,
                "isReplied" to true,
                "isRead" to true,
                "status" to "replied",
                "replyDate" to System.currentTimeMillis(),
                "repliedBy" to (FirebaseUtils.getCurrentUserId() ?: "admin"),
            )

        FirebaseUtils.firestore
            .collection(FirebaseUtils.CONTACT_MESSAGES_COLLECTION)
            .document(message.id)
            .update(updates)
            .addOnSuccessListener {
                android.widget.Toast
                    .makeText(this, "Reply sent successfully", android.widget.Toast.LENGTH_SHORT)
                    .show()

                // Send notification to user
                sendReplyNotificationToUser(message.userId, message.subject, replyText)
            }.addOnFailureListener { e ->
                android.widget.Toast
                    .makeText(this, "Failed to send reply: ${e.message}", android.widget.Toast.LENGTH_SHORT)
                    .show()
                android.util.Log.e("ContactMessages", "Error sending reply", e)
            }
    }

    private fun sendReplyNotificationToUser(
        userId: String,
        subject: String,
        reply: String,
    ) {
        // Create a notification for the user
        val notification =
            hashMapOf(
                "userId" to userId,
                "title" to "Reply to: $subject",
                "message" to reply,
                "type" to "contact_reply",
                "timestamp" to System.currentTimeMillis(),
                "read" to false,
            )

        FirebaseUtils.firestore
            .collection("user_notifications")
            .add(notification)
            .addOnFailureListener { e ->
                android.util.Log.e("ContactMessages", "Failed to send notification to user", e)
            }
    }

    private fun markAsRead(message: ContactMessage) {
        FirebaseUtils.firestore
            .collection(FirebaseUtils.CONTACT_MESSAGES_COLLECTION)
            .document(message.id)
            .update("isRead", true)
            .addOnSuccessListener {
                android.util.Log.d("ContactMessages", "Message marked as read")
            }.addOnFailureListener { e ->
                android.util.Log.e("ContactMessages", "Error marking message as read", e)
            }
    }

    private fun viewMessageDetails(message: ContactMessage) {
        // Show detailed message dialog
        val builder =
            androidx.appcompat.app.AlertDialog
                .Builder(this)
        val messageView = android.widget.LinearLayout(this)
        messageView.orientation = android.widget.LinearLayout.VERTICAL
        messageView.setPadding(50, 30, 50, 30)

        // Create TextView for message details
        val detailsText = android.widget.TextView(this)
        detailsText.text =
            """
            From: ${message.userName}
            Email: ${message.userEmail}
            Phone: ${message.userPhone}
            Subject: ${message.subject}
            
            Message:
            ${message.message}
            
            ${if (message.isReplied) "\nAdmin Reply:\n${message.adminReply}" else ""}
            """.trimIndent()
        detailsText.textSize = 16f
        messageView.addView(detailsText)

        builder
            .setTitle("Message Details")
            .setView(messageView)
            .setPositiveButton("Close", null)
            .setNeutralButton("Reply") { _, _ ->
                replyToMessage(message)
            }.show()

        // Mark as read
        if (!message.isRead) {
            markAsRead(message)
        }
    }
}
