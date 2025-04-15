package com.example.shapelearning.ui.games.matching

import android.app.AlertDialog // Added for completion dialog
import android.os.Bundle
import android.util.Log // Added
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // Added
import androidx.core.view.isVisible // Added
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView // Added for ItemDecoration
import com.example.shapelearning.R
import com.example.shapelearning.databinding.FragmentShapeMatchingBinding
import com.example.shapelearning.service.audio.AudioManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.shapelearning.utils.GridSpacingItemDecoration // Added (Create this utility class)

@AndroidEntryPoint
class ShapeMatchingFragment : Fragment(), ShapeMatchingAdapter.OnShapeClickListener {

    private var _binding: FragmentShapeMatchingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShapeMatchingViewModel by viewModels()
    private val args: ShapeMatchingFragmentArgs by navArgs()

    private lateinit var adapter: ShapeMatchingAdapter

    @Inject
    lateinit var audioManager: AudioManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShapeMatchingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()
        observeViewModel()

        Log.d("ShapeMatching", "Loading level ID: ${args.levelId}")
        viewModel.loadLevel(args.levelId)
    }

    private fun setupUI() {
        // Add content descriptions in XML

        binding.ivBack.setOnClickListener {
            audioManager.playSound(R.raw.button_click)
            findNavController().navigateUp()
        }
        // Initially hide score? Or set default text
        binding.tvScore.text = getString(R.string.score, 0) // Ensure string "Skor: %1$d" exists
    }

    private fun setupRecyclerView() {
        adapter = ShapeMatchingAdapter(this)
        // Calculate span count based on screen size or use a fixed value
        val spanCount = 4 // Adjust as needed
        binding.rvShapes.layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.rvShapes.adapter = adapter
        // Add spacing between grid items (Create GridSpacingItemDecoration utility class)
        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.grid_spacing) // Add dimens.xml entry
        binding.rvShapes.addItemDecoration(GridSpacingItemDecoration(spanCount, spacingInPixels, true))
        binding.rvShapes.setHasFixedSize(true) // Optimization if item size doesn't change
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar?.isVisible = isLoading // Add ProgressBar
            binding.rvShapes.isVisible = !isLoading
            binding.tvScore.isVisible = !isLoading
        }

        viewModel.matchingCards.observe(viewLifecycleOwner) { cards ->
            adapter.submitList(cards)
            Log.d("ShapeMatching", "Adapter submitted list with ${cards.size} cards.")
        }

        viewModel.score.observe(viewLifecycleOwner) { score ->
            binding.tvScore.text = getString(R.string.score, score)
        }

        viewModel.gameCompleted.observe(viewLifecycleOwner) { event -> // event burada Event<Boolean>? tipindedir
            event?.getContentIfNotHandled()?.let { completed -> // İçeriği al ve işlenmediyse çalıştır
                if (completed) { // completed artık Boolean tipindedir
                    Log.d("ShapeMatching", "Game completed!")
                    audioManager.playSound(R.raw.level_complete) // Ensure resource exists
                    showLevelCompleteDialog()
                    // viewModel.onGameCompleteShown() // Bu satıra artık gerek yok, getContentIfNotHandled zaten işaretliyor
                }
            }
        }

        // Observe match result for immediate audio feedback
        viewModel.matchResultEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { result -> // Use event wrapper
                when (result) {
                    ShapeMatchingViewModel.MatchResult.MATCH -> {
                        audioManager.playSound(R.raw.match_success) // Ensure resource exists
                    }
                    ShapeMatchingViewModel.MatchResult.NO_MATCH -> {
                        audioManager.playSound(R.raw.match_fail) // Ensure resource exists
                    }
                    ShapeMatchingViewModel.MatchResult.FIRST_SELECTION -> {
                        // Optional sound for first flip
                        // audioManager.playSound(R.raw.card_flip)
                    }
                }
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Log.e("ShapeMatching", "Error: $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.onErrorShown()
            }
        }
    }

    override fun onShapeClick(position: Int) {
        // Let the ViewModel handle the click logic
        viewModel.onCardClicked(position)
    }

    private fun showLevelCompleteDialog() {
        // Use requireContext() safely within fragment's lifecycle
        if (!isAdded) return // Check if fragment is attached

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.level_complete_title) // Add string resource
            .setMessage(getString(R.string.level_complete_message, viewModel.score.value ?: 0)) // Add string "Seviye tamamlandı! Skorunuz: %1$d"
            .setPositiveButton(R.string.dialog_next_level) { dialog, _ -> // Add string "Sonraki Seviye" or "Ana Menü"
                // TODO: Implement logic to go to next level or back to menu
                findNavController().navigateUp() // Simple navigate back for now
                dialog.dismiss()
            }
            .setNegativeButton(R.string.dialog_replay) { dialog, _ -> // Add string "Tekrar Oyna"
                viewModel.restartLevel() // Add restartLevel function to ViewModel
                dialog.dismiss()
            }
            .setCancelable(false) // Prevent dismissing by tapping outside
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvShapes.adapter = null // Clear adapter reference
        _binding = null
    }
}