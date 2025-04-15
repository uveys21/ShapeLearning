package com.example.shapelearning.data.model

/**
 * Kullanıcı veri modeli
 * Uygulamayı kullanan çocukları temsil eder
 */
data class User(
    val id: Int = 0, // Keep 0 as default for autoGenerate
    val name: String,
    val age: Int,
    val avatarResId: Int,
    val totalScore: Int = 0,
    val completedLevelsCount: Int = 0 // Renamed from completedLevels for clarity
)