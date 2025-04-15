package com.example.shapelearning.ui.games.tracing

import android.util.Log // Added
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shapelearning.data.model.Level
import com.example.shapelearning.data.model.Shape
import com.example.shapelearning.data.model.UserProgress
import com.example.shapelearning.data.preferences.SettingsPreferences
import com.example.shapelearning.data.repository.LevelRepository
import com.example.shapelearning.data.repository.ShapeRepository
import com.example.shapelearning.data.repository.UserProgressRepository
import com.example.shapelearning.data.repository.UserRepository // Added for user score/level count
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch // Added
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull // Added
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max // Added

@HiltViewModel
class ShapeTracingViewModel @Inject constructor(
    private val shapeRepository: ShapeRepository,
    private val levelRepository: LevelRepository,
    private val userProgressRepository: UserProgressRepository,
    private val userRepository: UserRepository, // Added
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val _currentLevel = MutableLiveData<Level?>()
    // val currentLevel: LiveData<Level?> = _currentLevel

    private val _currentShape = MutableLiveData<Shape?>()
    val currentShape: LiveData<Shape?> = _currentShape

    // Modified: Result is now the accuracy score
    private val _tracingResult = MutableLiveData<Float?>()
    val tracingResult: LiveData<Float?> = _tracingResult

    // Added states
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _progressSaved = MutableLiveData<Boolean>(false)
    val progressSaved: LiveData<Boolean> = _progressSaved

    private var targetOutlinePath: List<Pair<Float, Float>> = emptyList() // Store target path


    fun loadLevel(levelId: Int) {
        _isLoading.value = true
        _error.value = null
        _progressSaved.value = false // Reset save status
        viewModelScope.launch {
            levelRepository.getLevelById(levelId)
                .catch { e ->
                    Log.e("ShapeTracingVM", "Error loading level $levelId", e)
                    _error.postValue("Failed to load level.")
                    _isLoading.postValue(false)
                }
                .collectLatest { level ->
                    _currentLevel.value = level
                    if (level != null && level.shapes.isNotEmpty()) {
                        // Load the first shape for tracing (can be extended for multi-shape levels)
                        loadShape(level.shapes[0])
                    } else if (level != null) {
                        Log.w("ShapeTracingVM", "Level $levelId has no shapes.")
                        _error.postValue("Level has no shapes.")
                        _isLoading.postValue(false)
                    } else {
                        Log.e("ShapeTracingVM", "Level $levelId not found.")
                        _error.postValue("Level not found.")
                        _isLoading.postValue(false)
                    }
                }
        }
    }

    private fun loadShape(shapeId: Int) {
        viewModelScope.launch {
            shapeRepository.getShapeById(shapeId)
                .catch { e ->
                    Log.e("ShapeTracingVM", "Error loading shape $shapeId", e)
                    _error.postValue("Failed to load shape.")
                    _currentShape.postValue(null) // Clear shape on error
                    _isLoading.postValue(false)
                }
                .collectLatest { shape ->
                    _currentShape.value = shape
                    if(shape != null) {
                        // TODO: Load actual outline points for the shape
                        targetOutlinePath = getTargetOutlinePathForShape(shape)
                    } else {
                        Log.e("ShapeTracingVM", "Shape $shapeId is null.")
                        _error.postValue("Shape data is missing.")
                    }
                    _isLoading.value = false
                }
        }
    }

    // TODO: Implement this method to get actual outline points from Shape data or another source
    private fun getTargetOutlinePathForShape(shape: Shape): List<Pair<Float, Float>> {
        Log.w("ShapeTracingVM", "getTargetOutlinePathForShape not implemented for ${shape.id}. Using dummy data.")
        // Replace with actual logic based on shape.outlinePointsJson or similar
        return listOf(Pair(0.1f, 0.1f), Pair(0.9f, 0.1f), Pair(0.9f, 0.9f), Pair(0.1f, 0.9f), Pair(0.1f, 0.1f)) // Dummy square
    }


    fun checkTracing(tracingPath: List<Pair<Float, Float>>) {
        if (targetOutlinePath.isEmpty() || tracingPath.isEmpty()) {
            _tracingResult.postValue(0.0f) // Indicate failure if paths are missing
            return
        }

        // --- TODO: Implement a robust tracing accuracy algorithm ---
        val accuracy = calculateTracingAccuracy(tracingPath, targetOutlinePath)
        _tracingResult.postValue(accuracy)
    }

    // TODO: Implement a real accuracy calculation (e.g., using point distance, Hausdorff, etc.)
    private fun calculateTracingAccuracy(
        drawnPath: List<Pair<Float, Float>>,
        targetPath: List<Pair<Float, Float>>
    ): Float {
        Log.w("ShapeTracingVM", "calculateTracingAccuracy using placeholder logic!")
        // Very basic placeholder: Check distance of first/last points (BAD ALGORITHM)
        if (drawnPath.isEmpty() || targetPath.isEmpty()) return 0.0f
        val startDist = distance(drawnPath.first(), targetPath.first())
        val endDist = distance(drawnPath.last(), targetPath.last())
        // Normalize based on some expected max distance (e.g., diagonal = sqrt(2))
        val accuracy = 1.0f - ((startDist + endDist) / (2 * 1.414f))
        return accuracy.coerceIn(0.0f, 1.0f) * (drawnPath.size.toFloat() / 100f).coerceAtMost(1.0f) // Penalize short paths
    }

    private fun distance(p1: Pair<Float, Float>, p2: Pair<Float, Float>): Float {
        val dx = p1.first - p2.first
        val dy = p1.second - p2.second
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }


    fun saveProgress(accuracy: Float) {
        val userId = settingsPreferences.getCurrentUserId()
        val level = _currentLevel.value
        val shape = _currentShape.value

        if (userId == SettingsPreferences.NO_USER_ID || level == null || shape == null) {
            Log.w("ShapeTracingVM", "Cannot save progress: Missing user, level, or shape data.")
            _error.value = "Could not save progress."
            return
        }

        _progressSaved.value = false // Reset status
        viewModelScope.launch {
            try {
                // Calculate stars based on accuracy
                val stars = when {
                    accuracy >= 0.85f -> 3
                    accuracy >= 0.65f -> 2
                    accuracy >= 0.50f -> 1 // Success threshold defined in fragment is 0.7, but allow saving 1 star if >= 0.5? Adjust as needed.
                    else -> 0 // Should not happen if saveProgress is called on success, but good to handle
                }
                // Calculate score based on accuracy/stars
                val score = (accuracy * 100).toInt() + stars * 10 // Example scoring

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

                // Update user's total score and completed level count (only if first completion)
                userRepository.addUserScore(userId, score) // Add score from this attempt? Or only update total if new high score? Decide logic.
                if (isFirstCompletion) {
                    userRepository.incrementCompletedLevelsCounter(userId)
                }
                _progressSaved.postValue(true) // Indicate success
                Log.d("ShapeTracingVM", "Progress saved for level ${level.id}, user $userId. Score: $score, Stars: $stars")

            } catch (e: Exception) {
                Log.e("ShapeTracingVM", "Error saving progress for level ${level.id}, user $userId", e)
                _error.postValue("Failed to save progress.")
                _progressSaved.postValue(false)
            }
        }
    }

    // Call this from Fragment after showing the result/error
    fun onResultShown() {
        _tracingResult.value = null
    }
    fun onErrorShown() {
        _error.value = null
    }
}