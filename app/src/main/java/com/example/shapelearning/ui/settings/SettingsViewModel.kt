package com.example.shapelearning.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shapelearning.data.model.Difficulty // Correct enum
import com.example.shapelearning.data.model.Language // Correct enum
import com.example.shapelearning.data.model.Settings
import com.example.shapelearning.data.preferences.SettingsPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    // Holds the current state of settings being edited
    private val _settingsState = MutableLiveData<Settings>()
    val settingsState: LiveData<Settings> = _settingsState

    // Holds the initially loaded settings to check for changes
    private var initialSettings: Settings? = null

    // Event to signal save completion and language change status
    private val _saveEvent = MutableLiveData<Event<SaveResult>>()
    val saveEvent: LiveData<Event<SaveResult>> = _saveEvent

    // Added error state
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Selected language
    private val _selectedLanguage = MutableLiveData<Language>()
    val selectedLanguage: LiveData<Language> = _selectedLanguage

    // Selected difficulty
    private val _selectedDifficulty = MutableLiveData<Difficulty>()
    val selectedDifficulty: LiveData<Difficulty> = _selectedDifficulty


    // Handles user-initiated actions
    fun userAction(action: () -> Unit) {
        action()
    }

    // Data class'a language enum'ını ekle
    data class SaveResult(
        val success: Boolean,
        val languageChanged: Boolean,
        val newLanguage: Language // String yerine enum
    )


    fun loadSettings() {
        viewModelScope.launch {
            try {
                val loadedSettings = settingsPreferences.getSettings()
                initialSettings = loadedSettings // Store initial state
                _settingsState.value = loadedSettings // Update editable state
                _selectedLanguage.value = loadedSettings.language
                _selectedDifficulty.value = loadedSettings.difficulty
            } catch (e: Exception) {
                _error.postValue("Failed to load settings.")
                // Consider setting a default state if loading fails
                val defaultSettings = Settings()
                initialSettings = defaultSettings
                _settingsState.value = defaultSettings
            }
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        _settingsState.value?.let { currentState ->
            if (currentState.soundEnabled != enabled) {
                _settingsState.value = currentState.copy(soundEnabled = enabled)
            }
        }
    }

    fun setMusicEnabled(enabled: Boolean) {
        _settingsState.value?.let { currentState ->
            if (currentState.musicEnabled != enabled) {
                _settingsState.value = currentState.copy(musicEnabled = enabled)
            }
        }
    }

    fun setParentPin(pin: String) {
        _settingsState.value?.let { currentState ->
            // Add validation if needed
            if (currentState.parentPin != pin) {
                _settingsState.value = currentState.copy(parentPin = pin)
            }
        }
    }
    fun setDailyPlayTimeLimit(minutes: Int) {
        _settingsState.value?.let { currentState ->
            val validMinutes = minutes.coerceAtLeast(0)
            if (currentState.dailyPlayTimeLimit != validMinutes) {
                _settingsState.value = currentState.copy(dailyPlayTimeLimit = validMinutes)
            }
        }
    }




    fun saveSettings() {
        val currentSettings = _settingsState.value
        if (currentSettings == null) {
            _saveEvent.value = Event(SaveResult(success = false, languageChanged = false, newLanguage = initialSettings?.language ?: Language.TURKISH)) // Varsayılan dil
            return
        }

        viewModelScope.launch {
            try {
                settingsPreferences.saveSettings(currentSettings)
                val languageChanged = initialSettings?.language != currentSettings.language
                initialSettings = currentSettings

                _saveEvent.postValue(Event(SaveResult(
                    success = true,
                    languageChanged = languageChanged,
                    newLanguage = currentSettings.language // Enum değerini doğrudan gönder
                )))

            } catch (e: Exception) {
                // ... hata yönetimi ...
                _saveEvent.postValue(Event(SaveResult(success = false, languageChanged = false, newLanguage = initialSettings?.language ?: Language.TURKISH)))
            }
        }
    }

    // Call this from Fragment after showing the error message
    fun onErrorShown() {
        _error.value = null
    }
}