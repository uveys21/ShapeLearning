package com.example.shapelearning.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.shapelearning.R

enum class GameMode(
    @StringRes val titleResId: Int,
    @StringRes val descriptionResId: Int,
    @DrawableRes val iconResId: Int
) {
    DISCOVERY(
        R.string.game_mode_discovery_title,
        R.string.game_mode_discovery_description,
        R.drawable.ic_game_discovery
    ),
    TRACING(
        titleResId = R.string.game_mode_tracing_title,
        descriptionResId = R.string.game_mode_tracing_description,
        iconResId = R.drawable.ic_game_tracing
    ),
    MATCHING(
        titleResId = R.string.game_mode_matching_title,
        descriptionResId = R.string.game_mode_matching_description,
        iconResId = R.drawable.ic_game_matching
    ),
    SORTING(
        titleResId = R.string.game_mode_sorting_title,
        descriptionResId = R.string.game_mode_sorting_description,
        iconResId = R.drawable.ic_game_sorting
    ),
    PUZZLE(
        titleResId = R.string.game_mode_puzzle_title,
        descriptionResId = R.string.game_mode_puzzle_description,
        iconResId = R.drawable.ic_game_puzzle
    ),
    HUNT(
        titleResId = R.string.game_mode_hunt_title,
        descriptionResId = R.string.game_mode_hunt_description,
        iconResId = R.drawable.ic_game_hunt
    );

    // Ensure R.string.* and R.drawable.* resources exist
}