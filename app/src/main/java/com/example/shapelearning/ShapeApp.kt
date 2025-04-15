package com.example.shapelearning

import android.app.Application
import com.example.shapelearning.utils.LocaleHelper // Added
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ShapeApp : Application() {

    // Inject if needed application-wide, otherwise inject where used (Activities)
    // @Inject
    // lateinit var localeHelper: LocaleHelper

    override fun onCreate() {
        super.onCreate()
        // Uygulama başlangıç işlemleri
        // Optional: Set initial locale based on saved preferences here
        // Note: Setting locale here might affect system services before Activity locale is set.
        // It's generally safer to set it in each Activity's onCreate.
        // LocaleHelper(SettingsPreferences(this)).setLocale(this) // Example if not using Hilt early

        // Other init tasks (Logging, Crash Reporting, etc.)
        // Timber.plant(Timber.DebugTree()) // Example using Timber library
    }

    // Optional: Override attachBaseContext if setting locale very early is critical
    /*
    @Inject lateinit var localeHelper: LocaleHelper // Requires field injection setup for Application

    override fun attachBaseContext(base: Context) {
        // Set locale before anything else attaches to the context
        super.attachBaseContext(localeHelper.setLocale(base))
    }
    */
}