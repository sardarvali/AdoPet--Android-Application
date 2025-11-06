package com.syed.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.card.MaterialCardView
import com.syed.R
import com.syed.databinding.ItemPetCardBinding
import com.syed.models.Pet

class PetsAdapter(
    private val context: Context,
    private val onPetClick: (Pet) -> Unit,
    private val onAdoptClick: ((Pet) -> Unit)? = null,
    private val onEditClick: ((Pet) -> Unit)? = null,
    private val onDeleteClick: ((Pet) -> Unit)? = null,
    private val isAdminMode: Boolean = false,
) : ListAdapter<Pet, PetsAdapter.PetViewHolder>(PetDiffCallback()) {
    // MEMORY LEAK FIX: Use DiffUtil for efficient updates
    class PetDiffCallback : DiffUtil.ItemCallback<Pet>() {
        override fun areItemsTheSame(
            oldItem: Pet,
            newItem: Pet,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: Pet,
            newItem: Pet,
        ): Boolean = oldItem == newItem
    }

    fun updatePets(newPets: List<Pet>) {
        submitList(newPets.toList()) // Create new list to trigger DiffUtil
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): PetViewHolder {
        val binding = ItemPetCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PetViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: PetViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: PetViewHolder) {
        super.onViewRecycled(holder)
        // MEMORY LEAK FIX: Clear Glide requests when view is recycled
        holder.clearImage()
    }

    inner class PetViewHolder(
        private val binding: ItemPetCardBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        private val requestOptions =
            RequestOptions()
                .placeholder(R.drawable.placeholder_pet)
                .error(R.drawable.placeholder_pet)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .override(300, 200) // Limit image size for performance

        fun bind(pet: Pet) {
            binding.tvPetName.text = pet.name
            binding.tvPetBreed.text = pet.breed
            binding.tvPetAge.text = "${pet.age} years"
            binding.tvPetGender.text = pet.gender
            binding.tvLocation.text = pet.location

            // PERFORMANCE FIX: Optimized image loading with caching
            if (pet.imageUrls.isNotEmpty()) {
                Glide
                    .with(context)
                    .load(pet.imageUrls[0])
                    .apply(requestOptions)
                    .into(binding.ivPetImage)
            } else {
                binding.ivPetImage.setImageResource(R.drawable.placeholder_pet)
            }

            // Show/hide availability status using chip
            if (!pet.isAvailable) {
                binding.chipStatus.text = "Adopted"
                binding.chipStatus.visibility = android.view.View.VISIBLE
            } else {
                binding.chipStatus.text = "Available"
                binding.chipStatus.visibility = android.view.View.VISIBLE
            }

            // Handle admin mode
            if (isAdminMode) {
                binding.btnAdopt.text = "Edit"
                binding.btnViewDetails.text = "Delete"

                binding.btnAdopt.setOnClickListener { onEditClick?.invoke(pet) }
                binding.btnViewDetails.setOnClickListener { onDeleteClick?.invoke(pet) }
            } else {
                binding.btnAdopt.text = "Adopt Now"
                binding.btnViewDetails.text = "View Details"

                binding.btnAdopt.setOnClickListener { onAdoptClick?.invoke(pet) }
                binding.btnViewDetails.setOnClickListener { onPetClick(pet) }
            }

            // Card click
            binding.root.setOnClickListener { onPetClick(pet) }
        }

        fun clearImage() {
            // MEMORY LEAK FIX: Clear Glide requests to prevent memory leaks
            Glide.with(context).clear(binding.ivPetImage)
        }
    }
}
