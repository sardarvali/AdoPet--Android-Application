package com.syed

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.syed.utils.NotificationUtils

class PetAdoptionApplication : Application() {
    private lateinit var connectivityManager: ConnectivityManager
    private var isOnline = false

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Firebase App Check with improved error handling
        initializeAppCheck()

        // Initialize Places SDK with App Check
        initializePlacesSDK()

        // Initialize notification channels
        NotificationUtils.createNotificationChannels(this)

        // Initialize connectivity monitoring
        initializeConnectivityMonitoring()

        // Subscribe to admin notifications if user is admin
        NotificationUtils.subscribeToAdminTopic()
    }

    private fun initializeAppCheck() {
        try {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()

            // Use debug provider in debug builds to avoid rate limiting during development
            if (com.syed.BuildConfig.DEBUG) {
                android.util.Log.d("PetAdoptionApp", "Using Debug App Check Provider for development")
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance(),
                )
            } else {
                android.util.Log.d("PetAdoptionApp", "Using Play Integrity App Check Provider for production")
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance(),
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("PetAdoptionApp", "Failed to initialize App Check", e)
            // Fallback to debug provider if Play Integrity fails
            try {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance(),
                )
            } catch (fallbackException: Exception) {
                android.util.Log.e("PetAdoptionApp", "Failed to initialize fallback App Check", fallbackException)
            }
        }
    }

    /**
     * Initialize Places SDK with API key
     * App Check is already protecting Firebase services
     */
    private fun initializePlacesSDK() {
        try {
            // For now, we'll use the placeholder since BuildConfig field might not be generated yet
            // The main Maps SDK will use the API key from manifest
            val apiKey =
                try {
                    BuildConfig.GOOGLE_MAPS_API_KEY
                } catch (e: Exception) {
                    "YOUR_ACTUAL_GOOGLE_MAPS_API_KEY"
                }

            if (apiKey.isBlank() || apiKey == "YOUR_ACTUAL_GOOGLE_MAPS_API_KEY") {
                android.util.Log.w(
                    "PetAdoptionApp",
                    "⚠️ Google Maps API key not configured in gradle.properties. Places SDK features will be limited.",
                )
                return
            }

            // Initialize Places SDK with new API
            if (!Places.isInitialized()) {
                Places.initializeWithNewPlacesApiEnabled(applicationContext, apiKey)
                android.util.Log.d("PetAdoptionApp", "✅ Places SDK initialized with API key")
            }

            // Note: App Check token provider for Places SDK requires Places SDK 3.4.0+
            // For now, Firebase services (Firestore, Storage) are protected by App Check
            // Places SDK requests will use the API key with standard Google Cloud security
            android.util.Log.d("PetAdoptionApp", "✅ Places SDK ready (API key from gradle.properties)")
        } catch (e: Exception) {
            android.util.Log.e("PetAdoptionApp", "❌ Failed to initialize Places SDK", e)
        }
    }

    private fun initializeConnectivityMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest =
            NetworkRequest
                .Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()

        val networkCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    if (!isOnline) {
                        isOnline = true
                        android.util.Log.d("PetAdoptionApp", "Network connection restored")
                    }
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    isOnline = false
                    android.util.Log.d("PetAdoptionApp", "Network connection lost")
                }
            }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
