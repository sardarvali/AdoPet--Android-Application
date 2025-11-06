package com.syed.activities.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.syed.adapters.TipsAdapter
import com.syed.databinding.ActivityManageTipsBinding
import com.syed.models.Tip
import com.syed.utils.FirebaseUtils

class ManageTipsActivity : AdminBaseActivity() {
    private lateinit var binding: ActivityManageTipsBinding
    private lateinit var tipsAdapter: TipsAdapter
    private val tips = mutableListOf<Tip>()

    override fun onAdminVerified() {
        binding = ActivityManageTipsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadTips()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Tips"
    }

    private fun setupRecyclerView() {
        tipsAdapter =
            TipsAdapter(
                tips = tips,
                onItemClick = { tip ->
                    viewTip(tip)
                },
                isAdminMode = true,
                onEditClick = { tip ->
                    editTip(tip)
                },
                onDeleteClick = { tip ->
                    deleteTip(tip)
                },
            )

        binding.rvTips.layoutManager = LinearLayoutManager(this)
        binding.rvTips.adapter = tipsAdapter
    }

    private fun setupClickListeners() {
        // Use safe calls for UI elements that might not exist in layout
        binding.fabAddTip?.setOnClickListener {
            startActivity(Intent(this, AddEditTipActivity::class.java))
        }

        binding.btnAddTip?.setOnClickListener {
            startActivity(Intent(this, AddEditTipActivity::class.java))
        }
    }

    private fun loadTips() {
        binding.progressBar?.visibility = View.VISIBLE

        FirebaseUtils.firestore
            .collection(FirebaseUtils.TIPS_COLLECTION)
            .orderBy("dateAdded", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                binding.progressBar?.visibility = View.GONE

                if (error != null) {
                    android.util.Log.e("ManageTips", "Error loading tips", error)
                    binding.tvEmptyState?.visibility = View.VISIBLE
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    tips.clear()
                    for (document in snapshot.documents) {
                        try {
                            val tip = document.toObject(Tip::class.java)
                            tip?.let {
                                it.id = document.id
                                tips.add(it)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ManageTips", "Error parsing tip", e)
                        }
                    }
                    tipsAdapter.notifyDataSetChanged()

                    // Update empty state
                    binding.tvEmptyState?.visibility = if (tips.isEmpty()) View.VISIBLE else View.GONE
                    binding.layoutEmptyState?.visibility = if (tips.isEmpty()) View.VISIBLE else View.GONE

                    // Update count if available
                    binding.tvTipsCount?.text = "${tips.size} Tips"
                }
            }
    }

    private fun editTip(tip: Tip) {
        val intent = Intent(this, AddEditTipActivity::class.java)
        intent.putExtra("tip_id", tip.id)
        startActivity(intent)
    }

    private fun deleteTip(tip: Tip) {
        androidx.appcompat.app.AlertDialog
            .Builder(this)
            .setTitle("Delete Tip")
            .setMessage("Are you sure you want to delete this tip?")
            .setPositiveButton("Delete") { _, _ ->
                performDeleteTip(tip)
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDeleteTip(tip: Tip) {
        FirebaseUtils.firestore
            .collection(FirebaseUtils.TIPS_COLLECTION)
            .document(tip.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Tip deleted successfully", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete tip: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun viewTip(tip: Tip) {
        // TODO: Implement tip details view
        android.util.Log.d("ManageTips", "View tip: ${tip.id}")
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
