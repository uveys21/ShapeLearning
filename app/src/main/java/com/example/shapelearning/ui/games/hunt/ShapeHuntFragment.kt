package com.example.shapelearning.ui.games.hunt

import android.app.AlertDialog // Added
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
import com.example.shapelearning.R
import com.example.shapelearning.databinding.FragmentShapeHuntBinding
import com.example.shapelearning.service.audio.AudioManager
import com.example.shapelearning.ui.components.ShapeHuntView // Ensure import
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShapeHuntFragment : Fragment(), ShapeHuntView.OnShapeFoundListener { // Implement listener

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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()

        Log.d("ShapeHunt", "Loading level ID: ${args.levelId}")
        viewModel.loadLevel(args.levelId)
    }

    private fun setupUI() {
        // Add content descriptions in XML

        binding.ivBack.setOnClickListener {
            audioManager.playSound(R.raw.button_click)
            findNavController().navigateUp()
        }

        // Set the listener on the custom view
        binding.shapeHuntView.setOnShapeFoundListener(this) // Fragment handles tap events

        // Set initial score text
        binding.tvScore.text = getString(R.string.found_shapes, 0, 0) // Add string "Bulunan: %1$d / %2$d"
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar?.isVisible = isLoading // Add ProgressBar
            binding.contentGroup?.isVisible = !isLoading // Group content views
        }

        viewModel.huntSceneResId.observe(viewLifecycleOwner) { sceneResId ->
            if (sceneResId != null && sceneResId != 0) {
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
                // Update instructions using the shape name from resources
                binding.tvInstructions.text = getString(R.string.find_all_shapes, getString(shape.nameResId)) // Add string "Tüm %1$s şekillerini bul!"
                // Pass target shape ID to the custom view if it needs it (it currently doesn't)
                // binding.shapeHuntView.setTargetShapeId(shape.id)
            } else {
                binding.tvInstructions.text = getString(R.string.loading_level) // Add string "Seviye yükleniyor..."
            }
        }

        // Observe the list of found shapes to update the custom view overlay
        viewModel.foundShapesList.observe(viewLifecycleOwner) { foundShapes ->
            binding.shapeHuntView.updateFoundShapes(foundShapes ?: emptyList())
            Log.d("ShapeHunt", "Found shapes updated: ${foundShapes?.size ?: 0}")

            // Update score text
            val total = viewModel.totalShapesToFind.value ?: 0
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
                // Optional feedback if user clicks an already found spot
                Toast.makeText(requireContext(), R.string.hunt_already_found, Toast.LENGTH_SHORT).show() // Add string "Bunu zaten buldun!"
                audioManager.playSound(R.raw.already_found) // Optional sound
            }
        }

        viewModel.huntCompleted.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { completed ->
                if (completed) {
                    Log.d("ShapeHunt", "Hunt completed!")
                    audioManager.playSound(R.raw.level_complete)
                    showLevelCompleteDialog()
                }
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Log.e("ShapeHunt", "Error: $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
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
            .setMessage(R.string.level_complete_message_hunt) // Add string "Hepsini buldun!"
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