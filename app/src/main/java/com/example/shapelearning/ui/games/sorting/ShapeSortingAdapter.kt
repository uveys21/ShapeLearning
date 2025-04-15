package com.example.shapelearning.ui.games.sorting

import android.annotation.SuppressLint // Added for DiffUtil warning
import android.view.LayoutInflater
import android.view.ViewGroup
// import android.view.View // Removed (using root directly)
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shapelearning.R // Added
import com.example.shapelearning.databinding.ItemSortableShapeBinding

// Modified: Removed listener interface, added callback to start drag
class ShapeSortingAdapter(
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit // Callback to trigger drag
) : ListAdapter<ShapeSortingViewModel.SortableShape, ShapeSortingAdapter.ShapeViewHolder>(ShapeDiffCallback()) {

    // Removed OnShapeDragListener interface

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShapeViewHolder {
        val binding = ItemSortableShapeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        // Pass the drag callback to the ViewHolder
        return ShapeViewHolder(binding, onStartDrag)
    }

    override fun onBindViewHolder(holder: ShapeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Modified ViewHolder
    inner class ShapeViewHolder(
        private val binding: ItemSortableShapeBinding,
        private val startDragCallback: (RecyclerView.ViewHolder) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Start drag on long press (standard behavior)
            binding.root.setOnLongClickListener {
                startDragCallback(this) // Pass `this` ViewHolder to the callback
                true // Consume the long click
            }
            // Optional: Start drag on simple click/touch down for easier interaction for kids
            /*
             binding.root.setOnTouchListener { _, event ->
                 if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                     startDragCallback(this)
                 }
                 false // Let other listeners (like click) potentially handle it too
             }
            */
        }

        // Removed startDrag method - logic moved to Fragment's ItemTouchHelper

        fun bind(shape: ShapeSortingViewModel.SortableShape) {
            binding.ivShape.setImageResource(shape.imageResId)
            // Add content description (e.g., based on shape ID or properties)
            binding.root.contentDescription = binding.root.context.getString(R.string.sortable_shape_desc, shape.id) // Add string "Sortable shape ID %1$d"
        }
    }

    // Use areContentsTheSame for better updates when list changes
    private class ShapeDiffCallback : DiffUtil.ItemCallback<ShapeSortingViewModel.SortableShape>() {
        override fun areItemsTheSame(
            oldItem: ShapeSortingViewModel.SortableShape,
            newItem: ShapeSortingViewModel.SortableShape
        ): Boolean {
            // Use the unique ID from the original Shape model
            return oldItem.id == newItem.id
        }

        @SuppressLint("DiffUtilEquals") // Suppress warning if equals checks all relevant fields
        override fun areContentsTheSame(
            oldItem: ShapeSortingViewModel.SortableShape,
            newItem: ShapeSortingViewModel.SortableShape
        ): Boolean {
            // Compare all relevant properties of SortableShape
            return oldItem == newItem
        }
    }

    companion object {
        // Constants to identify the list types, used by ViewModel and Fragment
        const val SHAPES_SOURCE = 0
        const val CATEGORY_1 = 1
        const val CATEGORY_2 = 2
    }
}