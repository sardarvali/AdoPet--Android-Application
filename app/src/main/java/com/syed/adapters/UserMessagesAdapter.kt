package com.syed.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * UserMessagesAdapter - Placeholder
 * This file was unused and has been removed.
 */
class UserMessagesAdapter : RecyclerView.Adapter<UserMessagesAdapter.MessageViewHolder>() {
    class MessageViewHolder(
        parent: ViewGroup,
    ) : RecyclerView.ViewHolder(
            android.widget.FrameLayout(parent.context),
        )

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ) = MessageViewHolder(parent)

    override fun onBindViewHolder(
        holder: MessageViewHolder,
        position: Int,
    ) {}

    override fun getItemCount() = 0
}
