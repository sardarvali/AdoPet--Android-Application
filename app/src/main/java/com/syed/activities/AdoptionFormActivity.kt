package com.syed.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.syed.databinding.ActivityAdoptionFormBinding
import com.syed.models.Pet
import com.syed.utils.ErrorHandler
import com.syed.utils.FirebaseUtils
import com.syed.utils.NotificationUtils
import com.syed.utils.ValidationUtils

class AdoptionFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdoptionFormBinding
    private var pet: Pet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdoptionFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        val petId = intent.getStringExtra("petId")
        if (petId != null) {
            loadPetDetails(petId)
        } else {
            finish()
        }

        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Adoption Application"
    }

    private fun loadPetDetails(petId: String) {
        FirebaseUtils.firestore
            .collection(FirebaseUtils.PETS_COLLECTION)
            .document(petId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    pet = document.toObject(Pet::class.java)?.copy(id = document.id)
                    pet?.let { displayPetInfo(it) }
                } else {
                    Toast.makeText(this, "Pet not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load pet details", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayPetInfo(pet: Pet) {
        binding.tvPetName.text = pet.name
        binding.tvPetDetails.text = "${pet.breed} • ${pet.age} years old • ${pet.gender}"
    }

    private fun setupClickListeners() {
        binding.btnSubmit.setOnClickListener {
            submitAdoptionForm()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun submitAdoptionForm() {
        // Prevent multiple submissions
        if (!binding.btnSubmit.isEnabled) {
            return
        }

        val name =
            binding.etName.text
                .toString()
                .trim()
        val email =
            binding.etEmail.text
                .toString()
                .trim()
        val phone =
            binding.etPhone.text
                .toString()
                .trim()
        val address =
            binding.etAddress.text
                .toString()
                .trim()
        val experience =
            binding.etExperience.text
                .toString()
                .trim()
        val reason =
            binding.etReason.text
                .toString()
                .trim()

        // Comprehensive form validation
        val validationResult =
            ValidationUtils.validateAdoptionForm(
                name,
                email,
                phone,
                address,
                experience,
                reason,
            )

        if (!validationResult.isValid) {
            Toast.makeText(this, validationResult.message, Toast.LENGTH_LONG).show()
            return
        }

        val currentUser = FirebaseUtils.auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to submit adoption request", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button to prevent duplicate submissions
        binding.btnSubmit.isEnabled = false
        binding.btnCancel.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE

        submitToFirestore(name, email, phone, address, experience, reason)
    }

    private fun submitToFirestore(
        name: String,
        email: String,
        phone: String,
        address: String,
        experience: String,
        reason: String,
    ) {
        val currentUser = FirebaseUtils.auth.currentUser!!

        // Create adoption request with all required fields
        val requestData =
            hashMapOf(
                "userId" to currentUser.uid,
                "userName" to name,
                "userEmail" to email,
                "userPhone" to phone,
                "petId" to (pet?.id ?: ""),
                "petName" to (pet?.name ?: ""),
                "address" to address,
                "experience" to experience,
                "reason" to reason,
                "status" to "pending",
                "submittedAt" to System.currentTimeMillis(),
                "reviewedAt" to 0L,
                "reviewedBy" to "",
                "adminNotes" to "",
                // Add extra fields for compatibility
                "applicantName" to name,
                "applicantEmail" to email,
                "applicantPhone" to phone,
                "applicantAddress" to address,
                "petExperience" to experience,
                "adoptionReason" to reason,
                "submissionDate" to System.currentTimeMillis(),
            )

        FirebaseUtils.firestore
            .collection(FirebaseUtils.ADOPTION_REQUESTS_COLLECTION)
            .add(requestData)
            .addOnSuccessListener { documentReference ->
                android.util.Log.d("AdoptionForm", "Request submitted with ID: ${documentReference.id}")

                // Update pet status to show it has pending requests
                updatePetRequestStatus()

                // Send notifications to admins
                NotificationUtils.sendAdminNotification(
                    context = this,
                    title = "New Adoption Request",
                    message = "New adoption request for ${pet?.name} from $name",
                    type = NotificationUtils.TYPE_NEW_REQUEST,
                )

                binding.progressBar.visibility = android.view.View.GONE

                // Show success message
                androidx.appcompat.app.AlertDialog
                    .Builder(this)
                    .setTitle("Success!")
                    .setMessage(
                        "Your adoption request for ${pet?.name} has been submitted successfully! We will review it and contact you soon.",
                    ).setPositiveButton("OK") { _, _ ->
                        setResult(RESULT_OK)
                        finish()
                    }.setCancelable(false)
                    .show()
            }.addOnFailureListener { exception ->
                android.util.Log.e("AdoptionForm", "Failed to submit request", exception)
                binding.btnSubmit.isEnabled = true
                binding.btnCancel.isEnabled = true
                binding.progressBar.visibility = android.view.View.GONE
                ErrorHandler.handleFirebaseError(this, exception, "Failed to submit adoption request")
            }
    }

    private fun updatePetRequestStatus() {
        pet?.id?.let { petId ->
            val updates =
                hashMapOf<String, Any>(
                    "status" to "requested",
                    "lastUpdated" to System.currentTimeMillis(),
                )

            FirebaseUtils.firestore
                .collection(FirebaseUtils.PETS_COLLECTION)
                .document(petId)
                .update(updates)
                .addOnSuccessListener {
                    android.util.Log.d("AdoptionForm", "Pet status updated to requested")
                }.addOnFailureListener { e ->
                    android.util.Log.e("AdoptionForm", "Failed to update pet status", e)
                    // Don't fail the whole process if this update fails
                }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
