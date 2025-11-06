package com.syed.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * OfficeDetailsAdapter - Placeholder
 * This file was unused and has been removed.
 */
class OfficeDetailsAdapter : RecyclerView.Adapter<OfficeDetailsAdapter.OfficeViewHolder>() {
    class OfficeViewHolder(
        parent: ViewGroup,
    ) : RecyclerView.ViewHolder(
            android.widget.FrameLayout(parent.context),
        )

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ) = OfficeViewHolder(parent)

    override fun onBindViewHolder(
        holder: OfficeViewHolder,
        position: Int,
    ) {}

    override fun getItemCount() = 0
}
