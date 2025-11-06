package com.syed.activities.admin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import com.syed.databinding.ActivityAddEditShelterBinding
import com.syed.models.Shelter
import com.syed.utils.FirebaseUtils
import java.util.UUID

class AddEditShelterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddEditShelterBinding
    private var shelterId: String? = null
    private var selectedImages = mutableListOf<Uri>()
    private var uploadedImageUrls = mutableListOf<String>()
    private var selectedLocation: LatLng? = null
    private var isEditMode = false

    private val imagePickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetMultipleContents(),
        ) { uris ->
            if (uris != null && uris.isNotEmpty()) {
                selectedImages.clear()
                selectedImages.addAll(uris.take(5)) // Max 5 images
                binding.tvImageCount.text = "${selectedImages.size} images selected"
            }
        }

    private val locationPickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { data ->
                    val latitude = data.getDoubleExtra("latitude", 0.0)
                    val longitude = data.getDoubleExtra("longitude", 0.0)
                    val address = data.getStringExtra("address") ?: ""
                    val city = data.getStringExtra("city") ?: ""
                    val state = data.getStringExtra("state") ?: ""

                    // Update selected location
                    selectedLocation = LatLng(latitude, longitude)

                    // Update UI with selected location
                    binding.etAddress.setText(address)
                    binding.etCity.setText(city)
                    binding.etState.setText(state)

                    // Show location selected status
                    binding.tvLocationSelected.text = "📍 Location: ${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}"
                    binding.tvLocationSelected.visibility = View.VISIBLE

                    Toast.makeText(this, "Location selected successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditShelterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shelterId = intent.getStringExtra("shelterId")
        isEditMode = shelterId != null

        setupToolbar()
        setupSpinners()
        setupButtons()
        setupFacilitiesCheckboxes()

        if (isEditMode) {
            loadShelterData()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) "Edit Shelter" else "Add Shelter"
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

        binding.btnSelectLocation.setOnClickListener {
            openLocationPicker()
        }

        binding.btnSave.setOnClickListener {
            saveShelter()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupFacilitiesCheckboxes() {
        // Checkboxes are already in the layout
    }

    private fun openLocationPicker() {
        val intent = Intent(this, com.syed.activities.MapLocationPickerActivity::class.java)

        // If editing and location exists, pass current location to map
        selectedLocation?.let {
            intent.putExtra("latitude", it.latitude)
            intent.putExtra("longitude", it.longitude)
        }

        locationPickerLauncher.launch(intent)
    }

    private fun loadShelterData() {
        shelterId?.let { id ->
            binding.progressBar.visibility = View.VISIBLE

            FirebaseUtils.firestore
                .collection("shelters")
                .document(id)
                .get()
                .addOnSuccessListener { document ->
                    binding.progressBar.visibility = View.GONE

                    if (document.exists()) {
                        val shelter = document.toObject(Shelter::class.java)
                        shelter?.let { populateFields(it) }
                    }
                }.addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Failed to load shelter: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun populateFields(shelter: Shelter) {
        binding.etName.setText(shelter.name)
        binding.etDescription.setText(shelter.description)
        binding.etAddress.setText(shelter.address)
        binding.etCity.setText(shelter.city)
        binding.etState.setText(shelter.state)
        binding.etPincode.setText(shelter.pincode)
        binding.etContactPerson.setText(shelter.contactPerson)
        binding.etPhone.setText(shelter.phoneNumber)
        binding.etEmail.setText(shelter.email)
        binding.etWebsite.setText(shelter.website)
        binding.etCapacity.setText(shelter.capacity.toString())
        binding.etTimings.setText(shelter.timings)

        when (shelter.type.lowercase()) {
            "ngo" -> binding.spinnerType.setSelection(0)
            "government" -> binding.spinnerType.setSelection(1)
            "private" -> binding.spinnerType.setSelection(2)
        }

        uploadedImageUrls.addAll(shelter.images)
        binding.tvImageCount.text = "${uploadedImageUrls.size} images"

        shelter.location?.let {
            selectedLocation = LatLng(it.latitude, it.longitude)
            binding.tvLocationSelected.text = "Location selected"
        }

        // Set facilities checkboxes
        binding.cbVeterinary.isChecked = shelter.facilities.contains("veterinary")
        binding.cbFood.isChecked = shelter.facilities.contains("food")
        binding.cbShelter.isChecked = shelter.facilities.contains("shelter")
        binding.cbAdoption.isChecked = shelter.facilities.contains("adoption")
        binding.cbGrooming.isChecked = shelter.facilities.contains("grooming")
    }

    private fun saveShelter() {
        if (!validateInputs()) return

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        if (selectedImages.isNotEmpty()) {
            uploadImages { success ->
                if (success) {
                    saveShelterToFirestore()
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Toast.makeText(this, "Failed to upload images", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            saveShelterToFirestore()
        }
    }

    private fun validateInputs(): Boolean {
        if (binding.etName.text.isNullOrBlank()) {
            binding.etName.error = "Name is required"
            return false
        }
        if (binding.etAddress.text.isNullOrBlank()) {
            binding.etAddress.error = "Address is required"
            return false
        }
        if (binding.etPhone.text.isNullOrBlank()) {
            binding.etPhone.error = "Phone is required"
            return false
        }
        return true
    }

    private fun uploadImages(callback: (Boolean) -> Unit) {
        val storage = FirebaseStorage.getInstance()
        var uploadedCount = 0
        val totalImages = selectedImages.size

        selectedImages.forEach { uri ->
            val ref = storage.reference.child("shelters/${UUID.randomUUID()}.jpg")

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

    private fun saveShelterToFirestore() {
        val facilities = mutableListOf<String>()
        if (binding.cbVeterinary.isChecked) facilities.add("veterinary")
        if (binding.cbFood.isChecked) facilities.add("food")
        if (binding.cbShelter.isChecked) facilities.add("shelter")
        if (binding.cbAdoption.isChecked) facilities.add("adoption")
        if (binding.cbGrooming.isChecked) facilities.add("grooming")

        val shelter =
            Shelter(
                id = shelterId ?: "",
                name =
                    binding.etName.text
                        .toString()
                        .trim(),
                type =
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
                location = selectedLocation?.let { GeoPoint(it.latitude, it.longitude) },
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
                status = "approved",
                addedBy = FirebaseUtils.auth.currentUser?.uid ?: "",
                addedByType = "admin",
                dateModified = Timestamp.now(),
            )

        val collection = FirebaseUtils.firestore.collection("shelters")
        val task =
            if (isEditMode && shelterId != null) {
                collection.document(shelterId!!).set(shelter)
            } else {
                collection.add(shelter)
            }

        task
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Shelter saved successfully", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }.addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
                Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
