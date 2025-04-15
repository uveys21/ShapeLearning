package com.example.shapelearning.ui.games.puzzle

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
import com.example.shapelearning.data.repository.UserRepository // Added
import com.example.shapelearning.utils.Event // Added
import com.google.gson.Gson // Added
import com.google.gson.JsonSyntaxException // Added
import com.google.gson.reflect.TypeToken // Added
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job // Added
import kotlinx.coroutines.flow.* // Added
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max // Added

@HiltViewModel
class ShapePuzzleViewModel @Inject constructor(
    private val shapeRepository: ShapeRepository,
    private val levelRepository: LevelRepository,
    private val userProgressRepository: UserProgressRepository,
    private val userRepository: UserRepository, // Added
    private val settingsPreferences: SettingsPreferences,
    private val gson: Gson // Added for parsing positions
) : ViewModel() {

    private val _currentLevel = MutableLiveData<Level?>()
    // val currentLevel: LiveData<Level?> = _currentLevel

    private val _availableShapes = MutableLiveData<List<PuzzleShape>>(emptyList())
    val availableShapes: LiveData<List<PuzzleShape>> = _availableShapes

    // Map: Slot Index -> Placed Shape
    private val _placedShapesMap = MutableLiveData<Map<Int, PuzzleShape>>(emptyMap())
    val placedShapesMap: LiveData<Map<Int, PuzzleShape>> = _placedShapesMap

    private val _puzzleBackgroundResId = MutableLiveData<Int?>()
    val puzzleBackgroundResId: LiveData<Int?> = _puzzleBackgroundResId

    private val _selectedShape = MutableLiveData<PuzzleShape?>()
    val selectedShape: LiveData<PuzzleShape?> = _selectedShape

    private val _puzzleResult = MutableLiveData<Event<PuzzleResult>>()
    val puzzleResult: LiveData<Event<PuzzleResult>> = _puzzleResult

    // Added states
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Map: Slot Index -> Correct Shape ID (Loaded from Level)
    private val correctPuzzlePlacement = mutableMapOf<Int, Int>()
    private var shapeLoadingJobs = mutableListOf<Job>()
    private var originalAvailableShapes: List<PuzzleShape> = emptyList() // For reset

    fun loadLevel(levelId: Int) {
        _isLoading.value = true
        _error.value = null
        correctPuzzlePlacement.clear()
        shapeLoadingJobs.forEach { it.cancel() }
        shapeLoadingJobs.clear()

        viewModelScope.launch {
            levelRepository.getLevelById(levelId)
                .catch { e ->
                    Log.e("ShapePuzzleVM", "Error loading level $levelId", e)
                    _error.postValue("Failed to load level.")
                    _isLoading.postValue(false)
                }
                .collectLatest { level ->
                    _currentLevel.value = level
                    if (level != null && level.shapes.isNotEmpty()) {
                        // Load puzzle slot configuration first
                        parseShapePositions(level.shapePositionsJson)
                        if (correctPuzzlePlacement.isEmpty()){
                            Log.e("ShapePuzzleVM", "Failed to parse or empty shape positions for level $levelId")
                            _error.postValue("Level configuration error (positions).")
                            // Ensure correct number of shapes match position count
                            if(correctPuzzlePlacement.size != level.shapes.size){
                                Log.e("ShapePuzzleVM", "Mismatch between shape count (${level.shapes.size}) and position count (${correctPuzzlePlacement.size}) for level $levelId")
                                _error.postValue("Level configuration error (count mismatch).")
                            }
                        }
                        _puzzleBackgroundResId.postValue(level.backgroundImageResId)
                        loadShapes(level.shapes)

                    } else if (level != null) {
                        Log.w("ShapePuzzleVM", "Level $levelId has no shapes.")
                        _error.postValue("Level requires shapes for puzzle game.")
                        _isLoading.postValue(false)
                    } else {
                        Log.e("ShapePuzzleVM", "Level $levelId not found.")
                        _error.postValue("Level not found.")
                        _isLoading.postValue(false)
                    }
                }
        }
    }

    // TODO: Define a robust JSON structure for shape positions
    // Example JSON: {"positions": { "0": 1, "1": 3, "2": 2 }} (Slot Index -> Shape ID)
    private fun parseShapePositions(json: String?) {
        correctPuzzlePlacement.clear()
        if (json.isNullOrBlank()) {
            Log.e("ShapePuzzleVM", "Shape positions JSON is null or empty.")
            return
        }
        try {
            // Adjust parsing based on your actual JSON structure
            val type = object : TypeToken<Map<String, Map<String, Int>>>() {}.type
            val data: Map<String, Map<String, Int>>? = gson.fromJson(json, type)
            data?.get("positions")?.forEach { (key, value) ->
                correctPuzzlePlacement[key.toInt()] = value
            }
            Log.d("ShapePuzzleVM", "Parsed ${correctPuzzlePlacement.size} puzzle positions.")
        } catch (e: JsonSyntaxException) {
            Log.e("ShapePuzzleVM", "Error parsing shape positions JSON", e)
            _error.postValue("Level configuration error (parsing positions).")
        } catch (e: NumberFormatException) {
            Log.e("ShapePuzzleVM", "Error parsing slot index in shape positions JSON", e)
            _error.postValue("Level configuration error (parsing slot index).")
        }
    }


    private fun loadShapes(shapeIds: List<Int>) {
        val loadedShapes = MutableList<Shape?>(shapeIds.size) { null }
        var loadedCount = 0

        shapeIds.forEachIndexed { index, shapeId ->
            val job = viewModelScope.launch {
                shapeRepository.getShapeById(shapeId)
                    .catch { e ->
                        Log.e("ShapePuzzleVM", "Error loading shape $shapeId", e)
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
            Log.e("ShapePuzzleVM", "Failed to load any shapes for puzzle.")
            _error.value = "Failed to load shapes for this level."
            _isLoading.value = false
            return
        } else if (validShapes.size < loadedShapes.size) {
            Log.w("ShapePuzzleVM", "Some shapes failed to load for puzzle.")
        }

        setupPuzzle(validShapes)
        _isLoading.value = false
    }


    private fun setupPuzzle(shapes: List<Shape>) {
        // Create PuzzleShape objects for the available list
        val puzzleShapes = shapes.map { shape ->
            PuzzleShape(
                id = shape.id,
                imageResId = shape.imageResId,
                isPlaced = false
            )
        }.shuffled() // Shuffle the available shapes

        originalAvailableShapes = puzzleShapes // Store for reset
        _availableShapes.value = puzzleShapes
        _placedShapesMap.value = emptyMap() // Clear placed shapes
        _selectedShape.value = null // Clear selection
        _puzzleResult.value = null // Reset result
        Log.d("ShapePuzzleVM", "Puzzle setup complete with ${shapes.size} shapes.")
    }

    // Called when a shape in the RecyclerView is clicked
    fun selectShape(shape: PuzzleShape) {
        if (!shape.isPlaced) {
            _selectedShape.value = shape
            Log.d("ShapePuzzleVM", "Selected shape: ${shape.id}")
        }
    }

    // Called when a slot in the PuzzleAreaView is tapped
    fun placeSelectedShapeOnPuzzle(slotIndex: Int) {
        val shapeToPlace = _selectedShape.value
        val currentPlacedShapes = _placedShapesMap.value ?: emptyMap()

        if (shapeToPlace == null) {
            Log.d("ShapePuzzleVM", "Attempted to place but no shape selected.")
            // Optional: Provide feedback like "Select a shape first"
            return
        }

        if (shapeToPlace.isPlaced) {
            Log.d("ShapePuzzleVM", "Attempted to place an already placed shape: ${shapeToPlace.id}")
            return
        }

        if (currentPlacedShapes.containsKey(slotIndex)) {
            Log.d("ShapePuzzleVM", "Attempted to place shape in occupied slot: $slotIndex")
            // Optional: Feedback "Slot already filled"
            // Optional: Allow replacing? -> Need to move the existing shape back to available list
            return
        }

        // Place the shape
        val newPlacedMap = currentPlacedShapes.toMutableMap()
        val placedShape = shapeToPlace.copy(isPlaced = true)
        newPlacedMap[slotIndex] = placedShape
        _placedShapesMap.value = newPlacedMap
        Log.d("ShapePuzzleVM", "Placed shape ${placedShape.id} in slot $slotIndex")

        // Update the available shapes list (mark as placed)
        updateAvailableShapesList(placedShape)

        // Clear selection
        _selectedShape.value = null

        // Play sound for placement
        // TODO: Add validation sound? Play success sound only if correct?
        // For now, play generic place sound. Validation happens on Check.
        // audioManager.playSound(R.raw.shape_place) // Trigger sound from Fragment instead based on success?
    }


    // Helper to update the list displayed in the RecyclerView
    private fun updateAvailableShapesList(shapeJustPlaced: PuzzleShape) {
        _availableShapes.value = _availableShapes.value?.map {
            if (it.id == shapeJustPlaced.id) {
                it.copy(isPlaced = true) // Mark as placed in the source list
            } else {
                it
            }
        }
    }

    // Optional: Function to un-place a shape
    fun unplaceShape(slotIndex: Int) {
        val currentPlacedShapes = _placedShapesMap.value?.toMutableMap() ?: return
        val shapeToUnplace = currentPlacedShapes.remove(slotIndex)

        if (shapeToUnplace != null) {
            _placedShapesMap.value = currentPlacedShapes // Update map

            // Mark as available again in the RecyclerView list
            _availableShapes.value = _availableShapes.value?.map {
                if (it.id == shapeToUnplace.id) {
                    it.copy(isPlaced = false)
                } else {
                    it
                }
            }
            Log.d("ShapePuzzleVM", "Un-placed shape ${shapeToUnplace.id} from slot $slotIndex")
            // Optional: Select the un-placed shape automatically?
            // _selectedShape.value = shapeToUnplace.copy(isPlaced = false)
        }
    }


    fun checkPuzzle() {
        val placed = _placedShapesMap.value ?: emptyMap()

        // Check if all required slots are filled
        if (placed.size != correctPuzzlePlacement.size) {
            Log.d("ShapePuzzleVM", "Check failed: Incomplete. Placed: ${placed.size}, Required: ${correctPuzzlePlacement.size}")
            _puzzleResult.value = Event(PuzzleResult.INCOMPLETE)
            return
        }

        // Check if all placed shapes are in the correct slot
        var allCorrect = true
        for ((slotIndex, correctShapeId) in correctPuzzlePlacement) {
            val placedShape = placed[slotIndex]
            if (placedShape == null || placedShape.id != correctShapeId) {
                allCorrect = false
                Log.d("ShapePuzzleVM", "Check failed: Incorrect shape in slot $slotIndex. Expected $correctShapeId, Found ${placedShape?.id}")
                break // Exit loop early on first mistake
            }
        }

        Log.d("ShapePuzzleVM", "Puzzle check result: $allCorrect")
        _puzzleResult.value = Event(if (allCorrect) PuzzleResult.SUCCESS else PuzzleResult.FAILURE)
    }

    // Function to restart the level
    fun restartLevel() {
        Log.d("ShapePuzzleVM", "Restarting puzzle level.")
        _availableShapes.value = originalAvailableShapes.shuffled() // Reset available shapes
        _placedShapesMap.value = emptyMap() // Clear placed shapes
        _selectedShape.value = null
        _puzzleResult.value = null
    }

    fun saveProgress() {
        // Save progress similar to other game modes
        val userId = settingsPreferences.getCurrentUserId()
        val level = _currentLevel.value

        if (userId == SettingsPreferences.NO_USER_ID || level == null) {
            Log.w("ShapePuzzleVM", "Cannot save progress: Missing user or level data.")
            _error.value = "Could not save progress."
            return
        }

        viewModelScope.launch {
            try {
                // Grant max score/stars on successful puzzle completion
                val stars = 3
                val score = 100

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
                Log.d("ShapePuzzleVM", "Progress saved successfully for level ${level.id}")

            } catch (e: Exception) {
                Log.e("ShapePuzzleVM", "Error saving progress for level ${level.id}", e)
                _error.postValue("Failed to save progress.")
            }
        }
    }

    // Call this from Fragment after showing the error message
    fun onErrorShown() {
        _error.value = null
    }


    // --- Enums and Data Class ---
    enum class PuzzleResult {
        INCOMPLETE,
        SUCCESS,
        FAILURE
    }

    data class PuzzleShape(
        val id: Int,
        val imageResId: Int,
        val isPlaced: Boolean
    )
}