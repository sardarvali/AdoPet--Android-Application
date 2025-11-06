package com.syed.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.syed.adapters.ImagePagerAdapter
import com.syed.databinding.ActivityPetDetailsBinding
import com.syed.models.Pet
import com.syed.utils.FirebaseUtils

class PetDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPetDetailsBinding
    private var pet: Pet? = null
    private lateinit var imagePagerAdapter: ImagePagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPetDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        val petId = intent.getStringExtra("petId")
        if (petId != null) {
            loadPetDetails(petId)
        } else {
            finish()
        }

        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
    }

    private fun loadPetDetails(petId: String) {
        // Progress bar is not in the binding for the new layout, so we'll show a toast instead
        Toast.makeText(this, "Loading pet details...", Toast.LENGTH_SHORT).show()

        FirebaseUtils.firestore
            .collection(FirebaseUtils.PETS_COLLECTION)
            .document(petId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    pet = document.toObject(Pet::class.java)?.copy(id = document.id)
                    pet?.let { displayPetDetails(it) }
                } else {
                    Toast.makeText(this, "Pet not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load pet details", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayPetDetails(pet: Pet) {
        binding.tvPetName.text = pet.name
        binding.tvPetBreed.text = pet.breed
        binding.tvPetAge.text = "${pet.age} years"
        binding.tvPetGender.text = pet.gender
        binding.tvPetCategory.text = pet.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        binding.tvPetDescription.text = pet.description

        // Setup image pager
        if (pet.imageUrls.isNotEmpty()) {
            imagePagerAdapter = ImagePagerAdapter(pet.imageUrls)
            binding.viewPagerImages.adapter = imagePagerAdapter
            binding.dotsIndicator.setViewPager2(binding.viewPagerImages)
        }

        // Setup status chip
        if (pet.isAvailable) {
            binding.chipStatus.text = "Available"
            binding.chipStatus.setChipBackgroundColorResource(com.syed.R.color.success)
            binding.btnAdopt.visibility = View.VISIBLE
        } else {
            binding.chipStatus.text = "Adopted"
            binding.chipStatus.setChipBackgroundColorResource(com.syed.R.color.gray_500)
            binding.btnAdopt.visibility = View.GONE
        }

        supportActionBar?.title = pet.name
    }

    private fun setupClickListeners() {
        binding.btnAdopt.setOnClickListener {
            pet?.let { pet ->
                if (pet.isAvailable) {
                    val intent = Intent(this, AdoptionFormActivity::class.java)
                    intent.putExtra("petId", pet.id)
                    startActivity(intent)
                }
            }
        }

        binding.btnShare.setOnClickListener {
            pet?.let { pet ->
                val shareText =
                    "Check out ${pet.name}, a ${pet.breed} looking for a forever home!\n\n" +
                        "Age: ${pet.age} years\n" +
                        "Gender: ${pet.gender}\n" +
                        "Location: ${pet.location}\n\n" +
                        "${pet.description}"

                val shareIntent =
                    Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                startActivity(Intent.createChooser(shareIntent, "Share ${pet.name}"))
            }
        }

        // Contact functionality is now integrated into the share button
        // No separate contact button in the new modern layout
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
