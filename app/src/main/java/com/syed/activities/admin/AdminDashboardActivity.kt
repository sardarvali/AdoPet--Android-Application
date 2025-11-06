package com.syed.activities.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.syed.R
import com.syed.databinding.ActivityAdminDashboardBinding
import com.syed.utils.FirebaseUtils

class AdminDashboardActivity : AdminBaseActivity() {
    private lateinit var binding: ActivityAdminDashboardBinding

    override fun onAdminVerified() {
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()

        // Post the dashboard stats loading to ensure binding is fully initialized
        binding.root.post {
            loadDashboardStats()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Admin Dashboard"
    }

    private fun setupClickListeners() {
        // Manage Pets - Use ManagePetsActivity
        binding.cardManagePets.setOnClickListener {
            try {
                startActivity(Intent(this, ManagePetsActivity::class.java))
            } catch (e: Exception) {
                android.util.Log.e("AdminDashboard", "Error opening Manage Pets", e)
                Toast.makeText(this, "Error opening Manage Pets: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Manage Shelters
        binding.cardManageShelters?.setOnClickListener {
            try {
                startActivity(Intent(this, ManageSheltersActivity::class.java))
            } catch (e: Exception) {
                android.util.Log.e("AdminDashboard", "Error opening Manage Shelters", e)
                Toast.makeText(this, "Error opening Manage Shelters: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Shelter Requests
        binding.cardShelterRequests?.setOnClickListener {
            try {
                startActivity(Intent(this, ShelterRequestsActivity::class.java))
            } catch (e: Exception) {
                android.util.Log.e("AdminDashboard", "Error opening Shelter Requests", e)
                Toast.makeText(this, "Error opening Shelter Requests: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Manage Tips - Use ManageTipsActivity
        binding.cardManageTips?.setOnClickListener {
            try {
                startActivity(Intent(this, ManageTipsActivity::class.java))
            } catch (e: Exception) {
                android.util.Log.e("AdminDashboard", "Error opening Manage Tips", e)
                Toast.makeText(this, "Error opening Manage Tips: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Office Details
        binding.cardOfficeDetails.setOnClickListener {
            try {
                startActivity(Intent(this, OfficeDetailsActivity::class.java))
            } catch (e: Exception) {
                android.util.Log.e("AdminDashboard", "Error opening Office Details", e)
                Toast.makeText(this, "Error opening Office Details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Adoption Requests
        binding.cardAdoptionRequests.setOnClickListener {
            try {
                startActivity(Intent(this, AdoptionRequestsActivity::class.java))
            } catch (e: Exception) {
                android.util.Log.e("AdminDashboard", "Error opening Adoption Requests", e)
                Toast.makeText(this, "Error opening Adoption Requests: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Rescue Requests
        binding.cardRescueRequests?.setOnClickListener {
            try {
                startActivity(Intent(this, RescueRequestsActivity::class.java))
            } catch (e: Exception) {
                android.util.Log.e("AdminDashboard", "Error opening Rescue Requests", e)
                Toast.makeText(this, "Error opening Rescue Requests: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Contact Messages
        binding.cardContactMessages.setOnClickListener {
            try {
                startActivity(Intent(this, ContactMessagesActivity::class.java))
            } catch (e: Exception) {
                android.util.Log.e("AdminDashboard", "Error opening Contact Messages", e)
                Toast.makeText(this, "Error opening Contact Messages: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Remove the Add Admin card click since AddAdminActivity doesn't exist
        // TODO: Implement AddAdminActivity if needed
        binding.cardAddAdmin?.visibility = View.GONE

        // History/Reports
        binding.cardHistory?.setOnClickListener {
            try {
                startActivity(Intent(this, AdminHistoryActivity::class.java))
            } catch (e: Exception) {
                android.util.Log.e("AdminDashboard", "Error opening History", e)
                Toast.makeText(this, "Error opening History: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadDashboardStats() {
        // Load dashboard statistics
        loadPetsCount()
        loadRequestsCount()
        loadMessagesCount()
        loadShelterRequestsCount()
    }

    private fun loadPetsCount() {
        // Add null check to prevent crash
        if (!::binding.isInitialized) {
            android.util.Log.w("AdminDashboard", "Binding not initialized, skipping loadPetsCount")
            return
        }

        FirebaseUtils.firestore
            .collection(FirebaseUtils.PETS_COLLECTION)
            .get()
            .addOnSuccessListener { documents ->
                // Double check binding is still initialized
                if (!::binding.isInitialized) return@addOnSuccessListener

                val totalPets = documents.size()
                val availablePets =
                    documents.count { doc ->
                        doc.getBoolean("available") == true
                    }

                binding.tvPetsCount?.text = "$availablePets / $totalPets"
            }.addOnFailureListener { e ->
                // Double check binding is still initialized
                if (!::binding.isInitialized) return@addOnFailureListener

                android.util.Log.e("AdminDashboard", "Error loading pets count", e)
                binding.tvPetsCount?.text = "Error loading"
            }
    }

    private fun loadRequestsCount() {
        // Add null check to prevent crash
        if (!::binding.isInitialized) {
            android.util.Log.w("AdminDashboard", "Binding not initialized, skipping loadRequestsCount")
            return
        }

        FirebaseUtils.firestore
            .collection(FirebaseUtils.ADOPTION_REQUESTS_COLLECTION)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                // Double check binding is still initialized
                if (!::binding.isInitialized) return@addOnSuccessListener

                binding.tvRequestsCount?.text = documents.size().toString()
            }.addOnFailureListener {
                // Double check binding is still initialized
                if (!::binding.isInitialized) return@addOnFailureListener

                binding.tvRequestsCount?.text = "0"
            }
    }

    private fun loadMessagesCount() {
        // Add null check to prevent crash
        if (!::binding.isInitialized) {
            android.util.Log.w("AdminDashboard", "Binding not initialized, skipping loadMessagesCount")
            return
        }

        FirebaseUtils.firestore
            .collection(FirebaseUtils.CONTACT_MESSAGES_COLLECTION)
            .whereEqualTo("status", "unread")
            .get()
            .addOnSuccessListener { documents ->
                // Double check binding is still initialized
                if (!::binding.isInitialized) return@addOnSuccessListener

                binding.tvMessagesCount?.text = documents.size().toString()
            }.addOnFailureListener {
                // Double check binding is still initialized
                if (!::binding.isInitialized) return@addOnFailureListener

                binding.tvMessagesCount?.text = "0"
            }
    }

    private fun loadShelterRequestsCount() {
        // Add null check to prevent crash
        if (!::binding.isInitialized) {
            android.util.Log.w("AdminDashboard", "Binding not initialized, skipping loadShelterRequestsCount")
            return
        }

        FirebaseUtils.firestore
            .collection("shelter_requests")
            .whereEqualTo("status", "pending")
            .get()
//            .addOnSuccessListener { documents ->
//                // Double check binding is still initialized
//                if (!::binding.isInitialized) return@addOnSuccessListener
//
//                binding.tvShelterRequestsCount?.text = documents.size().toString()
//            }.addOnFailureListener {
//                // Double check binding is still initialized
//                if (!::binding.isInitialized) return@addOnFailureListener
//
//                binding.tvShelterRequestsCount?.text = "0"
//            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        // Refresh stats when returning to dashboard
        if (::binding.isInitialized) {
            loadDashboardStats()
        }
    }
}
