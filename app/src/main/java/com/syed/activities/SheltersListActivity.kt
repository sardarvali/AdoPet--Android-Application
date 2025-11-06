package com.syed.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.syed.adapters.SheltersAdapter
import com.syed.databinding.ActivitySheltersListBinding
import com.syed.models.Shelter
import com.syed.utils.FirebaseUtils

class SheltersListActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySheltersListBinding
    private lateinit var sheltersAdapter: SheltersAdapter
    private val shelters = mutableListOf<Shelter>()
    private var currentFilter = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySheltersListBinding.inflate(layoutInflater)
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
        supportActionBar?.title = "Pet Shelters"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        sheltersAdapter =
            SheltersAdapter(
                context = this,
                onShelterClick = { shelter ->
                    val intent = Intent(this, ShelterDetailsActivity::class.java)
                    intent.putExtra("shelterId", shelter.id)
                    startActivity(intent)
                },
                onDeleteClick = null,
                isAdminView = false,
            )

        binding.rvShelters.apply {
            layoutManager = LinearLayoutManager(this@SheltersListActivity)
            adapter = sheltersAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("All"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("NGO"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Government"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Private"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Nearby"))

        binding.tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    currentFilter =
                        when (tab?.position) {
                            0 -> "all"
                            1 -> "ngo"
                            2 -> "government"
                            3 -> "private"
                            4 -> "nearby"
                            else -> "all"
                        }
                    if (currentFilter == "nearby") {
                        loadNearbyShelters()
                    } else {
                        filterShelters()
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            },
        )
    }

    private fun setupFab() {
        binding.fabRegisterShelter.setOnClickListener {
            startActivity(Intent(this, RegisterShelterActivity::class.java))
        }
    }

    private fun loadShelters() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        // Only show approved shelters to regular users
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
                        android.util.Log.e("SheltersList", "Error parsing shelter", e)
                    }
                }
                filterShelters()
                binding.progressBar.visibility = View.GONE

                if (shelters.isEmpty()) {
                    binding.tvEmptyState.text = "No approved shelters yet. Admins will review shelter requests."
                    binding.tvEmptyState.visibility = View.VISIBLE
                }
            }.addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load shelters: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadNearbyShelters() {
        // In a real app, you'd get user's location and filter by distance
        Toast.makeText(this, "Finding nearby shelters...", Toast.LENGTH_SHORT).show()

        // For now, just show all shelters sorted by distance (mock)
        val sortedShelters = shelters.sortedBy { it.name }
        sheltersAdapter.updateShelters(sortedShelters)

        if (sortedShelters.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.tvEmptyState.text = "No shelters found nearby"
            binding.rvShelters.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvShelters.visibility = View.VISIBLE
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
            binding.tvEmptyState.text = "No shelters found"
            binding.rvShelters.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvShelters.visibility = View.VISIBLE
        }
    }
}
