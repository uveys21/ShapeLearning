package com.example.shapelearning.ui.main

import androidx.lifecycle.LiveData 
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shapelearning.data.model.User
import com.example.shapelearning.data.preferences.SettingsPreferences
import com.example.shapelearning.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch // Added
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

@HiltViewModel
class MainMenuViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val userId = settingsPreferences.getCurrentUserId()
            if (userId != SettingsPreferences.NO_USER_ID) {
                userRepository.getUserById(userId)
                    .distinctUntilChanged()
                    .catch { _currentUser.postValue(null) }
                    .collect { user -> _currentUser.value = user }
            }else{
                _currentUser.value = null
            }
        }
    }

    fun refreshUser() {
        loadCurrentUser()
    }
}