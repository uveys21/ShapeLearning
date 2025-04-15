package com.example.shapelearning.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Şekil veritabanı varlığı
 */
@Entity(tableName = "shapes")
data class ShapeEntity(
    @PrimaryKey val id: Int,
    val nameResId: Int,
    val descriptionResId: Int,
    val imageResId: Int,
    val outlineImageResId: Int,
    val realLifeImageResId: Int,
    val colorResId: Int,
    val corners: Int,
    val sides: Int,
    val difficulty: String, // Name of Difficulty enum
    val soundResId: Int? // Added: Nullable sound resource ID
    // val outlinePointsJson: String? // Example
)