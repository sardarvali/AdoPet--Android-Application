package com.syed.activities.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.syed.databinding.ActivitySendNotificationBinding
import com.syed.models.User
import com.syed.utils.FirebaseUtils
import com.syed.utils.NotificationUtils

class SendNotificationActivity : AdminBaseActivity() {
    private lateinit var binding: ActivitySendNotificationBinding

    override fun onAdminVerified() {
        binding = ActivitySendNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Send Notification"
    }

    private fun setupClickListeners() {
        binding.btnSendToAll.setOnClickListener {
            sendNotificationToAllUsers()
        }

        binding.btnSendToSpecific.setOnClickListener {
            // Future enhancement: select specific users
            Toast.makeText(this, "Feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendNotificationToAllUsers() {
        val title =
            binding.etNotificationTitle.text
                .toString()
                .trim()
        val message =
            binding.etNotificationMessage.text
                .toString()
                .trim()

        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSendToAll.isEnabled = false

        // Get all users
        FirebaseUtils.firestore
            .collection(FirebaseUtils.USERS_COLLECTION)
            .get()
            .addOnSuccessListener { documents ->
                var successCount = 0
                var errorCount = 0
                val totalUsers = documents.size()

                if (totalUsers == 0) {
                    Toast.makeText(this, "No users found", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    binding.btnSendToAll.isEnabled = true
                    return@addOnSuccessListener
                }

                documents.forEach { doc ->
                    val user = doc.toObject(User::class.java)

                    // Create notification document
                    val notification =
                        hashMapOf(
                            "userId" to user.uid,
                            "title" to title,
                            "message" to message,
                            "type" to "admin_message",
                            "timestamp" to System.currentTimeMillis(),
                            "read" to false,
                            "sentBy" to FirebaseUtils.auth.currentUser?.uid,
                        )

                    FirebaseUtils.firestore
                        .collection("notifications")
                        .add(notification)
                        .addOnSuccessListener {
                            successCount++
                            if (successCount + errorCount == totalUsers) {
                                showResult(successCount, errorCount)
                            }
                        }.addOnFailureListener {
                            errorCount++
                            if (successCount + errorCount == totalUsers) {
                                showResult(successCount, errorCount)
                            }
                        }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.btnSendToAll.isEnabled = true
            }
    }

    private fun showResult(
        successCount: Int,
        errorCount: Int,
    ) {
        binding.progressBar.visibility = View.GONE
        binding.btnSendToAll.isEnabled = true

        val message =
            "Sent to $successCount users" +
                if (errorCount > 0) " ($errorCount failed)" else ""
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        if (successCount > 0) {
            // Clear form
            binding.etNotificationTitle.text?.clear()
            binding.etNotificationMessage.text?.clear()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
