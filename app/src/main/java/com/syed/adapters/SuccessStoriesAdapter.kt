package com.syed.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.syed.R
import com.syed.models.SuccessStory
import java.text.SimpleDateFormat
import java.util.*

class SuccessStoriesAdapter(
    private val context: Context,
    private val isAdminMode: Boolean = false,
    private val onEditClick: ((SuccessStory) -> Unit)? = null,
    private val onDeleteClick: ((SuccessStory) -> Unit)? = null,
) : RecyclerView.Adapter<SuccessStoriesAdapter.StoryViewHolder>() {
    private var stories = listOf<SuccessStory>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun updateStories(newStories: List<SuccessStory>) {
        stories = newStories
        notifyDataSetChanged()
    }

    fun getStoryById(id: String): SuccessStory? = stories.find { it.id == id }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): StoryViewHolder {
        // Use existing pet item layout instead of missing success story layout
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_pet, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: StoryViewHolder,
        position: Int,
    ) {
        holder.bind(stories[position])
    }

    override fun getItemCount(): Int = stories.size

    inner class StoryViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvStoryTitle: TextView = itemView.findViewById(R.id.tvPetName)
        private val tvPetName: TextView = itemView.findViewById(R.id.tvPetBreed)
        private val tvOwnerName: TextView = itemView.findViewById(R.id.tvPetAge)
        private val tvStoryContent: TextView = itemView.findViewById(R.id.tvPetLocation)
        private val tvDate: TextView? = itemView.findViewById(R.id.tvPetAge) // Reuse existing view
        private val ivStoryImage: ImageView = itemView.findViewById(R.id.ivPetImage)
        private val btnEdit: View? = itemView.findViewById(R.id.btnAdopt)
        private val btnDelete: View? = itemView.findViewById(R.id.btnViewDetails)

        fun bind(story: SuccessStory) {
            tvStoryTitle.text = story.title
            tvPetName.text = "Pet: ${story.petName}"
            tvOwnerName.text = "Owner: ${story.ownerName}"
            tvStoryContent.text = story.story
            tvDate?.text = dateFormat.format(Date(story.dateAdded))

            // Load image if available
            if (story.imageUrl.isNotEmpty()) {
                Glide
                    .with(context)
                    .load(story.imageUrl)
                    .centerCrop()
                    .into(ivStoryImage)
                ivStoryImage.visibility = View.VISIBLE
            } else {
                ivStoryImage.visibility = View.GONE
            }

            // Show admin controls if in admin mode
            if (isAdminMode && btnEdit != null && btnDelete != null) {
                btnEdit.visibility = View.VISIBLE
                btnDelete.visibility = View.VISIBLE

                if (btnEdit is TextView) {
                    (btnEdit as TextView).text = "Edit"
                }
                if (btnDelete is TextView) {
                    (btnDelete as TextView).text = "Delete"
                }

                btnEdit.setOnClickListener {
                    onEditClick?.invoke(story)
                }

                btnDelete.setOnClickListener {
                    onDeleteClick?.invoke(story)
                }
            } else {
                btnEdit?.visibility = View.GONE
                btnDelete?.visibility = View.GONE
            }
        }
    }
}
