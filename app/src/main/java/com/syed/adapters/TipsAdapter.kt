package com.syed.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syed.databinding.ItemTipBinding
import com.syed.models.Tip
import java.text.SimpleDateFormat
import java.util.*

class TipsAdapter(
    private var tips: MutableList<Tip>,
    private val onItemClick: (Tip) -> Unit,
    private val isAdminMode: Boolean = false,
    private val onEditClick: ((Tip) -> Unit)? = null,
    private val onDeleteClick: ((Tip) -> Unit)? = null,
) : RecyclerView.Adapter<TipsAdapter.ViewHolder>() {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding =
            ItemTipBinding.inflate(
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
        holder.bind(tips[position])
    }

    override fun getItemCount() = tips.size

    fun updateTips(newTips: List<Tip>) {
        tips.clear()
        tips.addAll(newTips)
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemTipBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tip: Tip) {
            binding.apply {
                tvTitle.text = tip.title
                tvContent.text = tip.content
                chipCategory.text = tip.category
                tvDate.text = dateFormat.format(Date(tip.dateAdded))
                tvViews.text = "${tip.views} views"

                if (isAdminMode) {
                    layoutAdminActions.visibility = android.view.View.VISIBLE
                    btnEdit.setOnClickListener { onEditClick?.invoke(tip) }
                    btnDelete.setOnClickListener { onDeleteClick?.invoke(tip) }
                } else {
                    layoutAdminActions.visibility = android.view.View.GONE
                }

                root.setOnClickListener { onItemClick(tip) }
            }
        }
    }
}
