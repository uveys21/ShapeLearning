package com.example.shapelearning.ui.settings

import android.util.Log // Added
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shapelearning.data.model.Difficulty // Correct enum
import com.example.shapelearning.data.model.Language // Correct enum
import com.example.shapelearning.data.model.Settings
import com.example.shapelearning.data.preferences.SettingsPreferences
import com.example.shapelearning.utils.Event // Added
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
                Log.d("SettingsViewModel", "Settings loaded: $loadedSettings")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error loading settings", e)
                _error.postValue("Failed to load settings.")
                // Set default state if loading fails?
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
                Log.d("SettingsViewModel", "Sound enabled set to: $enabled")
            }
        }
    }

    fun setMusicEnabled(enabled: Boolean) {
        _settingsState.value?.let { currentState ->
            if (currentState.musicEnabled != enabled) {
                _settingsState.value = currentState.copy(musicEnabled = enabled)
                Log.d("SettingsViewModel", "Music enabled set to: $enabled")
            }
        }
    }

    fun setLanguage(language: Language) {
        _settingsState.value?.let { currentState ->
            if (currentState.language != language) {
                _settingsState.value = currentState.copy(language = language)
                Log.d("SettingsViewModel", "Language set to: ${language.name}")
            }
        }
    }

    fun setDifficulty(difficulty: Difficulty) {
        _settingsState.value?.let { currentState ->
            if (currentState.difficulty != difficulty) {
                _settingsState.value = currentState.copy(difficulty = difficulty)
                Log.d("SettingsViewModel", "Difficulty set to: ${difficulty.name}")
            }
        }
    }

    // Optional: Functions for PIN and Play Time Limit if UI exists
    fun setParentPin(pin: String) {
        _settingsState.value?.let { currentState ->
            // Add validation if needed
            if (currentState.parentPin != pin) {
                _settingsState.value = currentState.copy(parentPin = pin)
                Log.d("SettingsViewModel", "PIN updated")
            }
        }
    }
    fun setDailyPlayTimeLimit(minutes: Int) {
        _settingsState.value?.let { currentState ->
            val validMinutes = minutes.coerceAtLeast(0)
            if (currentState.dailyPlayTimeLimit != validMinutes) {
                _settingsState.value = currentState.copy(dailyPlayTimeLimit = validMinutes)
                Log.d("SettingsViewModel", "Play time limit set to: $validMinutes")
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