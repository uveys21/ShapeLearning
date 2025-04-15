package com.example.shapelearning.data.repository

import android.util.Log // Added
import com.example.shapelearning.data.local.dao.LevelDao
import com.example.shapelearning.data.local.entity.LevelEntity
import com.example.shapelearning.data.model.Difficulty
import com.example.shapelearning.data.model.GameMode
import com.example.shapelearning.data.model.Level
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException // Added
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch // Added
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seviye repository sınıfı
 * Seviye verilerine erişim için tek giriş noktası (Kullanıcı ilerlemesi HARİÇ)
 */
@Singleton
class LevelRepository @Inject constructor(
    private val levelDao: LevelDao,
    private val gson: Gson
) {
    /**
     * Tüm seviyeleri getir
     */
    fun getAllLevels(): Flow<List<Level>> {
        return levelDao.getAllLevels()
            .map { entities -> entities.mapNotNull { it.toDomain() } } // Use mapNotNull for safer parsing
            .catch { e ->
                Log.e("LevelRepository", "Error getting all levels", e)
                emit(emptyList())
            }
    }

    /**
     * Belirli bir oyun modundaki seviyeleri getir
     */
    fun getLevelsByGameMode(gameMode: GameMode): Flow<List<Level>> {
        return levelDao.getLevelsByGameMode(gameMode.name)
            .map { entities -> entities.mapNotNull { it.toDomain() } }
            .catch { e ->
                Log.e("LevelRepository", "Error getting levels by game mode ${gameMode.name}", e)
                emit(emptyList())
            }
    }

    /**
     * Belirli bir seviyeyi ID'ye göre getir
     */
    fun getLevelById(levelId: Int): Flow<Level?> { // Modified: Return nullable
        return levelDao.getLevelById(levelId)
            .map { entity -> entity?.toDomain() } // Map if not null
            .catch { e ->
                Log.e("LevelRepository", "Error getting level by ID $levelId", e)
                emit(null)
            }
    }

    /**
     * Belirli bir zorluk seviyesindeki seviyeleri getir
     */
    fun getLevelsByDifficulty(difficulty: Difficulty): Flow<List<Level>> {
        return levelDao.getLevelsByDifficulty(difficulty.name)
            .map { entities -> entities.mapNotNull { it.toDomain() } }
            .catch { e ->
                Log.e("LevelRepository", "Error getting levels by difficulty ${difficulty.name}", e)
                emit(emptyList())
            }
    }

    /**
     * Seviyeleri veritabanına ekle
     */
    suspend fun insertLevels(levels: List<Level>) {
        try {
            levelDao.insertLevels(levels.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e("LevelRepository", "Error inserting levels", e)
        }
    }

    /**
     * Tek bir seviyeyi veritabanına ekle
     */
    suspend fun insertLevel(level: Level) {
        try {
            levelDao.insertLevel(level.toEntity())
        } catch (e: Exception) {
            Log.e("LevelRepository", "Error inserting level ${level.id}", e)
        }
    }

    /**
     * Seviye tanım bilgilerini güncelle (örn. kilit durumu)
     */
    suspend fun updateLevelDefinition(level: Level) { // Renamed for clarity
        try {
            levelDao.updateLevel(level.toEntity()) // Assumes LevelEntity is updatable
        } catch (e: Exception) {
            Log.e("LevelRepository", "Error updating level definition ${level.id}", e)
        }
    }

    /**
     * Seviyenin kilit durumunu güncelle
     */
    suspend fun updateLevelLockStatus(levelId: Int, isLocked: Boolean) {
        try {
            levelDao.updateLevelLockStatus(levelId, isLocked)
        } catch (e: Exception) {
            Log.e("LevelRepository", "Error updating lock status for level $levelId", e)
        }
    }

    // Removed: updateLevelProgress function as it belongs to UserProgressRepository

    /**
     * LevelEntity'yi Level domain modeline dönüştür
     */
    private fun LevelEntity.toDomain(): Level? { // Return nullable for safety
        return try {
            val shapeIds: List<Int> = gson.fromJson(shapes, object : TypeToken<List<Int>>() {}.type) ?: emptyList()
            Level(
                id = id,
                nameResId = nameResId,
                gameMode = GameMode.valueOf(gameMode),
                difficulty = Difficulty.valueOf(difficulty),
                isLocked = isLocked,
                requiredScore = requiredScore,
                shapes = shapeIds,
                // User progress fields removed
                backgroundImageResId = backgroundImageResId, // Added
                huntSceneResId = huntSceneResId, // Added
                shapePositionsJson = shapePositionsJson // Added
            )
        } catch (e: JsonSyntaxException) {
            Log.e("LevelRepository", "Error parsing shapes JSON for level $id", e)
            null
        } catch (e: IllegalArgumentException) {
            Log.e("LevelRepository", "Error parsing enum for level $id (GameMode: $gameMode, Difficulty: $difficulty)", e)
            null
        }
    }

    /**
     * Level domain modelini LevelEntity'ye dönüştür
     */
    private fun Level.toEntity(): LevelEntity {
        // shapes listesinin tipini belirt
        val shapeList: List<Int> = this.shapes ?: emptyList()
        val typeToken = object : TypeToken<List<Int>>() {}.type
        val shapesJson: String = gson.toJson(shapeList, typeToken) // Tipi belirt

        return LevelEntity(
            id = id,
            nameResId = nameResId,
            gameMode = gameMode.name,
            difficulty = difficulty.name,
            isLocked = isLocked,
            requiredScore = requiredScore,
            shapes = shapesJson, // Oluşturulan JSON string'ini kullan
            backgroundImageResId = backgroundImageResId,
            huntSceneResId = huntSceneResId,
            shapePositionsJson = shapePositionsJson
        )
    }
}
