package com.syed.activities.admin

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.storage.FirebaseStorage
import com.syed.R
import com.syed.databinding.ActivityAddEditPetBinding
import com.syed.models.Pet
import com.syed.utils.FirebaseUtils
import com.syed.utils.NotificationUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AddEditPetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddEditPetBinding
    private var petToEdit: Pet? = null
    private var selectedImageUris: MutableList<Uri> = mutableListOf()
    private var hasUnsavedChanges = false

    // Modern image picker for multiple images
    private val multipleImagePickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetMultipleContents(),
        ) { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                selectedImageUris.addAll(uris)
                updateImagePreview()
                hasUnsavedChanges = true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditPetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check admin privileges
        checkAdminAccess()
    }

    private fun checkAdminAccess() {
        showLoading(getString(R.string.checking_access))

        lifecycleScope.launch {
            try {
                val isAdmin = FirebaseUtils.isCurrentUserAdminSuspend()
                hideLoading()

                if (!isAdmin) {
                    showError("Access denied. Admin privileges required.")
                    finish()
                } else {
                    initializeActivity()
                }
            } catch (e: Exception) {
                hideLoading()
                showError("Failed to verify admin access")
                finish()
            }
        }
    }

    private fun initializeActivity() {
        setupToolbar()
        setupDropdowns()
        setupClickListeners()
        setupTextWatchers()

        // Load pet data if editing
        intent.getStringExtra(EXTRA_PET_ID)?.let { petId ->
            loadPetForEdit(petId)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title =
                if (petToEdit != null) {
                    getString(R.string.edit_pet)
                } else {
                    getString(R.string.add_new_pet)
                }
        }
    }

    private fun setupDropdowns() {
        // Pet types dropdown
        val petTypes = resources.getStringArray(R.array.pet_types)
        val typeAdapter = ArrayAdapter(this, R.layout.dropdown_item, petTypes)
        binding.actvPetType.setAdapter(typeAdapter)

        // Gender dropdown
        val genders = resources.getStringArray(R.array.pet_genders)
        val genderAdapter = ArrayAdapter(this, R.layout.dropdown_item, genders)
        binding.actvGender.setAdapter(genderAdapter)

        // Set default selections
        if (petToEdit == null) {
            binding.actvPetType.setText(petTypes[0], false)
            binding.actvGender.setText(genders[0], false)
        }
    }

    private fun setupClickListeners() {
        binding.btnSelectImage.setOnClickListener {
            multipleImagePickerLauncher.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            if (validateFields()) {
                savePet()
            }
        }

        binding.btnCancel.setOnClickListener {
            handleBackPress()
        }

        binding.ivPetImage.setOnClickListener {
            multipleImagePickerLauncher.launch("image/*")
        }
    }

    private fun updateImagePreview() {
        if (selectedImageUris.isNotEmpty()) {
            loadImageIntoView(selectedImageUris[0])
            binding.btnSelectImage.text = "${selectedImageUris.size} image(s) selected"
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = { hasUnsavedChanges = true }

        binding.etPetName.addTextChangedListener { textWatcher() }
        binding.etBreed.addTextChangedListener { textWatcher() }
        binding.etAge.addTextChangedListener { textWatcher() }
        binding.etDescription.addTextChangedListener { textWatcher() }
        binding.etLocation.addTextChangedListener { textWatcher() }
        binding.actvPetType.addTextChangedListener { textWatcher() }
        binding.actvGender.addTextChangedListener { textWatcher() }
        binding.switchAvailable.setOnCheckedChangeListener { _, _ -> textWatcher() }
    }

    private fun loadPetForEdit(petId: String) {
        showLoading(getString(R.string.loading_pet_data))

        lifecycleScope.launch {
            try {
                val document =
                    FirebaseUtils.firestore
                        .collection(FirebaseUtils.PETS_COLLECTION)
                        .document(petId)
                        .get()
                        .await()

                if (document.exists()) {
                    petToEdit = document.toObject(Pet::class.java)?.copy(id = document.id)
                    populateFields()
                    hasUnsavedChanges = false
                } else {
                    showError("Pet not found")
                    finish()
                }
            } catch (e: Exception) {
                showError("Failed to load pet: ${e.localizedMessage}")
                finish()
            } finally {
                hideLoading()
            }
        }
    }

    private fun populateFields() {
        petToEdit?.let { pet ->
            binding.apply {
                etPetName.setText(pet.name)
                etBreed.setText(pet.breed)
                etAge.setText(pet.age)
                etDescription.setText(pet.description)
                etLocation.setText(pet.location)
                actvPetType.setText(pet.type, false)
                actvGender.setText(pet.gender, false)
                switchAvailable.isChecked = pet.available

                // Load existing images
                if (pet.imageUrls.isNotEmpty()) {
                    loadImageIntoView(pet.imageUrls[0])
                    binding.btnSelectImage.text = "${pet.imageUrls.size} image(s)"
                } else if (pet.imageUrl.isNotEmpty()) {
                    loadImageIntoView(pet.imageUrl)
                }
            }
        }
    }

    private fun loadImageIntoView(imageSource: Any) {
        Glide
            .with(this)
            .load(imageSource)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_placeholder)
            .centerCrop()
            .into(binding.ivPetImage)
    }

    private fun validateFields(): Boolean {
        var isValid = true

        binding.apply {
            // Pet name validation
            if (etPetName.text
                    .toString()
                    .trim()
                    .isEmpty()
            ) {
                tilPetName.error = getString(R.string.error_pet_name_required)
                isValid = false
            } else {
                tilPetName.error = null
            }

            // Breed validation
            if (etBreed.text
                    .toString()
                    .trim()
                    .isEmpty()
            ) {
                tilBreed.error = getString(R.string.error_breed_required)
                isValid = false
            } else {
                tilBreed.error = null
            }

            // Age validation
            val ageText = etAge.text.toString().trim()
            if (ageText.isEmpty()) {
                tilAge.error = getString(R.string.error_age_required)
                isValid = false
            } else {
                val age = ageText.toDoubleOrNull()
                if (age == null || age < 0 || age > 30) {
                    tilAge.error = getString(R.string.error_age_invalid)
                    isValid = false
                } else {
                    tilAge.error = null
                }
            }

            // Description validation
            if (etDescription.text
                    .toString()
                    .trim()
                    .isEmpty()
            ) {
                tilDescription.error = getString(R.string.error_description_required)
                isValid = false
            } else if (etDescription.text
                    .toString()
                    .trim()
                    .length < 20
            ) {
                tilDescription.error = getString(R.string.error_description_too_short)
                isValid = false
            } else {
                tilDescription.error = null
            }

            // Location validation
            if (etLocation.text
                    .toString()
                    .trim()
                    .isEmpty()
            ) {
                tilLocation.error = getString(R.string.error_location_required)
                isValid = false
            } else {
                tilLocation.error = null
            }

            // Image validation
            if (selectedImageUris.isEmpty() && petToEdit?.imageUrl.isNullOrEmpty() && petToEdit?.imageUrls.isNullOrEmpty()) {
                showError(getString(R.string.error_image_required))
                isValid = false
            }
        }

        return isValid
    }

    private fun savePet() {
        showLoading(getString(R.string.saving_pet))
        disableButtons()

        lifecycleScope.launch {
            try {
                val imageUrls = mutableListOf<String>()

                // Upload new images if selected
                if (selectedImageUris.isNotEmpty()) {
                    for (uri in selectedImageUris) {
                        val uploadedUrl = uploadImage(uri)
                        imageUrls.add(uploadedUrl)
                    }
                } else if (petToEdit != null) {
                    // Keep existing images
                    imageUrls.addAll(petToEdit!!.imageUrls)
                    if (imageUrls.isEmpty() && petToEdit!!.imageUrl.isNotEmpty()) {
                        imageUrls.add(petToEdit!!.imageUrl)
                    }
                }

                savePetToFirestore(imageUrls)
                hasUnsavedChanges = false
            } catch (e: Exception) {
                hideLoading()
                enableButtons()
                showError("Failed to save pet: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun uploadImage(uri: Uri): String {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageFileName = "pets/${UUID.randomUUID()}_${System.currentTimeMillis()}.jpg"
        val imageRef = storageRef.child(imageFileName)

        imageRef.putFile(uri).await()
        return imageRef.downloadUrl.await().toString()
    }

    private suspend fun savePetToFirestore(imageUrls: List<String>) {
        binding.apply {
            val pet =
                if (petToEdit != null) {
                    // Update existing pet
                    petToEdit!!.copy(
                        name = etPetName.text.toString().trim(),
                        type = actvPetType.text.toString(),
                        breed = etBreed.text.toString().trim(),
                        age = etAge.text.toString().trim(),
                        gender = actvGender.text.toString(),
                        description = etDescription.text.toString().trim(),
                        location = etLocation.text.toString().trim(),
                        imageUrl = imageUrls.firstOrNull() ?: "",
                        imageUrls = imageUrls,
                        available = switchAvailable.isChecked,
                        lastUpdated = System.currentTimeMillis(),
                    )
                } else {
                    // Create new pet
                    Pet(
                        name = etPetName.text.toString().trim(),
                        type = actvPetType.text.toString(),
                        breed = etBreed.text.toString().trim(),
                        age = etAge.text.toString().trim(),
                        gender = actvGender.text.toString(),
                        description = etDescription.text.toString().trim(),
                        location = etLocation.text.toString().trim(),
                        imageUrl = imageUrls.firstOrNull() ?: "",
                        imageUrls = imageUrls,
                        available = switchAvailable.isChecked,
                        addedBy = FirebaseUtils.getCurrentUserId() ?: "",
                        dateAdded = System.currentTimeMillis(),
                        lastUpdated = System.currentTimeMillis(),
                    )
                }

            val petRef =
                if (petToEdit != null) {
                    FirebaseUtils.firestore
                        .collection(FirebaseUtils.PETS_COLLECTION)
                        .document(petToEdit!!.id)
                } else {
                    FirebaseUtils.firestore
                        .collection(FirebaseUtils.PETS_COLLECTION)
                        .document()
                }

            petRef.set(pet).await()

            // Send notification for new pets
            if (petToEdit == null) {
                NotificationUtils.notifyNewPetAvailable(
                    this@AddEditPetActivity,
                    pet.name,
                    pet.type,
                )
            }

            hideLoading()
            val message =
                if (petToEdit != null) {
                    getString(R.string.pet_updated_successfully)
                } else {
                    getString(R.string.pet_added_successfully)
                }

            Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show()

            // Return result and finish
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun showLoading(message: String) {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.tvLoadingMessage.text = message
    }

    private fun hideLoading() {
        binding.loadingOverlay.visibility = View.GONE
    }

    private fun disableButtons() {
        binding.btnSave.isEnabled = false
        binding.btnCancel.isEnabled = false
        binding.btnSelectImage.isEnabled = false
    }

    private fun enableButtons() {
        binding.btnSave.isEnabled = true
        binding.btnCancel.isEnabled = true
        binding.btnSelectImage.isEnabled = true
    }

    private fun showError(message: String) {
        Snackbar
            .make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.dismiss)) { }
            .show()
    }

    private fun handleBackPress() {
        if (hasUnsavedChanges) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.unsaved_changes)
                .setMessage(R.string.unsaved_changes_message)
                .setPositiveButton(R.string.discard) { _, _ -> finish() }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        handleBackPress()
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        handleBackPress()
        super.onBackPressed()
    }

    companion object {
        const val EXTRA_PET_ID = "petId"
    }
}
