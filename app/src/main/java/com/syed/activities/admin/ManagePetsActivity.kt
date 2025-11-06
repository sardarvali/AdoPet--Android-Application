package com.syed.activities.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.syed.adapters.PetsAdapter
import com.syed.databinding.ActivityManagePetsBinding
import com.syed.models.Pet
import com.syed.utils.FirebaseUtils

class ManagePetsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManagePetsBinding
    private lateinit var petsAdapter: PetsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagePetsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check admin privileges first using proper async check
        FirebaseUtils.isCurrentUserAdmin { isAdmin ->
            if (!isAdmin) {
                Toast.makeText(this, "Access denied. Admin privileges required.", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                setupToolbar()
                setupRecyclerView()
                setupClickListeners()
                loadPets()
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Pets"
    }

    private fun setupRecyclerView() {
        petsAdapter =
            PetsAdapter(
                context = this,
                onPetClick = { pet -> editPet(pet) },
                onEditClick = { pet -> editPet(pet) },
                onDeleteClick = { pet -> confirmDeletePet(pet) },
                isAdminMode = true,
            )

        binding.rvPets.apply {
            adapter = petsAdapter
            layoutManager = GridLayoutManager(this@ManagePetsActivity, 2)
        }
    }

    private fun setupClickListeners() {
        binding.fabAddPet?.setOnClickListener {
            startActivity(Intent(this, AddEditPetActivity::class.java))
        }

        binding.swipeRefresh?.setOnRefreshListener {
            loadPets()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun loadPets() {
        binding.progressBar?.visibility = View.VISIBLE

        FirebaseUtils.firestore
            .collection(FirebaseUtils.PETS_COLLECTION)
            .orderBy("dateAdded", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val pets =
                    documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Pet::class.java).copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                petsAdapter.updatePets(pets)
                binding.progressBar?.visibility = View.GONE
                binding.tvEmptyState?.visibility = if (pets.isEmpty()) View.VISIBLE else View.GONE
            }.addOnFailureListener { e ->
                binding.progressBar?.visibility = View.GONE
                binding.tvEmptyState?.visibility = View.VISIBLE
                Toast.makeText(this, "Failed to load pets: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun editPet(pet: Pet) {
        val intent = Intent(this, AddEditPetActivity::class.java)
        intent.putExtra("petId", pet.id)
        intent.putExtra("isEdit", true)
        startActivity(intent)
    }

    private fun confirmDeletePet(pet: Pet) {
        AlertDialog
            .Builder(this)
            .setTitle("Delete Pet")
            .setMessage("Are you sure you want to delete ${pet.name}? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deletePet(pet)
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePet(pet: Pet) {
        binding.progressBar?.visibility = View.VISIBLE

        // Delete pet document
        FirebaseUtils.firestore
            .collection(FirebaseUtils.PETS_COLLECTION)
            .document(pet.id)
            .delete()
            .addOnSuccessListener {
                // Delete associated images from storage
                deleteImageFromStorage(pet.imageUrl)
                pet.imageUrls.forEach { imageUrl ->
                    deleteImageFromStorage(imageUrl)
                }

                Toast.makeText(this, "${pet.name} deleted successfully", Toast.LENGTH_SHORT).show()
                loadPets() // Reload the list
            }.addOnFailureListener { e ->
                binding.progressBar?.visibility = View.GONE
                Toast.makeText(this, "Failed to delete pet: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteImageFromStorage(imageUrl: String) {
        if (imageUrl.isNotEmpty()) {
            try {
                val storageRef =
                    com.google.firebase.storage.FirebaseStorage
                        .getInstance()
                        .getReferenceFromUrl(imageUrl)
                storageRef
                    .delete()
                    .addOnFailureListener { e ->
                        android.util.Log.e("ManagePets", "Failed to delete image from storage", e)
                    }
            } catch (e: Exception) {
                android.util.Log.e("ManagePets", "Error deleting image", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh pets list when returning from AddEditPetActivity
        loadPets()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
