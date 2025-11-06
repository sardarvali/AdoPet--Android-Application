package com.syed.models

import com.google.firebase.firestore.GeoPoint

/**
 * Location data class for storing coordinates
 */
data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "",
    val postalCode: String = "",
) {
    fun toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)

    fun isValid(): Boolean = latitude != 0.0 && longitude != 0.0

    fun distanceTo(other: LocationData): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            latitude,
            longitude,
            other.latitude,
            other.longitude,
            results,
        )
        return results[0] // Distance in meters
    }

    fun distanceToInKm(other: LocationData): Float = distanceTo(other) / 1000f

    companion object {
        fun fromGeoPoint(geoPoint: GeoPoint?): LocationData =
            if (geoPoint != null) {
                LocationData(
                    latitude = geoPoint.latitude,
                    longitude = geoPoint.longitude,
                )
            } else {
                LocationData()
            }
    }
}
