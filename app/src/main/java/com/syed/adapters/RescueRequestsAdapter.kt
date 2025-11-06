package com.syed.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syed.databinding.ItemRescueRequestBinding
import com.syed.models.RescueRequest
import java.text.SimpleDateFormat
import java.util.*

class RescueRequestsAdapter(
    private val context: Context,
    private val onUpdateStatusClick: (RescueRequest) -> Unit,
    private val onViewClick: (RescueRequest) -> Unit,
    private val onReplyClick: (RescueRequest) -> Unit = {},
) : RecyclerView.Adapter<RescueRequestsAdapter.RequestViewHolder>() {
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
        val binding = ItemRescueRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
        private val binding: ItemRescueRequestBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(request: RescueRequest) {
            binding.tvLocation.text = request.location
            binding.tvPetType.text = request.petType.capitalize()
            binding.tvUrgency.text = request.urgency.capitalize()
            binding.tvUserName.text = request.userName
            binding.tvUserPhone.text = request.userPhone
            binding.tvStatus.text = request.status.replace("_", " ").capitalize()
            binding.tvDescription.text = request.description

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.tvRequestDate.text = dateFormat.format(Date(request.requestDate))

            // Set status color
            val statusColor =
                when (request.status) {
                    "pending" -> android.R.color.holo_orange_dark
                    "in_progress" -> android.R.color.holo_blue_dark
                    "completed" -> android.R.color.holo_green_dark
                    "cancelled" -> android.R.color.holo_red_dark
                    else -> android.R.color.darker_gray
                }
            binding.tvStatus.setTextColor(context.getColor(statusColor))

            // Set urgency color
            val urgencyColor =
                when (request.urgency) {
                    "high" -> android.R.color.holo_red_dark
                    "medium" -> android.R.color.holo_orange_dark
                    "low" -> android.R.color.holo_green_dark
                    else -> android.R.color.darker_gray
                }
            binding.tvUrgency.setTextColor(context.getColor(urgencyColor))

            binding.btnUpdateStatus.setOnClickListener { onUpdateStatusClick(request) }
            binding.btnViewDetails.setOnClickListener { onViewClick(request) }
            binding.root.setOnClickListener { onViewClick(request) }
            binding.btnReply.setOnClickListener { onReplyClick(request) }
        }
    }
}
