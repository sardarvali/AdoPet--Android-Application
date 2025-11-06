package com.syed.activities.admin

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.syed.R
import com.syed.adapters.AdminHistoryAdapter
import com.syed.databinding.ActivityAdminHistoryBinding
import com.syed.models.AdminAction
import com.syed.utils.FirebaseUtils

class AdminHistoryActivity : AdminBaseActivity() {
    private lateinit var binding: ActivityAdminHistoryBinding
    private lateinit var adapter: AdminHistoryAdapter
    private val actions = mutableListOf<AdminAction>()

    override fun onAdminVerified() {
        binding = ActivityAdminHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadHistory()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Admin History"
    }

    private fun setupRecyclerView() {
        adapter = AdminHistoryAdapter(actions)
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter
    }

    private fun loadHistory() {
        binding.progressBar.visibility = View.VISIBLE

        FirebaseUtils.firestore
            .collection("admin_actions")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                actions.clear()

                for (document in documents) {
                    try {
                        val action = document.toObject(AdminAction::class.java)
                        actions.add(action)
                    } catch (e: Exception) {
                        android.util.Log.e("AdminHistory", "Error parsing action", e)
                    }
                }

                adapter.notifyDataSetChanged()
                binding.tvEmptyState.visibility = if (actions.isEmpty()) View.VISIBLE else View.GONE
            }.addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                android.util.Log.e("AdminHistory", "Error loading history", e)
            }
    }
}
