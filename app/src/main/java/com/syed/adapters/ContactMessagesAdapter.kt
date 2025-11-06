package com.syed.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syed.databinding.ItemContactMessageBinding
import com.syed.models.ContactMessage
import java.text.SimpleDateFormat
import java.util.*

class ContactMessagesAdapter(
    private val messages: List<ContactMessage>,
    private val onItemClick: (ContactMessage, String) -> Unit,
) : RecyclerView.Adapter<ContactMessagesAdapter.ViewHolder>() {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding =
            ItemContactMessageBinding.inflate(
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
        holder.bind(messages[position])
    }

    override fun getItemCount() = messages.size

    inner class ViewHolder(
        private val binding: ItemContactMessageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ContactMessage) {
            binding.apply {
                tvUserName.text = message.userName
                tvUserEmail.text = message.userEmail
                tvSubject.text = message.subject
                tvMessage.text = message.message
                tvDate.text = dateFormat.format(Date(message.sentDate))

                // Show read/unread status
                val statusText = if (message.isRead) "Read" else "Unread"
                val statusColor = if (message.isRead) android.graphics.Color.GREEN else android.graphics.Color.RED
                tvStatus.text = statusText
                tvStatus.setTextColor(statusColor)

                // Show reply status
                if (message.isReplied) {
                    tvReplyStatus.text = "Replied"
                    tvReplyStatus.setTextColor(android.graphics.Color.BLUE)
                } else {
                    tvReplyStatus.text = "No reply"
                    tvReplyStatus.setTextColor(android.graphics.Color.GRAY)
                }

                btnReply.setOnClickListener { onItemClick(message, "reply") }
                btnMarkRead.setOnClickListener { onItemClick(message, "mark_read") }
                root.setOnClickListener { onItemClick(message, "view") }
            }
        }
    }
}
