package com.syed.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.RangeSlider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.syed.R
import com.syed.adapters.PetsAdapter
import com.syed.models.Pet
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdvancedSearchActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PetsAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var searchButton: Button
    private lateinit var clearFiltersButton: Button

    // Filter views
    private lateinit var speciesChipGroup: ChipGroup
    private lateinit var breedInput: AutoCompleteTextView
    private lateinit var genderChipGroup: ChipGroup
    private lateinit var ageRangeSlider: RangeSlider
    private lateinit var sizeChipGroup: ChipGroup
    private lateinit var colorInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var vaccinatedCheckBox: CheckBox
    private lateinit var trainedCheckBox: CheckBox
    private lateinit var goodWithKidsCheckBox: CheckBox
    private lateinit var goodWithPetsCheckBox: CheckBox

    private val firestore = FirebaseFirestore.getInstance()
    private val pets = mutableListOf<Pet>()
    private val breeds = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_advanced_search)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Advanced Search"

        initializeViews()
        setupRecyclerView()
        setupFilters()
        loadBreeds()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.petsRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.emptyView)
        searchButton = findViewById(R.id.searchButton)
        clearFiltersButton = findViewById(R.id.clearFiltersButton)

        speciesChipGroup = findViewById(R.id.speciesChipGroup)
        breedInput = findViewById(R.id.breedInput)
        genderChipGroup = findViewById(R.id.genderChipGroup)
        ageRangeSlider = findViewById(R.id.ageRangeSlider)
        sizeChipGroup = findViewById(R.id.sizeChipGroup)
        colorInput = findViewById(R.id.colorInput)
        locationInput = findViewById(R.id.locationInput)
        vaccinatedCheckBox = findViewById(R.id.vaccinatedCheckBox)
        trainedCheckBox = findViewById(R.id.trainedCheckBox)
        goodWithKidsCheckBox = findViewById(R.id.goodWithKidsCheckBox)
        goodWithPetsCheckBox = findViewById(R.id.goodWithPetsCheckBox)

        searchButton.setOnClickListener { performSearch() }
        clearFiltersButton.setOnClickListener { clearFilters() }
    }

    private fun setupRecyclerView() {
        adapter =
            PetsAdapter(
                context = this,
                onPetClick = { pet ->
                    // Handle pet click - navigate to details
                    // Implementation depends on your navigation setup
                },
            )

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter
    }

    private fun setupFilters() {
        // Age range slider setup
        ageRangeSlider.valueFrom = 0f
        ageRangeSlider.valueTo = 20f
        ageRangeSlider.values = listOf(0f, 20f)
    }

    private fun loadBreeds() {
        lifecycleScope.launch {
            try {
                val snapshot =
                    firestore
                        .collection("pets")
                        .get()
                        .await()

                val breedSet = mutableSetOf<String>()
                for (doc in snapshot.documents) {
                    val breed = doc.getString("breed")
                    if (!breed.isNullOrEmpty()) {
                        breedSet.add(breed)
                    }
                }

                breeds.clear()
                breeds.addAll(breedSet.sorted())

                val breedAdapter =
                    ArrayAdapter(
                        this@AdvancedSearchActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        breeds,
                    )
                breedInput.setAdapter(breedAdapter)
            } catch (e: Exception) {
                Toast
                    .makeText(
                        this@AdvancedSearchActivity,
                        "Error loading breeds: ${e.message}",
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }
    }

    private fun performSearch() {
        progressBar.visibility = View.VISIBLE
        emptyView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Get selected filters
                val species = getSelectedChipText(speciesChipGroup)
                val breed = breedInput.text.toString().trim()
                val gender = getSelectedChipText(genderChipGroup)
                val ageRange = ageRangeSlider.values
                val size = getSelectedChipText(sizeChipGroup)
                val color = colorInput.text.toString().trim()
                val location = locationInput.text.toString().trim()

                android.util.Log.d("AdvancedSearch", "Searching with filters: species=$species, breed=$breed, gender=$gender")

                // Build query - using 'available' field instead of 'adopted'
                var query: Query =
                    firestore
                        .collection("pets")
                        .whereEqualTo("available", true) // Changed from adopted=false to available=true

                // Apply filters
                if (species.isNotEmpty()) {
                    query = query.whereEqualTo("type", species) // Changed from 'species' to 'type'
                }

                if (breed.isNotEmpty()) {
                    query = query.whereEqualTo("breed", breed)
                }

                if (gender.isNotEmpty()) {
                    query = query.whereEqualTo("gender", gender)
                }

                if (size.isNotEmpty()) {
                    query = query.whereEqualTo("size", size)
                }

                if (location.isNotEmpty()) {
                    query = query.whereEqualTo("location", location)
                }

                val snapshot = query.get().await()
                android.util.Log.d("AdvancedSearch", "Query returned ${snapshot.size()} results")

                pets.clear()
                for (doc in snapshot.documents) {
                    try {
                        val pet = doc.toObject(Pet::class.java)?.copy(id = doc.id)
                        pet?.let {
                            // Apply additional filters that can't be done in query
                            if (passesAdditionalFilters(it, ageRange, color)) {
                                pets.add(it)
                                android.util.Log.d("AdvancedSearch", "Added pet: ${it.name}")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AdvancedSearch", "Error parsing pet", e)
                    }
                }

                android.util.Log.d("AdvancedSearch", "Final result count: ${pets.size}")
                adapter.updatePets(pets) // Use updatePets instead of notifyDataSetChanged
                updateEmptyView()
                progressBar.visibility = View.GONE

                Toast
                    .makeText(
                        this@AdvancedSearchActivity,
                        "Found ${pets.size} pets",
                        Toast.LENGTH_SHORT,
                    ).show()
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast
                    .makeText(
                        this@AdvancedSearchActivity,
                        "Search error: ${e.message}",
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }
    }

    private fun passesAdditionalFilters(
        pet: Pet,
        ageRange: List<Float>,
        color: String,
    ): Boolean {
        // Age filter
        val age = pet.age.toFloatOrNull() ?: 0f
        if (age < ageRange[0] || age > ageRange[1]) {
            return false
        }

        // Color filter
        if (color.isNotEmpty() && !pet.color.contains(color, ignoreCase = true)) {
            return false
        }

        // Vaccinated filter
        if (vaccinatedCheckBox.isChecked && !pet.vaccinated) {
            return false
        }

        // Trained filter
        if (trainedCheckBox.isChecked && pet.description.contains("trained", ignoreCase = true).not()) {
            return false
        }

        // Good with kids filter
        if (goodWithKidsCheckBox.isChecked && pet.description.contains("kids", ignoreCase = true).not()) {
            return false
        }

        // Good with pets filter
        if (goodWithPetsCheckBox.isChecked && pet.description.contains("pets", ignoreCase = true).not()) {
            return false
        }

        return true
    }

    private fun getSelectedChipText(chipGroup: ChipGroup): String {
        val selectedId = chipGroup.checkedChipId
        if (selectedId != View.NO_ID) {
            val chip = chipGroup.findViewById<Chip>(selectedId)
            return chip?.text.toString()
        }
        return ""
    }

    private fun clearFilters() {
        speciesChipGroup.clearCheck()
        breedInput.text.clear()
        genderChipGroup.clearCheck()
        ageRangeSlider.values = listOf(0f, 20f)
        sizeChipGroup.clearCheck()
        colorInput.text.clear()
        locationInput.text.clear()
        vaccinatedCheckBox.isChecked = false
        trainedCheckBox.isChecked = false
        goodWithKidsCheckBox.isChecked = false
        goodWithPetsCheckBox.isChecked = false

        pets.clear()
        adapter.updatePets(pets)
        updateEmptyView()

        Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show()
    }

    private fun updateEmptyView() {
        if (pets.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
