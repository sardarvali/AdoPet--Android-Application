package com.syed.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.syed.R
import com.syed.databinding.ActivityMapLocationPickerBinding
import com.syed.models.LocationData
import com.syed.security.SecureApiKeyProvider
import com.syed.utils.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Map Location Picker Activity
 * Allows users to select a location on Google Maps
 */
class MapLocationPickerActivity :
    AppCompatActivity(),
    OnMapReadyCallback {
    private lateinit var binding: ActivityMapLocationPickerBinding
    private var googleMap: GoogleMap? = null
    private var selectedLocation: LocationData? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    companion object {
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_CITY = "city"
        const val EXTRA_STATE = "state"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapLocationPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        initializeMap()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Select Location"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupClickListeners() {
        binding.btnConfirmLocation.setOnClickListener {
            confirmLocation()
        }

        binding.btnMyLocation.setOnClickListener {
            getCurrentLocation()
        }
    }

    private fun initializeMap() {
        lifecycleScope.launch {
            try {
                // Check if we have valid Google Play Services
                try {
                    val mapFragment =
                        supportFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
                    mapFragment?.getMapAsync(this@MapLocationPickerActivity)
                } catch (e: Exception) {
                    android.util.Log.e("MapLocationPicker", "Failed to initialize map", e)

                    // Show detailed error message
                    withContext(Dispatchers.Main) {
                        androidx.appcompat.app.AlertDialog
                            .Builder(this@MapLocationPickerActivity)
                            .setTitle("Google Maps Not Available")
                            .setMessage(
                                "Unable to load Google Maps.\n\n" +
                                    "Possible reasons:\n" +
                                    "1. Invalid or missing Google Maps API Key\n" +
                                    "2. Google Play Services not installed\n" +
                                    "3. Maps SDK for Android not enabled\n\n" +
                                    "Error: ${e.message}",
                            ).setPositiveButton("OK") { _, _ -> finish() }
                            .setCancelable(false)
                            .show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MapLocationPicker", "Error in initializeMap", e)
                withContext(Dispatchers.Main) {
                    Toast
                        .makeText(
                            this@MapLocationPickerActivity,
                            "Error loading map: ${e.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                    finish()
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Set default location (India center or last selected location)
        val defaultLocation =
            intent.let {
                val lat = it.getDoubleExtra(EXTRA_LATITUDE, 20.5937)
                val lng = it.getDoubleExtra(EXTRA_LONGITUDE, 78.9629)
                LatLng(lat, lng)
            }

        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))

        // Enable location if permission granted
        enableMyLocation()

        // Set up map click listener
        googleMap?.setOnMapClickListener { latLng ->
            onLocationSelected(latLng)
        }

        // Set up map long click listener for more precise selection
        googleMap?.setOnMapLongClickListener { latLng ->
            onLocationSelected(latLng)
        }

        // If starting location provided, select it
        if (intent.hasExtra(EXTRA_LATITUDE)) {
            onLocationSelected(defaultLocation)
        }
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE,
            )
        }
    }

    private fun getCurrentLocation() {
        if (!LocationHelper.hasLocationPermission(this)) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
                LOCATION_PERMISSION_REQUEST_CODE,
            )
            return
        }

        binding.btnMyLocation.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            try {
                val location = LocationHelper.getCurrentLocation(this@MapLocationPickerActivity)
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    onLocationSelected(latLng)
                } else {
                    Toast
                        .makeText(this@MapLocationPickerActivity, "Unable to get current location", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                Toast
                    .makeText(this@MapLocationPickerActivity, "Error getting location: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            } finally {
                binding.btnMyLocation.isEnabled = true
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun onLocationSelected(latLng: LatLng) {
        // Clear previous markers
        googleMap?.clear()

        // Add marker at selected location
        googleMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Selected Location"),
        )

        // Reverse geocode to get address
        lifecycleScope.launch {
            val locationData = LocationHelper.reverseGeocode(this@MapLocationPickerActivity, latLng.latitude, latLng.longitude)
            selectedLocation = locationData
            updateLocationInfo(locationData)
        }
    }

    private fun updateLocationInfo(location: LocationData?) {
        if (location != null) {
            binding.tvSelectedAddress.text = location.address.ifEmpty { "Address not available" }
            binding.tvSelectedCoordinates.text = "Lat: ${String.format("%.6f", location.latitude)}, " +
                "Lng: ${String.format("%.6f", location.longitude)}"
            binding.btnConfirmLocation.isEnabled = true
        } else {
            binding.tvSelectedAddress.text = "Fetching address..."
            binding.btnConfirmLocation.isEnabled = false
        }
    }

    private fun confirmLocation() {
        val location = selectedLocation
        if (location != null) {
            val resultIntent =
                Intent().apply {
                    putExtra(EXTRA_LATITUDE, location.latitude)
                    putExtra(EXTRA_LONGITUDE, location.longitude)
                    putExtra(EXTRA_ADDRESS, location.address)
                    putExtra(EXTRA_CITY, location.city)
                    putExtra(EXTRA_STATE, location.state)
                }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } else {
            Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show()
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
                enableMyLocation()
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
