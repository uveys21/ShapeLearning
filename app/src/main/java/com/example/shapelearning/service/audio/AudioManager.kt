package com.example.shapelearning.service.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log // Added
import androidx.annotation.RawRes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope // Added
import kotlinx.coroutines.Dispatchers // Added
import kotlinx.coroutines.SupervisorJob // Added
import kotlinx.coroutines.launch // Added
import java.io.IOException // Added
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ses yönetimi sınıfı
 * Arka plan müziği ve ses efektlerini yönetir
 */
@Singleton
class AudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var soundPool: SoundPool? = null
    // Map: @RawRes Int -> SoundPool ID (Int)
    private val soundMap = mutableMapOf<Int, Int>()
    private var mediaPlayer: MediaPlayer? = null

    private var soundEnabled = true
    private var musicEnabled = true

    // Coroutine scope for background loading
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        initialize()
    }

    private fun initialize() {
        scope.launch { // Initialize on IO thread
            initSoundPool()
            // Optional: Preload common sounds here
            // preloadSounds(listOf(R.raw.button_click, R.raw.success, R.raw.failure))
        }
    }

    private fun initSoundPool() {
        if (soundPool != null) return // Avoid re-initialization

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8) // Increased max streams slightly
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.setOnLoadCompleteListener { pool, sampleId, status ->
            if (status == 0) {
                val resId = soundMap.entries.find { it.value == sampleId }?.key
                Log.d("AudioManager", "Sound loaded successfully: ID $sampleId (Res ID: $resId)")
            } else {
                val resId = soundMap.entries.find { it.value == sampleId }?.key
                Log.e("AudioManager", "Failed to load sound: ID $sampleId (Res ID: $resId), Status: $status")
                // Remove from map if failed?
                resId?.let { soundMap.remove(it) }
            }
        }
        Log.d("AudioManager", "SoundPool initialized.")
    }

    /**
     * Ses efektini yükle ve SoundPool ID'sini döndür.
     * Hata durumunda 0 döndürür.
     */
    private fun loadSoundInternal(@RawRes soundResId: Int): Int {
        if (soundPool == null) {
            Log.e("AudioManager", "SoundPool not initialized, cannot load sound $soundResId")
            return 0
        }
        if (soundMap.containsKey(soundResId)) {
            return soundMap[soundResId] ?: 0
        }

        val soundId = try {
            soundPool?.load(context, soundResId, 1) ?: 0
        } catch (e: Exception) {
            Log.e("AudioManager", "Exception loading sound resource ID: $soundResId", e)
            0
        }

        if (soundId != 0) {
            soundMap[soundResId] = soundId
            Log.d("AudioManager", "Sound loading initiated for Res ID: $soundResId -> SoundPool ID: $soundId")
        } else {
            Log.e("AudioManager", "Failed to initiate load for sound resource ID: $soundResId")
        }
        return soundId
    }

    /**
     * Asenkron olarak belirtilen sesleri önceden yükle.
     */
    fun preloadSounds(soundResIds: List<Int>) {
        scope.launch {
            if (soundPool == null) initSoundPool() // Ensure initialized
            soundResIds.forEach { resId ->
                if (!soundMap.containsKey(resId)) {
                    loadSoundInternal(resId)
                    // Small delay between loads might help SoundPool
                    kotlinx.coroutines.delay(50)
                }
            }
            Log.d("AudioManager", "Preloading sounds completed for: $soundResIds")
        }
    }

    /**
     * Ses efekti çal. Gerekirse sesi yükler.
     */
    fun playSound(@RawRes soundResId: Int, volume: Float = 1.0f, rate: Float = 1.0f, loop: Int = 0) {
        if (!soundEnabled) return
        if (soundPool == null) {
            Log.w("AudioManager", "SoundPool not ready, cannot play sound $soundResId")
            return
        }


        val soundId = soundMap[soundResId] ?: loadSoundInternal(soundResId) // Get existing or load

        if (soundId != 0) {
            // Check if sound is actually loaded (load is async) - ideally wait for onLoadComplete,
            // but playing immediately often works, SoundPool queues it.
            scope.launch(Dispatchers.Main) { // Play on Main thread recommended by some docs
                soundPool?.play(soundId, volume.coerceIn(0f,1f), volume.coerceIn(0f,1f), 1, loop, rate.coerceIn(0.5f, 2.0f))
                Log.d("AudioManager", "Playing sound Res ID: $soundResId (SoundPool ID: $soundId)")
            }
        } else {
            Log.e("AudioManager", "Cannot play sound, failed to load or invalid ID for Res ID: $soundResId")
        }
    }

     /**
      * Ortak ses efektini çal.
      */
     fun playSoundEffect(@RawRes soundResId: Int) {
         playSound(soundResId) // Use the existing playSound with default values
     }

    /**
     * Arka plan müziği çal. Mevcut müziği durdurur.
     */
    fun playBackgroundMusic(@RawRes musicResId: Int, loop: Boolean = true) {
        if (!musicEnabled) {
            Log.d("AudioManager", "Music disabled, not playing background music.")
            return
        }

        releaseMediaPlayer() // Stop and release previous music if any

        try {
            mediaPlayer = MediaPlayer.create(context, musicResId)?.apply {
                isLooping = loop
                setOnPreparedListener {
                    Log.d("AudioManager", "Background music prepared, starting: $musicResId")
                    start()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("AudioManager", "MediaPlayer error: What $what, Extra $extra for Res ID $musicResId")
                    releaseMediaPlayer() // Clean up on error
                    true // Error handled
                }
            }
            if (mediaPlayer == null) {
                Log.e("AudioManager", "MediaPlayer.create returned null for Res ID: $musicResId")
            }
        } catch (e: IOException) {
            Log.e("AudioManager", "IOException creating MediaPlayer for Res ID: $musicResId", e)
            releaseMediaPlayer()
        } catch (e: Exception) {
            Log.e("AudioManager", "Exception creating MediaPlayer for Res ID: $musicResId", e)
            releaseMediaPlayer()
        }
    }

    /**
     * Arka plan müziğini duraklat.
     */
    fun pauseBackgroundMusic() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                Log.d("AudioManager", "Background music paused.")
            }
        } catch (e: IllegalStateException) {
            Log.e("AudioManager", "Error pausing MediaPlayer", e)
            releaseMediaPlayer() // Reset if state is invalid
        }
    }

    /**
     * Arka plan müziğini devam ettir (eğer müzik etkinse).
     */
    fun resumeBackgroundMusic() {
        if (!musicEnabled) return // Don't resume if globally disabled

        try {
            if (mediaPlayer != null && mediaPlayer?.isPlaying == false) {
                // Check if mediaPlayer is in a prepared state before starting
                // Note: This check is complex. Easier to just try starting.
                mediaPlayer?.start()
                Log.d("AudioManager", "Background music resumed.")
            }
        } catch (e: IllegalStateException) {
            Log.e("AudioManager", "Error resuming MediaPlayer", e)
            releaseMediaPlayer() // Reset if state is invalid
        }
    }

    /**
     * Arka plan müziğini durdur ve kaynakları serbest bırak.
     */
    fun stopBackgroundMusic() {
        releaseMediaPlayer()
        Log.d("AudioManager", "Background music stopped and released.")
    }

    /**
     * Ses efektlerini etkinleştir/devre dışı bırak.
     */
    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
        Log.d("AudioManager", "Sound effects ${if (enabled) "enabled" else "disabled"}.")
        if (!enabled) {
            // Optional: Stop currently playing sounds? SoundPool doesn't have a global stop easily.
            // soundPool?.autoPause() // Might work depending on usage
        } else {
            // soundPool?.autoResume()
        }
    }

    /**
     * Müziği etkinleştir/devre dışı bırak.
     */
    fun setMusicEnabled(enabled: Boolean) {
        if (musicEnabled == enabled) return // No change
        musicEnabled = enabled
        Log.d("AudioManager", "Background music ${if (enabled) "enabled" else "disabled"}.")
        if (enabled) {
            // If music should be enabled, try resuming/starting if mediaPlayer exists
            resumeBackgroundMusic()
            // If mediaPlayer is null, maybe replay last known track? Needs state saving.
            // if (mediaPlayer == null && lastPlayedMusicResId != null) playBackgroundMusic(lastPlayedMusicResId)
        } else {
            // If disabling, pause the music
            pauseBackgroundMusic()
        }
    }

    /**
     * Tüm ses kaynaklarını (SoundPool ve MediaPlayer) serbest bırak.
     * Genellikle uygulamanın ana Activity'sinin onDestroy'unda çağrılır.
     */
    fun releaseResources() {
        Log.d("AudioManager", "Releasing all audio resources.")
        releaseMediaPlayer()
        releaseSoundPool()
        // Cancel coroutine scope jobs if needed, though SupervisorJob handles child failures
        // scope.cancel() // Usually not needed unless app is fully closing
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            try {
                if (isPlaying) {
                    stop()
                }
                // Reset listeners to prevent callbacks after release
                setOnPreparedListener(null)
                setOnErrorListener(null)
                reset() // Reset state machine
                release() // Release resources
            } catch (e: Exception) {
                Log.e("AudioManager", "Exception releasing MediaPlayer", e)
            }
        }
        mediaPlayer = null
    }

    private fun releaseSoundPool() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
        Log.d("AudioManager", "SoundPool released.")
    }
}