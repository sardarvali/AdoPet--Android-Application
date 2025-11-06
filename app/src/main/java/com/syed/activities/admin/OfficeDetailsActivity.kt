package com.syed.activities.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.syed.databinding.ActivityOfficeDetailsBinding
import com.syed.utils.FirebaseUtils

class OfficeDetailsActivity : AdminBaseActivity() {
    private lateinit var binding: ActivityOfficeDetailsBinding
    private var isEditing = false

    override fun onAdminVerified() {
        binding = ActivityOfficeDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        loadOfficeDetails()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Office Details"
    }

    private fun setupClickListeners() {
        // Use safe calls for UI elements that might not exist
        binding.btnSave?.setOnClickListener {
            if (isEditing) {
                saveOfficeDetails()
            } else {
                enableEditing()
            }
        }

        binding.btnCancel?.setOnClickListener {
            if (isEditing) {
                disableEditing()
                loadOfficeDetails() // Reload original data
            } else {
                finish()
            }
        }
    }

    private fun enableEditing() {
        isEditing = true
        // Enable editing with safe calls
        binding.etOfficeName?.isEnabled = true
        binding.etOfficeEmail?.isEnabled = true
        binding.etOfficeAddress?.isEnabled = true
        binding.etOfficePhone?.isEnabled = true
        binding.etWorkingHours?.isEnabled = true
        binding.etDescription?.isEnabled = true

        binding.btnSave?.text = "Save"
    }

    private fun disableEditing() {
        isEditing = false
        binding.etOfficeName?.isEnabled = false
        binding.etOfficeEmail?.isEnabled = false
        binding.etOfficeAddress?.isEnabled = false
        binding.etOfficePhone?.isEnabled = false
        binding.etWorkingHours?.isEnabled = false
        binding.etDescription?.isEnabled = false

        binding.btnSave?.text = "Edit Details"
    }

    private fun loadOfficeDetails() {
        binding.progressBar?.visibility = View.VISIBLE

        FirebaseUtils.firestore
            .collection("office_details")
            .document("main_office")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data
                    binding.etOfficeName?.setText(data?.get("name") as? String ?: "")
                    binding.etOfficeEmail?.setText(data?.get("email") as? String ?: "")
                    binding.etOfficeAddress?.setText(data?.get("address") as? String ?: "")
                    binding.etOfficePhone?.setText(data?.get("phone") as? String ?: "")
                    binding.etWorkingHours?.setText(data?.get("hours") as? String ?: "")
                    binding.etDescription?.setText(data?.get("description") as? String ?: "")
                } else {
                    setDefaultOfficeDetails()
                }
                binding.progressBar?.visibility = View.GONE
                disableEditing() // Start in view mode
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load office details: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar?.visibility = View.GONE
                setDefaultOfficeDetails()
            }
    }

    private fun setDefaultOfficeDetails() {
        binding.etOfficeName?.setText("Pet Adoption Center")
        binding.etOfficeEmail?.setText("info@petadoption.com")
        binding.etOfficeAddress?.setText("123 Pet Street, Animal City")
        binding.etOfficePhone?.setText("+1-555-0123")
        binding.etWorkingHours?.setText("Mon-Fri: 9AM-6PM, Sat: 10AM-4PM")
        binding.etDescription?.setText("We are dedicated to finding loving homes for pets in need.")
    }

    private fun saveOfficeDetails() {
        val officeData =
            hashMapOf(
                "name" to (binding.etOfficeName?.text?.toString() ?: ""),
                "email" to (binding.etOfficeEmail?.text?.toString() ?: ""),
                "address" to (binding.etOfficeAddress?.text?.toString() ?: ""),
                "phone" to (binding.etOfficePhone?.text?.toString() ?: ""),
                "hours" to (binding.etWorkingHours?.text?.toString() ?: ""),
                "description" to (binding.etDescription?.text?.toString() ?: ""),
                "lastUpdated" to System.currentTimeMillis(),
                "updatedBy" to (FirebaseUtils.auth.currentUser?.uid ?: ""),
            )

        binding.progressBar?.visibility = View.VISIBLE

        FirebaseUtils.firestore
            .collection("office_details")
            .document("main_office")
            .set(officeData)
            .addOnSuccessListener {
                binding.progressBar?.visibility = View.GONE
                Toast.makeText(this, "Office details saved successfully", Toast.LENGTH_SHORT).show()
                disableEditing()
            }.addOnFailureListener { e ->
                binding.progressBar?.visibility = View.GONE
                Toast.makeText(this, "Failed to save office details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
