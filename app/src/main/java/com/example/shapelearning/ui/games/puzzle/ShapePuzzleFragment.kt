package com.example.shapelearning.ui.games.puzzle // <<<--- Doğru paketi kontrol edin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // Eğer ViewModel kullanıyorsanız
import com.example.shapelearning.databinding.FragmentShapePuzzleBinding // <<<--- Doğru layout dosyasını kontrol edin

class ShapePuzzleFragment : Fragment() { // <<<--- Doğru sınıf adı ve miras

    private var _binding: FragmentShapePuzzleBinding? = null
    // Bu özellik yalnızca onCreateView ve onDestroyView arasında geçerlidir.
    private val binding get() = _binding!!

    // Eğer ViewModel kullanıyorsanız:
    // private val viewModel: ShapePuzzleViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShapePuzzleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: Buraya ShapePuzzleFragment'a özel kodlarınızı ekleyin
        // Örneğin:
        // - ViewModel'ı observe etme
        // - RecyclerView (varsa) ve Adapter'ı (ShapePuzzleAdapter?) ayarlama
        // - Tıklama dinleyicilerini ayarlama
        // - Puzzle mantığını başlatma

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Bellek sızıntılarını önlemek için binding'i temizle
    }
}