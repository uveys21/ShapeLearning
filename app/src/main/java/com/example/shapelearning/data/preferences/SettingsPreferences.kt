package com.example.shapelearning.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log // Added
import com.example.shapelearning.data.model.Difficulty
import com.example.shapelearning.data.model.Language
import com.example.shapelearning.data.model.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ayarlar tercihleri yöneticisi
 * Uygulama ayarlarını SharedPreferences üzerinde saklar ve yönetir
 * TODO: Consider migrating to Jetpack DataStore for better async and type safety.
 */
@Singleton
class SettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Use an encrypted version for sensitive data like PIN in production
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Ses etkin mi?
     */
    fun getSoundEnabled(): Boolean {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true) // Default true
    }

    /**
     * Ses etkinliğini ayarla
     */
    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    /**
     * Müzik etkin mi?
     */
    fun getMusicEnabled(): Boolean {
        return prefs.getBoolean(KEY_MUSIC_ENABLED, true) // Default true
    }

    /**
     * Müzik etkinliğini ayarla
     */
    fun setMusicEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply()
    }

    /**
     * Seçili dili getir
     */
    fun getLanguage(): Language {
        val languageCode = prefs.getString(KEY_LANGUAGE, Language.TURKISH.code) ?: Language.TURKISH.code
        // Find language by code, default to Turkish if not found or invalid
        return Language.values().find { it.code == languageCode } ?: Language.TURKISH
    }

    /**
     * Dili ayarla
     */
    fun setLanguage(language: Language) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    /**
     * Zorluk seviyesini getir
     */
    fun getDifficulty(): Difficulty {
        val difficultyName = prefs.getString(KEY_DIFFICULTY, Difficulty.EASY.name) ?: Difficulty.EASY.name
        return try {
            Difficulty.valueOf(difficultyName)
        } catch (e: IllegalArgumentException) {
            Log.w("SettingsPreferences", "Invalid difficulty name in prefs: $difficultyName. Defaulting to EASY.")
            Difficulty.EASY // Default to Easy if saved value is invalid
        }
    }

    /**
     * Zorluk seviyesini ayarla
     */
    fun setDifficulty(difficulty: Difficulty) {
        prefs.edit().putString(KEY_DIFFICULTY, difficulty.name).apply()
    }

    /**
     * Ebeveyn PIN kodunu getir
     */
    fun getParentPin(): String {
        // Consider using EncryptedSharedPreferences for PIN
        return prefs.getString(KEY_PARENT_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
    }

    /**
     * Ebeveyn PIN kodunu ayarla
     */
    fun setParentPin(pin: String) {
        // Basic validation, consider more robust validation
        if (pin.length == 4 && pin.all { it.isDigit() }) {
            prefs.edit().putString(KEY_PARENT_PIN, pin).apply()
        } else {
            Log.w("SettingsPreferences", "Invalid PIN format provided: $pin")
        }
    }

    /**
     * Günlük oyun süresi sınırını getir (dakika cinsinden)
     */
    fun getDailyPlayTimeLimit(): Int {
        return prefs.getInt(KEY_DAILY_PLAY_TIME_LIMIT, 0) // Default 0 (unlimited)
    }

    /**
     * Günlük oyun süresi sınırını ayarla (dakika cinsinden)
     */
    fun setDailyPlayTimeLimit(minutes: Int) {
        // Ensure non-negative value
        prefs.edit().putInt(KEY_DAILY_PLAY_TIME_LIMIT, minutes.coerceAtLeast(0)).apply()
    }

    /**
     * Aktif kullanıcı ID'sini getir
     */
    fun getCurrentUserId(): Int {
        return prefs.getInt(KEY_CURRENT_USER_ID, NO_USER_ID) // Use constant for no user
    }

    /**
     * Aktif kullanıcı ID'sini ayarla
     */
    fun setCurrentUserId(userId: Int) {
        prefs.edit().putInt(KEY_CURRENT_USER_ID, userId).apply()
    }

    /**
     * Tüm ayarları tek seferde getir
     */
    fun getSettings(): Settings {
        return Settings(
            soundEnabled = getSoundEnabled(),
            musicEnabled = getMusicEnabled(),
            language = getLanguage(),
            difficulty = getDifficulty(),
            parentPin = getParentPin(),
            dailyPlayTimeLimit = getDailyPlayTimeLimit()
        )
    }

    /**
     * Tüm ayarları tek seferde kaydet
     */
    fun saveSettings(settings: Settings) {
        prefs.edit().apply {
            putBoolean(KEY_SOUND_ENABLED, settings.soundEnabled)
            putBoolean(KEY_MUSIC_ENABLED, settings.musicEnabled)
            putString(KEY_LANGUAGE, settings.language.code)
            putString(KEY_DIFFICULTY, settings.difficulty.name)
            putString(KEY_PARENT_PIN, settings.parentPin) // Consider encrypting
            putInt(KEY_DAILY_PLAY_TIME_LIMIT, settings.dailyPlayTimeLimit.coerceAtLeast(0))
            apply() // Apply changes atomically
        }
    }

    companion object {
        private const val PREFS_NAME = "shape_learning_prefs"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_MUSIC_ENABLED = "music_enabled"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_DIFFICULTY = "difficulty"
        private const val KEY_PARENT_PIN = "parent_pin"
        private const val KEY_DAILY_PLAY_TIME_LIMIT = "daily_play_time_limit"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
        private const val DEFAULT_PIN = "0000"
        const val NO_USER_ID = -1 // Constant for indicating no user is selected
    }
}