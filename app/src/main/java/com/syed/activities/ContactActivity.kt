package com.syed.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.syed.databinding.ActivityContactBinding
import com.syed.models.ContactMessage
import com.syed.utils.FirebaseUtils

class ContactActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        loadUserInfo()
        loadOfficeDetails()
        checkUserMessages()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Contact Us"
    }

    private fun setupClickListeners() {
        binding.btnSendMessage.setOnClickListener {
            sendMessage()
        }

        binding.btnViewMyMessages.setOnClickListener {
            checkUserMessages()
        }

        binding.btnViewLocation.setOnClickListener {
            // Handle view location click - could open maps or show more details
            Toast.makeText(this, "Location feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserInfo() {
        val currentUser = FirebaseUtils.auth.currentUser
        currentUser?.let { user ->
            binding.etName.setText(user.displayName ?: "")
            binding.etEmail.setText(user.email ?: "")

            // Make email field read-only for authenticated users
            binding.etEmail.isEnabled = false
        }
    }

    private fun loadOfficeDetails() {
        // Remove references to non-existent views and simplify
        FirebaseUtils.firestore
            .collection("office_details")
            .document("main_office")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data
                    binding.tvOfficeAddress.text = data?.get("address") as? String ?: "Address not set"
                    binding.tvOfficePhone.text = data?.get("phone") as? String ?: "Phone not set"
                    binding.tvOfficeHours.text = data?.get("hours") as? String ?: "Hours not set"
                } else {
                    showDefaultOfficeDetails()
                }
            }.addOnFailureListener { e ->
                android.util.Log.e("ContactActivity", "Failed to load office details", e)
                showDefaultOfficeDetails()
            }
    }

    private fun showDefaultOfficeDetails() {
        binding.tvOfficeAddress.text = "123 Pet Love Street, Animal City, AC 12345"
        binding.tvOfficePhone.text = "+1-555-PET-LOVE (555-738-5683)"
        binding.tvOfficeHours.text = "Monday - Friday: 9:00 AM - 6:00 PM\nSaturday: 10:00 AM - 4:00 PM\nSunday: 12:00 PM - 4:00 PM"
    }

    private fun checkUserMessages() {
        val currentUser = FirebaseUtils.auth.currentUser
        currentUser?.let { user ->
            FirebaseUtils.firestore
                .collection(FirebaseUtils.CONTACT_MESSAGES_COLLECTION)
                .whereEqualTo("userEmail", user.email)
                .orderBy("sentDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        // Only set visibility if the view exists
                        binding.btnViewMyMessages?.visibility = View.VISIBLE
                        binding.btnViewMyMessages?.text = "View My Messages (${documents.size()})"

                        binding.btnViewMyMessages?.setOnClickListener {
                            // Show user's message history with admin replies
                            showUserMessageHistory(documents.map { it.toObject(ContactMessage::class.java) })
                        }
                    } else {
                        binding.btnViewMyMessages?.visibility = View.GONE
                    }
                }.addOnFailureListener { e ->
                    android.util.Log.e("ContactActivity", "Failed to load user messages", e)
                }
        }
    }

    private fun showUserMessageHistory(messages: List<ContactMessage>) {
        val intent = Intent(this, UserMessagesActivity::class.java)
        startActivity(intent)
    }

    private fun sendMessage() {
        val name =
            binding.etName.text
                .toString()
                .trim()
        val email =
            binding.etEmail.text
                .toString()
                .trim()
        val subject =
            binding.etSubject.text
                .toString()
                .trim()
        val message =
            binding.etMessage.text
                .toString()
                .trim()

        if (!validateForm(name, email, subject, message)) {
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSendMessage.isEnabled = false

        val currentUser = FirebaseUtils.auth.currentUser
        val contactMessage =
            ContactMessage(
                userName = name,
                userEmail = email,
                subject = subject,
                message = message,
                userId = currentUser?.uid ?: "",
                userPhone = "",
            )

        FirebaseUtils.firestore
            .collection(FirebaseUtils.CONTACT_MESSAGES_COLLECTION)
            .add(contactMessage)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Message sent successfully! We'll get back to you soon.", Toast.LENGTH_LONG).show()

                // Send notification to admin
                com.syed.utils.NotificationUtils
                    .notifyAdminNewRequest(this, "contact")

                // Clear form
                clearForm()

                // Refresh user messages
                checkUserMessages()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
            }.addOnCompleteListener {
                binding.progressBar.visibility = View.GONE
                binding.btnSendMessage.isEnabled = true
            }
    }

    private fun validateForm(
        name: String,
        email: String,
        subject: String,
        message: String,
    ): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()
        ) {
            binding.etEmail.error = "Please enter a valid email"
            isValid = false
        }

        if (subject.isEmpty()) {
            binding.etSubject.error = "Subject is required"
            isValid = false
        }

        if (message.isEmpty()) {
            binding.etMessage.error = "Message is required"
            isValid = false
        }

        return isValid
    }

    private fun clearForm() {
        binding.etSubject.text?.clear()
        binding.etMessage.text?.clear()

        // Clear errors
        binding.etName.error = null
        binding.etEmail.error = null
        binding.etSubject.error = null
        binding.etMessage.error = null
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
