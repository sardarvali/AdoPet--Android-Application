package com.syed.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.syed.R
import com.syed.chat.ChatManager
import java.text.SimpleDateFormat
import java.util.*

class ConversationsAdapter(
    private val currentUserId: String,
    private val onConversationClick: (ChatManager.Conversation) -> Unit,
    private val onConversationLongClick: (ChatManager.Conversation) -> Unit = {},
) : RecyclerView.Adapter<ConversationsAdapter.ConversationViewHolder>() {
    private val conversations = mutableListOf<ChatManager.Conversation>()
    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun updateConversations(newConversations: List<ChatManager.Conversation>) {
        conversations.clear()
        conversations.addAll(newConversations)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ConversationViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ConversationViewHolder,
        position: Int,
    ) {
        holder.bind(conversations[position])
    }

    override fun getItemCount(): Int = conversations.size

    inner class ConversationViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        private val avatarImage: ImageView = itemView.findViewById(R.id.avatarImage)
        private val nameText: TextView = itemView.findViewById(R.id.nameText)
        private val lastMessageText: TextView = itemView.findViewById(R.id.lastMessageText)
        private val timeText: TextView = itemView.findViewById(R.id.timestampText)
        private val unreadBadge: TextView = itemView.findViewById(R.id.tvUnreadCount)
        private val onlineIndicator: View = itemView.findViewById(R.id.onlineIndicator)
        private val typingIndicator: TextView = itemView.findViewById(R.id.typingIndicator)
        private val mutedIcon: ImageView? = itemView.findViewById(R.id.mutedIcon)
        private val pinnedIcon: ImageView? = itemView.findViewById(R.id.pinnedIcon)

        fun bind(conversation: ChatManager.Conversation) {
            // Set name and avatar
            if (conversation.conversationType == ChatManager.ConversationType.GROUP) {
                nameText.text = conversation.groupName ?: "Group Chat"
                if (conversation.groupImage?.isNotEmpty() == true) {
                    Glide
                        .with(itemView.context)
                        .load(conversation.groupImage)
                        .placeholder(R.drawable.ic_pets)
                        .circleCrop()
                        .into(avatarImage)
                } else {
                    avatarImage.setImageResource(R.drawable.ic_pets)
                }
            } else {
                // Direct conversation
                val otherUserId = conversation.participants.firstOrNull { it != currentUserId }
                nameText.text = conversation.participantNames[otherUserId] ?: "User"

                val photoUrl = conversation.participantImages[otherUserId]
                if (photoUrl?.isNotEmpty() == true) {
                    Glide
                        .with(itemView.context)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_person)
                        .circleCrop()
                        .into(avatarImage)
                } else {
                    avatarImage.setImageResource(R.drawable.ic_person)
                }

                // Online status for direct chats
                val isOnline = conversation.onlineStatus[otherUserId] ?: false
                onlineIndicator.visibility = if (isOnline) View.VISIBLE else View.GONE
            }

            // Last message
            if (conversation.typingUsers.isNotEmpty() && !conversation.typingUsers.contains(currentUserId)) {
                typingIndicator.visibility = View.VISIBLE
                lastMessageText.visibility = View.GONE
                typingIndicator.text = "typing..."
            } else {
                typingIndicator.visibility = View.GONE
                lastMessageText.visibility = View.VISIBLE

                val lastMsg =
                    when {
                        conversation.lastMessage.isEmpty() -> "No messages yet"
                        conversation.lastMessageSender == currentUserId -> "You: ${conversation.lastMessage}"
                        else -> conversation.lastMessage
                    }
                lastMessageText.text = lastMsg.take(50) + if (lastMsg.length > 50) "..." else ""
            }

            // Time
            val timestamp = conversation.lastMessageTime.toDate()
            val now = Date()
            val diffInDays = ((now.time - timestamp.time) / (1000 * 60 * 60 * 24)).toInt()

            timeText.text =
                when {
                    diffInDays == 0 -> timeFormat.format(timestamp)
                    diffInDays < 7 -> SimpleDateFormat("EEE", Locale.getDefault()).format(timestamp)
                    else -> dateFormat.format(timestamp)
                }

            // Unread badge
            val unreadCount = conversation.unreadCount[currentUserId] ?: 0
            if (unreadCount > 0) {
                unreadBadge.visibility = View.VISIBLE
                unreadBadge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
            } else {
                unreadBadge.visibility = View.GONE
            }

            // Muted icon
            val isMuted = conversation.isMuted[currentUserId] ?: false
            mutedIcon?.visibility = if (isMuted) View.VISIBLE else View.GONE

            // Pinned icon
            pinnedIcon?.visibility = if (conversation.pinnedMessages.isNotEmpty()) View.VISIBLE else View.GONE

            // Click listeners
            itemView.setOnClickListener {
                onConversationClick(conversation)
            }

            itemView.setOnLongClickListener {
                onConversationLongClick(conversation)
                true
            }
        }
    }
}
