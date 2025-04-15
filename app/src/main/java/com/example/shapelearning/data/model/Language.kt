package com.example.shapelearning.data.model

import androidx.annotation.StringRes
import com.example.shapelearning.R // Assuming R is imported correctly

/**
 * Dil enum sınıfı
 * Uygulamada desteklenen dilleri temsil eder
 */
enum class Language(val code: String, @StringRes val nameResId: Int) {
    TURKISH("tr", R.string.language_turkish),
    ENGLISH("en", R.string.language_english),
    GERMAN("de", R.string.language_german),
    FRENCH("fr", R.string.language_french),
    SPANISH("es", R.string.language_spanish),
    ARABIC("ar", R.string.language_arabic),
    RUSSIAN("ru", R.string.language_russian);

    // Ensure R.string.language_* resources exist
}
