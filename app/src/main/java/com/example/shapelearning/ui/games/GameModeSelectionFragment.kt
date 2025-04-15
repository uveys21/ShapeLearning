package com.example.shapelearning.ui.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.shapelearning.R // Drawable kaynakları için
import com.example.shapelearning.databinding.FragmentGameModeSelectionBinding

class GameModeSelectionFragment : Fragment() {

    private var _binding: FragmentGameModeSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var gameModeAdapter: GameModeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameModeSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadGameModes() // Örnek veriyi yükle
    }

    private fun setupRecyclerView() {
        gameModeAdapter = GameModeAdapter { selectedGameMode ->
            navigateToLevelSelection(selectedGameMode.id)
        }
        binding.rvGameModes.adapter = gameModeAdapter
        // LayoutManager XML'de tanımlı varsayıldı
    }

    private fun loadGameModes() {
        // TODO: Veriyi ViewModel'dan almalısınız!
        // Örnek veri - İkonların projenizde var olduğundan emin olun
        val gameModes = listOf(
            // XML'deki tools:src="@drawable/ic_game_discovery" ile eşleşen bir ikon kullanın
            GameMode(1, "Şekil Keşfi", "Şekilleri keşfet ve özelliklerini öğren", R.drawable.ic_game_discovery), // <-- Örnek ikon adı
            // Diğer oyun modları için de uygun ikonları ekleyin...
            GameMode(2, "Şekil İzleme", "Noktaları birleştirerek şekilleri çiz.", R.drawable.ic_game_tracing), // İkon adlarını güncelleyin
            GameMode(3, "Şekil Eşleştirme", "Aynı şekilleri bul ve eşleştir.", R.drawable.ic_game_matching),
            GameMode(4, "Şekil Sıralama", "Şekilleri istenen kategoriye taşı.", R.drawable.ic_game_sorting),
            GameMode(5, "Şekil Yapboz", "Parçaları birleştirerek şekli tamamla.", R.drawable.ic_game_puzzle),
            GameMode(6, "Şekil Avı", "Ortamdaki istenen şekli bul.", R.drawable.hunt_scene)
        )
        gameModeAdapter.submitList(gameModes)
    }

    private fun navigateToLevelSelection(gameModeId: Int) {
        // Navigation Component Safe Args kullanılıyor varsayıldı
        val action = GameModeSelectionFragmentDirections.actionGameModeSelectionFragmentToLevelSelectionFragment(gameModeId)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}