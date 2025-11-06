package com.syed.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.Query
import com.syed.R
import com.syed.adapters.MyAdoptionRequestsAdapter
import com.syed.databinding.ActivityMyAdoptionRequestsBinding
import com.syed.models.AdoptionRequest
import com.syed.utils.FirebaseUtils

class MyAdoptionRequestsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyAdoptionRequestsBinding
    private lateinit var adapter: MyAdoptionRequestsAdapter
    private val requests = mutableListOf<AdoptionRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAdoptionRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadMyRequests()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Adoption Requests"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter =
            MyAdoptionRequestsAdapter(
                context = this,
                requests = requests,
                onRequestClick = { request ->
                    showRequestDetails(request)
                },
            )

        binding.rvRequests.apply {
            adapter = this@MyAdoptionRequestsActivity.adapter
            layoutManager = LinearLayoutManager(this@MyAdoptionRequestsActivity)
        }
    }

    private fun loadMyRequests() {
        val currentUserId = FirebaseUtils.auth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE

        FirebaseUtils.firestore
            .collection("adoption_requests")
            .whereEqualTo("userId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                binding.progressBar.visibility = View.GONE

                if (error != null) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyState.text = "Error loading requests: ${error.message}"
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    requests.clear()
                    snapshots.documents.forEach { doc ->
                        try {
                            val request = doc.toObject(AdoptionRequest::class.java)?.copy(id = doc.id)
                            request?.let { requests.add(it) }
                        } catch (e: Exception) {
                            android.util.Log.e("MyAdoptionRequests", "Error parsing request", e)
                        }
                    }

                    if (requests.isNotEmpty()) {
                        adapter.updateRequests(requests)
                        binding.rvRequests.visibility = View.VISIBLE
                        binding.tvEmptyState.visibility = View.GONE
                    } else {
                        binding.rvRequests.visibility = View.GONE
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.tvEmptyState.text = "No adoption requests yet.\nAdopt a pet to get started!"
                    }
                } else {
                    binding.rvRequests.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyState.text = "No adoption requests yet.\nAdopt a pet to get started!"
                }
            }
    }

    private fun showRequestDetails(request: AdoptionRequest) {
        androidx.appcompat.app.AlertDialog
            .Builder(this)
            .setTitle("Request Details")
            .setMessage(buildRequestDetails(request))
            .setPositiveButton("OK", null)
            .show()
    }

    private fun buildRequestDetails(request: AdoptionRequest): String =
        """
            Pet: ${request.petName}
            
            Status: ${request.status.uppercase()}
            
            Submitted: ${java.text.SimpleDateFormat(
            "MMM dd, yyyy HH:mm",
            java.util.Locale.getDefault(),
        ).format(java.util.Date(request.timestamp))}
            
            ${if (request.status == "rejected" && !request.rejectionReason.isNullOrEmpty()) {
            "Rejection Reason:\n${request.rejectionReason}"
        } else if (request.status == "approved") {
            "Your request has been approved!\nThe shelter will contact you soon."
        } else {
            "Your request is under review.\nPlease wait for admin approval."
        }}
        """.trimIndent()

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
