package com.example.shapelearning.ui.games.hunt

import android.util.Log // Added
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shapelearning.R
import com.example.shapelearning.data.model.Level
import com.example.shapelearning.data.model.Shape
import com.example.shapelearning.data.model.UserProgress
import com.example.shapelearning.data.preferences.SettingsPreferences
import com.example.shapelearning.data.repository.LevelRepository
import com.example.shapelearning.data.repository.ShapeRepository
import com.example.shapelearning.data.repository.UserProgressRepository
import com.example.shapelearning.data.repository.UserRepository // Added
import com.example.shapelearning.service.audio.AudioManager // Added for sounds
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
import kotlin.math.sqrt // Added

@HiltViewModel
class ShapeHuntViewModel @Inject constructor(
    private val shapeRepository: ShapeRepository,
    private val levelRepository: LevelRepository,
    private val userProgressRepository: UserProgressRepository,
    private val userRepository: UserRepository, // Added
    private val settingsPreferences: SettingsPreferences,
    private val audioManager: AudioManager, // Added
    private val gson: Gson // Added
) : ViewModel() {

    private val _currentLevel = MutableLiveData<Level?>()
    // val currentLevel: LiveData<Level?> = _currentLevel

    private val _targetShape = MutableLiveData<Shape?>()
    val targetShape: LiveData<Shape?> = _targetShape

    private val _huntSceneResId = MutableLiveData<Int?>()
    val huntSceneResId: LiveData<Int?> = _huntSceneResId

    // List of shapes that have been successfully found
    private val _foundShapesList = MutableLiveData<List<FoundShape>>(emptyList())
    val foundShapesList: LiveData<List<FoundShape>> = _foundShapesList

    private val _totalShapesToFind = MutableLiveData<Int>(0)
    val totalShapesToFind: LiveData<Int> = _totalShapesToFind

    private val _huntCompleted = MutableLiveData<Event<Boolean>>()
    val huntCompleted: LiveData<Event<Boolean>> = _huntCompleted

    // Event for feedback when clicking an already found shape
    private val _shapeAlreadyFoundEvent = MutableLiveData<Event<Unit>>()
    val shapeAlreadyFoundEvent: LiveData<Event<Unit>> = _shapeAlreadyFoundEvent

    // Added states
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Internal list of hidden shape positions [Pair(x, y)] loaded from level data
    private var hiddenShapePositions: List<Pair<Float, Float>> = emptyList()
    private var shapeLoadingJobs = mutableListOf<Job>()

    private var currentLevelId : Int? = null // Store for restart

    // Constants
    private val FIND_DISTANCE_THRESHOLD = 0.08f // Adjusted threshold (8% of view dimension)

    fun loadLevel(levelId: Int) {
        if (_isLoading.value == true && currentLevelId == levelId) return // Avoid reloading same level if already loading
        currentLevelId = levelId
        _isLoading.value = true
        _error.value = null
        hiddenShapePositions = emptyList()
        _foundShapesList.value = emptyList() // Clear previous finds
        shapeLoadingJobs.forEach { it.cancel() }
        shapeLoadingJobs.clear()

        viewModelScope.launch {
            levelRepository.getLevelById(levelId)
                .catch { e ->
                    Log.e("ShapeHuntVM", "Error loading level $levelId", e)
                    _error.postValue("Failed to load level.")
                    _isLoading.postValue(false)
                }
                .collectLatest { level ->
                    _currentLevel.value = level
                    if (level != null && level.shapes.isNotEmpty()) {
                        // Assume first shape in list is the target
                        val targetShapeId = level.shapes[0]
                        loadTargetShape(targetShapeId) // Load target shape details

                        // Load positions and scene
                        parseShapePositions(level.shapePositionsJson) // Parse positions first
                        _huntSceneResId.postValue(level.huntSceneResId)
                        _totalShapesToFind.postValue(hiddenShapePositions.size)

                        if (hiddenShapePositions.isEmpty()) {
                            Log.e("ShapeHuntVM", "Failed to parse or empty shape positions for level $levelId")
                            _error.postValue("Level configuration error (positions).")
                        }

                    } else if (level != null) {
                        Log.w("ShapeHuntVM", "Level $levelId has no shapes defined.")
                        _error.postValue("Level requires a target shape for hunt game.")
                        _isLoading.postValue(false)
                    } else {
                        Log.e("ShapeHuntVM", "Level $levelId not found.")
                        _error.postValue("Level not found.")
                        _isLoading.postValue(false)
                    }
                }
        }
    }

    private fun loadTargetShape(shapeId: Int) {
        viewModelScope.launch {
            shapeRepository.getShapeById(shapeId)
                .catch { e ->
                    Log.e("ShapeHuntVM", "Error loading target shape $shapeId", e)
                    _error.postValue("Failed to load target shape.")
                    _targetShape.postValue(null)
                    _isLoading.postValue(false) // Stop loading on error
                }
                .collectLatest { shape ->
                    _targetShape.value = shape
                    if (shape == null) {
                        Log.e("ShapeHuntVM", "Target shape $shapeId is null.")
                        _error.postValue("Target shape data missing.")
                    }
                    // Only set isLoading false once shape is loaded *and* positions parsed (handled in loadLevel)
                    _isLoading.value = false // Set loading false after shape is loaded
                }
        }
    }

    // TODO: Define a robust JSON structure for shape positions
    // Example JSON: {"targetShapeId": 1, "positions": [ [0.2, 0.3], [0.5, 0.2], ... ]}
    private fun parseShapePositions(json: String?) {
        hiddenShapePositions = emptyList()
        if (json.isNullOrBlank()) {
            Log.e("ShapeHuntVM", "Shape positions JSON is null or empty.")
            return
        }
        try {
            // Adjust parsing based on your actual JSON structure
            // This example assumes a simple list of coordinate pairs
            val type = object : TypeToken<Map<String, List<List<Float>>>>() {}.type
            val data: Map<String, List<List<Float>>>? = gson.fromJson(json, type)
            hiddenShapePositions = data?.get("positions")?.mapNotNull { coords ->
                if (coords.size == 2) Pair(coords[0], coords[1]) else null
            } ?: emptyList()

            Log.d("ShapeHuntVM", "Parsed ${hiddenShapePositions.size} hidden shape positions.")
        } catch (e: JsonSyntaxException) {
            Log.e("ShapeHuntVM", "Error parsing shape positions JSON", e)
            _error.postValue("Level configuration error (parsing positions).")
        } catch (e: Exception) { // Catch other potential errors during parsing
            Log.e("ShapeHuntVM", "Unexpected error parsing shape positions JSON", e)
            _error.postValue("Level configuration error (parsing positions).")
        }
    }


    // Called from Fragment when user taps the hunt area
    fun validateFoundShape(tapPosition: Pair<Float, Float>) {
        if (_isLoading.value == true) return // Ignore taps while loading

        val targetShapeId = _targetShape.value?.id
        if (targetShapeId == null) {
            Log.w("ShapeHuntVM", "Target shape not loaded, cannot validate tap.")
            return
        }

        val currentFound = _foundShapesList.value ?: emptyList()

        // Find the closest hidden shape position to the tap
        var closestHiddenPos: Pair<Float, Float>? = null
        var minDistance = Float.MAX_VALUE

        for (hiddenPos in hiddenShapePositions) {
            val distance = calculateDistance(tapPosition, hiddenPos)
            if (distance < minDistance) {
                minDistance = distance
                closestHiddenPos = hiddenPos
            }
        }

        // Check if the closest position is within the threshold
        if (closestHiddenPos != null && minDistance < FIND_DISTANCE_THRESHOLD) {
            // Check if this specific hidden position has already been found
            val alreadyFound = currentFound.any { calculateDistance(it.position, closestHiddenPos!!) < 0.01f } // Check if already in list

            if (!alreadyFound) {
                // Shape Found!
                Log.d("ShapeHuntVM", "Shape found near: $closestHiddenPos")
                val newFoundShape = FoundShape(targetShapeId, closestHiddenPos)
                val updatedList = currentFound + newFoundShape // Add to list
                _foundShapesList.postValue(updatedList) // Update LiveData

                audioManager.playSound(R.raw.shape_found) // Play success sound

                // Check for completion
                if (updatedList.size == _totalShapesToFind.value) {
                    _huntCompleted.postValue(Event(true))
                    saveProgress()
                }
            } else {
                // Tapped near an already found shape
                Log.d("ShapeHuntVM", "Tapped near already found shape at: $closestHiddenPos")
                _shapeAlreadyFoundEvent.postValue(Event(Unit)) // Trigger feedback event
            }
        } else {
            // Tap was not close enough to any hidden shape
            Log.d("ShapeHuntVM", "Tap at $tapPosition was not close enough to any shape (min dist: $minDistance).")
            // Optional: Play a 'miss' sound?
            // audioManager.playSound(R.raw.hunt_miss)
        }
    }

    private fun calculateDistance(p1: Pair<Float, Float>, p2: Pair<Float, Float>): Float {
        val dx = p1.first - p2.first
        val dy = p1.second - p2.second
        return sqrt(dx * dx + dy * dy)
    }

    fun restartLevel() {
        Log.d("ShapeHuntVM", "Restarting hunt level.")
        // Reset found list, keep loaded level data
        _foundShapesList.value = emptyList() // Boş liste ataması doğru

        // _huntCompleted'a null atamayın! Event zaten tüketildi veya hiç gönderilmedi.
        // _huntCompleted.value = null // <<< BU SATIRI SİLİN veya YORUMLAYIN

        // İsteğe bağlı: Eğer başka bir state (örn. "restarted") bildirmek isterseniz
        // ayrı bir LiveData<Event<Unit>> kullanabilirsiniz.
    }


    fun saveProgress() {
        val userId = settingsPreferences.getCurrentUserId()
        val level = _currentLevel.value
        val foundCount = _foundShapesList.value?.size ?: 0
        val totalCount = _totalShapesToFind.value ?: 0

        if (userId == SettingsPreferences.NO_USER_ID || level == null || totalCount == 0) {
            Log.w("ShapeHuntVM", "Cannot save progress: Missing user, level, or total count data.")
            _error.value = "Could not save progress."
            return
        }

        viewModelScope.launch {
            try {
                // Calculate stars based on proportion found (even if not 100%)
                val stars = when {
                    foundCount == totalCount -> 3
                    foundCount >= totalCount * 0.6 -> 2
                    foundCount > 0 -> 1
                    else -> 0
                }
                // Score based on number found
                val score = foundCount * 20 // Example: 20 points per shape found

                val existingProgress = userProgressRepository.getUserProgressForLevel(userId, level.id).firstOrNull()
                val isFirstCompletion = existingProgress == null && foundCount == totalCount // Only count first *full* completion

                val newCompletionCount = (existingProgress?.completionCount ?: 0) + if(foundCount == totalCount) 1 else 0 // Increment only on full completion
                val highestScore = max(score, existingProgress?.score ?: 0)
                val highestStars = max(stars, existingProgress?.stars ?: 0)

                val userProgress = UserProgress(
                    userId = userId,
                    levelId = level.id,
                    stars = highestStars,
                    score = highestScore,
                    completionDate = System.currentTimeMillis(), // Update date on any save
                    completionCount = newCompletionCount
                )

                userProgressRepository.saveOrUpdateUserProgress(userProgress)

                userRepository.addUserScore(userId, score) // Add score achieved in this attempt
                if (isFirstCompletion) {
                    userRepository.incrementCompletedLevelsCounter(userId)
                }
                Log.d("ShapeHuntVM", "Progress saved successfully for level ${level.id}. Found: $foundCount/$totalCount")

            } catch (e: Exception) {
                Log.e("ShapeHuntVM", "Error saving progress for level ${level.id}", e)
                _error.postValue("Failed to save progress.")
            }
        }
    }

    // Call this from Fragment after showing the error message
    fun onErrorShown() {
        _error.value = null
    }

    // --- Data Class ---
    // Represents a shape that has been successfully found by the user
    data class FoundShape(
        val shapeId: Int, // ID of the shape type found
        val position: Pair<Float, Float> // The *actual hidden position* that was found (not the tap position)
    )
}