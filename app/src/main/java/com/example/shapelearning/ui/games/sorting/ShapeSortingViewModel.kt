package com.example.shapelearning.ui.games.sorting

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.shapelearning.R
import com.example.shapelearning.data.model.*
import com.example.shapelearning.data.preferences.SettingsPreferences
import com.example.shapelearning.data.repository.*
import com.example.shapelearning.utils.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext // Added
import kotlinx.coroutines.Job // Added
import kotlinx.coroutines.flow.* // Added
import kotlinx.coroutines.launch
import java.util.Collections.emptyList
import javax.inject.Inject
import kotlin.math.max // Added

@HiltViewModel
class ShapeSortingViewModel @Inject constructor(
    private val shapeRepository: ShapeRepository,
    private val levelRepository: LevelRepository,
    private val userProgressRepository: UserProgressRepository,
    private val userRepository: UserRepository, // Added
    private val settingsPreferences: SettingsPreferences,
    @ApplicationContext private val context: Context // Added for strings
) : ViewModel() {

    private val _currentLevel = MutableLiveData<Level?>()
    // val currentLevel: LiveData<Level?> = _currentLevel

    private val _originalShapes = MutableLiveData<List<SortableShape>>(emptyList()) // Store original for reset

    private val _shapesToSort = MutableLiveData<List<SortableShape>>(emptyList())
    val shapesToSort: LiveData<List<SortableShape>> = _shapesToSort

    private val _category1Shapes = MutableLiveData<List<SortableShape>>(emptyList())
    val category1Shapes: LiveData<List<SortableShape>> = _category1Shapes

    private val _category2Shapes = MutableLiveData<List<SortableShape>>(emptyList())
    val category2Shapes: LiveData<List<SortableShape>> = _category2Shapes

    // Store the type and derive the text/names
    private val _sortingType = MutableLiveData<SortingType>(SortingType.COLOR) // Default
    // val sortingType: LiveData<SortingType> = _sortingType

    val sortingCriteriaText: LiveData<String> = _sortingType.map { type ->
        when (type) {
            SortingType.COLOR -> context.getString(R.string.sort_by_color) // Add string resources
            SortingType.SHAPE_TYPE -> context.getString(R.string.sort_by_shape_type)
            SortingType.CORNERS -> context.getString(R.string.sort_by_corners)
            // Add more criteria if needed
        }
    }
    val category1Name: LiveData<String> = _sortingType.map { type ->
        // TODO: Define category names based on the type and actual criteria values
        // Example: If COLOR, determine which color is category 1
        getCategoryName(type, 1)
    }
    val category2Name: LiveData<String> = _sortingType.map { type ->
        // TODO: Define category names based on the type and actual criteria values
        getCategoryName(type, 2)
    }

    // Use Event for one-time results
    private val _sortingResult = MutableLiveData<Event<SortingResult>>()
    val sortingResult: LiveData<Event<SortingResult>> = _sortingResult

    // Added states
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var shapeLoadingJobs = mutableListOf<Job>()

    // Define actual criteria values for the current level
    private var category1Color: Int? = null // R.color ID
    private var category2Color: Int? = null
    private var category1ShapeType: ShapeType? = null // ANGULAR or ROUND
    private var category2ShapeType: ShapeType? = null
    private var category1CornerCount: Int? = null
    private var category2CornerCount: Int? = null


    fun loadLevel(levelId: Int) {
        _isLoading.value = true
        _error.value = null
        shapeLoadingJobs.forEach { it.cancel() }
        shapeLoadingJobs.clear()

        viewModelScope.launch {
            levelRepository.getLevelById(levelId)
                .catch { e ->
                    Log.e("ShapeSortingVM", "Error loading level $levelId", e)
                    _error.postValue("Failed to load level.")
                    _isLoading.postValue(false)
                }
                .collectLatest { level ->
                    _currentLevel.value = level
                    if (level != null && level.shapes.isNotEmpty()) {
                        loadShapes(level.shapes)
                    } else if (level != null) {
                        Log.w("ShapeSortingVM", "Level $levelId has no shapes.")
                        _error.postValue("Level requires shapes for sorting game.")
                        _isLoading.postValue(false)
                    } else {
                        Log.e("ShapeSortingVM", "Level $levelId not found.")
                        _error.postValue("Level not found.")
                        _isLoading.postValue(false)
                    }
                }
        }
    }

    private fun loadShapes(shapeIds: List<Int>) {
        val loadedShapes = MutableList<Shape?>(shapeIds.size) { null }
        var loadedCount = 0

        shapeIds.forEachIndexed { index, shapeId ->
            val job = viewModelScope.launch {
                shapeRepository.getShapeById(shapeId)
                    .catch { e ->
                        Log.e("ShapeSortingVM", "Error loading shape $shapeId", e)
                        loadedCount++
                        if (loadedCount == shapeIds.size) finalizeShapeLoading(loadedShapes)
                    }
                    .collect { shape ->
                        loadedShapes[index] = shape
                        loadedCount++
                        if (loadedCount == shapeIds.size) finalizeShapeLoading(loadedShapes)
                    }
            }
            shapeLoadingJobs.add(job)
        }
    }

    private fun finalizeShapeLoading(loadedShapes: List<Shape?>) {
        val validShapes = loadedShapes.filterNotNull()
        if (validShapes.isEmpty() && loadedShapes.isNotEmpty()) {
            Log.e("ShapeSortingVM", "Failed to load any shapes for sorting.")
            _error.value = "Failed to load shapes for this level."
            _isLoading.value = false
            return
        } else if (validShapes.size < loadedShapes.size) {
            Log.w("ShapeSortingVM", "Some shapes failed to load for sorting.")
        }

        setupSortingGame(validShapes) // Setup game with loaded shapes
        _isLoading.value = false
    }

    private fun setupSortingGame(shapes: List<Shape>) {
        if (shapes.isEmpty()){
            Log.w("ShapeSortingVM", "Cannot setup sorting game, no valid shapes.")
            return
        }
        // Determine sorting type (can be random or defined by level data)
        // val levelSortingType = _currentLevel.value?.sortingCriteriaType // Example from Level data
        _sortingType.value = SortingType.values().random() // Keep random for now

        // --- Define actual criteria for this level ---
        // This needs more sophisticated logic based on the shapes provided
        // Example for COLOR: Find two dominant colors
        val colors = shapes.map { it.colorResId }.distinct()
        category1Color = colors.getOrNull(0)
        category2Color = colors.getOrNull(1)
        // Example for SHAPE_TYPE
        category1ShapeType = ShapeType.ANGULAR
        category2ShapeType = ShapeType.ROUND
        // Example for CORNERS
        val corners = shapes.map { it.corners }.distinct().sorted()
        category1CornerCount = corners.getOrNull(0) // e.g., 0 for circle/oval
        category2CornerCount = corners.lastOrNull() // e.g., 4 for square/rectangle


        // Convert shapes to SortableShapes based on the *actual* properties
        val sortableShapes = shapes.map { shape ->
            SortableShape(
                id = shape.id,
                imageResId = shape.imageResId,
                colorResId = shape.colorResId,
                shapeType = if (shape.corners > 0) ShapeType.ANGULAR else ShapeType.ROUND,
                cornerCount = shape.corners
            )
        }.shuffled()

        _originalShapes.value = sortableShapes // Save original order/state for potential reset
        _shapesToSort.value = sortableShapes
        _category1Shapes.value = emptyList()
        _category2Shapes.value = emptyList()
        _sortingResult.value = null // Reset result
    }

    // TODO: Implement dynamic category naming based on actual criteria
    private fun getCategoryName(type: SortingType, categoryIndex: Int): String {
        return when (type) {
            SortingType.COLOR -> context.getString(if (categoryIndex == 1) R.string.category_color_1 else R.string.category_color_2, "Renk 1") // Add string with placeholder %1$s
            SortingType.SHAPE_TYPE -> context.getString(if (categoryIndex == 1) R.string.category_shape_1 else R.string.category_shape_2) // e.g., Köşeli / Yuvarlak
            SortingType.CORNERS -> context.getString(if (categoryIndex == 1) R.string.category_corners_1 else R.string.category_corners_2, "Az Köşe") // Add string with placeholder %1$s
        }
    }


    fun moveShape(shape: SortableShape, sourceAdapterType: Int, targetAdapterType: Int) {
        if (sourceAdapterType == targetAdapterType) return // No move if dropped on same list type

        viewModelScope.launch { // Ensure updates happen on main thread if needed, though ListAdapter handles diffing
            // Remove from source
            when (sourceAdapterType) {
                ShapeSortingAdapter.SHAPES_SOURCE -> _shapesToSort.value = _shapesToSort.value?.minus(shape)
                ShapeSortingAdapter.CATEGORY_1 -> _category1Shapes.value = _category1Shapes.value?.minus(shape)
                ShapeSortingAdapter.CATEGORY_2 -> _category2Shapes.value = _category2Shapes.value?.minus(shape)
            }

            // Add to target
            when (targetAdapterType) {
                ShapeSortingAdapter.SHAPES_SOURCE -> _shapesToSort.value = _shapesToSort.value?.plus(shape)
                ShapeSortingAdapter.CATEGORY_1 -> _category1Shapes.value = _category1Shapes.value?.plus(shape)
                ShapeSortingAdapter.CATEGORY_2 -> _category2Shapes.value = _category2Shapes.value?.plus(shape)
            }
            Log.d("ShapeSortingVM", "Moved shape ${shape.id}. Source: $sourceAdapterType, Target: $targetAdapterType")
        }
    }

    fun checkSorting() {
        val cat1 = _category1Shapes.value ?: emptyList()
        val cat2 = _category2Shapes.value ?: emptyList()
        val remaining = _shapesToSort.value ?: emptyList()

        if (remaining.isNotEmpty()) {
            Log.d("ShapeSortingVM", "Check failed: Shapes remaining in source list.")
            _sortingResult.value = Event(SortingResult.INCOMPLETE)
            return
        }
        if (cat1.isEmpty() && cat2.isEmpty()) {
            Log.d("ShapeSortingVM", "Check failed: Both categories are empty.")
            _sortingResult.value = Event(SortingResult.FAILURE) // Or INCOMPLETE?
            return
        }


        // Validate based on the *current* sorting type and defined criteria
        val type = _sortingType.value ?: SortingType.COLOR // Default if null
        val isCorrect = when (type) {
            SortingType.COLOR -> {
                val cat1Correct = cat1.all { it.colorResId == category1Color } || cat1.isEmpty()
                val cat2Correct = cat2.all { it.colorResId == category2Color } || cat2.isEmpty()
                // Check if items in cat1 actually belong to cat2 and vice-versa
                val cat1HasWrong = cat1.any { it.colorResId == category2Color }
                val cat2HasWrong = cat2.any { it.colorResId == category1Color }
                cat1Correct && cat2Correct && !cat1HasWrong && !cat2HasWrong
            }
            SortingType.SHAPE_TYPE -> {
                val cat1Correct = cat1.all { it.shapeType == category1ShapeType } || cat1.isEmpty()
                val cat2Correct = cat2.all { it.shapeType == category2ShapeType } || cat2.isEmpty()
                val cat1HasWrong = cat1.any { it.shapeType == category2ShapeType }
                val cat2HasWrong = cat2.any { it.shapeType == category1ShapeType }
                cat1Correct && cat2Correct && !cat1HasWrong && !cat2HasWrong
            }
            SortingType.CORNERS -> {
                // This logic needs refinement based on how categories are defined (e.g., less than X, equal to Y)
                val cat1Correct = cat1.all { it.cornerCount == category1CornerCount } || cat1.isEmpty() // Example: exact match
                val cat2Correct = cat2.all { it.cornerCount == category2CornerCount } || cat2.isEmpty() // Example: exact match
                val cat1HasWrong = cat1.any { it.cornerCount == category2CornerCount }
                val cat2HasWrong = cat2.any { it.cornerCount == category1CornerCount }
                cat1Correct && cat2Correct && !cat1HasWrong && !cat2HasWrong
            }
        }

        Log.d("ShapeSortingVM", "Sorting check result: $isCorrect")
        _sortingResult.value = Event(if (isCorrect) SortingResult.SUCCESS else SortingResult.FAILURE)
    }

    // Optional: Function to reset shapes to the source list
    fun resetSorting() {
        _shapesToSort.value = _originalShapes.value
        _category1Shapes.value = emptyList()
        _category2Shapes.value = emptyList()
        _sortingResult.value = null
    }


    fun saveProgress() {
        // Save progress similar to other game modes
        val userId = settingsPreferences.getCurrentUserId()
        val level = _currentLevel.value

        if (userId == SettingsPreferences.NO_USER_ID || level == null) {
            Log.w("ShapeSortingVM", "Cannot save progress: Missing user or level data.")
            _error.value = "Could not save progress."
            return
        }

        viewModelScope.launch {
            try {
                // Sorting is usually pass/fail, grant max score/stars on success
                val stars = 3
                val score = 100 // Or score based on time/moves?

                val existingProgress = userProgressRepository.getUserProgressForLevel(userId, level.id).firstOrNull()
                val isFirstCompletion = existingProgress == null

                val newCompletionCount = (existingProgress?.completionCount ?: 0) + 1
                val highestScore = max(score, existingProgress?.score ?: 0)
                val highestStars = max(stars, existingProgress?.stars ?: 0)

                val userProgress = UserProgress(
                    userId = userId,
                    levelId = level.id,
                    stars = highestStars,
                    score = highestScore,
                    completionDate = System.currentTimeMillis(),
                    completionCount = newCompletionCount
                )

                userProgressRepository.saveOrUpdateUserProgress(userProgress)

                userRepository.addUserScore(userId, score)
                if (isFirstCompletion) {
                    userRepository.incrementCompletedLevelsCounter(userId)
                }
                Log.d("ShapeSortingVM", "Progress saved successfully for level ${level.id}")
            } catch (e: Exception) {
                Log.e("ShapeSortingVM", "Error saving progress for level ${level.id}", e)
                _error.postValue("Failed to save progress.")
            }
        }
    }

    // Call this from Fragment after showing the error message
    fun onErrorShown() {
        _error.value = null
    }


    // --- Enums and Data Class ---
    enum class SortingType {
        COLOR,
        SHAPE_TYPE, // Angular vs Round
        CORNERS // Example: 0 vs >0, or specific counts
        // Add SIZE, SIDES etc. if needed
    }

    // Internal representation of shape types for sorting logic
    enum class ShapeType {
        ANGULAR,
        ROUND
    }

    enum class SortingResult {
        INCOMPLETE, // Not all items sorted
        SUCCESS,
        FAILURE
    }

    // Data class holding properties relevant for sorting
    data class SortableShape(
        val id: Int, // To match original Shape ID
        val imageResId: Int,
        // Add properties used for sorting criteria
        val colorResId: Int?,
        val shapeType: ShapeType?,
        val cornerCount: Int?
        // val sizeType: SizeType? // Example
    )
}