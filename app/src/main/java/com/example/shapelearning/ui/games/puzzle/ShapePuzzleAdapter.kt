package com.example.shapelearning.ui.games.puzzle

import android.annotation.SuppressLint // Added
import android.graphics.Color // Added for selection feedback
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat // Added
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shapelearning.R // Added
import com.example.shapelearning.databinding.ItemPuzzleShapeBinding

class ShapePuzzleAdapter(
    private val listener: OnShapeClickListener
) : ListAdapter<ShapePuzzleViewModel.PuzzleShape, ShapePuzzleAdapter.ShapeViewHolder>(ShapeDiffCallback()) {

    interface OnShapeClickListener {
        fun onShapeClick(shape: ShapePuzzleViewModel.PuzzleShape)
    }

    // Store the ID of the currently selected shape for highlighting
    private var selectedShapeId: Int? = null

    // Method to update the selected shape ID and refresh the relevant items
    @SuppressLint("NotifyDataSetChanged") // Use specific notifyItemChanged if performance is critical
    fun setSelectedShapeId(shapeId: Int?) {
        val previousSelectedId = selectedShapeId
        selectedShapeId = shapeId
        // Refresh previous and new selected items
        // Using notifyDataSetChanged is simpler here but less efficient
        notifyDataSetChanged()
        /* More efficient way:
        previousSelectedId?.let { findPositionById(it)?.let { pos -> notifyItemChanged(pos) } }
        selectedShapeId?.let { findPositionById(it)?.let { pos -> notifyItemChanged(pos) } }
         */
    }

    // Helper to find item position by ID (needed for efficient notifyItemChanged)
    private fun findPositionById(shapeId: Int): Int? {
        return currentList.indexOfFirst { it.id == shapeId }.takeIf { it != -1 }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShapeViewHolder {
        val binding = ItemPuzzleShapeBinding.inflate(
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
        private val binding: ItemPuzzleShapeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val shape = getItem(position)
                    // Listener is called even if placed, Fragment/VM decides action
                    listener.onShapeClick(shape)
                }
            }
        }

        fun bind(shape: ShapePuzzleViewModel.PuzzleShape) {
            binding.ivShape.setImageResource(shape.imageResId)

            // Visual feedback for placed shapes
            binding.root.alpha = if (shape.isPlaced) 0.4f else 1.0f // Make placed items more faded

            // Visual feedback for selected shape
            val isSelected = shape.id == selectedShapeId && !shape.isPlaced // <<< 'val' EKLENDİ
            binding.root.setBackgroundColor(
                if (isSelected) ContextCompat.getColor(binding.root.context, R.color.selected_highlight) // Add color
                else Color.TRANSPARENT
            )

            // Content Description
            binding.root.contentDescription = binding.root.context.getString(
                if (shape.isPlaced) R.string.puzzle_shape_placed_desc
                else if (isSelected) R.string.puzzle_shape_selected_desc // isSelected değişkenini kullan
                else R.string.puzzle_shape_available_desc,
                shape.id
            ) // Add strings like "Placed shape ID %1$d", "Selected shape ID %1$d", "Available shape ID %1$d"
        }
    }

    // Use areContentsTheSame for better updates
    private class ShapeDiffCallback : DiffUtil.ItemCallback<ShapePuzzleViewModel.PuzzleShape>() {
        override fun areItemsTheSame(
            oldItem: ShapePuzzleViewModel.PuzzleShape,
            newItem: ShapePuzzleViewModel.PuzzleShape
        ): Boolean {
            return oldItem.id == newItem.id
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(
            oldItem: ShapePuzzleViewModel.PuzzleShape,
            newItem: ShapePuzzleViewModel.PuzzleShape
        ): Boolean {
            // Compare isPlaced state as well
            return oldItem == newItem
        }
    }
}