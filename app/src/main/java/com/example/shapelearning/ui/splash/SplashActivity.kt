package com.example.shapelearning.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen // <<<--- IMPORT EKLEYİN
import androidx.lifecycle.lifecycleScope
// import com.example.shapelearning.R // setContentView kaldırıldığı için R importu gerekmeyebilir
import com.example.shapelearning.ui.main.MainActivity
import com.example.shapelearning.utils.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var localeHelper: LocaleHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        // --- installSplashScreen() ÇAĞRISINI super.onCreate'DAN ÖNCE EKLEYİN ---
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Locale'i burada ayarlamak hala mantıklı
        localeHelper.setLocale(this)

        // --- setContentView'İ KALDIRIN VEYA YORUM SATIRI YAPIN ---
        // setContentView(R.layout.activity_splash) // Tema zaten görseli sağlıyor

        // Opsiyonel: Splash'ı bir koşula bağlı tutmak için (örn: veri yüklenene kadar)
        // var isLoading = true // Durumu takip eden bir değişken
        // splashScreen.setKeepOnScreenCondition { isLoading }
        // // Yükleme bitince: isLoading = false

        // Coroutine ile gecikme ve navigasyon
        lifecycleScope.launch {
            // Gerekirse burada başlangıç işlemleri yapılabilir
            delay(SPLASH_DELAY)
            // isLoading = false // Eğer setKeepOnScreenCondition kullandıysanız
            navigateToMainActivity()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        // finish() // CLEAR_TASK nedeniyle gerekli değil
    }

    companion object {
        private const val SPLASH_DELAY = 1500L
    }
}