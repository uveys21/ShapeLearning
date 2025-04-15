package com.example.shapelearning.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build // Added for newer locale setting
import android.util.Log // Added
import com.example.shapelearning.data.preferences.SettingsPreferences
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dil yönetimi için yardımcı sınıf
 */
@Singleton
class LocaleHelper @Inject constructor(
    private val settingsPreferences: SettingsPreferences
) {

    companion object {
        /**
         * Get the current system default locale.
         */
        fun getSystemLocale(): Locale {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Resources.getSystem().configuration.locales.get(0)
            } else {
                @Suppress("DEPRECATION")
                Resources.getSystem().configuration.locale
            }
        }

        /**
         * Updates the resources of the given context to the specified language.
         */
        fun updateResources(context: Context, languageCode: String): Context {
            val locale = Locale(languageCode)
            Locale.setDefault(locale) // Set default for the JVM

            val resources = context.resources
            val configuration = Configuration(resources.configuration)

            // Set locale based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                configuration.setLocale(locale)
                val localeList = android.os.LocaleList(locale)
                android.os.LocaleList.setDefault(localeList)
                configuration.setLocales(localeList)
            } else {
                @Suppress("DEPRECATION")
                configuration.locale = locale
            }

            // Update context configuration
            @Suppress("DEPRECATION") // createConfigurationContext is the recommended way
            return context.createConfigurationContext(configuration)
        }
    }


    /**
     * Sets the locale based on saved preferences. Should be called early (e.g., Application or Activity onCreate).
     * Returns the context with the updated configuration.
     */
    fun setLocale(context: Context): Context {
        val languageCode = settingsPreferences.getLanguage().code
        Log.d("LocaleHelper", "Setting locale from preferences: $languageCode")
        return updateResources(context, languageCode)
    }

    /**
     * Sets a new locale and updates preferences. Used when user changes language in settings.
     * Returns the context with the updated configuration.
     */
    fun setNewLocale(context: Context, newLanguage: com.example.shapelearning.data.model.Language): Context {
        Log.d("LocaleHelper", "Setting new locale: ${newLanguage.code}")
        settingsPreferences.setLanguage(newLanguage) // Save the new language preference
        return updateResources(context, newLanguage.code)
    }
}