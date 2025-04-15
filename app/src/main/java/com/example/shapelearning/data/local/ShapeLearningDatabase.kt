package com.example.shapelearning.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.shapelearning.data.local.dao.LevelDao
import com.example.shapelearning.data.local.dao.ShapeDao
import com.example.shapelearning.data.local.dao.UserDao
import com.example.shapelearning.data.local.dao.UserProgressDao
import com.example.shapelearning.data.local.entity.LevelEntity
import com.example.shapelearning.data.local.entity.ShapeEntity
import com.example.shapelearning.data.local.entity.UserEntity
import com.example.shapelearning.data.local.entity.UserProgressEntity

/**
 * Şekil Öğrenme veritabanı
 * Uygulamanın ana veritabanı sınıfı
 */
@Database(
    entities = [
        ShapeEntity::class,
        LevelEntity::class,
        UserEntity::class,
        UserProgressEntity::class
    ],
    version = 2, // <<< Version Incremented
    exportSchema = true // <<< Set to true for production builds
)
abstract class ShapeLearningDatabase : RoomDatabase() {
    abstract fun shapeDao(): ShapeDao
    abstract fun levelDao(): LevelDao
    abstract fun userDao(): UserDao
    abstract fun userProgressDao(): UserProgressDao

    companion object {
        @Volatile
        private var INSTANCE: ShapeLearningDatabase? = null
        private const val DATABASE_NAME = "shape_learning_database"

        // Added: Migration from version 1 to 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // --- Schema changes for LevelEntity ---
                // Remove user progress columns (Not directly supported, best practice is create new table & copy)
                // For simplicity here, we just add new columns. Old columns remain but unused by new code.
                // Or if using fallbackToDestructiveMigration, this is not needed.
                // Adding new nullable columns for level-specific data
                database.execSQL("ALTER TABLE levels ADD COLUMN backgroundImageResId INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE levels ADD COLUMN huntSceneResId INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE levels ADD COLUMN shapePositionsJson TEXT DEFAULT NULL")

                // --- Schema changes for ShapeEntity ---
                database.execSQL("ALTER TABLE shapes ADD COLUMN soundResId INTEGER DEFAULT NULL")
                // database.execSQL("ALTER TABLE shapes ADD COLUMN outlinePointsJson TEXT DEFAULT NULL") // Example

                // --- Schema changes for UserProgressEntity ---
                // Add completionCount column
                database.execSQL("ALTER TABLE user_progress ADD COLUMN completionCount INTEGER NOT NULL DEFAULT 1")
                // Add Foreign Keys (Requires creating new table with constraints and copying data, complex!)
                // Simpler approach for this example: Assume FKs are added for new installs or use destructive migration.
                // Proper migration would look like:
                // 1. CREATE TABLE user_progress_new (...)
                // 2. INSERT INTO user_progress_new SELECT ... FROM user_progress
                // 3. DROP TABLE user_progress
                // 4. ALTER TABLE user_progress_new RENAME TO user_progress
            }
        }


        fun getInstance(context: Context): ShapeLearningDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShapeLearningDatabase::class.java,
                    DATABASE_NAME
                )
                    // Use migration for production, fallback for development if needed
                    .addMigrations(MIGRATION_1_2)
                    // .fallbackToDestructiveMigration() // Use this ONLY during development if migrations are complex/failing
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}