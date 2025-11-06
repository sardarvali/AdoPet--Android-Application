package com.syed.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.syed.R
import com.syed.databinding.ItemShelterBinding
import com.syed.models.Shelter

class SheltersAdapter(
    private val context: Context,
    private val onShelterClick: (Shelter) -> Unit,
    private val onDeleteClick: ((Shelter) -> Unit)?,
    private val isAdminView: Boolean,
) : RecyclerView.Adapter<SheltersAdapter.ShelterViewHolder>() {
    private val shelters = mutableListOf<Shelter>()

    fun updateShelters(newShelters: List<Shelter>) {
        shelters.clear()
        shelters.addAll(newShelters)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ShelterViewHolder {
        val binding = ItemShelterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShelterViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ShelterViewHolder,
        position: Int,
    ) {
        holder.bind(shelters[position])
    }

    override fun getItemCount() = shelters.size

    inner class ShelterViewHolder(
        private val binding: ItemShelterBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(shelter: Shelter) {
            binding.tvShelterName.text = shelter.name
            binding.chipShelterType.text = shelter.type.uppercase()
            binding.tvAddress.text = shelter.address
            binding.tvCity.text = "${shelter.city}, ${shelter.state}"
            binding.tvPhone.text = shelter.phoneNumber

            // Load first image
            if (shelter.images.isNotEmpty()) {
                Glide
                    .with(context)
                    .load(shelter.images[0])
                    .placeholder(R.drawable.placeholder_pet)
                    .into(binding.ivShelterImage)
            } else {
                binding.ivShelterImage.setImageResource(R.drawable.placeholder_pet)
            }

            // Handle admin mode
            if (isAdminView) {
                binding.btnViewDetails.text = "Delete"
                binding.btnCall.text = "Edit"

                binding.btnViewDetails.setOnClickListener {
                    onDeleteClick?.invoke(shelter)
                }
                binding.btnCall.setOnClickListener {
                    onShelterClick(shelter)
                }
            } else {
                binding.btnViewDetails.text = "View Details"
                binding.btnCall.text = "Call"

                binding.btnViewDetails.setOnClickListener {
                    onShelterClick(shelter)
                }
                binding.btnCall.setOnClickListener {
                    // Handle phone call
                    val phoneNumber = shelter.phoneNumber
                    val intent =
                        android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                            data = android.net.Uri.parse("tel:$phoneNumber")
                        }
                    context.startActivity(intent)
                }
            }

            binding.root.setOnClickListener {
                onShelterClick(shelter)
            }
        }
    }
}
