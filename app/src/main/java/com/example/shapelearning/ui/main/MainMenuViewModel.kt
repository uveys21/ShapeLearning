package com.example.shapelearning.ui.main

import android.util.Log // Added
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shapelearning.data.model.User
import com.example.shapelearning.data.preferences.SettingsPreferences
import com.example.shapelearning.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch // Added
import kotlinx.coroutines.flow.distinctUntilChanged // Added
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainMenuViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    // Optional: Add loading state
    // private val _isLoading = MutableLiveData<Boolean>(false)
    // val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        // _isLoading.value = true // Start loading
        viewModelScope.launch {
            val userId = settingsPreferences.getCurrentUserId()
            if (userId != SettingsPreferences.NO_USER_ID) { // Use constant
                Log.d("MainMenuViewModel", "Loading user with ID: $userId")
                userRepository.getUserById(userId)
                    .distinctUntilChanged() // Only emit if user data actually changes
                    .catch { e ->
                        Log.e("MainMenuViewModel", "Error loading user $userId", e)
                        _currentUser.postValue(null) // Set null on error
                        // _isLoading.postValue(false)
                    }
                    .collect { user ->
                        _currentUser.value = user // Update LiveData
                        // _isLoading.value = false // Stop loading
                        Log.d("MainMenuViewModel", "User collected: ${user?.name}")
                    }
            } else {
                Log.d("MainMenuViewModel", "No user ID found in preferences.")
                _currentUser.value = null // No user selected
                // _isLoading.value = false // Stop loading
            }
        }
    }

    // Function to refresh user data if needed
    fun refreshUser() {
        loadCurrentUser()
    }
}