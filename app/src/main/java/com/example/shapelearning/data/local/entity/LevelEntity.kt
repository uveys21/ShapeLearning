package com.example.shapelearning.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Seviye veritabanı varlığı
 */
@Entity(tableName = "levels")
data class LevelEntity(
    @PrimaryKey val id: Int,
    val nameResId: Int,
    val gameMode: String, // Name of GameMode enum
    val difficulty: String, // Name of Difficulty enum
    val isLocked: Boolean,
    val requiredScore: Int,
    val shapes: String, // JSON string of shape IDs

    // Removed user progress fields: stars, highScore, completionCount

    // Added fields for specific game modes (Nullable)
    val backgroundImageResId: Int?, // Added
    val huntSceneResId: Int?, // Added
    val shapePositionsJson: String? // Added: JSON string or null
)