package com.syed.activities.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.syed.R
import com.syed.adapters.SheltersAdapter
import com.syed.databinding.ActivityManageSheltersBinding
import com.syed.models.Shelter
import com.syed.utils.FirebaseUtils

class ManageSheltersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageSheltersBinding
    private lateinit var sheltersAdapter: SheltersAdapter
    private val shelters = mutableListOf<Shelter>()
    private var currentFilter = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageSheltersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupTabs()
        setupFab()
        loadShelters()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Shelters"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        sheltersAdapter =
            SheltersAdapter(
                context = this,
                onShelterClick = { shelter ->
                    val intent = Intent(this, AddEditShelterActivity::class.java)
                    intent.putExtra("shelterId", shelter.id)
                    startActivity(intent)
                },
                onDeleteClick = { shelter ->
                    showDeleteConfirmation(shelter)
                },
                isAdminView = true,
            )

        binding.rvShelters.apply {
            layoutManager = LinearLayoutManager(this@ManageSheltersActivity)
            adapter = sheltersAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("All"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("NGO"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Government"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Private"))

        binding.tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    currentFilter =
                        when (tab?.position) {
                            0 -> "all"
                            1 -> "ngo"
                            2 -> "government"
                            3 -> "private"
                            else -> "all"
                        }
                    filterShelters()
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            },
        )
    }

    private fun setupFab() {
        binding.fabAddShelter.setOnClickListener {
            startActivity(Intent(this, AddEditShelterActivity::class.java))
        }
    }

    private fun loadShelters() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        FirebaseUtils.firestore
            .collection("shelters")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { documents ->
                shelters.clear()
                for (doc in documents) {
                    try {
                        val shelter = doc.toObject(Shelter::class.java).copy(id = doc.id)
                        shelters.add(shelter)
                    } catch (e: Exception) {
                        android.util.Log.e("ManageShelters", "Error parsing shelter", e)
                    }
                }
                filterShelters()
                binding.progressBar.visibility = View.GONE
            }.addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load shelters: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterShelters() {
        val filtered =
            if (currentFilter == "all") {
                shelters
            } else {
                shelters.filter { it.type == currentFilter }
            }

        sheltersAdapter.updateShelters(filtered)

        if (filtered.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvShelters.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvShelters.visibility = View.VISIBLE
        }
    }

    private fun showDeleteConfirmation(shelter: Shelter) {
        AlertDialog
            .Builder(this)
            .setTitle("Delete Shelter")
            .setMessage("Are you sure you want to delete ${shelter.name}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteShelter(shelter)
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteShelter(shelter: Shelter) {
        FirebaseUtils.firestore
            .collection("shelters")
            .document(shelter.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Shelter deleted successfully", Toast.LENGTH_SHORT).show()
                loadShelters()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete shelter: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        loadShelters()
    }
}
