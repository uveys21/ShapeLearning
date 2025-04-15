package com.example.shapelearning.data.model

/**
 * Kullanıcı ilerleme veri modeli
 * Kullanıcının TEK BİR SEVİYEDEKİ ilerlemesini temsil eder
 */
data class UserProgress(
    val userId: Int,
    val levelId: Int,
    val stars: Int,          // Stars earned in this level (0-3)
    val score: Int,          // Highest score achieved in this level
    val completionDate: Long, // Timestamp of the first completion or last update
    val completionCount: Int = 1 // Added: How many times the level was completed successfully
)