package com.syed.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syed.databinding.ItemFeatureCardBinding
import com.syed.models.FeatureCard

class FeaturesAdapter(
    private val features: List<FeatureCard>,
    private val onFeatureClick: (FeatureCard) -> Unit,
) : RecyclerView.Adapter<FeaturesAdapter.FeatureViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): FeatureViewHolder {
        val binding =
            ItemFeatureCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return FeatureViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: FeatureViewHolder,
        position: Int,
    ) {
        holder.bind(features[position])
    }

    override fun getItemCount(): Int = features.size

    inner class FeatureViewHolder(
        private val binding: ItemFeatureCardBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(feature: FeatureCard) {
            binding.apply {
                tvFeatureTitle.text = feature.title
                tvFeatureSubtitle.text = feature.subtitle
                ivFeatureIcon.setImageResource(feature.iconRes)

                cardFeature.setOnClickListener {
                    onFeatureClick(feature)
                }
            }
        }
    }
}
