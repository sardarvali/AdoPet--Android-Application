package com.syed.activities.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.syed.adapters.RescueRequestsAdapter
import com.syed.databinding.ActivityRescueRequestsBinding
import com.syed.models.RescueRequest
import com.syed.utils.FirebaseUtils

class RescueRequestsActivity : AdminBaseActivity() {
    private lateinit var binding: ActivityRescueRequestsBinding
    private lateinit var requestsAdapter: RescueRequestsAdapter

    override fun onAdminVerified() {
        binding = ActivityRescueRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadRequests()
        setupSwipeRefresh()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Rescue Requests"
    }

    private fun setupRecyclerView() {
        requestsAdapter =
            RescueRequestsAdapter(
                context = this,
                onUpdateStatusClick = { request -> showStatusUpdateDialog(request) },
                onViewClick = { request -> viewRequestDetails(request) },
                onReplyClick = { request -> showReplyDialog(request) },
            )

        binding.rvRequests.apply {
            adapter = requestsAdapter
            layoutManager = LinearLayoutManager(this@RescueRequestsActivity)
        }
    }

    private fun loadRequests() {
        binding.progressBar.visibility = View.VISIBLE

        FirebaseUtils.firestore
            .collection(FirebaseUtils.RESCUE_REQUESTS_COLLECTION)
            .orderBy("requestDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val requests =
                    documents.mapNotNull { doc ->
                        doc.toObject(RescueRequest::class.java).copy(id = doc.id)
                    }
                requestsAdapter.updateRequests(requests)
                binding.progressBar.visibility = View.GONE
                binding.tvEmptyState.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
            }.addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
                Toast.makeText(this, "Failed to load rescue requests", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadRequests()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun showStatusUpdateDialog(request: RescueRequest) {
        val statuses = arrayOf("pending", "in_progress", "completed", "cancelled")
        val statusNames = arrayOf("Pending", "In Progress", "Completed", "Cancelled")

        AlertDialog
            .Builder(this)
            .setTitle("Update Status")
            .setSingleChoiceItems(statusNames, -1) { dialog, which ->
                val selectedStatus = statuses[which]
                dialog.dismiss()
                updateRequestStatus(request, selectedStatus)
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateRequestStatus(
        request: RescueRequest,
        status: String,
    ) {
        val updatedRequest =
            request.copy(
                status = status,
                responseDate = System.currentTimeMillis(),
            )

        FirebaseUtils.firestore
            .collection(FirebaseUtils.RESCUE_REQUESTS_COLLECTION)
            .document(request.id)
            .set(updatedRequest)
            .addOnSuccessListener {
                Toast.makeText(this, "Status updated to ${status.replace("_", " ").capitalize()}", Toast.LENGTH_SHORT).show()

                // Send notification to user about status update
                com.syed.utils.NotificationUtils
                    .notifyRescueRequestUpdate(
                        context = this,
                        requestId = request.id,
                        status = status,
                        adminMessage = "Your rescue request has been updated to: ${status.replace("_", " ")}",
                    )

                loadRequests()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show()
            }
    }

    private fun viewRequestDetails(request: RescueRequest) {
        val details =
            """
            Location: ${request.location}
            Pet Type: ${request.petType.capitalize()}
            Urgency: ${request.urgency.capitalize()}
            
            Reporter: ${request.userName}
            Email: ${request.userEmail}
            Phone: ${request.userPhone}
            
            Description:
            ${request.description}
            
            Status: ${request.status.replace("_", " ").capitalize()}
            Request Date: ${java.text.SimpleDateFormat(
                "MMM dd, yyyy HH:mm",
                java.util.Locale.getDefault(),
            ).format(java.util.Date(request.requestDate))}
            """.trimIndent()

        AlertDialog
            .Builder(this)
            .setTitle("Rescue Request Details")
            .setMessage(details)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showReplyDialog(request: RescueRequest) {
        val input = android.widget.EditText(this)
        input.hint = "Enter your reply to the user"
        input.setText(request.adminReply ?: "")
        input.setPadding(50, 30, 50, 30)

        AlertDialog
            .Builder(this)
            .setTitle("Reply to Rescue Request")
            .setMessage("Reply will be sent to: ${request.userEmail}")
            .setView(input)
            .setPositiveButton("Send Reply") { _, _ ->
                val reply = input.text.toString().trim()
                if (reply.isNotEmpty()) {
                    sendReply(request, reply)
                } else {
                    Toast.makeText(this, "Reply cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendReply(
        request: RescueRequest,
        reply: String,
    ) {
        val updatedRequest =
            request.copy(
                adminReply = reply,
                replyDate = System.currentTimeMillis(),
                isReplied = true,
            )

        FirebaseUtils.firestore
            .collection(FirebaseUtils.RESCUE_REQUESTS_COLLECTION)
            .document(request.id)
            .set(updatedRequest)
            .addOnSuccessListener {
                Toast.makeText(this, "Reply sent successfully!", Toast.LENGTH_SHORT).show()

                // Send notification to user about the reply
                val notification =
                    hashMapOf(
                        "userId" to request.userId,
                        "title" to "Rescue Request Reply",
                        "message" to "Admin replied to your rescue request: $reply",
                        "type" to "rescue_reply",
                        "timestamp" to System.currentTimeMillis(),
                        "read" to false,
                    )

                FirebaseUtils.firestore
                    .collection("notifications")
                    .add(notification)

                loadRequests()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send reply: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
