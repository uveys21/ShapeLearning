package com.example.shapelearning.ui.games.matching

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.shapelearning.data.model.Level
import com.example.shapelearning.data.model.Shape
import com.example.shapelearning.data.model.UserProgress
import com.example.shapelearning.data.preferences.SettingsPreferences
import com.example.shapelearning.data.repository.LevelRepository
import com.example.shapelearning.data.repository.ShapeRepository
import com.example.shapelearning.data.repository.UserProgressRepository
import com.example.shapelearning.data.repository.UserRepository
import com.example.shapelearning.utils.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShapeMatchingViewModel @Inject constructor(private val shapeRepository: ShapeRepository, private val levelRepository: LevelRepository, private val userProgressRepository: UserProgressRepository, private val userRepository: UserRepository, private val settingsPreferences: SettingsPreferences) : androidx.lifecycle.ViewModel() {

    private val _currentLevel = MutableLiveData<Level?>()
    // val currentLevel: LiveData<Level?> = _currentLevel

    private val _matchingCards = MutableLiveData<List<MatchingCard>>()
    val matchingCards: LiveData<List<MatchingCard>> = _matchingCards

    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score

    private val _gameCompleted = MutableLiveData<Event<Boolean>>()
    val gameCompleted: LiveData<Event<Boolean>> = _gameCompleted

    private val _matchResultEvent = MutableLiveData<Event<MatchResult>>()
    val matchResultEvent: LiveData<Event<MatchResult>> = _matchResultEvent

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading



    private var firstSelectedPosition: Int? = null
    private var secondSelectedPosition: Int? = null
    private var canSelect = true
    private var shapeLoadingJobs = mutableListOf<Job>()
    private var loadedShapesForLevel: List<Shape> = emptyList() // Store loaded shapes for restart

    fun loadLevel(levelId: Int) {
        _isLoading.value = true
        _score.value = 0
        resetSelections()
        canSelect = true
        shapeLoadingJobs.forEach { it.cancel() }
        shapeLoadingJobs.clear()

        androidx.lifecycle.viewModelScope.launch {
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
                        android.util.Log.w("ShapeMatchingVM", "Level $levelId has no shapes.")
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
            val job = androidx.lifecycle.viewModelScope.launch {
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
            android.util.Log.e("ShapeMatchingVM", "Failed to load any shapes for matching.")
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
        android.util.Log.d("ShapeMatchingVM", "Created ${cards.size} matching cards.")
    }

    fun onCardClicked(position: Int) {
        if (!canSelect) {
            android.util.Log.d("ShapeMatchingVM", "Cannot select card, waiting.")
            return
        }

        val currentCards = _matchingCards.value?.toMutableList() ?: return

        if (position < 0 || position >= currentCards.size) {
            android.util.Log.w("ShapeMatchingVM", "Invalid card position clicked: $position")
            return
        }
        val clickedCard = currentCards[position]

        // Ignore click if card is already matched or already flipped (and is the first selection)
        if (clickedCard.isMatched || (clickedCard.isFlipped && firstSelectedPosition == position)) {
            Log.d("ShapeMatchingVM", "Ignoring click on matched or already flipped card.")

            return
        }

        if (firstSelectedPosition != null && secondSelectedPosition != null) {
            Log.d("ShapeMatchingVM", "Two cards already selected.")

            return
        }


        // Flip the card
        currentCards[position] = clickedCard.copy(isFlipped = true)
        _matchingCards.value = currentCards // Update UI immediately

        if (firstSelectedPosition == null) {

            firstSelectedPosition = position
            _matchResultEvent.value = Event(MatchResult.FIRST_SELECTION) // Notify UI
            android.util.Log.d("ShapeMatchingVM", "First card selected at $position")
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
        androidx.lifecycle.viewModelScope.launch {
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

                Log.d("ShapeMatchingVM", "Match found!")
                val updatedCards = cards.toMutableList()
                updatedCards[firstPos] = firstCard.copy(isMatched = true)
                updatedCards[secondPos] = secondCard.copy(isMatched = true)
                _matchingCards.postValue(updatedCards) // Update UI

                _score.postValue((_score.value ?: 0) + 10) // Update score
                _matchResultEvent.postValue(Event(MatchResult.MATCH)) // Notify UI

                // Check for game completion
                if(updatedCards.all { it.isMatched }) {
                    android.util.Log.d("ShapeMatchingVM", "All cards matched!")
                    _gameCompleted.postValue(Event(true)) // Trigger completion event
                    saveProgress() // Save progress automatically on completion
                }
                resetSelections() // Reset selections for next pair
                canSelect = true // Allow selection again immediately



            } else {

                Log.d("ShapeMatchingVM", "No match.")
                _matchResultEvent.postValue(Event(MatchResult.NO_MATCH)) // Notify UI

                // Wait briefly, then flip back
                delay(1000L)


                // Check if state hasn't changed during delay
                val currentCardsAfterDelay = _matchingCards.value ?: return@launch
                if(currentCardsAfterDelay.size <= firstPos || currentCardsAfterDelay.size <= secondPos) {
                    android.util.Log.w("ShapeMatchingVM", "Card list changed during no-match delay. Aborting flip back.")
                    resetSelections()
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


    fun restartLevel() {
        android.util.Log.d("ShapeMatchingVM", "Restarting level.")
        createMatchingCards(loadedShapesForLevel)
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



        androidx.lifecycle.viewModelScope.launch {
            val totalCards = _matchingCards.value?.size ?: 2
            val maxPossibleScore = (totalCards / 2) * 10
            val stars = when {
                currentScore >= maxPossibleScore * 0.9 -> 3
                currentScore >= maxPossibleScore * 0.6 -> 2
                currentScore > 0 -> 1
                else -> 0
            }

            val existingProgress = userProgressRepository.getUserProgressForLevel(userId, level.id).firstOrNull()
            val isFirstCompletion = existingProgress == null
            val newCompletionCount = (existingProgress?.completionCount ?: 0) + 1
            val highestScore = kotlin.math.max(currentScore, existingProgress?.score ?: 0)
            val highestStars = kotlin.math.max(stars, existingProgress?.stars ?: 0)


            val userProgress = UserProgress(userId = userId, levelId = level.id, stars = highestStars, score = highestScore, completionDate = System.currentTimeMillis(), completionCount = newCompletionCount)
            try{
                userProgressRepository.saveOrUpdateUserProgress(userProgress)
            } catch (e: Exception) {
                android.util.Log.e("ShapeMatchingVM", "Error saving progress for level ${level.id}, user $userId", e)

            }


            userRepository.addUserScore(userId, currentScore)

            if(isFirstCompletion) {
                userRepository.incrementCompletedLevelsCounter(userId)
            }

            android.util.Log.d("ShapeMatchingVM", "Progress saved for level ${level.id}, user $userId. Score: $currentScore, Stars: $stars (Highest: $highestScore, $highestStars)")
        }


    }




    // --- Data class and Enum ---
    enum class MatchResult {
        FIRST_SELECTION,
        MATCH,
        NO_MATCH
    }

    data class MatchingCard(val shapeId: Int, val imageResId: Int, val isFlipped: Boolean, val isMatched: Boolean)
}