package com.syed.activities.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.syed.adapters.ShelterRequestsAdapter
import com.syed.databinding.ActivityShelterRequestsBinding
import com.syed.models.Shelter
import com.syed.models.ShelterRequest
import com.syed.utils.FirebaseUtils

class ShelterRequestsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShelterRequestsBinding
    private lateinit var requestsAdapter: ShelterRequestsAdapter
    private val requests = mutableListOf<ShelterRequest>()
    private var currentFilter = "pending"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShelterRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupTabs()
        loadRequests()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Shelter Registration Requests"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        requestsAdapter =
            ShelterRequestsAdapter(
                context = this,
                onApproveClick = { request ->
                    showApprovalDialog(request)
                },
                onRejectClick = { request ->
                    showRejectionDialog(request)
                },
                onViewClick = { request ->
                    showRequestDetails(request)
                },
            )

        binding.rvRequests.apply {
            layoutManager = LinearLayoutManager(this@ShelterRequestsActivity)
            adapter = requestsAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Pending"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Approved"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Rejected"))

        binding.tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    currentFilter =
                        when (tab?.position) {
                            0 -> "pending"
                            1 -> "approved"
                            2 -> "rejected"
                            else -> "pending"
                        }
                    filterRequests()
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            },
        )
    }

    private fun loadRequests() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        FirebaseUtils.firestore
            .collection("shelter_requests")
            .get()
            .addOnSuccessListener { documents ->
                requests.clear()
                for (doc in documents) {
                    try {
                        val request = doc.toObject(ShelterRequest::class.java).copy(id = doc.id)
                        requests.add(request)
                    } catch (e: Exception) {
                        android.util.Log.e("ShelterRequests", "Error parsing request", e)
                    }
                }
                filterRequests()
                binding.progressBar.visibility = View.GONE
            }.addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load requests: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterRequests() {
        val filtered = requests.filter { it.status == currentFilter }
        requestsAdapter.updateRequests(filtered)

        if (filtered.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.tvEmptyState.text = "No $currentFilter requests"
            binding.rvRequests.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvRequests.visibility = View.VISIBLE
        }
    }

    private fun showRequestDetails(request: ShelterRequest) {
        val details =
            """
            Shelter Name: ${request.shelterName}
            Type: ${request.shelterType}
            
            Contact Person: ${request.contactPerson}
            Phone: ${request.phoneNumber}
            Email: ${request.email}
            
            Address: ${request.address}
            City: ${request.city}, ${request.state}
            Pincode: ${request.pincode}
            
            Capacity: ${request.capacity}
            Timings: ${request.timings}
            
            Facilities: ${request.facilities.joinToString(", ")}
            
            Description:
            ${request.description}
            
            Submitted by: ${request.userName}
            User Email: ${request.userEmail}
            """.trimIndent()

        AlertDialog
            .Builder(this)
            .setTitle("Request Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showApprovalDialog(request: ShelterRequest) {
        AlertDialog
            .Builder(this)
            .setTitle("Approve Shelter Request")
            .setMessage("Are you sure you want to approve ${request.shelterName}?\n\nThis will create a new shelter listing.")
            .setPositiveButton("Approve") { _, _ ->
                approveRequest(request)
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRejectionDialog(request: ShelterRequest) {
        val input = android.widget.EditText(this)
        input.hint = "Reason for rejection (optional)"

        AlertDialog
            .Builder(this)
            .setTitle("Reject Request")
            .setMessage("Are you sure you want to reject ${request.shelterName}?")
            .setView(input)
            .setPositiveButton("Reject") { _, _ ->
                val reason = input.text.toString()
                rejectRequest(request, reason)
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun approveRequest(request: ShelterRequest) {
        // Create shelter from request
        val shelter =
            Shelter(
                name = request.shelterName,
                type = request.shelterType,
                description = request.description,
                address = request.address,
                city = request.city,
                state = request.state,
                pincode = request.pincode,
                location = GeoPoint(request.latitude, request.longitude),
                contactPerson = request.contactPerson,
                phoneNumber = request.phoneNumber,
                email = request.email,
                website = request.website,
                images = request.images,
                capacity = request.capacity,
                facilities = request.facilities,
                timings = request.timings,
                status = "approved",
                addedBy = request.userId,
                addedByType = "user",
                dateAdded = Timestamp.now(),
                dateModified = Timestamp.now(),
            )

        // Add to shelters collection
        FirebaseUtils.firestore
            .collection("shelters")
            .add(shelter)
            .addOnSuccessListener { shelterDoc ->
                // Update request status
                FirebaseUtils.firestore
                    .collection("shelter_requests")
                    .document(request.id)
                    .update(
                        mapOf(
                            "status" to "approved",
                            "dateProcessed" to Timestamp.now(),
                        ),
                    ).addOnSuccessListener {
                        Toast.makeText(this, "Request approved successfully", Toast.LENGTH_SHORT).show()
                        loadRequests()
                    }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to approve: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rejectRequest(
        request: ShelterRequest,
        reason: String,
    ) {
        FirebaseUtils.firestore
            .collection("shelter_requests")
            .document(request.id)
            .update(
                mapOf(
                    "status" to "rejected",
                    "adminNote" to reason,
                    "dateProcessed" to Timestamp.now(),
                ),
            ).addOnSuccessListener {
                Toast.makeText(this, "Request rejected", Toast.LENGTH_SHORT).show()
                loadRequests()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to reject: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
