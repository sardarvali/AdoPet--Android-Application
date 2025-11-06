package com.syed.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.syed.adapters.UserRescueRequestsAdapter
import com.syed.databinding.FragmentUserRequestsBinding
import com.syed.models.RescueRequest
import com.syed.utils.FirebaseUtils

class MyRescueRequestsFragment : Fragment() {
    private var _binding: FragmentUserRequestsBinding? = null
    private val binding get() = _binding!!

    private lateinit var requestsAdapter: UserRescueRequestsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUserRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadMyRequests()
        setupSwipeRefresh()
    }

    private fun setupRecyclerView() {
        requestsAdapter =
            UserRescueRequestsAdapter(requireContext()) { request ->
                // Handle request click - show details
            }

        binding.rvRequests.apply {
            adapter = requestsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadMyRequests() {
        val currentUser = FirebaseUtils.auth.currentUser
        if (currentUser == null) {
            binding.tvEmptyState.text = "Please login to view your requests"
            binding.tvEmptyState.visibility = View.VISIBLE
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        FirebaseUtils.firestore
            .collection(FirebaseUtils.RESCUE_REQUESTS_COLLECTION)
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("requestDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val requests =
                    documents.mapNotNull { doc ->
                        doc.toObject(RescueRequest::class.java).copy(id = doc.id)
                    }
                requestsAdapter.updateRequests(requests)
                binding.progressBar.visibility = View.GONE
                binding.tvEmptyState.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
            }.addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.tvEmptyState.text = "Failed to load your requests"
            }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadMyRequests()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
