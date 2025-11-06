package com.syed.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.syed.adapters.TipsAdapter
import com.syed.databinding.FragmentTipsBinding
import com.syed.models.Tip
import com.syed.utils.FirebaseUtils

class TipsFragment : Fragment() {
    private var _binding: FragmentTipsBinding? = null
    private val binding get() = _binding!!

    private lateinit var tipsAdapter: TipsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTipsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadTips()
        setupSwipeRefresh()
    }

    private fun setupRecyclerView() {
        tipsAdapter =
            TipsAdapter(
                tips = mutableListOf(),
                onItemClick = { tip ->
                    // Handle tip click if needed
                },
            )

        binding.rvTips.apply {
            adapter = tipsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadTips() {
        binding.progressBar.visibility = View.VISIBLE

        FirebaseUtils.firestore
            .collection(FirebaseUtils.TIPS_COLLECTION)
            .whereEqualTo("active", true)
            .orderBy("dateAdded", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val tips =
                    documents.mapNotNull { doc ->
                        doc.toObject(Tip::class.java).copy(id = doc.id)
                    }
                tipsAdapter.updateTips(tips)
                binding.progressBar.visibility = View.GONE
                binding.tvEmptyState.visibility = if (tips.isEmpty()) View.VISIBLE else View.GONE
            }.addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.tvEmptyState.text = "Failed to load tips"
            }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadTips()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
