package com.syed.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syed.databinding.ItemUserRescueRequestBinding
import com.syed.models.RescueRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserRescueRequestsAdapter(
    private val context: Context,
    private val onRequestClick: (RescueRequest) -> Unit,
) : RecyclerView.Adapter<UserRescueRequestsAdapter.RequestViewHolder>() {
    private val requests = mutableListOf<RescueRequest>()

    fun updateRequests(newRequests: List<RescueRequest>) {
        requests.clear()
        requests.addAll(newRequests)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RequestViewHolder {
        val binding = ItemUserRescueRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
        private val binding: ItemUserRescueRequestBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(request: RescueRequest) {
            binding.tvLocation.text = request.location
            binding.tvPetType.text = request.petType.capitalize()
            binding.tvUrgency.text = "Urgency: ${request.urgency.capitalize()}"
            binding.tvStatus.text = request.status.replace("_", " ").capitalize()
            binding.tvDescription.text = request.description

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.tvRequestDate.text = "Requested: ${dateFormat.format(Date(request.requestDate))}"

            if (request.responseDate > 0) {
                binding.tvResponseDate.text = "Responded: ${dateFormat.format(Date(request.responseDate))}"
                binding.tvResponseDate.visibility = android.view.View.VISIBLE
            } else {
                binding.tvResponseDate.visibility = android.view.View.GONE
            }

            // Set status and urgency colors
            when (request.status) {
                "pending" -> binding.tvStatus.setTextColor(context.getColor(android.R.color.holo_orange_dark))
                "in_progress" -> binding.tvStatus.setTextColor(context.getColor(android.R.color.holo_blue_dark))
                "completed" -> binding.tvStatus.setTextColor(context.getColor(android.R.color.holo_green_dark))
                "cancelled" -> binding.tvStatus.setTextColor(context.getColor(android.R.color.holo_red_dark))
            }

            when (request.urgency) {
                "high" -> binding.tvUrgency.setTextColor(context.getColor(android.R.color.holo_red_dark))
                "medium" -> binding.tvUrgency.setTextColor(context.getColor(android.R.color.holo_orange_dark))
                "low" -> binding.tvUrgency.setTextColor(context.getColor(android.R.color.holo_green_dark))
            }

            // Show admin message if available
            if (request.adminMessage.isNotEmpty()) {
                binding.tvAdminMessage.text = request.adminMessage
                binding.tvAdminMessage.visibility = android.view.View.VISIBLE
            } else {
                binding.tvAdminMessage.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onRequestClick(request) }
        }
    }
}
