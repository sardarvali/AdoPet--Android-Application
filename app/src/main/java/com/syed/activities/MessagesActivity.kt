package com.syed.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.syed.R
import com.syed.adapters.ConversationsAdapter
import com.syed.chat.ChatManager
import com.syed.utils.FirebaseUtils
import kotlinx.coroutines.launch

/**
 * MessagesActivity - Central hub for all user messages and conversations
 * Features:
 * - View all conversations
 * - Filter by adoption inquiries, shelter messages, etc.
 * - Quick access to chat
 */
class MessagesActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var conversationsAdapter: ConversationsAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var chatManager: ChatManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        chatManager = ChatManager()
        setupToolbar()
        setupViews()
        loadConversations()
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "💬 Messages"
        }
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.rvConversations)
        tabLayout = findViewById(R.id.tabLayout)

        recyclerView.layoutManager = LinearLayoutManager(this)
        conversationsAdapter =
            ConversationsAdapter(
                currentUserId = FirebaseUtils.auth.currentUser?.uid ?: "",
                onConversationClick = { conversation ->
                    openChat(conversation)
                },
            )
        recyclerView.adapter = conversationsAdapter

        // Setup tabs for filtering
        tabLayout.addTab(tabLayout.newTab().setText("All"))
        tabLayout.addTab(tabLayout.newTab().setText("Adoptions"))
        tabLayout.addTab(tabLayout.newTab().setText("Shelters"))
        tabLayout.addTab(tabLayout.newTab().setText("Support"))

        tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    filterConversations(tab?.position ?: 0)
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            },
        )
    }

    private fun loadConversations() {
        findViewById<View>(R.id.progressBar)?.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                chatManager.getConversationsFlow().collect { conversationsList ->
                    findViewById<View>(R.id.progressBar)?.visibility = View.GONE
                    conversationsAdapter.updateConversations(conversationsList)

                    // Show/hide empty state
                    findViewById<View>(R.id.tvNoMessages)?.visibility =
                        if (conversationsList.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                findViewById<View>(R.id.progressBar)?.visibility = View.GONE
                Toast.makeText(this@MessagesActivity, "Error loading messages: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterConversations(filterType: Int) {
        // 0 = All, 1 = Adoptions, 2 = Shelters, 3 = Support
        // This is a placeholder - implement actual filtering based on conversation type
        conversationsAdapter.notifyDataSetChanged()
    }

    private fun openChat(conversation: ChatManager.Conversation) {
        val intent = Intent(this, ChatActivity::class.java)
        val currentUserId = FirebaseUtils.auth.currentUser?.uid
        val otherUserId = conversation.participants.firstOrNull { it != currentUserId }

        intent.putExtra("conversationId", conversation.id)
        intent.putExtra("userId", otherUserId)
        intent.putExtra("userName", conversation.participantNames[otherUserId] ?: "User")
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}
