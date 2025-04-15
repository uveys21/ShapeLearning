package com.example.shapelearning.data.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes // Added
import androidx.annotation.StringRes

/**
 * Şekil veri modeli
 * Uygulamada kullanılan geometrik şekilleri temsil eder
 */
data class Shape(
    val id: Int,
    @StringRes val nameResId: Int,
    @StringRes val descriptionResId: Int,
    @DrawableRes val imageResId: Int,
    @DrawableRes val outlineImageResId: Int, // Used for tracing
    @DrawableRes val realLifeImageResId: Int,
    @ColorRes val colorResId: Int,
    val corners: Int,
    val sides: Int,
    val difficulty: Difficulty,
    @RawRes val soundResId: Int? = null // Added: Optional sound for the shape name/description
    // Add other properties if needed for sorting/puzzle/hunt (e.g., size category, specific outline points)
    // val outlinePointsJson: String? = null // Example for tracing points
)
/**
 * Zorluk seviyesi enum sınıfı
 */