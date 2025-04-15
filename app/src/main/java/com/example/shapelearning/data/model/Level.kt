package com.example.shapelearning.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Seviye veri modeli
 * Oyun modlarındaki seviyeleri temsil eder (Kullanıcı ilerlemesi HARİÇ)
 */
data class Level(
    val id: Int,
    @StringRes val nameResId: Int,
    val gameMode: GameMode,
    val difficulty: Difficulty,
    var isLocked: Boolean, // Modified: Can be updated
    val requiredScore: Int, // Score needed in previous levels to unlock this one
    val shapes: List<Int>, // Shape IDs used in this level

    // Removed user progress fields: stars, highScore, completionCount

    // Added fields for specific game modes
    @DrawableRes val backgroundImageResId: Int? = null, // Added: For Puzzle mode background
    @DrawableRes val huntSceneResId: Int? = null, // Added: For Hunt mode scene
    val shapePositionsJson: String? = null // Added: JSON string for Hunt/Puzzle positions/data
    // Add other level-specific config if needed (e.g., sorting criteria type)
    // val sortingCriteriaType: String? = null // Example
)