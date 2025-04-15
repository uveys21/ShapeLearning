package com.example.shapelearning.ui.settings

import android.content.Context // Removed (using requireContext())
import android.os.Bundle
import android.util.Log // Added
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioGroup // Added for listener type
import android.widget.Toast // Added
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer // Added for observing Event
import androidx.navigation.fragment.findNavController
import com.example.shapelearning.R
import com.example.shapelearning.data.model.Difficulty // Ensure import
import com.example.shapelearning.data.model.Language // Ensure import
// import com.example.shapelearning.data.model.Settings // No longer need direct model access
import com.example.shapelearning.databinding.FragmentSettingsBinding
import com.example.shapelearning.service.audio.AudioManager
import com.example.shapelearning.utils.EventObserver // Added (Use Event wrapper observer)
import com.example.shapelearning.utils.LocaleHelper // Ensure import
import dagger.hilt.android.AndroidEntryPoint
// import java.util.Locale // Removed, handled by LocaleHelper
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    @Inject
    lateinit var audioManager: AudioManager

    @Inject
    lateinit var localeHelper: LocaleHelper // Inject LocaleHelper

    private var languageSpinnerInitialized = false // Flag to prevent initial auto-selection trigger

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        languageSpinnerInitialized = false // Reset flag on view creation
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()

        // Load current settings
        viewModel.loadSettings()
    }

    private fun setupUI() {
        // Add content descriptions in XML

        binding.ivBack.setOnClickListener {
            audioManager.playSound(R.raw.button_click)
            findNavController().navigateUp()
        }

        // Sound settings
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            // Only trigger VM update if changed by user interaction
            if (binding.switchSound.isPressed) {
                audioManager.playSound(R.raw.button_click)
                viewModel.setSoundEnabled(isChecked)
                audioManager.setSoundEnabled(isChecked) // Update AudioManager directly for immediate effect
            }
        }

        binding.switchMusic.setOnCheckedChangeListener { _, isChecked ->
            if (binding.switchMusic.isPressed) {
                audioManager.playSound(R.raw.button_click)
                viewModel.setMusicEnabled(isChecked)
                audioManager.setMusicEnabled(isChecked) // Update AudioManager directly
            }
        }

        // Language selection
        setupLanguageSpinner()

        // Difficulty
        binding.radioGroupDifficulty.setOnCheckedChangeListener { group, checkedId ->
            // Check if the change is user-initiated (tricky with radio groups)
            // We can assume it is, or add more complex tracking if needed
            audioManager.playSound(R.raw.button_click)
            val difficulty = when (checkedId) {
                R.id.radioDifficultyEasy -> Difficulty.EASY
                R.id.radioDifficultyMedium -> Difficulty.MEDIUM
                R.id.radioDifficultyHard -> Difficulty.HARD
                else -> {
                    Log.w("SettingsFragment", "Unknown difficulty radio button checked: $checkedId")
                    null // Indicate unknown selection
                }
            }
            difficulty?.let { viewModel.setDifficulty(it) }
        }

        // Save button
        binding.btnSave.setOnClickListener {
            audioManager.playSound(R.raw.button_click)
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
        binding.spinnerLanguage.adapter = adapter

        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Prevent triggering on initial setup by the observer
                if (languageSpinnerInitialized && view != null) {
                    audioManager.playSound(R.raw.button_click)
                    val selectedLanguage = languages[position]
                    viewModel.setLanguage(selectedLanguage)
                    Log.d("SettingsFragment", "Language selected: ${selectedLanguage.name}")
                } else {
                    languageSpinnerInitialized = true // Mark as initialized after first auto-selection
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                languageSpinnerInitialized = true // Also mark initialized here
            }
        }
    }

    private fun observeViewModel() {
        viewModel.settingsState.observe(viewLifecycleOwner) { settings ->
            // Update UI elements, but prevent triggering listeners programmatically

            // Sound/Music (Avoid re-triggering listener)
            if(binding.switchSound.isChecked != settings.soundEnabled) {
                binding.switchSound.isChecked = settings.soundEnabled
            }
            if(binding.switchMusic.isChecked != settings.musicEnabled) {
                binding.switchMusic.isChecked = settings.musicEnabled
            }

            // Language (Set selection carefully)
            val languageIndex = Language.values().indexOf(settings.language)
            if (languageIndex != -1 && binding.spinnerLanguage.selectedItemPosition != languageIndex) {
                // Allow the listener to be triggered once here during initial load
                languageSpinnerInitialized = false // Temporarily disable listener block
                binding.spinnerLanguage.setSelection(languageIndex, false) // Set selection without animation
                // Re-enable after a short delay or rely on the flag logic in the listener
            }


            // Difficulty (Avoid re-triggering listener)
            val difficultyId = when (settings.difficulty) {
                Difficulty.EASY -> R.id.radioDifficultyEasy
                Difficulty.MEDIUM -> R.id.radioDifficultyMedium
                Difficulty.HARD -> R.id.radioDifficultyHard
            }
            // Check current selection before setting to avoid loop
            if (binding.radioGroupDifficulty.checkedRadioButtonId != difficultyId) {
                binding.radioGroupDifficulty.check(difficultyId)
            }

            Log.d("SettingsFragment", "Settings UI updated: Lang=${settings.language}, Diff=${settings.difficulty}")
        }

        // SettingsFragment.kt -> observeViewModel -> saveEvent observer
        viewModel.saveEvent.observe(viewLifecycleOwner, EventObserver { saveResult ->
            if (saveResult.success) {
                Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show()
                if (saveResult.languageChanged) {
                    // Doğrudan Language enum'ını gönder
                    localeHelper.setNewLocale(requireContext(), saveResult.newLanguage) // <<< DÜZELTİLDİ
                    requireActivity().recreate()
                } else {
                    findNavController().navigateUp()
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