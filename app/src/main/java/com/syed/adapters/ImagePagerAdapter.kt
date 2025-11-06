package com.syed.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.syed.R
import com.syed.databinding.ItemImagePagerBinding

class ImagePagerAdapter(
    private val imageUrls: List<String>,
) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ImageViewHolder {
        val binding = ItemImagePagerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ImageViewHolder,
        position: Int,
    ) {
        holder.bind(imageUrls[position])
    }

    override fun getItemCount(): Int = imageUrls.size

    inner class ImageViewHolder(
        private val binding: ItemImagePagerBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(imageUrl: String) {
            Glide
                .with(binding.root.context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_pet)
                .error(R.drawable.placeholder_pet)
                .centerCrop()
                .into(binding.imageView)
        }
    }
}
