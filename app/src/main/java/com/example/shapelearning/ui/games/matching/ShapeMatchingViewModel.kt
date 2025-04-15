package com.example.shapelearning.ui.games.matching

import android.util.Log // Added
import androidx.lifecycle.* // Added
import com.example.shapelearning.data.model.Level
import com.example.shapelearning.data.model.Shape
import com.example.shapelearning.data.model.UserProgress
import com.example.shapelearning.data.preferences.SettingsPreferences
import com.example.shapelearning.data.repository.LevelRepository
import com.example.shapelearning.data.repository.ShapeRepository
import com.example.shapelearning.data.repository.UserProgressRepository
import com.example.shapelearning.data.repository.UserRepository // Added
import com.example.shapelearning.utils.Event // Added (Create Event wrapper class)
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job // Added
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.* // Added
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max // Added

@HiltViewModel
class ShapeMatchingViewModel @Inject constructor(
    private val shapeRepository: ShapeRepository,
    private val levelRepository: LevelRepository,
    private val userProgressRepository: UserProgressRepository,
    private val userRepository: UserRepository, // Added
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val _currentLevel = MutableLiveData<Level?>()
    // val currentLevel: LiveData<Level?> = _currentLevel

    private val _matchingCards = MutableLiveData<List<MatchingCard>>()
    val matchingCards: LiveData<List<MatchingCard>> = _matchingCards

    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score

    // Use event for one-time actions like completion dialog
    private val _gameCompleted = MutableLiveData<Event<Boolean>>()
    val gameCompleted: LiveData<Event<Boolean>> = _gameCompleted

    // Use event for match result feedback to avoid re-triggering on config change
    private val _matchResultEvent = MutableLiveData<Event<MatchResult>>()
    val matchResultEvent: LiveData<Event<MatchResult>> = _matchResultEvent

    // Added states
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error


    private var firstSelectedPosition: Int? = null
    private var secondSelectedPosition: Int? = null
    private var canSelect = true
    private var shapeLoadingJobs = mutableListOf<Job>()
    private var loadedShapesForLevel: List<Shape> = emptyList() // Store loaded shapes for restart

    fun loadLevel(levelId: Int) {
        _isLoading.value = true
        _error.value = null
        _score.value = 0 // Reset score
        resetSelections()
        canSelect = true
        shapeLoadingJobs.forEach { it.cancel() }
        shapeLoadingJobs.clear()

        viewModelScope.launch {
            levelRepository.getLevelById(levelId)
                .catch { e ->
                    Log.e("ShapeMatchingVM", "Error loading level $levelId", e)
                    _error.postValue("Failed to load level.")
                    _isLoading.postValue(false)
                }
                .collectLatest { level ->
                    _currentLevel.value = level
                    if (level != null && level.shapes.isNotEmpty()) {
                        loadShapes(level.shapes)
                    } else if (level != null) {
                        Log.w("ShapeMatchingVM", "Level $levelId has no shapes.")
                        _error.postValue("Level requires shapes for matching game.")
                        _isLoading.postValue(false)
                    } else {
                        Log.e("ShapeMatchingVM", "Level $levelId not found.")
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
                        Log.e("ShapeMatchingVM", "Error loading shape $shapeId", e)
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
            Log.e("ShapeMatchingVM", "Failed to load any shapes for matching.")
            _error.value = "Failed to load shapes for this level."
            _isLoading.value = false
            return
        } else if (validShapes.size < loadedShapes.size) {
            Log.w("ShapeMatchingVM", "Some shapes failed to load.")
        }
        loadedShapesForLevel = validShapes // Store for restart
        createMatchingCards(validShapes)
        _isLoading.value = false
    }


    private fun createMatchingCards(shapes: List<Shape>) {
        if (shapes.isEmpty()) {
            _matchingCards.value = emptyList()
            return
        }
        val cards = mutableListOf<MatchingCard>()
        shapes.forEach { shape ->
            // Need two cards per shape for matching
            cards.add(MatchingCard(shape.id, shape.imageResId, false, false))
            cards.add(MatchingCard(shape.id, shape.imageResId, false, false))
        }
        cards.shuffle()
        _matchingCards.value = cards
        _score.value = 0 // Reset score when new cards are set
        _gameCompleted.value = Event(false) // Reset completion status
        resetSelections()
        canSelect = true
        Log.d("ShapeMatchingVM", "Created ${cards.size} matching cards.")
    }

    fun onCardClicked(position: Int) {
        if (!canSelect) {
            Log.d("ShapeMatchingVM", "Cannot select card, waiting.")
            return
        }

        val currentCards = _matchingCards.value?.toMutableList() ?: return
        // Check position bounds
        if (position < 0 || position >= currentCards.size) {
            Log.w("ShapeMatchingVM", "Invalid card position clicked: $position")
            return
        }
        val clickedCard = currentCards[position]

        // Ignore click if card is already matched or already flipped (and is the first selection)
        if (clickedCard.isMatched || (clickedCard.isFlipped && firstSelectedPosition == position)) {
            Log.d("ShapeMatchingVM", "Ignoring click on matched or already flipped card.")
            return
        }

        // Prevent clicking more than two cards
        if (firstSelectedPosition != null && secondSelectedPosition != null) {
            Log.d("ShapeMatchingVM", "Two cards already selected.")
            return
        }


        // Flip the card
        currentCards[position] = clickedCard.copy(isFlipped = true)
        _matchingCards.value = currentCards // Update UI immediately

        if (firstSelectedPosition == null) {
            // First card selected
            firstSelectedPosition = position
            _matchResultEvent.value = Event(MatchResult.FIRST_SELECTION) // Notify UI
            Log.d("ShapeMatchingVM", "First card selected at $position")
        } else {
            // Second card selected (check it's not the same card)
            if (firstSelectedPosition == position) return // Should not happen due to earlier check, but safety first

            secondSelectedPosition = position
            Log.d("ShapeMatchingVM", "Second card selected at $position")
            canSelect = false // Disable further selections until check is complete
            checkMatch()
        }
    }

    private fun checkMatch() {
        viewModelScope.launch { // Use coroutine for delay
            val cards = _matchingCards.value ?: return@launch
            val firstPos = firstSelectedPosition
            val secondPos = secondSelectedPosition

            if (firstPos == null || secondPos == null) {
                Log.e("ShapeMatchingVM", "checkMatch called with null positions")
                resetSelections() // Reset state
                canSelect = true
                return@launch
            }

            val firstCard = cards[firstPos]
            val secondCard = cards[secondPos]

            if (firstCard.shapeId == secondCard.shapeId) {
                // Match found!
                Log.d("ShapeMatchingVM", "Match found!")
                val updatedCards = cards.toMutableList()
                updatedCards[firstPos] = firstCard.copy(isMatched = true)
                updatedCards[secondPos] = secondCard.copy(isMatched = true)
                _matchingCards.postValue(updatedCards) // Update UI

                _score.postValue((_score.value ?: 0) + 10) // Update score
                _matchResultEvent.postValue(Event(MatchResult.MATCH)) // Notify UI

                // Check for game completion
                if (updatedCards.all { it.isMatched }) {
                    Log.d("ShapeMatchingVM", "All cards matched!")
                    _gameCompleted.postValue(Event(true)) // Trigger completion event
                    saveProgress() // Save progress automatically on completion
                }
                resetSelections() // Reset selections for next pair
                canSelect = true // Allow selection again immediately


            } else {
                // No match
                Log.d("ShapeMatchingVM", "No match.")
                _matchResultEvent.postValue(Event(MatchResult.NO_MATCH)) // Notify UI

                // Wait briefly, then flip back
                delay(1000L)

                // Check if state hasn't changed during delay
                val currentCardsAfterDelay = _matchingCards.value ?: return@launch
                if(currentCardsAfterDelay.size <= firstPos || currentCardsAfterDelay.size <= secondPos) {
                    Log.w("ShapeMatchingVM", "Card list changed during no-match delay. Aborting flip back.")
                    resetSelections()
                    canSelect = true
                    return@launch
                }
                // Only flip back if they are still flipped and not matched (safety check)
                if (currentCardsAfterDelay[firstPos].isFlipped && !currentCardsAfterDelay[firstPos].isMatched &&
                    currentCardsAfterDelay[secondPos].isFlipped && !currentCardsAfterDelay[secondPos].isMatched)
                {
                    val updatedCards = currentCardsAfterDelay.toMutableList()
                    updatedCards[firstPos] = cards[firstPos].copy(isFlipped = false)
                    updatedCards[secondPos] = cards[secondPos].copy(isFlipped = false)
                    _matchingCards.postValue(updatedCards)
                    Log.d("ShapeMatchingVM", "Flipping cards back at $firstPos and $secondPos")
                } else {
                    Log.d("ShapeMatchingVM", "Cards state changed during delay, not flipping back.")
                }

                resetSelections()
                canSelect = true // Allow selection again
            }
        }
    }

    private fun resetSelections() {
        firstSelectedPosition = null
        secondSelectedPosition = null
        // canSelect is managed within the checkMatch coroutine flow
    }

    // Called by Fragment when completion dialog is shown/handled
    fun onGameCompleteShown() {
        // Optional: Prevent dialog from showing again on config change
        // _gameCompleted.value = Event(false) // Or handle via Event wrapper
    }

    // Function to restart the current level
    fun restartLevel() {
        Log.d("ShapeMatchingVM", "Restarting level.")
        createMatchingCards(loadedShapesForLevel) // Recreate cards from stored shapes
    }


    private fun saveProgress() {
        val userId = settingsPreferences.getCurrentUserId()
        val level = _currentLevel.value
        val currentScore = _score.value ?: 0

        if (userId == SettingsPreferences.NO_USER_ID || level == null) {
            Log.w("ShapeMatchingVM", "Cannot save progress: Missing user or level data.")
            _error.value = "Could not save progress." // Show error via LiveData
            return
        }

        viewModelScope.launch {
            try {
                // Calculate stars based on score (adjust thresholds as needed)
                val totalCards = _matchingCards.value?.size ?: 2
                val maxPossibleScore = (totalCards / 2) * 10 // 10 points per pair
                val stars = when {
                    currentScore >= maxPossibleScore * 0.9 -> 3 // 90% for 3 stars
                    currentScore >= maxPossibleScore * 0.6 -> 2 // 60% for 2 stars
                    currentScore > 0 -> 1 // Any score > 0 gets 1 star
                    else -> 0
                }

                val existingProgress = userProgressRepository.getUserProgressForLevel(userId, level.id).firstOrNull()
                val isFirstCompletion = existingProgress == null

                val newCompletionCount = (existingProgress?.completionCount ?: 0) + 1
                val highestScore = max(currentScore, existingProgress?.score ?: 0)
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

                // Update user's total score and completed level count
                userRepository.addUserScore(userId, currentScore) // Decide if score should be additive or based on high score delta
                if (isFirstCompletion) {
                    userRepository.incrementCompletedLevelsCounter(userId)
                }
                Log.d("ShapeMatchingVM", "Progress saved for level ${level.id}, user $userId. Score: $currentScore, Stars: $stars (Highest: $highestScore, $highestStars)")

            } catch (e: Exception) {
                Log.e("ShapeMatchingVM", "Error saving progress for level ${level.id}, user $userId", e)
                _error.postValue("Failed to save progress.")
            }
        }
    }

    // Call this from Fragment after showing the error message
    fun onErrorShown() {
        _error.value = null
    }

    // --- Data class and Enum ---
    enum class MatchResult {
        FIRST_SELECTION,
        MATCH,
        NO_MATCH
    }

    // Ensure Parcelize if passed between fragments, otherwise not needed
    data class MatchingCard(
        val shapeId: Int,
        val imageResId: Int,
        val isFlipped: Boolean,
        val isMatched: Boolean
    )
}