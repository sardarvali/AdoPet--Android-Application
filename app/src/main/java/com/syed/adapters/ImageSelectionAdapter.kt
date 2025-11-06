package com.syed.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * ImageSelectionAdapter - Placeholder
 * This file was unused and has been removed.
 */
class ImageSelectionAdapter : RecyclerView.Adapter<ImageSelectionAdapter.ImageViewHolder>() {
    class ImageViewHolder(
        parent: ViewGroup,
    ) : RecyclerView.ViewHolder(
            android.widget.FrameLayout(parent.context),
        )

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ) = ImageViewHolder(parent)

    override fun onBindViewHolder(
        holder: ImageViewHolder,
        position: Int,
    ) {}

    override fun getItemCount() = 0
}
