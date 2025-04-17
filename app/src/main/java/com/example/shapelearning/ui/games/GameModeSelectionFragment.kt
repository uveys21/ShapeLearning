package com.example.shapelearning.ui.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.shapelearning.databinding.FragmentGameModeSelectionBinding

class GameModeSelectionFragment : Fragment() {

    private var _binding: FragmentGameModeSelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GameModeSelectionViewModel by viewModels()
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

        binding?.apply {
            setupRecyclerView()
        }

        gameModeAdapter = GameModeAdapter { selectedGameMode -> navigateToLevelSelection(selectedGameMode.id) }

        binding.rvGameModes.adapter = gameModeAdapter
        // LayoutManager XML'de tanımlı varsayıldı

        viewModel.gameModes.observe(viewLifecycleOwner) { gameModes ->
            gameModeAdapter.submitList(gameModes)
        }
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