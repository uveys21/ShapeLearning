package com.example.shapelearning.data.repository

import android.util.Log // Added
import com.example.shapelearning.data.local.dao.UserProgressDao
import com.example.shapelearning.data.local.entity.UserProgressEntity
import com.example.shapelearning.data.model.UserProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch // Added
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kullanıcı ilerleme repository sınıfı
 * Kullanıcı ilerleme verilerine erişim için tek giriş noktası
 */
@Singleton
class UserProgressRepository @Inject constructor(
    private val userProgressDao: UserProgressDao
) {
    /**
     * Belirli bir kullanıcının tüm ilerlemelerini getir
     */
    fun getAllUserProgress(userId: Int): Flow<List<UserProgress>> { // Renamed
        return userProgressDao.getAllUserProgress(userId) // Changed DAO call
            .map { entities -> entities.map { it.toDomain() } }
            .catch { e ->
                Log.e("UserProgressRepo", "Error getting all progress for user $userId", e)
                emit(emptyList())
            }
    }

    /**
     * Belirli bir kullanıcının belirli bir seviyedeki ilerlemesini getir
     */
    fun getUserProgressForLevel(userId: Int, levelId: Int): Flow<UserProgress?> {
        return userProgressDao.getUserProgressForLevel(userId, levelId)
            .map { entity -> entity?.toDomain() }
            .catch { e ->
                Log.e("UserProgressRepo", "Error getting progress for user $userId, level $levelId", e)
                emit(null)
            }
    }

    /**
     * Kullanıcı ilerlemesini kaydet veya güncelle (DAO's REPLACE handles this)
     */
    suspend fun saveOrUpdateUserProgress(userProgress: UserProgress) { // Renamed
        try {
            userProgressDao.insertUserProgress(userProgress.toEntity())
        } catch (e: Exception) {
            Log.e("UserProgressRepo", "Error saving/updating progress for user ${userProgress.userId}, level ${userProgress.levelId}", e)
        }
    }

    // Removed updateUserProgress as saveOrUpdateUserProgress covers it due to REPLACE strategy

    /**
     * Belirli bir kullanıcının tüm ilerlemelerini sil
     */
    suspend fun deleteAllUserProgress(userId: Int) { // Renamed
        try {
            userProgressDao.deleteAllUserProgress(userId) // Changed DAO call
        } catch (e: Exception) {
            Log.e("UserProgressRepo", "Error deleting all progress for user $userId", e)
        }
    }

    /**
     * Belirli bir kullanıcının belirli bir seviyedeki ilerlemesini sil
     */
    suspend fun deleteUserProgressForLevel(userId: Int, levelId: Int) {
        try {
            userProgressDao.deleteUserProgressForLevel(userId, levelId)
        } catch (e: Exception) {
            Log.e("UserProgressRepo", "Error deleting progress for user $userId, level $levelId", e)
        }
    }

    /**
     * UserProgressEntity'yi UserProgress domain modeline dönüştür
     */
    private fun UserProgressEntity.toDomain(): UserProgress {
        return UserProgress(
            userId = userId,
            levelId = levelId,
            stars = stars,
            score = score, // Assuming this is highest score
            completionDate = completionDate,
            completionCount = completionCount // Added mapping
        )
    }

    /**
     * UserProgress domain modelini UserProgressEntity'ye dönüştür
     */
    private fun UserProgress.toEntity(): UserProgressEntity {
        return UserProgressEntity(
            userId = userId,
            levelId = levelId,
            stars = stars,
            score = score,
            completionDate = completionDate,
            completionCount = completionCount // Added mapping
        )
    }
}