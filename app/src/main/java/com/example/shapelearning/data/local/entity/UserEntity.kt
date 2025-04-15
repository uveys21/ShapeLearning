package com.example.shapelearning.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Kullanıcı veritabanı varlığı
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val age: Int,
    val avatarResId: Int,
    val totalScore: Int = 0,
    val completedLevels: Int = 0 // Renamed internally for consistency with old DAO query
)