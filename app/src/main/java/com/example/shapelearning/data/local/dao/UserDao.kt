package com.example.shapelearning.data.local.dao

import androidx.room.*
import com.example.shapelearning.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Kullanıcı veri erişim nesnesi (DAO)
 * Kullanıcı verileri için veritabanı işlemlerini tanımlar
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: Int): Flow<UserEntity?> // Modified: Return nullable

    @Insert(onConflict = OnConflictStrategy.IGNORE) // Use IGNORE if you want to prevent duplicate names/users potentially
    suspend fun insertUser(user: UserEntity): Long // Returns row ID or -1 if ignored/failed

    @Update
    suspend fun updateUser(user: UserEntity)

    // Adds score to the existing total score
    @Query("UPDATE users SET totalScore = totalScore + :scoreToAdd WHERE id = :userId")
    suspend fun updateUserScore(userId: Int, scoreToAdd: Int)

    // Increments the counter by 1
    @Query("UPDATE users SET completedLevels = completedLevels + 1 WHERE id = :userId")
    suspend fun incrementCompletedLevels(userId: Int)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}