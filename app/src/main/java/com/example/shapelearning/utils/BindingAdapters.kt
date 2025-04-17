package com.example.shapelearning.utils

import android.R
import android.view.View
import android.widget.*
import androidx.databinding.BindingAdapter
import com.example.shapelearning.data.model.Difficulty
import com.example.shapelearning.data.model.Language

object BindingAdapters {

    @JvmStatic
    @BindingAdapter("app:selectedLanguage", "app:onLanguageSelected", requireAll = false)
    fun bindSpinnerLanguage(spinner: Spinner, selectedLanguage: Language?, onLanguageSelected: ((Language?) -> Unit)?) {
        val adapter = spinner.adapter as? ArrayAdapter<String> ?: return
        val languages = Language.values()

        if (selectedLanguage != null) {
            val position = languages.indexOf(selectedLanguage)
            if (position != -1 && spinner.selectedItemPosition != position) {
                spinner.setSelection(position)
            }
        } else {
            spinner.setSelection(0) // Or handle the null case as appropriate
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (spinner.isPressed) { // Only respond to user interaction
                    val language = if (position in languages.indices) languages[position] else null
                    onLanguageSelected?.invoke(language)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing or handle the case where nothing is selected (if needed)
            }
        }
    }

    @JvmStatic
    @BindingAdapter("app:selectedDifficulty", "app:onDifficultySelected", requireAll = false)
    fun bindRadioGroupDifficulty(radioGroup: RadioGroup, selectedDifficulty: Difficulty?, onDifficultySelected: ((Difficulty?) -> Unit)?) {
        val checkedId = when (selectedDifficulty) {
            Difficulty.EASY -> com.example.shapelearning.R.id.radioDifficultyEasy
            Difficulty.MEDIUM -> com.example.shapelearning.R.id.radioDifficultyMedium
            Difficulty.HARD -> com.example.shapelearning.R.id.radioDifficultyHard
            else -> -1 // Or a default value if appropriate
        }

        // 2. Set up an OnCheckedChangeListener.
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val difficulty = getDifficultyForCheckedId(checkedId) ?: Difficulty.EASY // Assuming default is EASY
            onDifficultySelected(difficulty)
        }
    }
}
