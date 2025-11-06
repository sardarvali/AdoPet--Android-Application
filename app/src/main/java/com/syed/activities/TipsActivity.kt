package com.syed.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.syed.adapters.TipsAdapter
import com.syed.databinding.ActivityTipsBinding
import com.syed.models.Tip
import com.syed.utils.FirebaseUtils

class TipsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTipsBinding
    private lateinit var tipsAdapter: TipsAdapter
    private var isAdminMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTipsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if opened in admin mode
        isAdminMode = intent.getBooleanExtra("isAdminMode", false)

        setupToolbar()
        setupRecyclerView()
        setupAdminControls()
        loadTips()
        setupSwipeRefresh()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isAdminMode) "Manage Tips" else "Pet Care Tips"
    }

    private fun setupAdminControls() {
        if (isAdminMode) {
            // Show add tip button for admin
            binding.fabAddTip?.visibility = View.VISIBLE
            binding.fabAddTip?.setOnClickListener {
                startActivity(android.content.Intent(this, com.syed.activities.admin.AddEditTipActivity::class.java))
            }
        } else {
            binding.fabAddTip?.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        tipsAdapter =
            TipsAdapter(
                tips = mutableListOf(),
                onItemClick = { tip ->
                    // Handle tip click if needed - could show detailed tip view
                },
                isAdminMode = isAdminMode,
                onEditClick = { tip ->
                    if (isAdminMode) {
                        startActivity(
                            android.content.Intent(this, com.syed.activities.admin.AddEditTipActivity::class.java).apply {
                                putExtra("tipId", tip.id)
                            },
                        )
                    }
                },
                onDeleteClick = { tip ->
                    if (isAdminMode) {
                        deleteTip(tip)
                    }
                },
            )

        binding.rvTips.apply {
            adapter = tipsAdapter
            layoutManager = LinearLayoutManager(this@TipsActivity)
        }
    }

    private fun loadTips() {
        binding.progressBar.visibility = View.VISIBLE

        // Use snapshot listener for real-time updates
        FirebaseUtils.firestore
            .collection(FirebaseUtils.TIPS_COLLECTION)
            .whereEqualTo("isPublished", true)
            .orderBy("dateAdded", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE

                if (error != null) {
                    android.util.Log.e("TipsActivity", "Error loading tips", error)
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyState.text = "Failed to load tips"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val tips =
                        snapshot.mapNotNull { doc ->
                            try {
                                doc.toObject(Tip::class.java).copy(id = doc.id)
                            } catch (e: Exception) {
                                android.util.Log.e("TipsActivity", "Error parsing tip", e)
                                null
                            }
                        }
                    tipsAdapter.updateTips(tips)
                    binding.tvEmptyState.visibility = if (tips.isEmpty()) View.VISIBLE else View.GONE
                    if (tips.isEmpty()) {
                        binding.tvEmptyState.text = "No tips available yet"
                    }
                }
            }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadTips()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun deleteTip(tip: Tip) {
        androidx.appcompat.app.AlertDialog
            .Builder(this)
            .setTitle("Delete Tip")
            .setMessage("Are you sure you want to delete this tip?")
            .setPositiveButton("Delete") { _, _ ->
                FirebaseUtils.firestore
                    .collection(FirebaseUtils.TIPS_COLLECTION)
                    .document(tip.id)
                    .delete()
                    .addOnSuccessListener {
                        android.widget.Toast
                            .makeText(this, "Tip deleted successfully", android.widget.Toast.LENGTH_SHORT)
                            .show()
                        loadTips() // Refresh the list
                    }.addOnFailureListener { e ->
                        android.widget.Toast
                            .makeText(this, "Failed to delete tip: ${e.message}", android.widget.Toast.LENGTH_SHORT)
                            .show()
                    }
            }.setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
