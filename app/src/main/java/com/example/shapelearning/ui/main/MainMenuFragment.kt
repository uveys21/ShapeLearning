package com.example.shapelearning.ui.main

import android.os.Bundle
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
import com.example.shapelearning.utils.NavigationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainMenuFragment : Fragment() {

    private var _binding: FragmentMainMenuBinding? = null
    private val binding get() = _binding!! // Use with caution, check nullability

    private val viewModel: MainMenuViewModel by viewModels()

    @Inject
    lateinit var audioManager: AudioManager

    @Inject
    lateinit var navigationHelper: NavigationHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainMenuBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Add content descriptions to CardViews/ImageViews in fragment_main_menu.xml
        binding?.apply {
            progressBar.isVisible = true
            tvUserName.isVisible = false
            cardViewPlay.setOnClickListener {
                audioManager.playSoundEffect(R.raw.button_click)
                navigationHelper.navigateToGameModeSelection(findNavController())
            }
            cardViewSettings.setOnClickListener {
                audioManager.playSoundEffect(R.raw.button_click)
                navigationHelper.navigateToSettings(findNavController())
            }
            cardViewParentZone.setOnClickListener {
                audioManager.playSoundEffect(R.raw.button_click)
                navigationHelper.navigateToParentPin(findNavController())
            }
        }
    }

    private fun observeViewModel() {
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            binding?.apply {
                progressBar.isVisible = false
                tvUserName.isVisible = true
                if (user != null) {
                    tvUserName.text = getString(R.string.hello_user, user.name)
                } else {
                    tvUserName.text = getString(R.string.hello_guest)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}