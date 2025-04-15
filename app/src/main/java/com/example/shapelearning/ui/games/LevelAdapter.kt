package com.example.shapelearning.ui.games

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shapelearning.databinding.ItemLevelBinding
import com.example.shapelearning.data.model.Level

class LevelAdapter(
    private val onLevelClicked: (Level) -> Unit
) : ListAdapter<Level, LevelAdapter.LevelViewHolder>(LevelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
        val binding = ItemLevelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LevelViewHolder(binding, onLevelClicked)
    }

    override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LevelViewHolder(
        private val binding: ItemLevelBinding,
        private val onLevelClicked: (Level) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(level: Level) {
            binding.tvLevelNumber.text = level.id.toString()
            binding.root.isEnabled = !level.isLocked
            binding.root.setOnClickListener { onLevelClicked(level) }
        }
    }

    class LevelDiffCallback : DiffUtil.ItemCallback<Level>() {
        override fun areItemsTheSame(oldItem: Level, newItem: Level) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Level, newItem: Level) = oldItem == newItem
    }
}