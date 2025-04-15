package com.example.shapelearning.ui.games.matching

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat // Added
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shapelearning.R // Added
import com.example.shapelearning.databinding.ItemMatchingShapeBinding

class ShapeMatchingAdapter(
    private val listener: OnShapeClickListener
) : ListAdapter<ShapeMatchingViewModel.MatchingCard, ShapeMatchingAdapter.ShapeViewHolder>(ShapeDiffCallback()) {

    interface OnShapeClickListener {
        fun onShapeClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShapeViewHolder {
        val binding = ItemMatchingShapeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ShapeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShapeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ShapeViewHolder(
        private val binding: ItemMatchingShapeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition // Use bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Optional: Add small animation on click before notifying listener
                    listener.onShapeClick(position)
                }
            }
        }

        fun bind(card: ShapeMatchingViewModel.MatchingCard) {
            binding.ivShape.setImageResource(android.R.color.transparent) // Clear previous image
            binding.viewOverlay.visibility = View.GONE // Reset overlay

            // Set content description based on state
            binding.root.contentDescription = if (card.isFlipped || card.isMatched) {
                binding.root.context.getString(R.string.card_description_prefix) + " ${card.shapeId}" // Add string "Card showing shape ID "
            } else {
                binding.root.context.getString(R.string.card_description_hidden) // Add string "Hidden card"
            }


            if (card.isFlipped || card.isMatched) {
                binding.ivShape.setImageResource(card.imageResId)
                // Apply visual feedback for matched cards
                if (card.isMatched) {
                    binding.viewOverlay.visibility = View.VISIBLE // Show overlay for matched
                    // binding.root.alpha = 0.7f // Example: Fade matched cards slightly
                } else {
                    binding.root.alpha = 1.0f // Ensure non-matched are fully visible
                }
                binding.ivShape.setBackgroundColor(ContextCompat.getColor(binding.root.context, android.R.color.transparent)) // Remove card back

            } else {
                // Card is face down
                binding.ivShape.setImageResource(R.drawable.card_back) // Use a drawable for card back
                binding.root.alpha = 1.0f
                // Optional: Set background color for face-down card
                // binding.ivShape.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.card_back_color)) // Add color resource
            }

            // TODO: Implement flip animation here if desired
            // Example (requires animator XML files R.animator.card_flip_*, and careful state management):
            /*
            val scale = binding.root.context.resources.displayMetrics.density
            binding.ivShape.cameraDistance = 8000 * scale
            binding.cardBack.cameraDistance = 8000 * scale // If you have a separate view for the back

            val flipOut = AnimatorInflater.loadAnimator(binding.root.context, R.animator.card_flip_out) as AnimatorSet
            val flipIn = AnimatorInflater.loadAnimator(binding.root.context, R.animator.card_flip_in) as AnimatorSet

            if (card.isFlipped && !wasFlippedPreviously) { // Need to track previous state
               flipOut.setTarget(binding.cardBack) // Animate out the back
               flipIn.setTarget(binding.ivShape) // Animate in the front
               flipOut.start()
               flipIn.start()
            } else if (!card.isFlipped && wasFlippedPreviously) {
                flipOut.setTarget(binding.ivShape) // Animate out the front
                flipIn.setTarget(binding.cardBack) // Animate in the back
                flipOut.start()
                flipIn.start()
            } else {
                // Set initial visibility without animation
                binding.ivShape.visibility = if(card.isFlipped || card.isMatched) View.VISIBLE else View.GONE
                binding.cardBack.visibility = if(card.isFlipped || card.isMatched) View.GONE else View.VISIBLE
            }
            */
        }
    }

    private class ShapeDiffCallback : DiffUtil.ItemCallback<ShapeMatchingViewModel.MatchingCard>() {
        override fun areItemsTheSame(
            oldItem: ShapeMatchingViewModel.MatchingCard,
            newItem: ShapeMatchingViewModel.MatchingCard
        ): Boolean {
            // Need a unique ID per card instance if multiple identical shapes exist
            // For simplicity, assume position is stable enough if list isn't drastically reordered
            // A better approach is adding a unique 'cardInstanceId' to MatchingCard
            return oldItem.shapeId == newItem.shapeId // Simplified check, might cause issues if list changes drastically
        }

        override fun areContentsTheSame(
            oldItem: ShapeMatchingViewModel.MatchingCard,
            newItem: ShapeMatchingViewModel.MatchingCard
        ): Boolean {
            // Checks all fields: isFlipped, isMatched, imageResId, shapeId
            return oldItem == newItem
        }
    }
}