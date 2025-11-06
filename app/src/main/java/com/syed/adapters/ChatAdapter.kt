package com.syed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.syed.R
import com.syed.models.ChatMessage

/**
 * Secure RecyclerView adapter for displaying chat messages
 * Uses ListAdapter with DiffUtil for optimal performance
 */
class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.MessageViewHolder>(ChatDiffCallback()) {
    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
    }

    inner class MessageViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.tvMessage)

        fun bind(message: ChatMessage) {
            messageText.text = message.message
        }
    }

    override fun getItemViewType(position: Int): Int = if (getItem(position).isUser) VIEW_TYPE_USER else VIEW_TYPE_AI

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): MessageViewHolder {
        val layoutResId =
            if (viewType == VIEW_TYPE_USER) {
                R.layout.item_message_user
            } else {
                R.layout.item_message_ai
            }

        val view = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MessageViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    private class ChatDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(
            oldItem: ChatMessage,
            newItem: ChatMessage,
        ): Boolean = oldItem.timestamp == newItem.timestamp

        override fun areContentsTheSame(
            oldItem: ChatMessage,
            newItem: ChatMessage,
        ): Boolean = oldItem == newItem
    }
}
