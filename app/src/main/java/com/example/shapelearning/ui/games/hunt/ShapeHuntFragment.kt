package com.example.shapelearning.ui.games.hunt

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible // Added
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.shapelearning.R
import com.example.shapelearning.databinding.FragmentShapeHuntBinding
import com.example.shapelearning.service.audio.AudioManager
import com.example.shapelearning.ui.components.ShapeHuntView // Ensure import
import javax.inject.Inject

class ShapeHuntFragment : Fragment(), ShapeHuntView.OnShapeFoundListener {

    // Implement listener
    // Binding property to access views safely
    private var _binding: FragmentShapeHuntBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShapeHuntViewModel by viewModels()
    private val args: ShapeHuntFragmentArgs by navArgs()

    @Inject
    lateinit var audioManager: AudioManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShapeHuntBinding.inflate(inflater, container, false)
        val view = binding.root

        with(binding) {
            setupUI()
            observeViewModel()
        }

        Log.d("ShapeHunt", "Loading level ID: ${args.levelId}")
        viewModel.loadLevel(args.levelId)
    }

    private fun setupUI() {
        // Add content descriptions in XML
        ivBack.setOnClickListener {
            audioManager.playSound(R.raw.button_click)
            findNavController().navigateUp()
        }

        // Set the listener on the custom view
        shapeHuntView.setOnShapeFoundListener(this)
        tvScore.text = getString(R.string.found_shapes, 0, 0)
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar?.isVisible = isLoading // Add ProgressBar
            binding.contentGroup?.isVisible = !isLoading // Group content views
        }

        viewModel.huntSceneResId.observe(viewLifecycleOwner) { sceneResId ->
            if (sceneResId != 0) {
                binding.ivHuntScene.setImageResource(sceneResId)
                binding.ivHuntScene.isVisible = true
            } else {
                Log.w("ShapeHunt", "No hunt scene resource provided.")
                binding.ivHuntScene.isVisible = false
                // Optionally show error or default background
            }
        }

        viewModel.targetShape.observe(viewLifecycleOwner) { shape ->
            if (shape != null) {
                binding.tvInstructions.text = getString(R.string.find_all_shapes, getString(shape.nameResId))
            } else {
                binding.tvInstructions.text = getString(R.string.loading_level) // Add string "Seviye yükleniyor..."
            }
        }

        // Observe the list of found shapes to update the custom view overlay
        viewModel.foundShapesList.observe(viewLifecycleOwner) { foundShapes ->
            shapeHuntView.updateFoundShapes(foundShapes ?: emptyList())

            // Update score text
            val total = viewModel.totalShapesToFind.value ?: 0 // Safe access
            val found = foundShapes?.size ?: 0
            binding.tvScore.text = getString(R.string.found_shapes, found, total)
        }

        viewModel.totalShapesToFind.observe(viewLifecycleOwner) { total ->
            // Update score text if total changes (e.g., on level load)
            val found = viewModel.foundShapesList.value?.size ?: 0
            binding.tvScore.text = getString(R.string.found_shapes, found, total ?: 0)
        }

        viewModel.shapeAlreadyFoundEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let {
                // Use Snackbar instead of Toast
                audioManager.playSound(R.raw.already_found) // Optional sound
            }
        }

        viewModel.huntCompleted.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { completed ->
                if (completed) {
                    audioManager.playSound(R.raw.level_complete)
                    showLevelCompleteDialog()
                }
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Log.e("ShapeHunt", "Error: $it")
                viewModel.onErrorShown()
            }
        }
    }

    // Called when the ShapeHuntView detects a tap (potential find)
    override fun onShapeFoundAttempt(tapPosition: Pair<Float, Float>) {
        Log.d("ShapeHunt", "Shape found attempt at: $tapPosition")
        // Let the ViewModel validate the tap against hidden shape positions
        viewModel.validateFoundShape(tapPosition)
        // Play tap sound feedback immediately?
        // audioManager.playSound(R.raw.hunt_tap)
        // Success/Failure sound played by ViewModel after validation
    }

    private fun showLevelCompleteDialog() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.level_complete_title)
            .setMessage(R.string.level_complete_message_hunt)
            .setPositiveButton(R.string.dialog_next_level) { dialog, _ ->
                // TODO: Go to next level or menu
                findNavController().navigateUp()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.dialog_replay) { dialog, _ ->
                viewModel.restartLevel() // Add restart method to VM
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}