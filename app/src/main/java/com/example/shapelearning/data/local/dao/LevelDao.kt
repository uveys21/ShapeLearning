package com.example.shapelearning.data.local.dao

import androidx.room.*
import com.example.shapelearning.data.local.entity.LevelEntity
import kotlinx.coroutines.flow.Flow

/**
 * Seviye veri erişim nesnesi (DAO)
 * Seviye verileri için veritabanı işlemlerini tanımlar (Kullanıcı ilerlemesi HARİÇ)
 */
@Dao
interface LevelDao {
    @Query("SELECT * FROM levels")
    fun getAllLevels(): Flow<List<LevelEntity>>

    @Query("SELECT * FROM levels WHERE gameMode = :gameMode")
    fun getLevelsByGameMode(gameMode: String): Flow<List<LevelEntity>>

    @Query("SELECT * FROM levels WHERE id = :levelId")
    fun getLevelById(levelId: Int): Flow<LevelEntity?> // Modified: Return nullable

    @Query("SELECT * FROM levels WHERE difficulty = :difficulty")
    fun getLevelsByDifficulty(difficulty: String): Flow<List<LevelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevels(levels: List<LevelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevel(level: LevelEntity)

    @Update
    suspend fun updateLevel(level: LevelEntity) // Updates the level definition

    @Query("UPDATE levels SET isLocked = :isLocked WHERE id = :levelId")
    suspend fun updateLevelLockStatus(levelId: Int, isLocked: Boolean)

    // Removed: Query to update user progress (stars, score)
    // @Query("UPDATE levels SET stars = :stars, highScore = :score WHERE id = :levelId")
    // suspend fun updateLevelProgress(levelId: Int, stars: Int, score: Int) // REMOVED

    @Delete
    suspend fun deleteLevel(level: LevelEntity)
}