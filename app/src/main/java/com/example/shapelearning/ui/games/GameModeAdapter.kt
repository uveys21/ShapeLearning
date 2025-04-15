package com.example.shapelearning.ui.games

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shapelearning.R // Gerekli olabilir
import com.example.shapelearning.databinding.ItemGameModeBinding // item_game_mode.xml için Binding

// GameMode veri sınıfı (Bu sınıfın projenizde uygun bir yerde tanımlı olduğundan emin olun)
// Önceki örnektekiyle aynı kalabilir:
data class GameMode(
    val id: Int,
    val name: String, // tvGameModeTitle için
    val description: String, // tvGameModeDescription için
    val iconResId: Int // ivGameModeIcon için
)

class GameModeAdapter(
    private val onItemClicked: (GameMode) -> Unit
) : ListAdapter<GameMode, GameModeAdapter.GameModeViewHolder>(GameModeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameModeViewHolder {
        val binding = ItemGameModeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GameModeViewHolder(binding, onItemClicked)
    }

    override fun onBindViewHolder(holder: GameModeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class GameModeViewHolder(
        private val binding: ItemGameModeBinding,
        private val onItemClicked: (GameMode) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) { // binding.root CardView'a karşılık gelir

        fun bind(gameMode: GameMode) {
            // XML'deki ID'lere göre verileri bağla
            binding.ivGameModeIcon.setImageResource(gameMode.iconResId)
            binding.tvGameModeTitle.text = gameMode.name           // <<<--- ID GÜNCELLENDİ
            binding.tvGameModeDescription.text = gameMode.description // <<<--- ID GÜNCELLENDİ

            // Tıklama dinleyicisini CardView'ın root'una (itemView) ekle
            binding.root.setOnClickListener {
                onItemClicked(gameMode)
            }
            // Erişilebilirlik için Content Description
            // Başlık ve açıklamayı birleştirmek iyi bir pratik
            binding.root.contentDescription = "${gameMode.name}. ${gameMode.description}"
        }
    }

    // DiffUtil aynı kalabilir
    class GameModeDiffCallback : DiffUtil.ItemCallback<GameMode>() {
        override fun areItemsTheSame(oldItem: GameMode, newItem: GameMode): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GameMode, newItem: GameMode): Boolean {
            return oldItem == newItem
        }
    }
}