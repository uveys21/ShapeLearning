package com.example.shapelearning.data.model

/**
 * Ayarlar veri modeli
 * Uygulama ayarlarını temsil eder
 */
data class Settings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val language: Language = Language.TURKISH, // Default language
    val difficulty: Difficulty = Difficulty.EASY, // Default difficulty
    val parentPin: String = "0000", // Default PIN
    val dailyPlayTimeLimit: Int = 0 // Dakika cinsinden, 0 = sınırsız
)
