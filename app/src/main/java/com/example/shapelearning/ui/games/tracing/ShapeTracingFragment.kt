package com.example.shapelearning.ui.games.tracing

import android.os.Bundle
import android.util.Log // Added
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // Added for feedback
import androidx.core.view.isVisible // Added
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.shapelearning.R
import com.example.shapelearning.databinding.FragmentShapeTracingBinding
import com.example.shapelearning.service.audio.AudioManager
import com.example.shapelearning.ui.components.TracingView // Ensure import
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShapeTracingFragment : Fragment() {

    private var _binding: FragmentShapeTracingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShapeTracingViewModel by viewModels()
    private val args: ShapeTracingFragmentArgs by navArgs()

    @Inject
    lateinit var audioManager: AudioManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShapeTracingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()

        Log.d("ShapeTracing", "Loading level ID: ${args.levelId}")
        viewModel.loadLevel(args.levelId)
    }

    private fun setupUI() {
        // Add content descriptions to Buttons/ImageViews in XML

        binding.ivBack.setOnClickListener {
            audioManager.playSound(R.raw.button_click)
            findNavController().navigateUp()
        }

        binding.btnClear.setOnClickListener {
            audioManager.playSound(R.raw.button_click) // Maybe a different sound?
            binding.tracingView.clearDrawing()
        }

        binding.btnCheck.setOnClickListener {
            audioManager.playSound(R.raw.button_click)
            checkTracing()
        }

        // Optional: Listener for tracing completion on the view itself
        /*
        binding.tracingView.setOnTracingCompleteListener { path ->
             viewModel.checkTracing(path)
        }
        */
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar?.isVisible = isLoading // Add ProgressBar to XML
            binding.contentGroup?.isVisible = !isLoading // Group content views
        }

        viewModel.currentShape.observe(viewLifecycleOwner) { shape ->
            if (shape != null) {
                binding.tvShapeName.text = getString(shape.nameResId)
                // Set the outline image behind the tracing view
                binding.ivShapeOutline.setImageResource(shape.outlineImageResId)
                // Pass outline resource or data to TracingView if needed for validation
                binding.tracingView.setShapeOutlineData(shape.outlineImageResId) // Modify TracingView if needed
                binding.tracingView.clearDrawing() // Clear previous drawing
                binding.btnCheck.isEnabled = true // Enable check button
            } else {
                binding.tvShapeName.text = ""
                binding.ivShapeOutline.setImageDrawable(null)
                binding.tracingView.clearDrawing()
                binding.btnCheck.isEnabled = false // Disable check if no shape
            }
        }

        viewModel.tracingResult.observe(viewLifecycleOwner) { result ->
            // result is now accuracy score (Float?)
            if (result != null) {
                handleTracingResult(result)
                viewModel.onResultShown() // Notify VM result is handled
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Log.e("ShapeTracing", "Error: $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.onErrorShown()
            }
        }

        viewModel.progressSaved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                Log.d("ShapeTracing", "Progress saved successfully.")
                // Optionally navigate back or to next level after a delay
                // findNavController().navigateUp()
            }
        }
    }

    private fun checkTracing() {
        val tracingPath = binding.tracingView.getTracingPath()
        if (tracingPath.size < 5) { // Basic check: Don't check if path is too short
            Toast.makeText(requireContext(), R.string.tracing_too_short, Toast.LENGTH_SHORT).show() // Add string resource
            return
        }
        binding.btnCheck.isEnabled = false // Disable check button while checking
        viewModel.checkTracing(tracingPath)
    }

    private fun handleTracingResult(accuracy: Float) {
        binding.btnCheck.isEnabled = true // Re-enable check button
        val successThreshold = 0.7f // Example threshold

        if (accuracy >= successThreshold) {
            audioManager.playSound(R.raw.success) // Ensure resource exists
            showFeedback(true, accuracy)
            viewModel.saveProgress(accuracy) // Pass accuracy to save correct stars
        } else {
            audioManager.playSound(R.raw.failure) // Ensure resource exists
            showFeedback(false, accuracy)
            // Optionally clear the drawing automatically on failure after a delay
            // binding.tracingView.postDelayed({ binding.tracingView.clearDrawing() }, 1000)
        }
    }

    private fun showFeedback(isSuccess: Boolean, accuracy: Float) {
        // TODO: Implement better visual feedback (animations, score display)
        val percentage = (accuracy * 100).toInt()
        val message = if (isSuccess) {
            getString(R.string.tracing_success_feedback, percentage) // e.g., "Başarılı! Doğruluk: %1$d%%"
        } else {
            getString(R.string.tracing_failure_feedback, percentage) // e.g., "Tekrar Dene! Doğruluk: %1$d%%"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

        // Example: Change background or show an overlay
        // binding.feedbackOverlay?.setBackgroundColor(if (isSuccess) Color.GREEN else Color.RED)
        // binding.feedbackOverlay?.isVisible = true
        // binding.feedbackOverlay?.postDelayed({ binding.feedbackOverlay?.isVisible = false }, 1500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}