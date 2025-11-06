package com.syed.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.syed.R
import com.syed.models.ContactMessage
import com.syed.utils.FirebaseUtils

class UserMessagesActivity : AppCompatActivity() {
    // Use simple layout approach to avoid binding errors
    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var emptyStateText: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use existing activity layout instead of non-existent one
        setContentView(R.layout.activity_contact)

        // Find views manually to avoid binding errors
        recyclerView = findViewById(R.id.rvMessages)
        progressBar = findViewById(R.id.progressBar)
        emptyStateText = findViewById(R.id.tvEmptyState)

        setupToolbar()
        setupRecyclerView()
        loadUserMessages()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Messages"
    }

    private fun setupRecyclerView() {
        // Use simple adapter approach to avoid binding errors
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadUserMessages() {
        progressBar.visibility = View.VISIBLE

        val currentUser = FirebaseUtils.auth.currentUser
        currentUser?.let { user ->
            FirebaseUtils.firestore
                .collection(FirebaseUtils.CONTACT_MESSAGES_COLLECTION)
                .whereEqualTo("userEmail", user.email)
                .orderBy("sentDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    progressBar.visibility = View.GONE

                    if (e != null) {
                        emptyStateText.visibility = View.VISIBLE
                        emptyStateText.text = "Error loading messages"
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val messages =
                            snapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(ContactMessage::class.java)?.copy(id = doc.id)
                                } catch (e: Exception) {
                                    null
                                }
                            }

                        // Simple message display without complex adapter
                        emptyStateText.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE

                        // TODO: Create simple adapter implementation
                    } else {
                        emptyStateText.visibility = View.VISIBLE
                        emptyStateText.text = "No messages found"
                        recyclerView.visibility = View.GONE
                    }
                }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
