package com.syed.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Helper class for Google Places API operations
 * Integrated with App Check for enhanced security
 *
 * Features:
 * - Location autocomplete
 * - Place details fetching
 * - Place search
 * - Reverse geocoding
 * - Protected by App Check
 */
object PlacesHelper {
    private const val TAG = "PlacesHelper"

    /**
     * Get Places client instance
     *
     * @param context Application or Activity context
     * @return PlacesClient for making API calls
     */
    fun getPlacesClient(context: Context): PlacesClient = Places.createClient(context)

    /**
     * Search for places using autocomplete
     *
     * @param context Application context
     * @param query Search query
     * @param sessionToken Session token for billing optimization
     * @return List of autocomplete predictions
     */
    suspend fun findPlaceAutocomplete(
        context: Context,
        query: String,
        sessionToken: AutocompleteSessionToken? = null,
    ): List<com.google.android.libraries.places.api.model.AutocompletePrediction> =
        suspendCancellableCoroutine { continuation ->
            try {
                val client = getPlacesClient(context)

                // Create autocomplete request
                val request =
                    FindAutocompletePredictionsRequest
                        .builder()
                        .setQuery(query)
                        .apply {
                            sessionToken?.let { setSessionToken(it) }
                        }.build()

                // Execute request
                client
                    .findAutocompletePredictions(request)
                    .addOnSuccessListener { response ->
                        Log.d(TAG, "✅ Autocomplete success: ${response.autocompletePredictions.size} results")
                        continuation.resume(response.autocompletePredictions)
                    }.addOnFailureListener { exception ->
                        Log.e(TAG, "❌ Autocomplete failed", exception)
                        continuation.resumeWithException(exception)
                    }

                continuation.invokeOnCancellation {
                    // Cleanup if coroutine is cancelled
                    Log.d(TAG, "Autocomplete request cancelled")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in findPlaceAutocomplete", e)
                continuation.resumeWithException(e)
            }
        }

    /**
     * Fetch place details by place ID
     *
     * @param context Application context
     * @param placeId Google Place ID
     * @param placeFields Fields to fetch (default: name, address, coordinates)
     * @return Place object with requested details
     */
    suspend fun fetchPlaceDetails(
        context: Context,
        placeId: String,
        placeFields: List<Place.Field> =
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.PHONE_NUMBER,
                Place.Field.WEBSITE_URI,
            ),
    ): Place =
        suspendCancellableCoroutine { continuation ->
            try {
                val client = getPlacesClient(context)

                // Create fetch place request
                val request = FetchPlaceRequest.builder(placeId, placeFields).build()

                // Execute request
                client
                    .fetchPlace(request)
                    .addOnSuccessListener { response ->
                        Log.d(TAG, "✅ Place details fetched: ${response.place.name}")
                        continuation.resume(response.place)
                    }.addOnFailureListener { exception ->
                        Log.e(TAG, "❌ Fetch place details failed", exception)
                        continuation.resumeWithException(exception)
                    }

                continuation.invokeOnCancellation {
                    Log.d(TAG, "Fetch place details cancelled")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in fetchPlaceDetails", e)
                continuation.resumeWithException(e)
            }
        }

    /**
     * Create a new autocomplete session token
     * Use this to group multiple autocomplete requests together for billing optimization
     *
     * @return AutocompleteSessionToken
     */
    fun createSessionToken(): AutocompleteSessionToken = AutocompleteSessionToken.newInstance()

    /**
     * Check if Places SDK is initialized
     *
     * @return true if initialized, false otherwise
     */
    fun isInitialized(): Boolean = Places.isInitialized()
}
