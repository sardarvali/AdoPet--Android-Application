package com.syed.activities.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.syed.adapters.AdminUsersAdapter
import com.syed.databinding.ActivityAddAdminBinding
import com.syed.models.User
import com.syed.utils.FirebaseUtils

class AddAdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddAdminBinding
    private lateinit var adminAdapter: AdminUsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check admin privileges first
        if (!isUserAdmin()) {
            Toast.makeText(this, "Access denied. Admin privileges required.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding = ActivityAddAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadAdminUsers()
    }

    private fun isUserAdmin(): Boolean {
        val currentUser = FirebaseUtils.auth.currentUser
        return currentUser?.email == "forwork.syed@gmail.com"
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Admins"
    }

    private fun setupRecyclerView() {
        adminAdapter =
            AdminUsersAdapter(
                onRemoveAdminClick = { user -> confirmRemoveAdmin(user) },
            )

        binding.rvAdmins.apply {
            adapter = adminAdapter
            layoutManager = LinearLayoutManager(this@AddAdminActivity)
        }
    }

    private fun setupClickListeners() {
        binding.btnAddAdmin.setOnClickListener {
            addAdmin()
        }
    }

    private fun loadAdminUsers() {
        binding.progressBar?.visibility = View.VISIBLE

        FirebaseUtils.firestore
            .collection(FirebaseUtils.USERS_COLLECTION)
            .whereEqualTo("isAdmin", true)
            .get()
            .addOnSuccessListener { documents ->
                val adminList =
                    documents.mapNotNull { doc ->
                        doc.toObject(User::class.java).copy(uid = doc.id)
                    }
                adminAdapter.updateAdmins(adminList)
                binding.progressBar?.visibility = View.GONE

                binding.tvEmptyState?.visibility = if (adminList.isEmpty()) View.VISIBLE else View.GONE
            }.addOnFailureListener { e ->
                binding.progressBar?.visibility = View.GONE
                Toast.makeText(this, "Failed to load admin users: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addAdmin() {
        val email =
            binding.etAdminEmail.text
                .toString()
                .trim()

        if (email.isEmpty()) {
            binding.etAdminEmail.error = "Email is required"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()
        ) {
            binding.etAdminEmail.error = "Invalid email format"
            return
        }

        binding.btnAddAdmin.isEnabled = false
        binding.progressBar?.visibility = View.VISIBLE

        // First check if user exists
        FirebaseUtils.firestore
            .collection(FirebaseUtils.USERS_COLLECTION)
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // User doesn't exist, show error
                    Toast.makeText(this, "User with email $email not found. They must register first.", Toast.LENGTH_LONG).show()
                } else {
                    // User exists, make them admin
                    val userDoc = documents.first()
                    val user = userDoc.toObject(User::class.java)

                    if (user.isAdmin) {
                        Toast.makeText(this, "User is already an admin", Toast.LENGTH_SHORT).show()
                    } else {
                        makeUserAdmin(userDoc.id, user)
                    }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error searching for user: ${e.message}", Toast.LENGTH_SHORT).show()
            }.addOnCompleteListener {
                binding.btnAddAdmin.isEnabled = true
                binding.progressBar?.visibility = View.GONE
            }
    }

    private fun makeUserAdmin(
        userId: String,
        user: User,
    ) {
        val updatedUser =
            user.copy(
                isAdmin = true,
                isActive = true,
            )

        FirebaseUtils.firestore
            .collection(FirebaseUtils.USERS_COLLECTION)
            .document(userId)
            .set(updatedUser)
            .addOnSuccessListener {
                Toast.makeText(this, "User promoted to admin successfully", Toast.LENGTH_SHORT).show()
                binding.etAdminEmail.setText("")
                loadAdminUsers() // Refresh the list
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to promote user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmRemoveAdmin(user: User) {
        // Prevent removing the main admin
        if (user.email == "forwork.syed@gmail.com") {
            Toast.makeText(this, "Cannot remove the main admin", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog
            .Builder(this)
            .setTitle("Remove Admin")
            .setMessage("Are you sure you want to remove admin privileges from ${user.name ?: user.email}?")
            .setPositiveButton("Remove") { _, _ ->
                removeAdmin(user)
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeAdmin(user: User) {
        val updatedUser = user.copy(isAdmin = false)

        FirebaseUtils.firestore
            .collection(FirebaseUtils.USERS_COLLECTION)
            .document(user.uid)
            .set(updatedUser)
            .addOnSuccessListener {
                Toast.makeText(this, "Admin privileges removed successfully", Toast.LENGTH_SHORT).show()
                loadAdminUsers() // Refresh the list
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to remove admin: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
