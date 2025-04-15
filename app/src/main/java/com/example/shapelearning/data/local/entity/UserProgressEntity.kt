package com.example.shapelearning.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Kullanıcı ilerleme veritabanı varlığı
 */
@Entity(
    tableName = "user_progress",
    primaryKeys = ["userId", "levelId"],
    // Added Foreign Keys for data integrity and cascade deletes
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE // Delete progress if user is deleted
        ),
        ForeignKey(
            entity = LevelEntity::class,
            parentColumns = ["id"],
            childColumns = ["levelId"],
            onDelete = ForeignKey.CASCADE // Delete progress if level is deleted (optional)
        )
    ],
    indices = [Index("userId"), Index("levelId")] // Add indices for performance
)
data class UserProgressEntity(
    val userId: Int,
    val levelId: Int,
    val stars: Int,
    val score: Int, // Highest score
    val completionDate: Long,
    val completionCount: Int = 1 // Added: Default to 1 on first save
)