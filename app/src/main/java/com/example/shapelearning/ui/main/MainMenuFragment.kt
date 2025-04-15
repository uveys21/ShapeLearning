package com.example.shapelearning.ui.main

import android.os.Bundle
import android.util.Log // Added
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible // Added for visibility toggling
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.shapelearning.R
import com.example.shapelearning.databinding.FragmentMainMenuBinding
import com.example.shapelearning.service.audio.AudioManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainMenuFragment : Fragment() {

    private var _binding: FragmentMainMenuBinding? = null
    private val binding get() = _binding!! // Use with caution, check nullability

    private val viewModel: MainMenuViewModel by viewModels()

    @Inject
    lateinit var audioManager: AudioManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Add content descriptions to CardViews/ImageViews in fragment_main_menu.xml

        // Show loading indicator initially
        binding.progressBar.isVisible = true // Assuming you add a ProgressBar with id="progressBar"
        binding.tvUserName.isVisible = false

        binding.cardViewPlay.setOnClickListener {
            audioManager.playSound(R.raw.button_click) // Ensure resource exists
            // TODO: Check if a user is selected before navigating, or navigate to user selection
            findNavController().navigate(R.id.action_mainMenuFragment_to_gameModeSelectionFragment)
        }

        binding.cardViewSettings.setOnClickListener {
            audioManager.playSound(R.raw.button_click)
            findNavController().navigate(R.id.action_mainMenuFragment_to_settingsFragment)
        }

        binding.cardViewParentZone.setOnClickListener {
            audioManager.playSound(R.raw.button_click)
            findNavController().navigate(R.id.action_mainMenuFragment_to_parentPinFragment) // Ensure this action exists
        }
    }

    private fun observeViewModel() {
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            binding.progressBar.isVisible = false // Hide loading
            binding.tvUserName.isVisible = true // Show text view
            if (user != null) {
                binding.tvUserName.text = getString(R.string.hello_user, user.name) // Ensure R.string.hello_user exists ("Merhaba, %1$s!")
                Log.d("MainMenuFragment", "Current user loaded: ${user.name}")
            } else {
                // Handle case where no user is selected
                binding.tvUserName.text = getString(R.string.hello_guest) // Ensure R.string.hello_guest exists ("Merhaba Misafir!")
                // Optionally disable play button or navigate to user selection
                Log.d("MainMenuFragment", "No current user selected.")
            }
        }
        // Observe loading state if added to ViewModel
        /*
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.tvUserName.isVisible = !isLoading && viewModel.currentUser.value != null
        }
        */
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks
    }
}