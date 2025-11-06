package com.syed.activities.admin

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.syed.databinding.ActivityAddEditTipBinding
import com.syed.models.Tip
import com.syed.utils.FirebaseUtils

class AddEditTipActivity : AdminBaseActivity() {
    private lateinit var binding: ActivityAddEditTipBinding
    private var editingTip: Tip? = null
    private var isEditMode = false

    override fun onAdminVerified() {
        binding = ActivityAddEditTipBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUI()
        setupClickListeners()

        // Check if editing existing tip
        val tipId = intent.getStringExtra("tip_id")
        if (!tipId.isNullOrEmpty()) {
            isEditMode = true
            loadTipForEditing(tipId)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) "Edit Tip" else "Add New Tip"
    }

    private fun setupUI() {
        // Set active and published by default for new tips
        if (!isEditMode) {
            binding.switchActive.isChecked = true
        }
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveTip()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun loadTipForEditing(tipId: String) {
        binding.progressBar.visibility = View.VISIBLE

        FirebaseUtils.firestore
            .collection(FirebaseUtils.TIPS_COLLECTION)
            .document(tipId)
            .get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE

                if (document.exists()) {
                    val tip = document.toObject(Tip::class.java)?.copy(id = document.id)
                    if (tip != null) {
                        editingTip = tip
                        populateFields(tip)
                    }
                } else {
                    Toast.makeText(this, "Tip not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }.addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error loading tip: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun populateFields(tip: Tip) {
        binding.etTitle.setText(tip.title)
        binding.etContent.setText(tip.content)
        binding.etCategory.setText(tip.category)
        binding.switchActive.isChecked = tip.isActive
    }

    private fun saveTip() {
        val title =
            binding.etTitle.text
                .toString()
                .trim()
        val content =
            binding.etContent.text
                .toString()
                .trim()
        val category =
            binding.etCategory.text
                .toString()
                .trim()
        val isActive = binding.switchActive.isChecked

        // Validation
        if (title.isEmpty()) {
            binding.etTitle.error = "Title is required"
            binding.etTitle.requestFocus()
            return
        }

        if (content.isEmpty()) {
            binding.etContent.error = "Content is required"
            binding.etContent.requestFocus()
            return
        }

        if (content.length < 10) {
            binding.etContent.error = "Content must be at least 10 characters"
            binding.etContent.requestFocus()
            return
        }

        if (category.isEmpty()) {
            binding.etCategory.error = "Category is required"
            binding.etCategory.requestFocus()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentTime = System.currentTimeMillis()

        val tip =
            if (isEditMode && editingTip != null) {
                // Update existing tip
                editingTip!!.copy(
                    title = title,
                    content = content,
                    category = category,
                    isActive = isActive,
                    isPublished = true, // Always published when saved
                    updatedAt = currentTime,
                    lastUpdated = currentTime,
                )
            } else {
                // Create new tip
                Tip(
                    title = title,
                    content = content,
                    category = category,
                    isActive = isActive,
                    isPublished = true, // Publish by default
                    createdAt = currentTime,
                    updatedAt = currentTime,
                    dateAdded = currentTime,
                    lastUpdated = currentTime,
                    authorId = currentUser?.uid ?: "",
                    authorName = currentUser?.displayName ?: currentUser?.email ?: "Admin",
                )
            }

        val operation =
            if (isEditMode) {
                FirebaseUtils.firestore
                    .collection(FirebaseUtils.TIPS_COLLECTION)
                    .document(editingTip!!.id)
                    .set(tip)
            } else {
                FirebaseUtils.firestore
                    .collection(FirebaseUtils.TIPS_COLLECTION)
                    .add(tip)
            }

        operation
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                val message = if (isEditMode) "Tip updated successfully" else "Tip added successfully"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
                val message = if (isEditMode) "Failed to update tip" else "Failed to add tip"
                Toast.makeText(this, "$message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
