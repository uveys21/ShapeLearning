package com.example.shapelearning.ui.games.discovery

import android.os.Bundle
import android.util.Log // Added
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible // Added
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.shapelearning.R
import com.example.shapelearning.data.model.Shape // Use correct Shape model
import com.example.shapelearning.databinding.FragmentShapeDiscoveryBinding
import com.example.shapelearning.service.audio.AudioManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShapeDiscoveryFragment : Fragment() {

    private var _binding: FragmentShapeDiscoveryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShapeDiscoveryViewModel by viewModels()
    private val args: ShapeDiscoveryFragmentArgs by navArgs()

    @Inject
    lateinit var audioManager: AudioManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShapeDiscoveryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()

        // Pass level ID to ViewModel
        Log.d("ShapeDiscovery", "Loading level ID: ${args.levelId}")
        viewModel.loadLevel(args.levelId)
    }

    private fun setupUI() {
        // Add content descriptions to ImageViews and Buttons in XML

        binding.ivBack.setOnClickListener {
            audioManager.playSound(R.raw.button_click)
            findNavController().navigateUp()
        }

        // Audio button plays sound for the current shape
        binding.btnAudio.setOnClickListener {
            viewModel.currentShape.value?.let { shape ->
                // Use soundResId from Shape model if available, otherwise fallback
                shape.soundResId?.let { soundId ->
                    audioManager.playSound(soundId)
                } ?: run {
                    // Fallback if soundResId is not defined in Shape model/DB
                    Log.w("ShapeDiscovery", "SoundResId not found for shape ${shape.id}, playing default.")
                    // audioManager.playSound(getFallbackShapeSoundResource(shape.id)) // Use fallback if needed
                }
            }
        }

        binding.btnPrevious.setOnClickListener {
            audioManager.playSound(R.raw.button_click)
            viewModel.previousShape()
        }

        binding.btnNext.setOnClickListener {
            audioManager.playSound(R.raw.button_click)
            viewModel.nextShape()
        }

        binding.btnRotate.setOnClickListener {
            audioManager.playSound(R.raw.shape_rotate) // Ensure resource exists
            rotateShape()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Show/hide loading indicator (add ProgressBar to XML)
            binding.progressBar?.isVisible = isLoading // Assuming progressBar exists
            binding.contentGroup?.isVisible = !isLoading // Assuming content is in a group
        }

        viewModel.currentShape.observe(viewLifecycleOwner) { shape ->
            if (shape != null) {
                updateShapeUI(shape)
                // Auto-play sound when shape changes (optional)
                shape.soundResId?.let { audioManager.playSound(it) }
            } else {
                // Handle case where shape is null (e.g., show error message)
                Log.e("ShapeDiscovery", "Current shape is null.")
                // Optionally show an error view or navigate back
            }
        }

        viewModel.isFirstShape.observe(viewLifecycleOwner) { isFirst ->
            binding.btnPrevious.isEnabled = !isFirst
            binding.btnPrevious.alpha = if (isFirst) 0.5f else 1.0f
        }

        viewModel.isLastShape.observe(viewLifecycleOwner) { isLast ->
            binding.btnNext.isEnabled = !isLast
            binding.btnNext.alpha = if (isLast) 0.5f else 1.0f
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                // Show error message to the user (e.g., using a Toast or Snackbar)
                Log.e("ShapeDiscovery", "Error observed: $it")
                // Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.onErrorShown() // Notify ViewModel error has been shown
            }
        }
    }

    private fun updateShapeUI(shape: Shape) {
        binding.tvShapeName.text = getString(shape.nameResId)
        binding.ivShape.setImageResource(shape.imageResId)
        // Consider adding placeholder/error drawables for Glide/Coil if loading from network
        binding.tvShapeDescription.text = getString(shape.descriptionResId)
        binding.tvShapeCorners.text = getString(R.string.shape_corners, shape.corners) // Ensure string exists
        binding.tvShapeSides.text = getString(R.string.shape_sides, shape.sides) // Ensure string exists
        binding.ivRealLifeExample.setImageResource(shape.realLifeImageResId)
    }


    private fun rotateShape() {
        try {
            val rotateAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_360) // Ensure anim exists
            binding.ivShape.startAnimation(rotateAnimation)
        } catch (e: Exception) {
            Log.e("ShapeDiscovery", "Error loading or starting rotation animation", e)
        }
    }

    // Removed getShapeSoundResource as logic moved to use shape.soundResId

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}