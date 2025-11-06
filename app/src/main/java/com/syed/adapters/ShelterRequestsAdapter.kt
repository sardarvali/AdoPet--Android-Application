package com.syed.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syed.databinding.ItemShelterRequestBinding
import com.syed.models.ShelterRequest
import java.text.SimpleDateFormat
import java.util.Locale

class ShelterRequestsAdapter(
    private val context: Context,
    private val onApproveClick: (ShelterRequest) -> Unit,
    private val onRejectClick: (ShelterRequest) -> Unit,
    private val onViewClick: (ShelterRequest) -> Unit,
) : RecyclerView.Adapter<ShelterRequestsAdapter.RequestViewHolder>() {
    private val requests = mutableListOf<ShelterRequest>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun updateRequests(newRequests: List<ShelterRequest>) {
        requests.clear()
        requests.addAll(newRequests)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RequestViewHolder {
        val binding =
            ItemShelterRequestBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: RequestViewHolder,
        position: Int,
    ) {
        holder.bind(requests[position])
    }

    override fun getItemCount() = requests.size

    inner class RequestViewHolder(
        private val binding: ItemShelterRequestBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(request: ShelterRequest) {
            binding.tvShelterName.text = request.shelterName
            binding.tvShelterType.text = request.shelterType.uppercase()
            binding.tvUserName.text = "Submitted by: ${request.userName}"
            binding.tvUserEmail.text = request.userEmail
            binding.tvAddress.text = "${request.address}, ${request.city}"
            binding.tvPhone.text = request.phoneNumber
            binding.tvDate.text = "Submitted: ${dateFormat.format(request.dateSubmitted.toDate())}"

            // Status badge
            binding.tvStatus.text = request.status.uppercase()
            binding.tvStatus.setBackgroundResource(
                when (request.status) {
                    "pending" -> com.syed.R.drawable.bg_category_chip
                    "approved" -> com.syed.R.drawable.bg_adopted_status
                    "rejected" -> com.syed.R.drawable.bg_remove_button
                    else -> com.syed.R.drawable.bg_status_chip
                },
            )

            // Show/hide action buttons based on status
            if (request.status == "pending") {
                binding.btnApprove.visibility = android.view.View.VISIBLE
                binding.btnReject.visibility = android.view.View.VISIBLE

                binding.btnApprove.setOnClickListener {
                    onApproveClick(request)
                }

                binding.btnReject.setOnClickListener {
                    onRejectClick(request)
                }
            } else {
                binding.btnApprove.visibility = android.view.View.GONE
                binding.btnReject.visibility = android.view.View.GONE
            }

            binding.btnView.setOnClickListener {
                onViewClick(request)
            }
        }
    }
}
