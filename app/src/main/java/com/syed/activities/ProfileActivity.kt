package com.syed.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.syed.databinding.ActivityProfileBinding
import com.syed.models.User
import com.syed.utils.FirebaseUtils
import com.syed.utils.PermissionUtils
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 2001
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        loadUserProfile()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Profile"
    }

    private fun setupClickListeners() {
        binding.ivProfileImage.setOnClickListener {
            if (PermissionUtils.hasStoragePermissions(this)) {
                selectImage()
            } else {
                PermissionUtils.requestImagePermissions(this) {
                    selectImage()
                }
            }
        }

        binding.btnEditProfile.setOnClickListener {
            enableEditing(true)
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        binding.btnCancelEdit.setOnClickListener {
            enableEditing(false)
            loadUserProfile()
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun loadUserProfile() {
        val firebaseUser = FirebaseUtils.auth.currentUser
        if (firebaseUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        // Load user data from Firestore
        FirebaseUtils.firestore
            .collection(FirebaseUtils.USERS_COLLECTION)
            .document(firebaseUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    currentUser = document.toObject(User::class.java)
                    displayUserInfo(currentUser)
                } else {
                    // Create user profile if doesn't exist
                    val newUser =
                        User(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            name = firebaseUser.displayName ?: "",
                            phone = "",
                            address = "",
                            profileImageUrl = firebaseUser.photoUrl?.toString() ?: "",
                        )
                    currentUser = newUser
                    displayUserInfo(newUser)
                }
                binding.progressBar.visibility = View.GONE
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun displayUserInfo(user: User?) {
        user?.let {
            binding.etName.setText(it.name)
            binding.etEmail.setText(it.email)
            binding.etPhone.setText(it.phone)
            binding.etAddress.setText(it.address)

            // Display profile image
            if (it.profileImageUrl.isNotEmpty()) {
                Glide
                    .with(this)
                    .load(it.profileImageUrl)
                    .circleCrop()
                    .into(binding.ivProfileImage)
            }

            // Display user statistics
            loadUserStatistics(it.uid)

            // Check if user is admin and show admin button
            checkAdminStatus()
        }
    }

    private fun checkAdminStatus() {
        FirebaseUtils.isCurrentUserAdmin { isAdmin ->
            if (isAdmin) {
                // Show admin badge or button if layout supports it
                android.util.Log.d("ProfileActivity", "User is admin")
                Toast.makeText(this, "✨ Admin Access Granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserStatistics(userId: String) {
        // Load adoption requests count
        FirebaseUtils.firestore
            .collection(FirebaseUtils.ADOPTION_REQUESTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                binding.tvAdoptionRequestsCount.text = documents.size().toString()
            }

        // Load rescue requests count
        FirebaseUtils.firestore
            .collection(FirebaseUtils.RESCUE_REQUESTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                binding.tvRescueRequestsCount.text = documents.size().toString()
            }

        // Load contact messages count
        FirebaseUtils.firestore
            .collection(FirebaseUtils.CONTACT_MESSAGES_COLLECTION)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                binding.tvMessagesCount.text = documents.size().toString()
            }
    }

    private fun enableEditing(enable: Boolean) {
        binding.etName.isEnabled = enable
        binding.etPhone.isEnabled = enable
        binding.etAddress.isEnabled = enable

        binding.btnEditProfile.visibility = if (enable) View.GONE else View.VISIBLE
        binding.btnSaveProfile.visibility = if (enable) View.VISIBLE else View.GONE
        binding.btnCancelEdit.visibility = if (enable) View.VISIBLE else View.GONE
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            Glide
                .with(this)
                .load(selectedImageUri)
                .circleCrop()
                .into(binding.ivProfileImage)
        }
    }

    private fun saveProfile() {
        val name =
            binding.etName.text
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

        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSaveProfile.isEnabled = false

        if (selectedImageUri != null) {
            uploadImageAndSaveProfile(name, phone, address)
        } else {
            saveProfileToFirestore(name, phone, address, currentUser?.profileImageUrl ?: "")
        }
    }

    private fun uploadImageAndSaveProfile(
        name: String,
        phone: String,
        address: String,
    ) {
        val firebaseUser = FirebaseUtils.auth.currentUser ?: return
        val storageRef = FirebaseStorage.getInstance().reference
        val imageFileName = "profile_${firebaseUser.uid}_${System.currentTimeMillis()}.jpg"
        val imageRef = storageRef.child("users/${firebaseUser.uid}/$imageFileName")

        selectedImageUri?.let { uri ->
            imageRef
                .putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        saveProfileToFirestore(name, phone, address, downloadUrl.toString())
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    binding.btnSaveProfile.isEnabled = true
                }
        }
    }

    private fun saveProfileToFirestore(
        name: String,
        phone: String,
        address: String,
        imageUrl: String,
    ) {
        val firebaseUser = FirebaseUtils.auth.currentUser ?: return

        // Use update() instead of set() to preserve admin fields
        val updates =
            hashMapOf<String, Any>(
                "name" to name,
                "phone" to phone,
                "address" to address,
                "profileImageUrl" to imageUrl,
            )

        FirebaseUtils.firestore
            .collection(FirebaseUtils.USERS_COLLECTION)
            .document(firebaseUser.uid)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                // Update current user with new values while preserving admin status
                currentUser = currentUser?.copy(
                    name = name,
                    phone = phone,
                    address = address,
                    profileImageUrl = imageUrl,
                ) ?: User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    name = name,
                    phone = phone,
                    address = address,
                    profileImageUrl = imageUrl,
                )
                enableEditing(false)
                selectedImageUri = null
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }.addOnCompleteListener {
                binding.progressBar.visibility = View.GONE
                binding.btnSaveProfile.isEnabled = true
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showDeleteAccountDialog() {
        AlertDialog
            .Builder(this)
            .setTitle("Delete Account")
            .setMessage(
                "This will permanently delete:\n\n" +
                    "• Your profile and account\n" +
                    "• All adoption requests\n" +
                    "• All rescue requests\n" +
                    "• All messages and data\n\n" +
                    "This action cannot be undone.\n\n" +
                    "Are you sure you want to continue?",
            ).setPositiveButton("Delete Forever") { _, _ ->
                deleteUserAccount()
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUserAccount() {
        binding.progressBar.visibility = View.VISIBLE

        // Use lifecycleScope to handle async deletion
        lifecycleScope.launch {
            val success = FirebaseUtils.deleteUserAccount()

            if (success) {
                Toast
                    .makeText(
                        this@ProfileActivity,
                        "Account deleted successfully",
                        Toast.LENGTH_SHORT,
                    ).show()

                // Navigate to login activity
                val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                binding.progressBar.visibility = View.GONE
                Toast
                    .makeText(
                        this@ProfileActivity,
                        "Failed to delete account. Please try again.",
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }
    }
}
