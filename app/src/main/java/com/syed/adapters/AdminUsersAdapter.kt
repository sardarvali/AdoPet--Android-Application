package com.syed.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syed.databinding.ItemAdminUserBinding
import com.syed.models.User
import java.text.SimpleDateFormat
import java.util.*

class AdminUsersAdapter(
    private val onRemoveAdminClick: (User) -> Unit,
) : RecyclerView.Adapter<AdminUsersAdapter.AdminUserViewHolder>() {
    private var admins = listOf<User>()

    fun updateAdmins(newAdmins: List<User>) {
        admins = newAdmins
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): AdminUserViewHolder {
        val binding =
            ItemAdminUserBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return AdminUserViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: AdminUserViewHolder,
        position: Int,
    ) {
        holder.bind(admins[position])
    }

    override fun getItemCount(): Int = admins.size

    inner class AdminUserViewHolder(
        private val binding: ItemAdminUserBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.apply {
                tvUserName.text = user.name.ifEmpty { "No Name" }
                tvUserEmail.text = user.email

                // Format join date
                user.joinedDate?.let { timestamp ->
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    tvJoinedDate.text = "Joined: ${dateFormat.format(Date(timestamp))}"
                }

                // Set remove button click listener
                btnRemoveAdmin.setOnClickListener { onRemoveAdminClick(user) }

                // Disable remove button for main admin
                btnRemoveAdmin.isEnabled = user.email != "forwork.syed@gmail.com"
            }
        }
    }
}
