package com.syed.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syed.databinding.ItemUserAdoptionRequestBinding
import com.syed.models.AdoptionRequest
import java.text.SimpleDateFormat
import java.util.*

class UserAdoptionRequestsAdapter(
    private val context: Context,
    private val onRequestClick: (AdoptionRequest) -> Unit,
) : RecyclerView.Adapter<UserAdoptionRequestsAdapter.RequestViewHolder>() {
    private val requests = mutableListOf<AdoptionRequest>()

    fun updateRequests(newRequests: List<AdoptionRequest>) {
        requests.clear()
        requests.addAll(newRequests)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RequestViewHolder {
        val binding = ItemUserAdoptionRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: RequestViewHolder,
        position: Int,
    ) {
        holder.bind(requests[position])
    }

    override fun getItemCount(): Int = requests.size

    inner class RequestViewHolder(
        private val binding: ItemUserAdoptionRequestBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(request: AdoptionRequest) {
            binding.tvPetName.text = request.petName
            binding.tvStatus.text = request.status.capitalize()

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.tvRequestDate.text = "Requested: ${dateFormat.format(Date(request.requestDate))}"

            if (request.responseDate > 0) {
                binding.tvResponseDate.text = "Responded: ${dateFormat.format(Date(request.responseDate))}"
                binding.tvResponseDate.visibility = android.view.View.VISIBLE
            } else {
                binding.tvResponseDate.visibility = android.view.View.GONE
            }

            // Set status color and show admin message if available
            when (request.status) {
                "pending" -> {
                    binding.tvStatus.setTextColor(context.getColor(android.R.color.holo_orange_dark))
                    binding.tvAdminMessage.visibility = android.view.View.GONE
                }
                "approved" -> {
                    binding.tvStatus.setTextColor(context.getColor(android.R.color.holo_green_dark))
                    if (request.adminMessage.isNotEmpty()) {
                        binding.tvAdminMessage.text = request.adminMessage
                        binding.tvAdminMessage.visibility = android.view.View.VISIBLE
                    }
                }
                "rejected" -> {
                    binding.tvStatus.setTextColor(context.getColor(android.R.color.holo_red_dark))
                    if (request.adminMessage.isNotEmpty()) {
                        binding.tvAdminMessage.text = request.adminMessage
                        binding.tvAdminMessage.visibility = android.view.View.VISIBLE
                    }
                }
            }

            binding.root.setOnClickListener { onRequestClick(request) }
        }
    }
}
