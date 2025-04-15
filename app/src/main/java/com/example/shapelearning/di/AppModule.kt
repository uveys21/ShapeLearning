package com.example.shapelearning.di

import android.content.Context
import com.example.shapelearning.data.local.ShapeLearningDatabase
import com.example.shapelearning.data.local.dao.LevelDao
import com.example.shapelearning.data.local.dao.ShapeDao
import com.example.shapelearning.data.local.dao.UserDao
import com.example.shapelearning.data.local.dao.UserProgressDao
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ShapeLearningDatabase {
        // getInstance now handles migrations correctly if defined
        return ShapeLearningDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideShapeDao(database: ShapeLearningDatabase): ShapeDao {
        return database.shapeDao()
    }

    @Provides
    @Singleton
    fun provideLevelDao(database: ShapeLearningDatabase): LevelDao {
        return database.levelDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: ShapeLearningDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideUserProgressDao(database: ShapeLearningDatabase): UserProgressDao {
        return database.userProgressDao()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        // Configure Gson here if needed (e.g., date adapters)
        return GsonBuilder().create()
    }

    // Add provides for LocaleHelper if needed elsewhere via injection
    // @Provides
    // @Singleton
    // fun provideLocaleHelper(settingsPreferences: SettingsPreferences): LocaleHelper {
    //     return LocaleHelper(settingsPreferences)
    // }
}