package com.example.shapelearning.ui.games.tracing

import android.os.Bundle
import android.util.Log // Added
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
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

        viewModel.loadLevel(ShapeTracingFragmentArgs.fromBundle(requireArguments()).levelId)
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
        // Add content descriptions to Buttons/ImageViews in XML
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar?.isVisible = isLoading // Add ProgressBar to XML
            binding.contentGroup?.isVisible = !isLoading // Group content views
        }

        viewModel.currentShape.observe(viewLifecycleOwner) { currentShape ->
            currentShape?.let { shape ->
                binding.tvShapeName.text = getString(it.nameResId)
                binding.ivShapeOutline.setImageResource(it.outlineImageResId)
                binding.tracingView.setShapeOutlineData(it.outlineImageResId)
                binding.tracingView.clearDrawing() // Clear previous drawing
                binding.btnCheck.isEnabled = true // Enable check button

            } else {
                binding.tvShapeName.text = ""
                binding.ivShapeOutline.setImageDrawable(null)
                binding.tracingView.clearDrawing()
                binding.btnCheck.isEnabled = false // Disable check if no shape
            }
        }

        viewModel.tracingResult.observe(viewLifecycleOwner) { accuracy ->
            accuracy?.let { handleTracingResult(it) }
            viewModel.onResultShown()
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Log.e("ShapeTracing", "Error: $it")
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.onErrorShown()
            }
        }

        viewModel.progressSaved.observe(viewLifecycleOwner) { saved ->
            if (saved == true) {
                Log.d("ShapeTracing", "Progress saved successfully.")
                // Optionally navigate back or to next level after a delay
                // findNavController().navigateUp()
            }
        }
    }

    private fun checkTracing() {
        val tracingPath = binding.tracingView.getTracingPath()
        if (tracingPath.size < 5) {
            Toast.makeText(context, R.string.tracing_too_short, Toast.LENGTH_SHORT).show()
            return
        }
        binding.btnCheck.isEnabled = false
        viewModel.checkTracing(tracingPath)
    }

    private fun handleTracingResult(accuracy: Float) {
        binding.btnCheck.isEnabled = true
        val successThreshold = 0.7f

        if (accuracy >= successThreshold) {
            audioManager.playSound(R.raw.success)
            showFeedback(true, accuracy)
            viewModel.saveProgress(accuracy)
        } else {
            audioManager.playSound(R.raw.failure)
            showFeedback(false, accuracy)
        }
    }

    private fun showFeedback(isSuccess: Boolean, accuracy: Float) {
        val percentage = (accuracy * 100).toInt()
        val message = if (isSuccess) {
            getString(R.string.tracing_success_feedback, percentage)
        } else {
            getString(R.string.tracing_failure_feedback, percentage)
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
     }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}