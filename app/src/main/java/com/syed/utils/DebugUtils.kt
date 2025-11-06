package com.syed.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object DebugUtils {
    private const val TAG = "DebugUtils"

    /**
     * Logs the current Firebase Authentication status
     */
    fun logFirebaseAuthStatus() {
        try {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser

            if (currentUser != null) {
                Log.d(TAG, "Firebase Auth Status: User is signed in")
                Log.d(TAG, "User ID: ${currentUser.uid}")
                Log.d(TAG, "User Email: ${currentUser.email}")
                Log.d(TAG, "User Display Name: ${currentUser.displayName}")
                Log.d(TAG, "User Email Verified: ${currentUser.isEmailVerified}")
            } else {
                Log.d(TAG, "Firebase Auth Status: No user signed in")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Firebase Auth status: ${e.message}", e)
        }
    }

    /**
     * Tests Firestore permissions by attempting basic operations
     */
    fun testFirestorePermissions() {
        try {
            val db = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()

            Log.d(TAG, "Testing Firestore Permissions...")

            // Test reading from pets collection
            db
                .collection("pets")
                .limit(1)
                .get()
                .addOnSuccessListener { documents ->
                    Log.d(TAG, "✓ Firestore READ permission: Success (${documents.size()} documents)")
                }.addOnFailureListener { exception ->
                    Log.e(TAG, "✗ Firestore READ permission: Failed - ${exception.message}")
                }

            // Test write permissions (only if user is authenticated)
            if (auth.currentUser != null) {
                val testDoc = db.collection("debug_test").document("test")
                testDoc
                    .set(mapOf("timestamp" to System.currentTimeMillis()))
                    .addOnSuccessListener {
                        Log.d(TAG, "✓ Firestore WRITE permission: Success")
                        // Clean up test document
                        testDoc.delete()
                    }.addOnFailureListener { exception ->
                        Log.e(TAG, "✗ Firestore WRITE permission: Failed - ${exception.message}")
                    }
            } else {
                Log.d(TAG, "⚠ Firestore WRITE permission: Skipped (no authenticated user)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error testing Firestore permissions: ${e.message}", e)
        }
    }
}
