package com.example.shapelearning.ui.games.discovery

import android.util.Log // Added
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map // map import'u LiveData dönüşümleri için kalabilir
import androidx.lifecycle.viewModelScope
import com.example.shapelearning.data.model.Level
import com.example.shapelearning.data.model.Shape
import com.example.shapelearning.data.repository.LevelRepository
import com.example.shapelearning.data.repository.ShapeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShapeDiscoveryViewModel @Inject constructor(
    private val shapeRepository: ShapeRepository,
    private val levelRepository: LevelRepository
) : ViewModel() {

    // _currentLevel'ı nullable (Level?) yapın
    private val _currentLevel = MutableLiveData<Level?>() // <<< DEĞİŞİKLİK BURADA
    // val currentLevel: LiveData<Level?> = _currentLevel // Expose if needed by UI

    private val _shapes = MutableLiveData<List<Shape>>(emptyList())

    private val _currentShape = MutableLiveData<Shape?>()
    val currentShape: LiveData<Shape?> = _currentShape

    private val _currentShapeIndex = MutableLiveData(0)

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    val isFirstShape: LiveData<Boolean> = _currentShapeIndex.map { index ->
        // shapes boşsa veya index 0 ise true döndür
        index == 0 || _shapes.value.isNullOrEmpty()
    }
    val isLastShape: LiveData<Boolean> = _currentShapeIndex.map { index ->
        val shapes = _shapes.value
        // shapes boşsa veya son index'teyse true döndür
        shapes.isNullOrEmpty() || index >= shapes.size - 1
    }


    private var shapeLoadingJobs = mutableListOf<Job>()

    fun loadLevel(levelId: Int) {
        _isLoading.value = true
        _error.value = null
        _currentLevel.value = null // Seviye yüklenirken önceki seviyeyi temizle
        _shapes.value = emptyList() // Şekilleri temizle
        _currentShape.value = null // Mevcut şekli temizle
        _currentShapeIndex.value = 0 // Index'i sıfırla
        shapeLoadingJobs.forEach { it.cancel() }
        shapeLoadingJobs.clear()

        viewModelScope.launch {
            levelRepository.getLevelById(levelId)
                .catch { e ->
                    Log.e("ShapeDiscoveryVM", "Error loading level $levelId", e)
                    _error.postValue("Failed to load level data.")
                    _isLoading.postValue(false)
                }
                .collectLatest { level -> // level burada nullable (Level?)
                    // Nullable LiveData'ya nullable değer ataması artık geçerli
                    _currentLevel.value = level // <<< ARTIK HATA VERMEMELİ

                    if (level != null) {
                        if (level.shapes.isNotEmpty()) {
                            loadShapes(level.shapes)
                        } else {
                            Log.w("ShapeDiscoveryVM", "Level $levelId has no shapes.")
                            _error.postValue("Level has no shapes.")
                            _shapes.postValue(emptyList())
                            _currentShape.postValue(null)
                            _isLoading.postValue(false)
                        }
                    } else {
                        Log.e("ShapeDiscoveryVM", "Level $levelId not found.")
                        _error.postValue("Level not found.")
                        _isLoading.postValue(false)
                        // shapes ve currentShape zaten null/empty olarak ayarlandı
                    }
                }
        }
    }

    // loadShapes ve finalizeShapeLoading fonksiyonları aynı kalabilir

    private fun loadShapes(shapeIds: List<Int>) {
        val loadedShapes = MutableList<Shape?>(shapeIds.size) { null }
        var loadedCount = 0

        // Ensure loading state remains true until shapes are loaded
        // _isLoading.value = true // Zaten loadLevel başında ayarlandı

        shapeIds.forEachIndexed { index, shapeId ->
            val job = viewModelScope.launch {
                shapeRepository.getShapeById(shapeId)
                    .catch { e ->
                        Log.e("ShapeDiscoveryVM", "Error loading shape $shapeId", e)
                        loadedCount++
                        if (loadedCount == shapeIds.size) finalizeShapeLoading(loadedShapes)
                    }
                    .collect { shape ->
                        loadedShapes[index] = shape
                        loadedCount++
                        // Finalize only after attempting to load all shapes
                        if (loadedCount == shapeIds.size) finalizeShapeLoading(loadedShapes)
                    }
            }
            shapeLoadingJobs.add(job)
        }
    }

    private fun finalizeShapeLoading(loadedShapes: List<Shape?>) {
        val validShapes = loadedShapes.filterNotNull()
        if (validShapes.isEmpty() && loadedShapes.isNotEmpty()) {
            Log.e("ShapeDiscoveryVM", "Failed to load any shapes.")
            _error.value = "Failed to load shapes for this level."
        } else if (validShapes.size < loadedShapes.size) {
            Log.w("ShapeDiscoveryVM", "Some shapes failed to load.")
        }

        _shapes.value = validShapes
        if (validShapes.isNotEmpty()) {
            // Set index only if shapes are loaded
            _currentShapeIndex.postValue(0) // postValue to ensure UI update from background if needed
            _currentShape.postValue(validShapes[0])
        } else {
            _currentShapeIndex.postValue(0) // Reset index even if no shapes
            _currentShape.postValue(null) // Set current shape to null if loading failed
        }
        _isLoading.postValue(false) // Set loading false after shapes processed
    }


    fun nextShape() {
        val currentShapes = _shapes.value ?: return
        val currentIndex = _currentShapeIndex.value ?: 0

        if (currentIndex < currentShapes.size - 1) {
            val nextIndex = currentIndex + 1
            _currentShapeIndex.value = nextIndex
            _currentShape.value = currentShapes[nextIndex]
        }
    }

    fun previousShape() {
        val currentShapes = _shapes.value ?: return
        val currentIndex = _currentShapeIndex.value ?: 0

        if (currentIndex > 0) {
            val prevIndex = currentIndex - 1
            _currentShapeIndex.value = prevIndex
            _currentShape.value = currentShapes[prevIndex]
        }
    }

    fun onErrorShown() {
        _error.value = null
    }
}