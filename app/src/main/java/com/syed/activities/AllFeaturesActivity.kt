package com.syed.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.syed.R
import com.syed.adapters.FeaturesAdapter
import com.syed.databinding.ActivityAllFeaturesBinding
import com.syed.models.FeatureAction
import com.syed.models.FeatureCard

/**
 * Activity to show all available features in the app
 * This includes all features from navigation drawer and home page
 */
class AllFeaturesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAllFeaturesBinding
    private lateinit var featuresAdapter: FeaturesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllFeaturesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupFeaturesGrid()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "All Features"
        }
    }

    private fun setupFeaturesGrid() {
        // Complete list of all features available in the app
        val features =
            listOf(
                // Pet Browsing
                FeatureCard("1", "All Pets", "Browse all available pets", R.drawable.ic_pets, FeatureAction.ALL_PETS),
                FeatureCard("2", "Dogs", "Find dogs for adoption", R.drawable.ic_dog, FeatureAction.DOGS),
                FeatureCard("3", "Cats", "Find cats for adoption", R.drawable.ic_cat, FeatureAction.CATS),
                FeatureCard("4", "Other Pets", "Birds, rabbits & more", R.drawable.ic_other_pets, FeatureAction.OTHER_PETS),
                // Rescue & Shelters
                FeatureCard("5", "Rescue Request", "Report a pet in need", R.drawable.ic_rescue, FeatureAction.RESCUE),
                FeatureCard("6", "Shelters", "Find nearby shelters", R.drawable.ic_home, FeatureAction.SHELTERS),
                FeatureCard("7", "Nearby Shelters", "Shelters near you", R.drawable.ic_location, FeatureAction.NEARBY_SHELTERS),
                FeatureCard("8", "Register Shelter", "Register your shelter", R.drawable.ic_home, FeatureAction.REGISTER_SHELTER),
                // Health & Care
                FeatureCard("9", "Health Tracker", "Track pet health", R.drawable.ic_favorite, FeatureAction.HEALTH_TRACKER),
                FeatureCard("10", "Pet Identification", "Identify pet breeds", R.drawable.ic_pets, FeatureAction.PET_IDENTIFICATION),
                FeatureCard("11", "Tips", "Pet care tips", R.drawable.ic_tips, FeatureAction.TIPS),
                // User Management
                FeatureCard("12", "My Requests", "View your requests", R.drawable.ic_requests, FeatureAction.MY_REQUESTS),
                FeatureCard("13", "Profile", "Your profile", R.drawable.ic_person, FeatureAction.PROFILE),
                // Contact & Support
                FeatureCard("14", "Contact Us", "Get help & support", R.drawable.ic_contact, FeatureAction.CONTACT),
            )

        featuresAdapter =
            FeaturesAdapter(features) { feature ->
                handleFeatureClick(feature.action)
            }

        binding.rvAllFeatures.apply {
            adapter = featuresAdapter
            layoutManager = GridLayoutManager(this@AllFeaturesActivity, 3)
        }
    }

    private fun handleFeatureClick(action: FeatureAction) {
        val intent =
            when (action) {
                FeatureAction.ALL_PETS -> Intent(this, PetsListActivity::class.java)
                FeatureAction.DOGS ->
                    Intent(this, PetsListActivity::class.java).apply {
                        putExtra("category", "Dog")
                    }
                FeatureAction.CATS ->
                    Intent(this, PetsListActivity::class.java).apply {
                        putExtra("category", "Cat")
                    }
                FeatureAction.OTHER_PETS ->
                    Intent(this, PetsListActivity::class.java).apply {
                        putExtra("category", "Other")
                    }
                FeatureAction.RESCUE -> Intent(this, RescueRequestActivity::class.java)
                FeatureAction.SHELTERS -> Intent(this, SheltersListActivity::class.java)
                FeatureAction.NEARBY_SHELTERS -> Intent(this, NearbySheltersActivity::class.java)
                FeatureAction.REGISTER_SHELTER -> Intent(this, RegisterShelterActivity::class.java)
                FeatureAction.HEALTH_TRACKER -> Intent(this, PetHealthTrackerActivity::class.java)
                FeatureAction.PET_IDENTIFICATION -> Intent(this, PetIdentificationActivity::class.java)
                FeatureAction.MY_REQUESTS -> Intent(this, MyRequestsActivity::class.java)
                FeatureAction.TIPS -> Intent(this, TipsActivity::class.java)
                FeatureAction.CONTACT -> Intent(this, ContactActivity::class.java)
                FeatureAction.PROFILE -> Intent(this, ProfileActivity::class.java)
                else -> {
                    Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
