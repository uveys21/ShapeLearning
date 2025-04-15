package com.example.shapelearning.ui.main
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
// import androidx.navigation.ui.setupActionBarWithNavController // Removed if no ActionBar
import com.example.shapelearning.R
import com.example.shapelearning.data.preferences.SettingsPreferences
import com.example.shapelearning.databinding.ActivityMainBinding
import com.example.shapelearning.service.audio.AudioManager
import com.example.shapelearning.utils.LocaleHelper // Added
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    @Inject
    lateinit var settingsPreferences: SettingsPreferences // Keep for audio settings
    @Inject
    lateinit var audioManager: AudioManager
    @Inject
    lateinit var localeHelper: LocaleHelper // Inject LocaleHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        // Önce super.onCreate() çağrılır, bu Hilt enjeksiyonunu tetikler.
        super.onCreate(savedInstanceState)
        
        // Artık Hilt enjeksiyonu tamamlandığı için localeHelper'a erişim güvenlidir.
        localeHelper.setLocale(this)
        
        // UI Inflation ve diğer kurulumlar
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigation()
        initAudio()
        // Opsiyonel: Locale değişikliklerini gözlemleme
    }
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        // setupActionBarWithNavController(navController) // Only if using ActionBar
    }
    private fun initAudio() {
        // Load sound/music enabled status from preferences
        val soundEnabled = settingsPreferences.getSoundEnabled()
        val musicEnabled = settingsPreferences.getMusicEnabled()
        audioManager.setSoundEnabled(soundEnabled)
        audioManager.setMusicEnabled(musicEnabled)
        // Start background music if enabled
        if (musicEnabled) {
            audioManager.playBackgroundMusic(R.raw.main_theme)
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        // Handle Up navigation using the NavController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
    override fun onPause() {
        super.onPause()
        // Pause music when the activity is not in the foreground
        audioManager.pauseBackgroundMusic()
    }
    override fun onResume() {
        super.onResume()
        // Resume music if enabled when the activity comes back to the foreground
        // AudioManager internally checks if music should be enabled
        audioManager.resumeBackgroundMusic()
    }
    override fun onDestroy() {
        super.onDestroy()
        // Release audio resources when the activity is destroyed
        audioManager.releaseResources()
    }
}
