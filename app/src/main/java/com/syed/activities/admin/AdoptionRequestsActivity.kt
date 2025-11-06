package com.syed.activities.admin

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.syed.R
import com.syed.adapters.AdoptionRequestsAdapter
import com.syed.databinding.ActivityAdoptionRequestsBinding
import com.syed.models.AdoptionRequest
import com.syed.utils.FirebaseUtils

class AdoptionRequestsActivity : AdminBaseActivity() {
    private lateinit var binding: ActivityAdoptionRequestsBinding
    private lateinit var adapter: AdoptionRequestsAdapter
    private val requests = mutableListOf<AdoptionRequest>()

    override fun onAdminVerified() {
        binding = ActivityAdoptionRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadRequests()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Adoption Requests"
    }

    private fun setupRecyclerView() {
        adapter =
            AdoptionRequestsAdapter(requests) { request, action ->
                when (action) {
                    "approve" -> approveRequest(request)
                    "reject" -> rejectRequest(request)
                    "view" -> viewRequestDetails(request)
                }
            }

        binding.rvRequests.layoutManager = LinearLayoutManager(this)
        binding.rvRequests.adapter = adapter
    }

    private fun loadRequests() {
        binding.progressBar.visibility = View.VISIBLE

        FirebaseUtils.firestore
            .collection(FirebaseUtils.ADOPTION_REQUESTS_COLLECTION)
            .orderBy("submittedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE

                if (error != null) {
                    android.util.Log.e("AdoptionRequests", "Error loading requests", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    requests.clear()
                    for (document in snapshot.documents) {
                        try {
                            val request = document.toObject(AdoptionRequest::class.java)
                            request?.let {
                                it.id = document.id
                                requests.add(it)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("AdoptionRequests", "Error parsing request", e)
                        }
                    }
                    adapter.notifyDataSetChanged()

                    binding.tvEmptyState.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
                }
            }
    }

    private fun approveRequest(request: AdoptionRequest) {
        androidx.appcompat.app.AlertDialog
            .Builder(this)
            .setTitle("Approve Adoption Request")
            .setMessage("Approve adoption request for ${request.petName} by ${request.userName}?")
            .setPositiveButton("Approve") { _, _ ->
                updateRequestStatus(request, "approved", null)
                android.widget.Toast
                    .makeText(this, "Request approved", android.widget.Toast.LENGTH_SHORT)
                    .show()
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectRequest(request: AdoptionRequest) {
        val editText =
            android.widget.EditText(this).apply {
                hint = "Enter rejection reason (optional)"
                minLines = 3
                maxLines = 5
                setPadding(50, 30, 50, 30)
            }

        androidx.appcompat.app.AlertDialog
            .Builder(this)
            .setTitle("Reject Adoption Request")
            .setMessage("Reject adoption request for ${request.petName} by ${request.userName}?")
            .setView(editText)
            .setPositiveButton("Reject") { _, _ ->
                val reason = editText.text.toString().trim()
                updateRequestStatus(request, "rejected", reason)
                android.widget.Toast
                    .makeText(this, "Request rejected", android.widget.Toast.LENGTH_SHORT)
                    .show()
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateRequestStatus(
        request: AdoptionRequest,
        status: String,
        rejectionReason: String? = null,
    ) {
        val updates =
            mutableMapOf(
                "status" to status,
                "reviewedAt" to System.currentTimeMillis(),
                "reviewedBy" to FirebaseUtils.auth.currentUser?.uid,
                "responseDate" to System.currentTimeMillis(),
            )

        if (status == "rejected" && !rejectionReason.isNullOrEmpty()) {
            updates["rejectionReason"] = rejectionReason
            updates["adminMessage"] = rejectionReason
        }

        FirebaseUtils.firestore
            .collection(FirebaseUtils.ADOPTION_REQUESTS_COLLECTION)
            .document(request.id)
            .update(updates as Map<String, Any>)
            .addOnSuccessListener {
                android.util.Log.d("AdoptionRequests", "Request $status successfully")
            }.addOnFailureListener { e ->
                android.util.Log.e("AdoptionRequests", "Error updating request", e)
                android.widget.Toast
                    .makeText(
                        this,
                        "Failed to update request: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
            }
    }

    private fun viewRequestDetails(request: AdoptionRequest) {
        val details =
            buildString {
                appendLine("📝 REQUEST DETAILS")
                appendLine("━━━━━━━━━━━━━━━━━━━━")
                appendLine()
                appendLine("🐾 Pet: ${request.petName}")
                appendLine("👤 Applicant: ${request.userName}")
                appendLine("📧 Email: ${request.userEmail}")
                appendLine("📱 Phone: ${request.userPhone}")
                appendLine()
                appendLine("🏠 Address:")
                appendLine(request.address)
                appendLine()
                appendLine("💭 Reason for Adoption:")
                appendLine(request.reason)
                appendLine()
                appendLine("🎓 Pet Experience:")
                appendLine(request.experience)
                appendLine()
                appendLine("📅 Submitted: ${formatDate(request.submittedAt)}")
                appendLine("📊 Status: ${request.status.uppercase()}")

                if (request.status == "rejected" && !request.rejectionReason.isNullOrEmpty()) {
                    appendLine()
                    appendLine("❌ Rejection Reason:")
                    appendLine(request.rejectionReason)
                }
            }

        androidx.appcompat.app.AlertDialog
            .Builder(this)
            .setTitle("Request Details")
            .setMessage(details)
            .setPositiveButton("Close", null)
            .setNeutralButton("Approve") { _, _ -> approveRequest(request) }
            .setNegativeButton("Reject") { _, _ -> rejectRequest(request) }
            .show()
    }

    private fun formatDate(timestamp: Long): String {
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date(timestamp))
    }
}
