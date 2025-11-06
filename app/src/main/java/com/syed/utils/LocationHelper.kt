package com.syed.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.syed.models.LocationData
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Location Helper Utility
 * Handles location fetching and geocoding
 */
object LocationHelper {
    private var fusedLocationClient: FusedLocationProviderClient? = null

    fun initialize(context: Context) {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        }
    }

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(context: Context): Boolean =
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Get current location
     */
    suspend fun getCurrentLocation(context: Context): LocationData? {
        if (!hasLocationPermission(context)) {
            android.util.Log.w("LocationHelper", "Location permission not granted")
            return null
        }

        initialize(context)

        return try {
            val cancellationTokenSource = CancellationTokenSource()

            @Suppress("MissingPermission")
            val location =
                fusedLocationClient
                    ?.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token,
                    )?.await()

            if (location != null) {
                // Reverse geocode to get address
                val addressData = reverseGeocode(context, location.latitude, location.longitude)
                LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = addressData?.address ?: "",
                    city = addressData?.city ?: "",
                    state = addressData?.state ?: "",
                    country = addressData?.country ?: "",
                    postalCode = addressData?.postalCode ?: "",
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationHelper", "Failed to get current location", e)
            null
        }
    }

    /**
     * Get last known location (faster but may be outdated)
     */
    suspend fun getLastKnownLocation(context: Context): LocationData? {
        if (!hasLocationPermission(context)) {
            return null
        }

        initialize(context)

        return try {
            @Suppress("MissingPermission")
            val location = fusedLocationClient?.lastLocation?.await()

            if (location != null) {
                val addressData = reverseGeocode(context, location.latitude, location.longitude)
                LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = addressData?.address ?: "",
                    city = addressData?.city ?: "",
                    state = addressData?.state ?: "",
                    country = addressData?.country ?: "",
                    postalCode = addressData?.postalCode ?: "",
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationHelper", "Failed to get last known location", e)
            null
        }
    }

    /**
     * Reverse geocode: Convert coordinates to address
     */
    suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double,
    ): LocationData? =
        suspendCancellableCoroutine { continuation ->
            try {
                if (!Geocoder.isPresent()) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                val geocoder = Geocoder(context, Locale.getDefault())

                @Suppress("DEPRECATION")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                    ) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val address = addresses[0]
                            continuation.resume(
                                LocationData(
                                    latitude = latitude,
                                    longitude = longitude,
                                    address = address.getAddressLine(0) ?: "",
                                    city = address.locality ?: "",
                                    state = address.adminArea ?: "",
                                    country = address.countryName ?: "",
                                    postalCode = address.postalCode ?: "",
                                ),
                            )
                        } else {
                            continuation.resume(null)
                        }
                    }
                } else {
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        continuation.resume(
                            LocationData(
                                latitude = latitude,
                                longitude = longitude,
                                address = address.getAddressLine(0) ?: "",
                                city = address.locality ?: "",
                                state = address.adminArea ?: "",
                                country = address.countryName ?: "",
                                postalCode = address.postalCode ?: "",
                            ),
                        )
                    } else {
                        continuation.resume(null)
                    }
                }
            } catch (e: IOException) {
                android.util.Log.e("LocationHelper", "Geocoding failed", e)
                continuation.resume(null)
            }
        }

    /**
     * Forward geocode: Convert address to coordinates
     */
    suspend fun forwardGeocode(
        context: Context,
        addressString: String,
    ): LocationData? =
        suspendCancellableCoroutine { continuation ->
            try {
                if (!Geocoder.isPresent()) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                val geocoder = Geocoder(context, Locale.getDefault())

                @Suppress("DEPRECATION")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocationName(addressString, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val address = addresses[0]
                            continuation.resume(
                                LocationData(
                                    latitude = address.latitude,
                                    longitude = address.longitude,
                                    address = address.getAddressLine(0) ?: "",
                                    city = address.locality ?: "",
                                    state = address.adminArea ?: "",
                                    country = address.countryName ?: "",
                                    postalCode = address.postalCode ?: "",
                                ),
                            )
                        } else {
                            continuation.resume(null)
                        }
                    }
                } else {
                    val addresses = geocoder.getFromLocationName(addressString, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        continuation.resume(
                            LocationData(
                                latitude = address.latitude,
                                longitude = address.longitude,
                                address = address.getAddressLine(0) ?: "",
                                city = address.locality ?: "",
                                state = address.adminArea ?: "",
                                country = address.countryName ?: "",
                                postalCode = address.postalCode ?: "",
                            ),
                        )
                    } else {
                        continuation.resume(null)
                    }
                }
            } catch (e: IOException) {
                android.util.Log.e("LocationHelper", "Forward geocoding failed", e)
                continuation.resume(null)
            }
        }

    /**
     * Calculate distance between two locations in kilometers
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000f // Convert to km
    }
}
