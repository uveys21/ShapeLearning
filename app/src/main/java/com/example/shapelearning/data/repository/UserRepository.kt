package com.example.shapelearning.data.repository

import android.util.Log // Added
import com.example.shapelearning.data.local.dao.UserDao
import com.example.shapelearning.data.local.entity.UserEntity
import com.example.shapelearning.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch // Added
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kullanıcı repository sınıfı
 * Kullanıcı verilerine erişim için tek giriş noktası
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {
    /**
     * Tüm kullanıcıları getir
     */
    fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { e ->
                Log.e("UserRepository", "Error getting all users", e)
                emit(emptyList())
            }
    }

    /**
     * Belirli bir kullanıcıyı ID'ye göre getir
     */
    fun getUserById(userId: Int): Flow<User?> { // Modified: Return nullable
        return userDao.getUserById(userId)
            .map { entity -> entity?.toDomain() }
            .catch { e ->
                Log.e("UserRepository", "Error getting user by ID $userId", e)
                emit(null)
            }
    }

    /**
     * Yeni kullanıcı ekle ve eklenen kullanıcının ID'sini döndür
     */
    suspend fun insertUser(user: User): Long {
        return try {
            userDao.insertUser(user.toEntity())
        } catch (e: Exception) {
            Log.e("UserRepository", "Error inserting user ${user.name}", e)
            -1L // Return -1 or throw exception on error
        }
    }

    /**
     * Kullanıcı bilgilerini güncelle
     */
    suspend fun updateUser(user: User) {
        try {
            userDao.updateUser(user.toEntity())
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user ${user.id}", e)
        }
    }

    /**
     * Kullanıcının TOPLAM skorunu güncelle (genellikle artırarak)
     */
    suspend fun addUserScore(userId: Int, scoreToAdd: Int) { // Renamed for clarity
        if (scoreToAdd <= 0) return // Don't add zero or negative score
        try {
            userDao.updateUserScore(userId, scoreToAdd)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user score for user $userId", e)
        }
    }

    /**
     * Kullanıcının tamamladığı seviye sayısını artır (sadece 1 artırır)
     * Not: Bu fonksiyonun çağrıldığı yerde ilk tamamlama kontrolü yapılmalı.
     */
    suspend fun incrementCompletedLevelsCounter(userId: Int) { // Renamed for clarity
        try {
            userDao.incrementCompletedLevels(userId)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error incrementing completed levels for user $userId", e)
        }
    }

    /**
     * Kullanıcıyı sil
     */
    suspend fun deleteUser(user: User) {
        try {
            // Consider deleting associated UserProgress entries here or via cascade delete
            userDao.deleteUser(user.toEntity())
        } catch (e: Exception) {
            Log.e("UserRepository", "Error deleting user ${user.id}", e)
        }
    }

    /**
     * UserEntity'yi User domain modeline dönüştür
     */
    private fun UserEntity.toDomain(): User {
        return User(
            id = id,
            name = name,
            age = age,
            avatarResId = avatarResId,
            totalScore = totalScore,
            completedLevelsCount = completedLevels // Renamed mapping
        )
    }

    /**
     * User domain modelini UserEntity'ye dönüştür
     */
    private fun User.toEntity(): UserEntity {
        // Ensure id is 0 if it's a new user for autoGenerate to work
        val entityId = if (id <= 0) 0 else id
        return UserEntity(
            id = entityId,
            name = name,
            age = age,
            avatarResId = avatarResId,
            totalScore = totalScore,
            completedLevels = completedLevelsCount // Renamed mapping
        )
    }
}
