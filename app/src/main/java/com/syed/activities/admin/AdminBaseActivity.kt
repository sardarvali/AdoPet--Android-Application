package com.syed.activities.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.syed.MainActivity
import com.syed.utils.FirebaseUtils

abstract class AdminBaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verifyAdminAccess()
    }

    private fun verifyAdminAccess() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            redirectToLogin()
            return
        }

        // Check if user is admin with better error handling
        FirebaseUtils.firestore
            .collection(FirebaseUtils.USERS_COLLECTION)
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val isAdmin = document.getBoolean("isAdmin") ?: false
                    val admin = document.get("admin") // Check alternate field name

                    android.util.Log.d("AdminBaseActivity", "User document exists. isAdmin=$isAdmin, admin=$admin")
                    android.util.Log.d("AdminBaseActivity", "All fields: ${document.data}")

                    // Check both possible field names
                    if (isAdmin || admin == true) {
                        onAdminVerified()
                    } else {
                        android.util.Log.w("AdminBaseActivity", "User ${currentUser.email} is not an admin")
                        showUnauthorizedAccess()
                    }
                } else {
                    android.util.Log.w("AdminBaseActivity", "User document does not exist for ${currentUser.uid}")
                    showUnauthorizedAccess()
                }
            }.addOnFailureListener { exception ->
                android.util.Log.e("AdminBaseActivity", "Error checking admin status for ${currentUser.email}", exception)
                showUnauthorizedAccess()
            }
    }

    private fun redirectToLogin() {
        Toast.makeText(this, "Please login to access admin features", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showUnauthorizedAccess() {
        Toast.makeText(this, "Unauthorized access. Admin privileges required.", Toast.LENGTH_LONG).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    /**
     * Called when admin access is verified
     * Subclasses should override this to initialize their UI
     */
    abstract fun onAdminVerified()

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
