package com.example.shapelearning.data.local.dao

import androidx.room.*
import com.example.shapelearning.data.local.entity.ShapeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Şekil veri erişim nesnesi (DAO)
 * Şekil verileri için veritabanı işlemlerini tanımlar
 */
@Dao
interface ShapeDao {
    @Query("SELECT * FROM shapes")
    fun getAllShapes(): Flow<List<ShapeEntity>>

    @Query("SELECT * FROM shapes WHERE id = :shapeId")
    fun getShapeById(shapeId: Int): Flow<ShapeEntity?> // Modified: Return nullable

    @Query("SELECT * FROM shapes WHERE difficulty = :difficulty")
    fun getShapesByDifficulty(difficulty: String): Flow<List<ShapeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShapes(shapes: List<ShapeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShape(shape: ShapeEntity)

    @Update
    suspend fun updateShape(shape: ShapeEntity)

    @Delete
    suspend fun deleteShape(shape: ShapeEntity)
}