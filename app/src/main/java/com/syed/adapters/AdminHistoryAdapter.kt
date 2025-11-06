package com.syed.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syed.databinding.ItemAdminActionBinding
import com.syed.models.AdminAction
import java.text.SimpleDateFormat
import java.util.*

class AdminHistoryAdapter(
    private val actions: List<AdminAction>,
) : RecyclerView.Adapter<AdminHistoryAdapter.ViewHolder>() {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding =
            ItemAdminActionBinding.inflate(
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
        holder.bind(actions[position])
    }

    override fun getItemCount() = actions.size

    inner class ViewHolder(
        private val binding: ItemAdminActionBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(action: AdminAction) {
            binding.apply {
                tvAdminName.text = action.adminName
                tvAction.text = action.action.replaceFirstChar { it.uppercase() }
                tvEntityType.text = action.entityType.replace("_", " ").replaceFirstChar { it.uppercase() }
                tvDescription.text = action.description
                tvDate.text = dateFormat.format(Date(action.timestamp))

                // Set action color based on type
                val actionColor =
                    when (action.action.lowercase()) {
                        "approved" -> android.graphics.Color.GREEN
                        "rejected" -> android.graphics.Color.RED
                        "deleted" -> android.graphics.Color.RED
                        "added", "created" -> android.graphics.Color.BLUE
                        "updated", "modified" -> android.graphics.Color.parseColor("#FFA500") // Orange
                        else -> android.graphics.Color.GRAY
                    }
                tvAction.setTextColor(actionColor)
            }
        }
    }
}
