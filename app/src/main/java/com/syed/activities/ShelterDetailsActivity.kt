package com.syed.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.syed.R
import com.syed.adapters.ImageSliderAdapter
import com.syed.databinding.ActivityShelterDetailsBinding
import com.syed.models.Shelter
import com.syed.utils.FirebaseUtils

class ShelterDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShelterDetailsBinding
    private var shelterId: String? = null
    private var shelter: Shelter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShelterDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shelterId = intent.getStringExtra("shelterId")

        setupToolbar()
        setupButtons()
        loadShelterDetails()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Shelter Details"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupButtons() {
        binding.btnCall.setOnClickListener {
            shelter?.phoneNumber?.let { phone ->
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(intent)
            }
        }

        binding.btnEmail.setOnClickListener {
            shelter?.email?.let { email ->
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                startActivity(intent)
            }
        }

        binding.btnDirections.setOnClickListener {
            shelter?.location?.let { location ->
                val uri = "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}(${shelter?.name})"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                intent.setPackage("com.google.android.apps.maps")
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Google Maps not installed", Toast.LENGTH_SHORT).show()
                }
            } ?: Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
        }

        binding.btnWebsite.setOnClickListener {
            shelter?.website?.let { website ->
                if (website.isNotBlank()) {
                    val intent =
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                if (website.startsWith("http")) website else "https://$website",
                            ),
                        )
                    startActivity(intent)
                }
            }
        }
    }

    private fun loadShelterDetails() {
        shelterId?.let { id ->
            binding.progressBar.visibility = View.VISIBLE

            FirebaseUtils.firestore
                .collection("shelters")
                .document(id)
                .get()
                .addOnSuccessListener { document ->
                    binding.progressBar.visibility = View.GONE

                    if (document.exists()) {
                        shelter = document.toObject(Shelter::class.java)?.copy(id = document.id)
                        shelter?.let { displayShelterDetails(it) }
                    } else {
                        Toast.makeText(this, "Shelter not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }.addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun displayShelterDetails(shelter: Shelter) {
        binding.tvName.text = shelter.name
        binding.tvType.text = shelter.type.uppercase()
        binding.tvDescription.text = shelter.description
        binding.tvAddress.text = shelter.address
        binding.tvCity.text = "${shelter.city}, ${shelter.state} - ${shelter.pincode}"
        binding.tvContactPerson.text = shelter.contactPerson
        binding.tvPhone.text = shelter.phoneNumber
        binding.tvEmail.text = shelter.email
        binding.tvWebsite.text = shelter.website.ifBlank { "Not provided" }
        binding.tvCapacity.text = "${shelter.currentOccupancy}/${shelter.capacity}"
        binding.tvTimings.text = shelter.timings.ifBlank { "Contact for timings" }

        // Load images
        if (shelter.images.isNotEmpty()) {
            binding.viewPager.visibility = View.VISIBLE
            val adapter = ImageSliderAdapter(this, shelter.images)
            binding.viewPager.adapter = adapter
            binding.dotsIndicator.setViewPager2(binding.viewPager)
        } else {
            binding.viewPager.visibility = View.GONE
            Glide
                .with(this)
                .load(R.drawable.placeholder_pet)
                .into(binding.ivPlaceholder)
            binding.ivPlaceholder.visibility = View.VISIBLE
        }

        // Add facility chips
        binding.chipGroupFacilities.removeAllViews()
        shelter.facilities.forEach { facility ->
            val chip = Chip(this)
            chip.text = facility.replaceFirstChar { it.uppercase() }
            chip.isClickable = false
            binding.chipGroupFacilities.addView(chip)
        }

        // Show/hide website button
        binding.btnWebsite.visibility = if (shelter.website.isBlank()) View.GONE else View.VISIBLE
    }
}
