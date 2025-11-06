package com.syed.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.tabs.TabLayout
import com.syed.adapters.SheltersAdapter
import com.syed.databinding.ActivityNearbySheltersBinding
import com.syed.models.LocationData
import com.syed.models.Shelter
import com.syed.utils.FirebaseUtils
import com.syed.utils.LocationHelper
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class NearbySheltersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNearbySheltersBinding
    private lateinit var sheltersAdapter: SheltersAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val shelters = mutableListOf<Shelter>()
    private val allShelters = mutableListOf<Shelter>()
    private var userLocation: Location? = null
    private var currentRadius = 10.0 // Default 10 km radius
    private var currentFilter = "all"

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNearbySheltersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupToolbar()
        setupRecyclerView()
        setupTabs()
        setupRadiusFilter()

        checkLocationPermissionAndLoad()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "📍 Nearby Shelters"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        sheltersAdapter =
            SheltersAdapter(
                context = this,
                onShelterClick = { shelter ->
                    val intent = Intent(this, ShelterDetailsActivity::class.java)
                    intent.putExtra("shelterId", shelter.id)
                    startActivity(intent)
                },
                onDeleteClick = null,
                isAdminView = false,
            )

        binding.rvShelters.apply {
            layoutManager = LinearLayoutManager(this@NearbySheltersActivity)
            adapter = sheltersAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("All"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("NGO"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Government"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Private"))

        binding.tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    currentFilter =
                        when (tab?.position) {
                            0 -> "all"
                            1 -> "ngo"
                            2 -> "government"
                            3 -> "private"
                            else -> "all"
                        }
                    filterShelters()
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            },
        )
    }

    private fun setupRadiusFilter() {
        binding.chipRadius5km.setOnClickListener {
            currentRadius = 5.0
            updateRadiusChips()
            filterSheltersByDistance()
        }

        binding.chipRadius10km.setOnClickListener {
            currentRadius = 10.0
            updateRadiusChips()
            filterSheltersByDistance()
        }

        binding.chipRadius25km.setOnClickListener {
            currentRadius = 25.0
            updateRadiusChips()
            filterSheltersByDistance()
        }

        binding.chipRadius50km.setOnClickListener {
            currentRadius = 50.0
            updateRadiusChips()
            filterSheltersByDistance()
        }

        updateRadiusChips()
    }

    private fun updateRadiusChips() {
        binding.chipRadius5km.isChecked = currentRadius == 5.0
        binding.chipRadius10km.isChecked = currentRadius == 10.0
        binding.chipRadius25km.isChecked = currentRadius == 25.0
        binding.chipRadius50km.isChecked = currentRadius == 50.0
    }

    private fun checkLocationPermissionAndLoad() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocationAndLoadShelters()
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        ) {
            AlertDialog
                .Builder(this)
                .setTitle("Location Permission Needed")
                .setMessage("This app needs location permission to show nearby shelters. Please grant the permission.")
                .setPositiveButton("OK") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE,
                    )
                }.setNegativeButton("Cancel") { _, _ ->
                    loadAllShelters()
                }.show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE,
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndLoadShelters()
            } else {
                Toast.makeText(this, "Location permission denied. Showing all shelters.", Toast.LENGTH_LONG).show()
                loadAllShelters()
            }
        }
    }

    private fun getCurrentLocationAndLoadShelters() {
        if (!LocationHelper.hasLocationPermission(this)) {
            loadAllShelters()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.tvCurrentLocation.text = "📍 Getting your location..."

        // Use LocationHelper for better location handling
        lifecycleScope.launch {
            try {
                val locationData = LocationHelper.getCurrentLocation(this@NearbySheltersActivity)

                if (locationData != null) {
                    // Convert LocationData to Location for compatibility
                    userLocation =
                        Location("").apply {
                            latitude = locationData.latitude
                            longitude = locationData.longitude
                        }

                    binding.tvCurrentLocation.text =
                        "📍 Your Location: ${locationData.latitude.format(2)}, ${locationData.longitude.format(2)}\n" +
                        "${locationData.address.takeIf { it.isNotEmpty() } ?: "Address not available"}"

                    loadShelters()
                } else {
                    binding.tvCurrentLocation.text = "📍 Location unavailable"
                    Toast
                        .makeText(
                            this@NearbySheltersActivity,
                            "Unable to get current location. Showing all shelters.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    loadAllShelters()
                }
            } catch (e: Exception) {
                binding.tvCurrentLocation.text = "📍 Location error"
                Toast
                    .makeText(
                        this@NearbySheltersActivity,
                        "Error getting location: ${e.message}",
                        Toast.LENGTH_SHORT,
                    ).show()
                loadAllShelters()
            }
        }
    }

    private fun loadShelters() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        FirebaseUtils.firestore
            .collection("shelters")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { documents ->
                allShelters.clear()
                for (doc in documents) {
                    try {
                        val shelter = doc.toObject(Shelter::class.java).copy(id = doc.id)
                        allShelters.add(shelter)
                    } catch (e: Exception) {
                        android.util.Log.e("NearbyShelters", "Error parsing shelter", e)
                    }
                }

                filterSheltersByDistance()
                binding.progressBar.visibility = View.GONE
            }.addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error loading shelters: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
    }

    private fun loadAllShelters() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE
        binding.tvCurrentLocation.text = "📍 Location not available - Showing all shelters"

        FirebaseUtils.firestore
            .collection("shelters")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { documents ->
                allShelters.clear()
                for (doc in documents) {
                    try {
                        val shelter = doc.toObject(Shelter::class.java).copy(id = doc.id)
                        allShelters.add(shelter)
                    } catch (e: Exception) {
                        android.util.Log.e("NearbyShelters", "Error parsing shelter", e)
                    }
                }

                shelters.clear()
                shelters.addAll(allShelters)
                filterShelters()
                binding.progressBar.visibility = View.GONE
            }.addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error loading shelters: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
    }

    private fun filterSheltersByDistance() {
        if (userLocation == null) {
            shelters.clear()
            shelters.addAll(allShelters)
            filterShelters()
            return
        }

        val currentLocationData =
            LocationData(
                latitude = userLocation!!.latitude,
                longitude = userLocation!!.longitude,
            )

        // Use LocationHelper for distance calculation
        val sheltersWithDistance =
            allShelters.mapNotNull { shelter ->
                shelter.location?.let { geoPoint ->
                    val shelterLocation =
                        LocationData(
                            latitude = geoPoint.latitude,
                            longitude = geoPoint.longitude,
                        )
                    val distance = currentLocationData.distanceToInKm(shelterLocation)
                    Pair(shelter, distance)
                }
            }

        // Filter by radius and sort by distance
        shelters.clear()
        shelters.addAll(
            sheltersWithDistance
                .filter { it.second <= currentRadius }
                .sortedBy { it.second }
                .map { it.first },
        )

        filterShelters()

        val shelterCount = shelters.size
        val locationText =
            binding.tvCurrentLocation.text
                .toString()
                .split("•")[0]
                .trim()
        binding.tvCurrentLocation.text = "$locationText • Found $shelterCount shelter(s) within ${currentRadius.toInt()} km"
    }

    private fun filterShelters() {
        val filteredShelters =
            if (currentFilter == "all") {
                shelters
            } else {
                shelters.filter { it.type.equals(currentFilter, ignoreCase = true) }
            }

        sheltersAdapter.updateShelters(filteredShelters)

        if (filteredShelters.isEmpty()) {
            showEmptyState()
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvShelters.visibility = View.VISIBLE
        }
    }

    private fun showEmptyState() {
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.rvShelters.visibility = View.GONE
        binding.tvEmptyState.text =
            "No shelters found within ${currentRadius.toInt()} km\n\nTry increasing the search radius or check back later."
    }

    // Haversine formula to calculate distance between two points
    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val earthRadius = 6371.0 // Radius in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a =
            sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}
