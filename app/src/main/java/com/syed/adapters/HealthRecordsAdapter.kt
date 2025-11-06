package com.syed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.syed.R
import com.syed.models.HealthRecord
import java.text.SimpleDateFormat
import java.util.*

class HealthRecordsAdapter(
    private val records: List<HealthRecord>,
    private val onEditClick: (HealthRecord) -> Unit,
    private val onDeleteClick: (HealthRecord) -> Unit,
) : RecyclerView.Adapter<HealthRecordsAdapter.ViewHolder>() {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_health_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    inner class ViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        private val typeText: TextView = itemView.findViewById(R.id.typeText)
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)
        private val dateText: TextView = itemView.findViewById(R.id.dateText)
        private val vetText: TextView = itemView.findViewById(R.id.vetText)
        private val costText: TextView = itemView.findViewById(R.id.costText)
        private val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(record: HealthRecord) {
            typeText.text = record.type
            titleText.text = record.title
            descriptionText.text = record.description
            dateText.text = dateFormat.format(Date(record.date))
            vetText.text = if (record.vetName.isNotEmpty()) "Vet: ${record.vetName}" else ""
            costText.text = if (record.cost > 0) "$${String.format("%.2f", record.cost)}" else ""

            editButton.setOnClickListener { onEditClick(record) }
            deleteButton.setOnClickListener { onDeleteClick(record) }

            // Show/hide optional fields
            vetText.visibility = if (record.vetName.isNotEmpty()) View.VISIBLE else View.GONE
            costText.visibility = if (record.cost > 0) View.VISIBLE else View.GONE
        }
    }
}
