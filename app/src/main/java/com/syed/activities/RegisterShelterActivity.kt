package com.syed.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import com.syed.databinding.ActivityRegisterShelterBinding
import com.syed.models.ShelterRequest
import com.syed.utils.FirebaseUtils
import java.util.UUID

class RegisterShelterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterShelterBinding
    private var selectedImages = mutableListOf<Uri>()
    private var uploadedImageUrls = mutableListOf<String>()
    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0

    private val imagePickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetMultipleContents(),
        ) { uris ->
            if (uris != null && uris.isNotEmpty()) {
                selectedImages.clear()
                selectedImages.addAll(uris.take(5))
                binding.tvImageCount.text = "${selectedImages.size} images selected"
            }
        }

    private val locationPickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { data ->
                    selectedLatitude = data.getDoubleExtra(MapLocationPickerActivity.EXTRA_LATITUDE, 0.0)
                    selectedLongitude = data.getDoubleExtra(MapLocationPickerActivity.EXTRA_LONGITUDE, 0.0)
                    val address = data.getStringExtra(MapLocationPickerActivity.EXTRA_ADDRESS) ?: ""
                    val city = data.getStringExtra(MapLocationPickerActivity.EXTRA_CITY) ?: ""
                    val state = data.getStringExtra(MapLocationPickerActivity.EXTRA_STATE) ?: ""

                    // Update UI with selected location
                    binding.etAddress.setText(address)
                    binding.etCity.setText(city)
                    binding.etState.setText(state)
                    binding.tvLocationCoordinates.text =
                        "📍 Location: ${String.format("%.6f", selectedLatitude)}, ${
                            String.format(
                                "%.6f",
                                selectedLongitude,
                            )
                        }"
                    Toast.makeText(this, "Location selected successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterShelterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSpinners()
        setupButtons()

        // Check if user already has a shelter request
        checkExistingShelterRequest()
    }

    private fun checkExistingShelterRequest() {
        val currentUser = FirebaseUtils.auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        // Check in shelter_requests collection
        FirebaseUtils.firestore
            .collection("shelter_requests")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { requestsSnapshot ->
                if (!requestsSnapshot.isEmpty) {
                    // User already has a request
                    val request = requestsSnapshot.documents[0]
                    val status = request.getString("status") ?: "pending"

                    binding.progressBar.visibility = View.GONE

                    androidx.appcompat.app.AlertDialog
                        .Builder(this)
                        .setTitle("Shelter Request Exists")
                        .setMessage(
                            "You have already submitted a shelter request.\n\n" +
                                "Status: ${status.uppercase()}\n\n" +
                                "You can only submit one shelter request per account.",
                        ).setPositiveButton("OK") { _, _ -> finish() }
                        .setCancelable(false)
                        .show()
                } else {
                    // Check in shelters collection (approved shelters)
                    checkApprovedShelter(currentUser.uid)
                }
            }.addOnFailureListener { e ->
                android.util.Log.e("RegisterShelter", "Error checking requests", e)
                binding.progressBar.visibility = View.GONE
                // Allow to continue if check fails
            }
    }

    private fun checkApprovedShelter(userId: String) {
        FirebaseUtils.firestore
            .collection("shelters")
            .whereEqualTo("addedBy", userId)
            .get()
            .addOnSuccessListener { sheltersSnapshot ->
                binding.progressBar.visibility = View.GONE

                if (!sheltersSnapshot.isEmpty) {
                    // User already has an approved shelter
                    androidx.appcompat.app.AlertDialog
                        .Builder(this)
                        .setTitle("Shelter Already Registered")
                        .setMessage(
                            "Your shelter has already been approved and registered!\n\n" +
                                "You can only register one shelter per account.",
                        ).setPositiveButton("OK") { _, _ -> finish() }
                        .setCancelable(false)
                        .show()
                }
                // If no shelter found, user can proceed with registration
            }.addOnFailureListener { e ->
                android.util.Log.e("RegisterShelter", "Error checking shelters", e)
                binding.progressBar.visibility = View.GONE
                // Allow to continue if check fails
            }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Register Your Shelter"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSpinners() {
        val types = arrayOf("NGO", "Government", "Private")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        binding.spinnerType.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnSelectImages.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnPickLocation.setOnClickListener {
            openLocationPicker()
        }

        binding.btnSubmit.setOnClickListener {
            submitRequest()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun openLocationPicker() {
        val intent = Intent(this, MapLocationPickerActivity::class.java)
        // Pass current location if already selected
        if (selectedLatitude != 0.0 && selectedLongitude != 0.0) {
            intent.putExtra(MapLocationPickerActivity.EXTRA_LATITUDE, selectedLatitude)
            intent.putExtra(MapLocationPickerActivity.EXTRA_LONGITUDE, selectedLongitude)
        }
        locationPickerLauncher.launch(intent)
    }

    private fun submitRequest() {
        if (!validateInputs()) return

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false

        if (selectedImages.isNotEmpty()) {
            uploadImages { success ->
                if (success) {
                    saveRequestToFirestore()
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    Toast.makeText(this, "Failed to upload images", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            saveRequestToFirestore()
        }
    }

    private fun validateInputs(): Boolean {
        if (binding.etShelterName.text.isNullOrBlank()) {
            binding.etShelterName.error = "Shelter name is required"
            return false
        }
        if (binding.etAddress.text.isNullOrBlank()) {
            binding.etAddress.error = "Address is required"
            return false
        }
        if (binding.etContactPerson.text.isNullOrBlank()) {
            binding.etContactPerson.error = "Contact person is required"
            return false
        }
        if (binding.etPhone.text.isNullOrBlank()) {
            binding.etPhone.error = "Phone number is required"
            return false
        }
        if (binding.etEmail.text.isNullOrBlank()) {
            binding.etEmail.error = "Email is required"
            return false
        }
        return true
    }

    private fun uploadImages(callback: (Boolean) -> Unit) {
        val storage = FirebaseStorage.getInstance()
        var uploadedCount = 0
        val totalImages = selectedImages.size

        selectedImages.forEach { uri ->
            val ref = storage.reference.child("shelter_requests/${UUID.randomUUID()}.jpg")

            ref
                .putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    ref.downloadUrl.addOnSuccessListener { downloadUri ->
                        uploadedImageUrls.add(downloadUri.toString())
                        uploadedCount++

                        if (uploadedCount == totalImages) {
                            callback(true)
                        }
                    }
                }.addOnFailureListener {
                    callback(false)
                }
        }
    }

    private fun saveRequestToFirestore() {
        val currentUser = FirebaseUtils.auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Double-check before saving to prevent race conditions
        FirebaseUtils.firestore
            .collection("shelter_requests")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    // Request already exists
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    Toast
                        .makeText(
                            this,
                            "You have already submitted a shelter request!",
                            Toast.LENGTH_LONG,
                        ).show()
                    finish()
                    return@addOnSuccessListener
                }

                // Proceed with saving
                proceedToSaveRequest(currentUser)
            }.addOnFailureListener { e ->
                android.util.Log.e("RegisterShelter", "Error checking duplicate", e)
                // If check fails, proceed anyway (better to allow than block)
                proceedToSaveRequest(currentUser)
            }
    }

    private fun proceedToSaveRequest(currentUser: com.google.firebase.auth.FirebaseUser) {
        val facilities = mutableListOf<String>()
        if (binding.cbVeterinary.isChecked) facilities.add("veterinary")
        if (binding.cbFood.isChecked) facilities.add("food")
        if (binding.cbShelter.isChecked) facilities.add("shelter")
        if (binding.cbAdoption.isChecked) facilities.add("adoption")
        if (binding.cbGrooming.isChecked) facilities.add("grooming")

        val request =
            ShelterRequest(
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "User",
                userEmail = currentUser.email ?: "",
                shelterName =
                    binding.etShelterName.text
                        .toString()
                        .trim(),
                shelterType =
                    binding.spinnerType.selectedItem
                        .toString()
                        .lowercase(),
                description =
                    binding.etDescription.text
                        .toString()
                        .trim(),
                address =
                    binding.etAddress.text
                        .toString()
                        .trim(),
                city =
                    binding.etCity.text
                        .toString()
                        .trim(),
                state =
                    binding.etState.text
                        .toString()
                        .trim(),
                pincode =
                    binding.etPincode.text
                        .toString()
                        .trim(),
                latitude = selectedLatitude,
                longitude = selectedLongitude,
                contactPerson =
                    binding.etContactPerson.text
                        .toString()
                        .trim(),
                phoneNumber =
                    binding.etPhone.text
                        .toString()
                        .trim(),
                email =
                    binding.etEmail.text
                        .toString()
                        .trim(),
                website =
                    binding.etWebsite.text
                        .toString()
                        .trim(),
                images = uploadedImageUrls,
                capacity =
                    binding.etCapacity.text
                        .toString()
                        .toIntOrNull() ?: 0,
                facilities = facilities,
                timings =
                    binding.etTimings.text
                        .toString()
                        .trim(),
                status = "pending",
                dateSubmitted = Timestamp.now(),
            )

        FirebaseUtils.firestore
            .collection("shelter_requests")
            .add(request)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Request submitted successfully! Admin will review it.", Toast.LENGTH_LONG).show()
                setResult(Activity.RESULT_OK)
                finish()
            }.addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.btnSubmit.isEnabled = true
                Toast.makeText(this, "Failed to submit: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
