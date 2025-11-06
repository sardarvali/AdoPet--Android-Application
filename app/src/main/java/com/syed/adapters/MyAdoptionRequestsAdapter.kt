package com.syed.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.syed.R
import com.syed.databinding.ItemAdoptionRequestUserBinding
import com.syed.models.AdoptionRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyAdoptionRequestsAdapter(
    private val context: Context,
    private var requests: List<AdoptionRequest>,
    private val onRequestClick: (AdoptionRequest) -> Unit,
) : RecyclerView.Adapter<MyAdoptionRequestsAdapter.ViewHolder>() {
    fun updateRequests(newRequests: List<AdoptionRequest>) {
        requests = newRequests
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding =
            ItemAdoptionRequestUserBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.bind(requests[position])
    }

    override fun getItemCount() = requests.size

    inner class ViewHolder(
        private val binding: ItemAdoptionRequestUserBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(request: AdoptionRequest) {
            binding.apply {
                tvPetName.text = request.petName
                tvStatus.text = request.status.uppercase()
                tvDate.text = formatDate(request.timestamp)

                // Set status color
                val statusColor =
                    when (request.status.lowercase()) {
                        "pending" -> ContextCompat.getColor(context, R.color.status_pending)
                        "approved" -> ContextCompat.getColor(context, R.color.status_approved)
                        "rejected" -> ContextCompat.getColor(context, R.color.status_rejected)
                        else -> ContextCompat.getColor(context, R.color.text_secondary)
                    }
                tvStatus.setTextColor(statusColor)

                // Show reason if rejected
                if (request.status == "rejected" && !request.rejectionReason.isNullOrEmpty()) {
                    tvRejectionReason.visibility = android.view.View.VISIBLE
                    tvRejectionReason.text = "Reason: ${request.rejectionReason}"
                } else {
                    tvRejectionReason.visibility = android.view.View.GONE
                }

                root.setOnClickListener { onRequestClick(request) }
            }
        }

        private fun formatDate(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return dateFormat.format(Date(timestamp))
        }
    }
}
