package com.example.shapelearning.ui.games

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.shapelearning.R
import com.example.shapelearning.data.model.GameMode

class GameModeSelectionViewModel : ViewModel() {

    private val _gameModes = MutableLiveData<List<GameMode>>()
    val gameModes: LiveData<List<GameMode>> = _gameModes

    init {
        loadGameModes()
    }

    private fun loadGameModes() {
        // Initialize with the same game modes as in the Fragment
        _gameModes.value = listOf(
            GameMode(R.string.game_mode_discovery_title, R.string.game_mode_discovery_description, R.drawable.ic_game_discovery),
            GameMode(R.string.game_mode_tracing_title, R.string.game_mode_tracing_description, R.drawable.ic_game_tracing),
            GameMode(R.string.game_mode_matching_title, R.string.game_mode_matching_description, R.drawable.ic_game_matching),
            GameMode(R.string.game_mode_sorting_title, R.string.game_mode_sorting_description, R.drawable.ic_game_sorting),
            GameMode(R.string.game_mode_puzzle_title, R.string.game_mode_puzzle_description, R.drawable.ic_game_puzzle),
            GameMode(R.string.game_mode_hunt_title, R.string.game_mode_hunt_description, R.drawable.ic_game_hunt)
        )
    }
}