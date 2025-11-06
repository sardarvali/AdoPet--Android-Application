package com.syed.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.syed.models.User
import kotlinx.coroutines.tasks.await

object FirebaseUtils {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    val storage: FirebaseStorage = FirebaseStorage.getInstance()

    // Collections
    const val USERS_COLLECTION = "users"
    const val PETS_COLLECTION = "pets"
    const val ADOPTION_REQUESTS_COLLECTION = "adoption_requests"
    const val RESCUE_REQUESTS_COLLECTION = "rescue_requests"
    const val CONTACT_MESSAGES_COLLECTION = "contact_messages"
    const val TIPS_COLLECTION = "tips"
    const val OFFICE_DETAILS_COLLECTION = "office_details"
    const val SUCCESS_STORIES_COLLECTION = "success_stories"

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Check if current user is admin (Callback version)
     * Checks both 'isAdmin' and 'admin' field names for compatibility
     */
    fun isCurrentUserAdmin(callback: (Boolean) -> Unit) {
        val currentUser = auth.currentUser

        currentUser?.uid?.let { uid ->
            firestore
                .collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val isAdmin = document.getBoolean("isAdmin") ?: false
                        val admin = document.getBoolean("admin") ?: false

                        android.util.Log.d("FirebaseUtils", "isAdmin check: isAdmin=$isAdmin, admin=$admin")

                        callback(isAdmin || admin)
                    } else {
                        android.util.Log.w("FirebaseUtils", "User document does not exist")
                        callback(false)
                    }
                }.addOnFailureListener { e ->
                    android.util.Log.e("FirebaseUtils", "Error checking admin status", e)
                    callback(false)
                }
        } ?: callback(false)
    }

    /**
     * Check if current user is admin (Suspend version for coroutines)
     * Checks both 'isAdmin' and 'admin' field names for compatibility
     */
    suspend fun isCurrentUserAdminSuspend(): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false

            // Check in Firestore only
            val document =
                firestore
                    .collection(USERS_COLLECTION)
                    .document(currentUser.uid)
                    .get()
                    .await()

            val isAdmin = document.getBoolean("isAdmin") ?: false
            val admin = document.getBoolean("admin") ?: false

            android.util.Log.d("FirebaseUtils", "isAdminSuspend check: isAdmin=$isAdmin, admin=$admin")

            isAdmin || admin
        } catch (e: Exception) {
            SecureLogger.e("FirebaseUtils", "Error checking admin status", e)
            false
        }
    }

    /**
     * Check if a specific user is admin by user ID
     */
    suspend fun isUserAdmin(userId: String): Boolean =
        try {
            val document =
                firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .get()
                    .await()

            document.getBoolean("isAdmin") ?: false
        } catch (e: Exception) {
            SecureLogger.e("FirebaseUtils", "Error checking admin status for user", e)
            false
        }

    fun createOrUpdateUserDocument(callback: ((Boolean) -> Unit)? = null) {
        val currentUser = auth.currentUser ?: return

        // First, check if user document already exists to preserve admin status
        firestore
            .collection(USERS_COLLECTION)
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                val existingIsAdmin = document.getBoolean("isAdmin") ?: false
                val existingAdmin = document.getBoolean("admin") ?: false

                val user =
                    User(
                        uid = currentUser.uid,
                        email = currentUser.email ?: "",
                        name = currentUser.displayName ?: document.getString("name") ?: "",
                        isAdmin = existingIsAdmin || existingAdmin, // Preserve existing admin status
                        isActive = true,
                        joinedDate = document.getLong("joinedDate") ?: System.currentTimeMillis(),
                    )

                // Use merge to preserve other fields including admin status
                firestore
                    .collection(USERS_COLLECTION)
                    .document(currentUser.uid)
                    .set(
                        user,
                        com.google.firebase.firestore.SetOptions
                            .merge(),
                    ).addOnSuccessListener {
                        callback?.invoke(true)
                    }.addOnFailureListener {
                        callback?.invoke(false)
                    }
            }.addOnFailureListener {
                callback?.invoke(false)
            }
    }

    /**
     * Create or update user document (Suspend version)
     */
    suspend fun createOrUpdateUserDocumentSuspend(): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false

            // First check if user document exists to preserve admin status
            val document =
                firestore
                    .collection(USERS_COLLECTION)
                    .document(currentUser.uid)
                    .get()
                    .await()

            val existingIsAdmin = document.getBoolean("isAdmin") ?: false
            val existingAdmin = document.getBoolean("admin") ?: false

            val user =
                User(
                    uid = currentUser.uid,
                    email = currentUser.email ?: "",
                    name = currentUser.displayName ?: document.getString("name") ?: "",
                    isAdmin = existingIsAdmin || existingAdmin, // Preserve admin status
                    isActive = true,
                    joinedDate = document.getLong("joinedDate") ?: System.currentTimeMillis(),
                )

            // Use merge to preserve other fields including admin status
            firestore
                .collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .set(
                    user,
                    com.google.firebase.firestore.SetOptions
                        .merge(),
                ).await()

            true
        } catch (e: Exception) {
            SecureLogger.e("FirebaseUtils", "Error creating/updating user document", e)
            false
        }
    }

    /**
     * Get current user email
     */
    fun getCurrentUserEmail(): String? = auth.currentUser?.email

    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    /**
     * Sign out current user
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Delete user account and all associated data
     */
    suspend fun deleteUserAccount(): Boolean {
        return try {
            val userId = getCurrentUserId() ?: return false

            // Delete adoption requests
            val adoptionRequests =
                firestore
                    .collection(ADOPTION_REQUESTS_COLLECTION)
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

            adoptionRequests.documents.forEach { it.reference.delete().await() }

            // Delete rescue requests
            val rescueRequests =
                firestore
                    .collection(RESCUE_REQUESTS_COLLECTION)
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

            rescueRequests.documents.forEach { it.reference.delete().await() }

            // Delete contact messages
            val contactMessages =
                firestore
                    .collection(CONTACT_MESSAGES_COLLECTION)
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

            contactMessages.documents.forEach { it.reference.delete().await() }

            // Delete user document
            firestore
                .collection(USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()

            // Delete Firebase Auth account
            auth.currentUser?.delete()?.await()

            true
        } catch (e: Exception) {
            SecureLogger.e("FirebaseUtils", "Error deleting user account", e)
            false
        }
    }
}
