package com.example.shapelearning.data.local.dao

import androidx.room.*
import com.example.shapelearning.data.local.entity.UserProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * Kullanıcı ilerleme veri erişim nesnesi (DAO)
 * Kullanıcı ilerleme verileri için veritabanı işlemlerini tanımlar
 */
@Dao
interface UserProgressDao {
    // Renamed for clarity
    @Query("SELECT * FROM user_progress WHERE userId = :userId")
    fun getAllUserProgress(userId: Int): Flow<List<UserProgressEntity>>

    @Query("SELECT * FROM user_progress WHERE userId = :userId AND levelId = :levelId")
    fun getUserProgressForLevel(userId: Int, levelId: Int): Flow<UserProgressEntity?>

    // This acts as insert or update
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProgress(userProgress: UserProgressEntity)

    // Update is implicitly handled by insertUserProgress with REPLACE strategy
    // @Update
    // suspend fun updateUserProgress(userProgress: UserProgressEntity) // REMOVED (or keep if specific update logic needed)

    // Renamed for clarity
    @Query("DELETE FROM user_progress WHERE userId = :userId")
    suspend fun deleteAllUserProgress(userId: Int)

    @Query("DELETE FROM user_progress WHERE userId = :userId AND levelId = :levelId")
    suspend fun deleteUserProgressForLevel(userId: Int, levelId: Int)

    // Optional: Query to get highest score directly
    @Query("SELECT MAX(score) FROM user_progress WHERE userId = :userId AND levelId = :levelId")
    fun getHighestScoreForLevel(userId: Int, levelId: Int): Flow<Int?>

    // Optional: Query to check if a level was ever completed
    @Query("SELECT EXISTS(SELECT 1 FROM user_progress WHERE userId = :userId AND levelId = :levelId LIMIT 1)")
    fun hasCompletedLevel(userId: Int, levelId: Int): Flow<Boolean>
}