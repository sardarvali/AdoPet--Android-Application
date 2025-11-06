package com.syed.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.syed.activities.AdoptionFormActivity
import com.syed.activities.PetDetailsActivity
import com.syed.adapters.PetsAdapter
import com.syed.databinding.FragmentPetsBinding
import com.syed.models.Pet
import com.syed.utils.FirebaseUtils

class CatsFragment : Fragment() {
    private var _binding: FragmentPetsBinding? = null
    private val binding get() = _binding!!

    private lateinit var petsAdapter: PetsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadCats()
        setupSwipeRefresh()
    }

    private fun setupRecyclerView() {
        petsAdapter =
            PetsAdapter(
                context = requireContext(),
                onPetClick = { pet -> openPetDetails(pet) },
                onAdoptClick = { pet -> openAdoptionForm(pet) },
            )

        binding.rvPets.apply {
            adapter = petsAdapter
            layoutManager = GridLayoutManager(context, 2)
        }
    }

    private fun loadCats() {
        binding.progressBar.visibility = View.VISIBLE

        FirebaseUtils.firestore
            .collection(FirebaseUtils.PETS_COLLECTION)
            .whereEqualTo("type", "cat")
            .whereEqualTo("available", true)
            .orderBy("dateAdded", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val pets =
                    documents.mapNotNull { doc ->
                        doc.toObject(Pet::class.java).copy(id = doc.id)
                    }
                petsAdapter.updatePets(pets)
                binding.progressBar.visibility = View.GONE
                binding.tvEmptyState.visibility = if (pets.isEmpty()) View.VISIBLE else View.GONE
            }.addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.tvEmptyState.text = "Failed to load cats"
            }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadCats()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun openPetDetails(pet: Pet) {
        val intent = Intent(requireContext(), PetDetailsActivity::class.java)
        intent.putExtra("petId", pet.id)
        startActivity(intent)
    }

    private fun openAdoptionForm(pet: Pet) {
        val intent = Intent(requireContext(), AdoptionFormActivity::class.java)
        intent.putExtra("petId", pet.id)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
