package com.example.shapelearning.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.shapelearning.R
import com.example.shapelearning.data.model.Difficulty // Ensure import
import com.example.shapelearning.utils.NavigationHelper
import com.example.shapelearning.data.model.Language // Ensure import
// import com.example.shapelearning.data.model.Settings // No longer need direct model access
import com.example.shapelearning.databinding.FragmentSettingsBinding
import com.example.shapelearning.service.audio.AudioManager
import com.example.shapelearning.utils.EventObserver
import com.example.shapelearning.utils.LocaleHelper // Ensure import
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.fragment.app.viewModels
import android.util.Log

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    @Inject
    lateinit var audioManager: AudioManager
    @Inject
    lateinit var navigationHelper: NavigationHelper

    private var _binding: FragmentSettingsBinding? = null

    @Inject
    lateinit var localeHelper: LocaleHelper // Inject LocaleHelper

    private var languageSpinnerInitialized = false // Flag to prevent initial auto-selection trigger

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false).apply {
            viewModel = this@SettingsFragment.viewModel
            lifecycleOwner = viewLifecycleOwner // VERY IMPORTANT
        }
        languageSpinnerInitialized = false // Reset flag on view creation
        return binding?.root
    }

        setupUI()
        observeViewModel(binding) // Pass binding to observeViewModel

        // Load current settings
        viewModel.loadSettings()
    }

    private val viewModel: SettingsViewModel by viewModels()
    }

    private fun setupUI() {
        // Add content descriptions in XML

        binding.ivBack.setOnClickListener {
            audioManager.playSoundEffect(R.raw.button_click)
            navigationHelper.navigateUp(findNavController())
        }

        // Sound settings (Now handled by Data Binding, but keep AudioManager update)
       binding?.switchSound?.setOnCheckedChangeListener { _, isChecked ->
           if (binding?.switchSound?.isPressed == true) {
               audioManager.playSoundEffect(R.raw.button_click)
               // Data Binding will update ViewModel, but we still need to update AudioManager
               audioManager.setSoundEnabled(isChecked)
           }
       }

       binding?.switchMusic?.setOnCheckedChangeListener { _, isChecked ->
           if (binding?.switchMusic?.isPressed == true) {
               audioManager.playSoundEffect(R.raw.button_click)
               // Data Binding will update ViewModel, but we still need to update AudioManager
               audioManager.setMusicEnabled(isChecked)
           }
       }

        val binding = _binding ?: return
        // Language selection
        setupLanguageSpinner()



        // Save button
        binding.btnSave.setOnClickListener {
            audioManager.playSoundEffect(R.raw.button_click)
            viewModel.saveSettings() // ViewModel handles saving via Preferences
            // Language change requires activity recreation, handled in observer now
        }
    }

      private fun setupLanguageSpinner() {
        // Use the Language enum defined in the data layer
        val languages = Language.values()
        val languageNames = languages.map { getString(it.nameResId) } // Get names from resources

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            languageNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding?.spinnerLanguage?.adapter = adapter    private fun observeViewModel(binding: FragmentSettingsBinding?) {


        // SettingsFragment.kt -> observeViewModel -> saveEvent observer
        viewModel.saveEvent.observe(viewLifecycleOwner, EventObserver { saveResult ->
            if (saveResult.success) {
                Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show()
                if (saveResult.languageChanged) {
                    // Doğrudan Language enum'ını gönder
                    localeHelper.setNewLocale(requireContext(), saveResult.newLanguage) // <<< DÜZELTİLDİ
                    requireActivity().recreate()
                } else {
                    navigationHelper.navigateUp(findNavController())
                }
            } else {
                Toast.makeText(requireContext(), R.string.settings_save_error, Toast.LENGTH_SHORT).show()
            }
        })

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Log.e("SettingsFragment", "Error: $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.onErrorShown()
            }
        }
    }

    // Removed applyLanguageChange - handled by LocaleHelper and recreate()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}