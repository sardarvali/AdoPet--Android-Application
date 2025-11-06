package com.syed.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syed.databinding.ItemAdoptionRequestBinding
import com.syed.models.AdoptionRequest
import java.text.SimpleDateFormat
import java.util.*

class AdoptionRequestsAdapter(
    private val requests: List<AdoptionRequest>,
    private val onItemClick: (AdoptionRequest, String) -> Unit,
) : RecyclerView.Adapter<AdoptionRequestsAdapter.ViewHolder>() {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding =
            ItemAdoptionRequestBinding.inflate(
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
        private val binding: ItemAdoptionRequestBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(request: AdoptionRequest) {
            binding.apply {
                tvUserName.text = request.userName
                tvUserEmail.text = request.userEmail
                tvPetName.text = request.petName
                chipStatus.text = request.status.uppercase()
                tvDate.text = dateFormat.format(Date(request.submittedAt))

                // Set status chip color
                val statusColorRes =
                    when (request.status.lowercase()) {
                        "pending" -> com.syed.R.color.warning
                        "approved" -> com.syed.R.color.success
                        "rejected" -> com.syed.R.color.error
                        else -> com.syed.R.color.text_secondary
                    }
                chipStatus.setChipBackgroundColorResource(statusColorRes)

                btnApprove.setOnClickListener { onItemClick(request, "approve") }
                btnReject.setOnClickListener { onItemClick(request, "reject") }
                root.setOnClickListener { onItemClick(request, "view") }
            }
        }
    }
}
