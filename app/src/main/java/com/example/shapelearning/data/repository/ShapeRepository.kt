package com.example.shapelearning.data.repository

import android.util.Log // Added for logging
import com.example.shapelearning.data.local.dao.ShapeDao
import com.example.shapelearning.data.local.entity.ShapeEntity
import com.example.shapelearning.data.model.Difficulty
import com.example.shapelearning.data.model.Shape
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch // Added for error handling
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Şekil repository sınıfı
 * Şekil verilerine erişim için tek giriş noktası
 */
@Singleton
class ShapeRepository @Inject constructor(
    private val shapeDao: ShapeDao
) {
    /**
     * Tüm şekilleri getir
     */
    fun getAllShapes(): Flow<List<Shape>> {
        return shapeDao.getAllShapes()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { e ->
                Log.e("ShapeRepository", "Error getting all shapes", e)
                emit(emptyList()) // Emit empty list on error
            }
    }

    /**
     * Belirli bir şekli ID'ye göre getir
     */
    fun getShapeById(shapeId: Int): Flow<Shape?> { // Modified: Return nullable Shape
        return shapeDao.getShapeById(shapeId)
            .map { entity -> entity?.toDomain() } // Map if entity is not null
            .catch { e ->
                Log.e("ShapeRepository", "Error getting shape by ID $shapeId", e)
                emit(null) // Emit null on error
            }
    }

    /**
     * Belirli bir zorluk seviyesindeki şekilleri getir
     */
    fun getShapesByDifficulty(difficulty: Difficulty): Flow<List<Shape>> {
        return shapeDao.getShapesByDifficulty(difficulty.name)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { e ->
                Log.e("ShapeRepository", "Error getting shapes by difficulty ${difficulty.name}", e)
                emit(emptyList())
            }
    }

    /**
     * Şekilleri veritabanına ekle
     */
    suspend fun insertShapes(shapes: List<Shape>) {
        try {
            shapeDao.insertShapes(shapes.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e("ShapeRepository", "Error inserting shapes", e)
        }
    }

    /**
     * Tek bir şekli veritabanına ekle
     */
    suspend fun insertShape(shape: Shape) {
        try {
            shapeDao.insertShape(shape.toEntity())
        } catch (e: Exception) {
            Log.e("ShapeRepository", "Error inserting shape ${shape.id}", e)
        }
    }

    /**
     * Şekil bilgilerini güncelle
     */
    suspend fun updateShape(shape: Shape) {
        try {
            shapeDao.updateShape(shape.toEntity())
        } catch (e: Exception) {
            Log.e("ShapeRepository", "Error updating shape ${shape.id}", e)
        }
    }

    /**
     * Şekli veritabanından sil
     */
    suspend fun deleteShape(shape: Shape) {
        try {
            shapeDao.deleteShape(shape.toEntity())
        } catch (e: Exception) {
            Log.e("ShapeRepository", "Error deleting shape ${shape.id}", e)
        }
    }

    /**
     * ShapeEntity'yi Shape domain modeline dönüştür
     */
    private fun ShapeEntity.toDomain(): Shape {
        return Shape(
            id = id,
            nameResId = nameResId,
            descriptionResId = descriptionResId,
            imageResId = imageResId,
            outlineImageResId = outlineImageResId,
            realLifeImageResId = realLifeImageResId,
            colorResId = colorResId,
            corners = corners,
            sides = sides,
            difficulty = Difficulty.valueOf(difficulty), // Consider adding try-catch for robustness
            soundResId = soundResId // Added mapping
            // outlinePointsJson = outlinePointsJson // Example mapping
        )
    }

    /**
     * Shape domain modelini ShapeEntity'ye dönüştür
     */
    private fun Shape.toEntity(): ShapeEntity {
        return ShapeEntity(
            id = id,
            nameResId = nameResId,
            descriptionResId = descriptionResId,
            imageResId = imageResId,
            outlineImageResId = outlineImageResId,
            realLifeImageResId = realLifeImageResId,
            colorResId = colorResId,
            corners = corners,
            sides = sides,
            difficulty = difficulty.name,
            soundResId = soundResId // Added mapping
            // outlinePointsJson = outlinePointsJson // Example mapping
        )
    }
}