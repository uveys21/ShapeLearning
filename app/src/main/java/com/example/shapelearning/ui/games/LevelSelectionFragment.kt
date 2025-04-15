package com.example.shapelearning.ui.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shapelearning.R
import com.example.shapelearning.data.model.Difficulty
import com.example.shapelearning.data.model.GameMode
import com.example.shapelearning.data.model.Level
import com.example.shapelearning.databinding.FragmentLevelSelectionBinding

class LevelSelectionFragment : Fragment() {

    private var _binding: FragmentLevelSelectionBinding? = null
    private val binding get() = _binding!!
    private lateinit var levelAdapter: LevelAdapter
    private val args: LevelSelectionFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLevelSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val gameModeId = args.gameModeId
        val gameMode = GameMode.values().getOrNull(gameModeId) ?: GameMode.DISCOVERY

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        setupRecyclerView()
        loadLevels(gameMode)
    }


    private fun loadLevels(gameMode: GameMode) {
        val levels = listOf(
            // Discovery Modu (Kolay, Orta, Zor)
            Level(
                id = 1,
                nameResId = R.string.level_1,
                gameMode = GameMode.DISCOVERY,
                difficulty = Difficulty.EASY,
                isLocked = false,
                requiredScore = 0,
                shapes = listOf(1, 2),
                backgroundImageResId = R.drawable.bg_discovery_easy,
                huntSceneResId = null,
                shapePositionsJson = null
            ),
            Level(
                id = 2,
                nameResId = R.string.level_2,
                gameMode = GameMode.DISCOVERY,
                difficulty = Difficulty.MEDIUM,
                isLocked = true,
                requiredScore = 50,
                shapes = listOf(3, 4),
                backgroundImageResId = R.drawable.bg_discovery_medium,
                huntSceneResId = null,
                shapePositionsJson = null
            ),
            Level(
                id = 3,
                nameResId = R.string.level_3,
                gameMode = GameMode.DISCOVERY,
                difficulty = Difficulty.HARD,
                isLocked = true,
                requiredScore = 100,
                shapes = listOf(5, 6),
                backgroundImageResId = R.drawable.bg_discovery_hard,
                huntSceneResId = null,
                shapePositionsJson = null
            ),

            // Tracing Modu (Kolay, Orta, Zor)
            Level(
                id = 4,
                nameResId = R.string.level_1,
                gameMode = GameMode.TRACING,
                difficulty = Difficulty.EASY,
                isLocked = false,
                requiredScore = 0,
                shapes = listOf(7, 8),
                backgroundImageResId = R.drawable.bg_tracing_easy,
                huntSceneResId = null,
                shapePositionsJson = null
            ),
            Level(
                id = 5,
                nameResId = R.string.level_2,
                gameMode = GameMode.TRACING,
                difficulty = Difficulty.MEDIUM,
                isLocked = true,
                requiredScore = 50,
                shapes = listOf(9, 10),
                backgroundImageResId = R.drawable.bg_tracing_medium,
                huntSceneResId = null,
                shapePositionsJson = null
            ),
            Level(
                id = 6,
                nameResId = R.string.level_3,
                gameMode = GameMode.TRACING,
                difficulty = Difficulty.HARD,
                isLocked = true,
                requiredScore = 100,
                shapes = listOf(11, 12),
                backgroundImageResId = R.drawable.bg_tracing_hard,
                huntSceneResId = null,
                shapePositionsJson = null
            ),

            // Matching Modu (Kolay, Orta, Zor)
            Level(
                id = 7,
                nameResId = R.string.level_1,
                gameMode = GameMode.MATCHING,
                difficulty = Difficulty.EASY,
                isLocked = false,
                requiredScore = 0,
                shapes = listOf(13, 14),
                backgroundImageResId = R.drawable.bg_matching_easy,
                huntSceneResId = null,
                shapePositionsJson = null
            ),
            Level(
                id = 8,
                nameResId = R.string.level_2,
                gameMode = GameMode.MATCHING,
                difficulty = Difficulty.MEDIUM,
                isLocked = true,
                requiredScore = 50,
                shapes = listOf(15, 16),
                backgroundImageResId = R.drawable.bg_matching_medium,
                huntSceneResId = null,
                shapePositionsJson = null
            ),
            Level(
                id = 9,
                nameResId = R.string.level_3,
                gameMode = GameMode.MATCHING,
                difficulty = Difficulty.HARD,
                isLocked = true,
                requiredScore = 100,
                shapes = listOf(17, 18),
                backgroundImageResId = R.drawable.bg_matching_hard,
                huntSceneResId = null,
                shapePositionsJson = null
            ),

            // Sorting Modu (Kolay, Orta, Zor)
            Level(
                id = 10,
                nameResId = R.string.level_1,
                gameMode = GameMode.SORTING,
                difficulty = Difficulty.EASY,
                isLocked = false,
                requiredScore = 0,
                shapes = listOf(19, 20),
                backgroundImageResId = R.drawable.bg_sorting_easy,
                huntSceneResId = null,
                shapePositionsJson = null
            ),
            Level(
                id = 11,
                nameResId = R.string.level_2,
                gameMode = GameMode.SORTING,
                difficulty = Difficulty.MEDIUM,
                isLocked = true,
                requiredScore = 50,
                shapes = listOf(21, 22),
                backgroundImageResId = R.drawable.bg_sorting_medium,
                huntSceneResId = null,
                shapePositionsJson = null
            ),
            Level(
                id = 12,
                nameResId = R.string.level_3,
                gameMode = GameMode.SORTING,
                difficulty = Difficulty.HARD,
                isLocked = true,
                requiredScore = 100,
                shapes = listOf(23, 24),
                backgroundImageResId = R.drawable.bg_sorting_hard,
                huntSceneResId = null,
                shapePositionsJson = null
            ),

            // Puzzle Modu (Kolay, Orta, Zor)
            Level(
                id = 13,
                nameResId = R.string.level_1,
                gameMode = GameMode.PUZZLE,
                difficulty = Difficulty.EASY,
                isLocked = false,
                requiredScore = 0,
                shapes = listOf(25, 26),
                backgroundImageResId = R.drawable.bg_puzzle_easy,
                huntSceneResId = null,
                shapePositionsJson = null
            ),
            Level(
                id = 14,
                nameResId = R.string.level_2,
                gameMode = GameMode.PUZZLE,
                difficulty = Difficulty.MEDIUM,
                isLocked = true,
                requiredScore = 50,
                shapes = listOf(27, 28),
                backgroundImageResId = R.drawable.bg_puzzle_medium,
                huntSceneResId = null,
                shapePositionsJson = null
            ),
            Level(
                id = 15,
                nameResId = R.string.level_3,
                gameMode = GameMode.PUZZLE,
                difficulty = Difficulty.HARD,
                isLocked = true,
                requiredScore = 100,
                shapes = listOf(29, 30),
                backgroundImageResId = R.drawable.bg_puzzle_hard,
                huntSceneResId = null,
                shapePositionsJson = null
            ),

            // Hunt Modu (Kolay, Orta, Zor)
            Level(
                id = 16,
                nameResId = R.string.level_1,
                gameMode = GameMode.HUNT,
                difficulty = Difficulty.EASY,
                isLocked = false,
                requiredScore = 0,
                shapes = listOf(31, 32),
                backgroundImageResId = null,
                huntSceneResId = R.drawable.scene_hunt_easy,
                shapePositionsJson = null
            ),
            Level(
                id = 17,
                nameResId = R.string.level_2,
                gameMode = GameMode.HUNT,
                difficulty = Difficulty.MEDIUM,
                isLocked = true,
                requiredScore = 50,
                shapes = listOf(33, 34),
                backgroundImageResId = null,
                huntSceneResId = R.drawable.scene_hunt_medium,
                shapePositionsJson = null
            ),
            Level(
                id = 18,
                nameResId = R.string.level_3,
                gameMode = GameMode.HUNT,
                difficulty = Difficulty.HARD,
                isLocked = true,
                requiredScore = 100,
                shapes = listOf(35, 36),
                backgroundImageResId = null,
                huntSceneResId = R.drawable.scene_hunt_hard,
                shapePositionsJson = null
            )
        )
        levelAdapter.submitList(levels.filter { it.gameMode == gameMode })
    }
    // LevelSelectionFragment.kt içinde
    private fun setupRecyclerView() {
        levelAdapter = LevelAdapter { selectedLevel ->
            // GameMode'a göre uygun fragment'a yönlendir
            when (selectedLevel.gameMode) {
                GameMode.DISCOVERY -> navigateToShapeDiscovery(selectedLevel.id)
                GameMode.TRACING -> navigateToShapeTracing(selectedLevel.id)
                GameMode.MATCHING -> navigateToShapeMatching(selectedLevel.id)
                GameMode.SORTING -> navigateToShapeSorting(selectedLevel.id)
                GameMode.PUZZLE -> navigateToShapePuzzle(selectedLevel.id)
                GameMode.HUNT -> navigateToShapeHunt(selectedLevel.id)
                else -> {} // Varsayılan durum
            }
        }
        binding.rvLevels.adapter = levelAdapter
        binding.rvLevels.layoutManager = GridLayoutManager(requireContext(), 3)
    }

    // Navigation fonksiyonları
    private fun navigateToShapeDiscovery(levelId: Int) {
        val action = LevelSelectionFragmentDirections.actionLevelSelectionFragmentToShapeDiscoveryFragment(levelId)
        findNavController().navigate(action)
    }

    private fun navigateToShapeTracing(levelId: Int) {
        val action = LevelSelectionFragmentDirections.actionLevelSelectionFragmentToShapeTracingFragment(levelId)
        findNavController().navigate(action)
    }

    private fun navigateToShapeMatching(levelId: Int) {
        val action = LevelSelectionFragmentDirections.actionLevelSelectionFragmentToShapeTracingFragment(levelId)
        findNavController().navigate(action)
    }

    private fun navigateToShapeSorting(levelId: Int) {
        val action = LevelSelectionFragmentDirections.actionLevelSelectionFragmentToShapeTracingFragment(levelId)
        findNavController().navigate(action)
    }

    private fun navigateToShapePuzzle(levelId: Int) {
        val action = LevelSelectionFragmentDirections.actionLevelSelectionFragmentToShapeTracingFragment(levelId)
        findNavController().navigate(action)
    }

    private fun navigateToShapeHunt(levelId: Int) {
        val action = LevelSelectionFragmentDirections.actionLevelSelectionFragmentToShapeTracingFragment(levelId)
        findNavController().navigate(action)
    }

// Diğer modlar için benzer fonksiyonlar...

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}