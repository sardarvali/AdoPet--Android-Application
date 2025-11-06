package com.syed.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.syed.R
import com.syed.adapters.PetsAdapter
import com.syed.databinding.ActivityPetsListBinding
import com.syed.models.Pet
import com.syed.utils.FirebaseUtils

class PetsListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPetsListBinding
    private lateinit var petsAdapter: PetsAdapter
    private var petType: String = "all"
    private var isAdminMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPetsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        petType = intent.getStringExtra("pet_type") ?: "all"
        val title = intent.getStringExtra("title") ?: "Pets"
        isAdminMode = intent.getBooleanExtra("isAdminMode", false)

        setupToolbar(title)
        setupRecyclerView()
        setupAdminControls()
        loadPets()
        setupSwipeRefresh()
        setupSearchFunctionality()
    }

    private fun setupToolbar(title: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isAdminMode) "Manage $title" else title
    }

    private fun setupAdminControls() {
        if (isAdminMode) {
            // Verify admin permissions before showing admin controls
            FirebaseUtils.isCurrentUserAdmin { isAdmin ->
                if (isAdmin) {
                    binding.fabAddPet?.visibility = View.VISIBLE
                    binding.fabAddPet?.setOnClickListener {
                        startActivity(Intent(this, com.syed.activities.admin.AddEditPetActivity::class.java))
                    }
                } else {
                    // User is not admin, hide admin controls and show regular mode
                    binding.fabAddPet?.visibility = View.GONE
                    isAdminMode = false
                    android.widget.Toast
                        .makeText(this, "Admin access denied", android.widget.Toast.LENGTH_SHORT)
                        .show()
                }
            }
        } else {
            binding.fabAddPet?.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        petsAdapter =
            PetsAdapter(
                context = this,
                isAdminMode = isAdminMode,
                onPetClick = { pet -> openPetDetails(pet) },
                onAdoptClick = { pet ->
                    if (isAdminMode) {
                        // Open edit pet for admin
                        startActivity(
                            Intent(this, com.syed.activities.admin.AddEditPetActivity::class.java).apply {
                                putExtra("petId", pet.id)
                            },
                        )
                    } else {
                        openAdoptionForm(pet)
                    }
                },
                onEditClick = { pet ->
                    if (isAdminMode) {
                        startActivity(
                            Intent(this, com.syed.activities.admin.AddEditPetActivity::class.java).apply {
                                putExtra("petId", pet.id)
                            },
                        )
                    }
                },
                onDeleteClick = { pet ->
                    if (isAdminMode) {
                        deletePet(pet)
                    }
                },
            )

        binding.rvPets.apply {
            adapter = petsAdapter
            layoutManager = GridLayoutManager(this@PetsListActivity, 2)
        }
    }

    private fun loadPets() {
        binding.progressBar.visibility = View.VISIBLE
        android.util.Log.d("PetsListActivity", "Loading pets with type: $petType")

        // First, let's do a diagnostic query to see ALL pets in Firestore
        FirebaseUtils.firestore
            .collection(FirebaseUtils.PETS_COLLECTION)
            .get()
            .addOnSuccessListener { allDocs ->
                android.util.Log.d("PetsListActivity", "=== DIAGNOSTIC: Total pets in Firestore: ${allDocs.size()} ===")
                allDocs.forEach { doc ->
                    android.util.Log.d(
                        "PetsListActivity",
                        "Pet ${doc.id}: type=${doc.get(
                            "type",
                        )}, available=${doc.get(
                            "available",
                        )}, isAvailable=${doc.get("isAvailable")}, actuallyAvailable=${doc.get("actuallyAvailable")}",
                    )
                }
            }

        val query =
            if (petType == "all") {
                // Query using 'available' field instead of 'isAvailable'
                FirebaseUtils.firestore
                    .collection(FirebaseUtils.PETS_COLLECTION)
                    .whereEqualTo("available", true)
            } else {
                // Make type comparison case-insensitive by converting to proper case
                val properCaseType = petType.replaceFirstChar { it.uppercase() } // "cat" -> "Cat", "dog" -> "Dog"
                FirebaseUtils.firestore
                    .collection(FirebaseUtils.PETS_COLLECTION)
                    .whereEqualTo("type", properCaseType)
                    .whereEqualTo("available", true)
            }

        query
            .orderBy("dateAdded", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                android.util.Log.d("PetsListActivity", "Query successful, found ${documents.size()} documents")

                val pets =
                    documents.mapNotNull { doc ->
                        try {
                            android.util.Log.d("PetsListActivity", "Processing pet doc: ${doc.id}")
                            android.util.Log.d("PetsListActivity", "Pet data: ${doc.data}")
                            val pet = doc.toObject(Pet::class.java).copy(id = doc.id)
                            android.util.Log.d("PetsListActivity", "Successfully parsed pet: ${pet.name}, type: ${pet.type}")
                            pet
                        } catch (e: Exception) {
                            android.util.Log.e("PetsListActivity", "Error parsing pet document ${doc.id}", e)
                            android.util.Log.e("PetsListActivity", "Document data: ${doc.data}")
                            null
                        }
                    }

                android.util.Log.d("PetsListActivity", "Total pets parsed: ${pets.size}")
                petsAdapter.updatePets(pets)
                binding.progressBar.visibility = View.GONE
                binding.tvEmptyState.visibility = if (pets.isEmpty()) View.VISIBLE else View.GONE

                if (pets.isEmpty()) {
                    binding.tvEmptyState.text = "No pets available in this category"
                }
            }.addOnFailureListener { e ->
                android.util.Log.e("PetsListActivity", "Failed to load pets", e)
                android.util.Log.e("PetsListActivity", "Error message: ${e.message}")
                android.util.Log.e("PetsListActivity", "Error type: ${e.javaClass.simpleName}")

                binding.progressBar.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE

                // Check if it's an index error
                if (e.message?.contains("index", ignoreCase = true) == true) {
                    binding.tvEmptyState.text = "Database index required. Please check Firebase Console."
                    android.widget.Toast
                        .makeText(
                            this,
                            "Firestore index required. Check logcat for URL",
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                } else {
                    binding.tvEmptyState.text = "Failed to load pets: ${e.message}"
                }
            }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadPets()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun openPetDetails(pet: Pet) {
        val intent = Intent(this, PetDetailsActivity::class.java)
        intent.putExtra("petId", pet.id)
        startActivity(intent)
    }

    private fun openAdoptionForm(pet: Pet) {
        val intent = Intent(this, AdoptionFormActivity::class.java)
        intent.putExtra("petId", pet.id)
        startActivity(intent)
    }

    private fun deletePet(pet: Pet) {
        androidx.appcompat.app.AlertDialog
            .Builder(this)
            .setTitle("Delete Pet")
            .setMessage("Are you sure you want to delete ${pet.name}? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                FirebaseUtils.firestore
                    .collection(FirebaseUtils.PETS_COLLECTION)
                    .document(pet.id)
                    .delete()
                    .addOnSuccessListener {
                        android.widget.Toast
                            .makeText(this, "Pet deleted successfully", android.widget.Toast.LENGTH_SHORT)
                            .show()
                        loadPets() // Refresh the list
                    }.addOnFailureListener { e ->
                        android.widget.Toast
                            .makeText(this, "Failed to delete pet: ${e.message}", android.widget.Toast.LENGTH_SHORT)
                            .show()
                    }
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupSearchFunctionality() {
        binding.etSearch?.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {
                    searchPets(s?.toString())
                }

                override fun afterTextChanged(s: android.text.Editable?) {}
            },
        )

        // Set up IME action (when user presses search on keyboard)
        binding.etSearch?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                // Hide keyboard
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(binding.etSearch?.windowToken, 0)
                true
            } else {
                false
            }
        }
    }

    private fun searchPets(query: String?) {
        if (query.isNullOrEmpty()) {
            loadPets()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        val searchQuery = query.lowercase()

        // Search in: name, breed, type, description
        FirebaseUtils.firestore
            .collection(FirebaseUtils.PETS_COLLECTION)
            .whereEqualTo("available", true)
            .get()
            .addOnSuccessListener { documents ->
                val pets =
                    documents.mapNotNull { doc ->
                        try {
                            val pet = doc.toObject(Pet::class.java).copy(id = doc.id)

                            // Filter pets by search query
                            val matchesSearch =
                                pet.name.lowercase().contains(searchQuery) ||
                                    pet.breed.lowercase().contains(searchQuery) ||
                                    pet.type.lowercase().contains(searchQuery) ||
                                    pet.description.lowercase().contains(searchQuery)

                            if (matchesSearch) pet else null
                        } catch (e: Exception) {
                            android.util.Log.e("PetsListActivity", "Error parsing pet", e)
                            null
                        }
                    }

                petsAdapter.updatePets(pets)
                binding.progressBar.visibility = View.GONE
                binding.tvEmptyState.visibility = if (pets.isEmpty()) View.VISIBLE else View.GONE

                if (pets.isEmpty()) {
                    binding.tvEmptyState.text = "No pets found matching \"$query\""
                }
            }.addOnFailureListener { e ->
                android.util.Log.e("PetsListActivity", "Search failed", e)
                binding.progressBar.visibility = View.GONE
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
