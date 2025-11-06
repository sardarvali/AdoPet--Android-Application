package com.syed.activities.admin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.storage.FirebaseStorage
import com.syed.R
import com.syed.adapters.SuccessStoriesAdapter
import com.syed.models.SuccessStory
import com.syed.utils.FirebaseUtils
import com.syed.utils.NotificationUtils
import java.util.*

class ManageSuccessStoriesActivity : AdminBaseActivity() {
    private lateinit var storiesAdapter: SuccessStoriesAdapter
    private var selectedImageUri: Uri? = null
    private var editingStoryId: String? = null
    private val PICK_IMAGE_REQUEST = 2001

    override fun onAdminVerified() {
        // Use existing layout instead of non-existent binding
        setContentView(R.layout.activity_manage_pets)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadSuccessStories()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Success Stories"
    }

    private fun setupRecyclerView() {
        storiesAdapter =
            SuccessStoriesAdapter(
                context = this,
                isAdminMode = true,
                onEditClick = { story -> startEditingStory(story) },
                onDeleteClick = { story -> confirmDeleteStory(story) },
            )

        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvPets)?.apply {
            adapter = storiesAdapter
            layoutManager = LinearLayoutManager(this@ManageSuccessStoriesActivity)
        }
    }

    private fun setupClickListeners() {
        // Use existing FAB or create simple buttons
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddPet)?.let { fab ->
            fab.setOnClickListener { startAddingStory() }
        }
    }

    private fun startAddingStory() {
        editingStoryId = null
        showAddEditDialog(null)
    }

    private fun startEditingStory(story: SuccessStory) {
        editingStoryId = story.id
        showAddEditDialog(story)
    }

    private fun showAddEditDialog(story: SuccessStory?) {
        // Create a simple dialog for adding/editing stories
        val dialog =
            AlertDialog
                .Builder(this)
                .setTitle(if (story != null) "Edit Success Story" else "Add Success Story")
                .setMessage("This feature is being developed. Please use the web admin panel for now.")
                .setPositiveButton("OK", null)
                .create()

        dialog.show()
    }

    private fun loadSuccessStories() {
        findViewById<android.widget.ProgressBar>(R.id.progressBar)?.visibility = View.VISIBLE

        FirebaseUtils.firestore
            .collection(FirebaseUtils.SUCCESS_STORIES_COLLECTION)
            .orderBy("dateAdded", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val stories =
                    documents.mapNotNull { doc ->
                        try {
                            doc.toObject(SuccessStory::class.java).copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                storiesAdapter.updateStories(stories)
                findViewById<android.widget.ProgressBar>(R.id.progressBar)?.visibility = View.GONE

                val emptyState = findViewById<android.widget.TextView>(R.id.tvEmptyState)
                emptyState?.visibility = if (stories.isEmpty()) View.VISIBLE else View.GONE
                emptyState?.text = "No success stories found"
            }.addOnFailureListener { e ->
                findViewById<android.widget.ProgressBar>(R.id.progressBar)?.visibility = View.GONE
                Toast.makeText(this, "Failed to load success stories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDeleteStory(story: SuccessStory) {
        AlertDialog
            .Builder(this)
            .setTitle("Delete Success Story")
            .setMessage("Are you sure you want to delete the story of ${story.petName}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteStory(story)
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteStory(story: SuccessStory) {
        FirebaseUtils.firestore
            .collection(FirebaseUtils.SUCCESS_STORIES_COLLECTION)
            .document(story.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Success story deleted", Toast.LENGTH_SHORT).show()
                loadSuccessStories()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete story: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
