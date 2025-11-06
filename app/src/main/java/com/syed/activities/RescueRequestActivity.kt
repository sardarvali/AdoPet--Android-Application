package com.syed.activities

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.syed.databinding.ActivityRescueRequestBinding
import com.syed.models.RescueRequest
import com.syed.utils.FirebaseUtils
import kotlinx.coroutines.launch

class RescueRequestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRescueRequestBinding
    private var selectedPetType = ""
    private var selectedPriority = ""
    private var selectedImageUris: MutableList<Uri> = mutableListOf()
    private var fetchedLocation: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRescueRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        setupChipGroups()
        loadUserInfo()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pet Rescue Request"
    }

    private fun setupClickListeners() {
        binding.btnSubmitRequest.setOnClickListener {
            submitRescueRequest()
        }

        binding.btnEmergencyCall?.setOnClickListener {
            // Handle emergency call
            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
            intent.data = android.net.Uri.parse("tel:911")
            startActivity(intent)
        }

        binding.btnAddImage?.setOnClickListener {
            // Handle image addition with proper permissions
            if (com.syed.utils.PermissionUtils
                    .hasStoragePermissions(this)
            ) {
                selectImages()
            } else {
                com.syed.utils.PermissionUtils.requestImagePermissions(this) {
                    selectImages()
                }
            }
        }

        // TODO: Add location fetch button when LocationHelper.getCurrentLocation is properly set up
        // For now, users must manually enter location
    }

    private fun selectImages() {
        val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE, true)
        @Suppress("DEPRECATION")
        startActivityForResult(intent, 3001)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: android.content.Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3001 && resultCode == android.app.Activity.RESULT_OK && data != null) {
            // Handle multiple images
            if (data.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    selectedImageUris.add(imageUri)
                }
                binding.btnAddImage?.text = "✓ ${selectedImageUris.size} Image(s) Selected"
                Toast.makeText(this, "${selectedImageUris.size} images selected", Toast.LENGTH_SHORT).show()
            } else if (data.data != null) {
                // Single image
                selectedImageUris.add(data.data!!)
                binding.btnAddImage?.text = "✓ Image Selected"
                Toast.makeText(this, "Image selected successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupChipGroups() {
        // Setup pet type chips
        binding.chipGroupPetType.setOnCheckedStateChangeListener { group, checkedIds ->
            when {
                checkedIds.contains(binding.chipDog.id) -> selectedPetType = "dog"
                checkedIds.contains(binding.chipCat.id) -> selectedPetType = "cat"
                checkedIds.contains(binding.chipOther.id) -> selectedPetType = "other"
                else -> selectedPetType = ""
            }
        }

        // Setup priority chips
        binding.chipGroupPriority.setOnCheckedStateChangeListener { group, checkedIds ->
            when {
                checkedIds.contains(binding.chipHigh.id) -> selectedPriority = "high"
                checkedIds.contains(binding.chipMedium.id) -> selectedPriority = "medium"
                checkedIds.contains(binding.chipLow.id) -> selectedPriority = "low"
                else -> selectedPriority = ""
            }
        }

        // Set default selection to medium priority
        binding.chipMedium.isChecked = true
        selectedPriority = "medium"
    }

    private fun loadUserInfo() {
        val currentUser = FirebaseUtils.auth.currentUser
        currentUser?.let { user ->
            // For the new layout, we don't have name and email fields directly
            // The user info is handled through the authenticated user
        }
    }

    private fun submitRescueRequest() {
        val location =
            binding.etLocation.text
                .toString()
                .trim()
        val phone =
            binding.etPhone.text
                .toString()
                .trim()
        val description =
            binding.etDescription.text
                .toString()
                .trim()

        // Validation
        if (location.isEmpty()) {
            binding.etLocation.error = "Location is required"
            return
        }
        if (phone.isEmpty()) {
            binding.etPhone.error = "Phone number is required"
            return
        }
        if (description.isEmpty()) {
            binding.etDescription.error = "Description is required"
            return
        }
        if (selectedPetType.isEmpty()) {
            Toast.makeText(this, "Please select a pet type", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedPriority.isEmpty()) {
            Toast.makeText(this, "Please select a priority level", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = FirebaseUtils.auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to submit a rescue request", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmitRequest.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        // If images are selected, upload them first
        if (selectedImageUris.isNotEmpty()) {
            uploadImagesAndSubmitRequest(currentUser, location, phone, description)
        } else {
            submitRequestToFirestore(currentUser, location, phone, description, emptyList())
        }
    }

    private fun uploadImagesAndSubmitRequest(
        currentUser: com.google.firebase.auth.FirebaseUser,
        location: String,
        phone: String,
        description: String,
    ) {
        val storageRef =
            com.google.firebase.storage.FirebaseStorage
                .getInstance()
                .reference

        val uploadedUrls = mutableListOf<String>()
        var uploadCount = 0

        for ((index, uri) in selectedImageUris.withIndex()) {
            val imageFileName = "rescue_${currentUser.uid}_${System.currentTimeMillis()}_$index.jpg"
            val imageRef = storageRef.child("rescue_requests/${currentUser.uid}/$imageFileName")

            imageRef
                .putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        uploadedUrls.add(downloadUrl.toString())
                        uploadCount++

                        if (uploadCount == selectedImageUris.size) {
                            submitRequestToFirestore(currentUser, location, phone, description, uploadedUrls)
                        }
                    }
                }.addOnFailureListener { e ->
                    uploadCount++
                    Toast.makeText(this, "Failed to upload image ${index + 1}: ${e.message}", Toast.LENGTH_SHORT).show()

                    if (uploadCount == selectedImageUris.size) {
                        // Submit with whatever was uploaded successfully
                        submitRequestToFirestore(currentUser, location, phone, description, uploadedUrls)
                    }
                }
        }
    }

    private fun submitRequestToFirestore(
        currentUser: com.google.firebase.auth.FirebaseUser,
        location: String,
        phone: String,
        description: String,
        imageUrls: List<String>,
    ) {
        val rescueRequest =
            RescueRequest(
                userEmail = currentUser.email ?: "",
                userName = currentUser.displayName ?: "Anonymous",
                userPhone = phone,
                petType = selectedPetType,
                location = location,
                description = description,
                urgency = selectedPriority,
                requestDate = System.currentTimeMillis(),
                status = "pending",
                userId = currentUser.uid,
                imageUrl = imageUrls.firstOrNull() ?: "",
                imageUrls = imageUrls,
            )

        FirebaseUtils.firestore
            .collection(FirebaseUtils.RESCUE_REQUESTS_COLLECTION)
            .add(rescueRequest)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Rescue request submitted successfully!", Toast.LENGTH_LONG).show()

                // Send notification to admin
                sendAdminNotification(documentReference.id, location, selectedPetType)

                showSuccessCard()
                clearForm()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to submit request: ${e.message}", Toast.LENGTH_SHORT).show()
            }.addOnCompleteListener {
                binding.btnSubmitRequest.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun sendAdminNotification(
        requestId: String,
        location: String,
        petType: String,
    ) {
        // Send notification to admin about new rescue request
        val notification =
            hashMapOf(
                "title" to "🆘 New Rescue Request",
                "message" to "New $petType rescue request from $location",
                "type" to "new_rescue_request",
                "requestId" to requestId,
                "timestamp" to System.currentTimeMillis(),
                "read" to false,
                "isAdminNotification" to true,
            )

        FirebaseUtils.firestore
            .collection("admin_notifications")
            .add(notification)
            .addOnFailureListener { e ->
                android.util.Log.e("RescueRequest", "Failed to send admin notification", e)
            }
    }

    private fun showSuccessCard() {
        binding.cardSuccess?.visibility = View.VISIBLE
        // Fix smoothScrollTo error - use scrollTo instead
        binding.root.post {
            binding.cardSuccess?.let { card ->
                val y = card.top
                binding.root.scrollTo(0, y)
            }
        }
    }

    private fun clearForm() {
        binding.etLocation.setText("")
        binding.etPhone.setText("")
        binding.etDescription.setText("")

        // Reset image selection
        selectedImageUris.clear()
        binding.btnAddImage?.text = "📷 Add Image (Optional)"

        // Reset chip selections
        binding.chipGroupPetType.clearCheck()
        binding.chipGroupPriority.clearCheck()

        // Set default priority back to medium
        binding.chipMedium.isChecked = true
        selectedPriority = "medium"
        selectedPetType = ""

        // Clear errors
        binding.etLocation.error = null
        binding.etPhone.error = null
        binding.etDescription.error = null
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
