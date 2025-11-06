package com.syed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.syed.R
import com.syed.chat.ChatManager
import java.text.SimpleDateFormat
import java.util.*

class ChatMessagesAdapter(
    private val currentUserId: String,
    private val onMessageLongClick: (ChatManager.ChatMessage) -> Unit = {},
    private val onReactionClick: (ChatManager.ChatMessage, String) -> Unit = { _, _ -> },
    private val onMediaClick: (ChatManager.ChatMessage) -> Unit = {},
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val messages = mutableListOf<ChatManager.ChatMessage>()
    private val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    fun updateMessages(newMessages: List<ChatManager.ChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (messages[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        if (viewType == VIEW_TYPE_SENT) {
            val view =
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view =
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val message = messages[position]
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    inner class SentMessageViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.tvMessage)
        private val timeText: TextView = itemView.findViewById(R.id.tvTimestamp)

        fun bind(message: ChatManager.ChatMessage) {
            if (message.isDeleted) {
                messageText.text = "🚫 You deleted this message"
                timeText.text = timeFormat.format(message.timestamp.toDate())
                return
            }

            messageText.text = message.message
            timeText.text = timeFormat.format(message.timestamp.toDate())

            itemView.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }

    inner class ReceivedMessageViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.tvMessage)
        private val timeText: TextView = itemView.findViewById(R.id.tvTimestamp)

        fun bind(message: ChatManager.ChatMessage) {
            if (message.isDeleted) {
                messageText.text = "🚫 This message was deleted"
                timeText.text = timeFormat.format(message.timestamp.toDate())
                return
            }

            messageText.text = message.message
            timeText.text = timeFormat.format(message.timestamp.toDate())

            itemView.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }
}
