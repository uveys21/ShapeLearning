plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt") // Hilt ve Room için gerekli
    kotlin("plugin.parcelize") // Parcelable için
    alias(libs.plugins.navigation.safeargs) // Navigation Safe Args için
    alias(libs.plugins.hilt) // Hilt için
}
android {
    namespace = "com.example.shapelearning" // Paket adınızı doğrulayın
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.shapelearning"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Room şema konumu
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
        vectorDrawables { // Bu bloğu kullanmak gerekebilir
            useSupportLibrary = true
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true // Yayınlama için true yapıldı (performans iyileştirmesi)
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Debug için özel ayarlar buraya eklenebilir
        }
    }
    // JVM Ayarları - Sadece yeni yaklaşım kullanıldı
    kotlin {
        jvmToolchain(17) // Veya JDK 17 kullanıyorsanız 17
    }
    // Eski compileOptions/kotlinOptions bloğu tamamen kaldırıldı
    
    buildFeatures {
        dataBinding = true // Eğer aktif kullanıyorsanız
        viewBinding = true // Eğer aktif kullanıyorsanız (genellikle biri tercih edilir)
    }
}
// Espresso sürümünü zorlama (Önceki hatayı çözmek için)
configurations.all {
    resolutionStrategy {
        force("androidx.test.espresso:espresso-core:3.5.1")
    }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    // Navigation bağımlılıkları - Tekrarlar kaldırıldı
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler) // Hilt için KAPT işlemcisi
    // UI Kütüphaneleri
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    // Material kütüphanesi - Tekrar kaldırıldı
    implementation(libs.material) // com.google.android.material:material
    // Gson
    implementation(libs.gson)
    // Splash Screen
    implementation(libs.androidx.core.splashscreen)
    // Room
    implementation(libs.room.runtime)
    kapt(libs.room.compiler) // Room için KAPT işlemcisi
    implementation(libs.room.ktx) // Room Kotlin uzantıları
    // Test Bağımlılıkları
    testImplementation(libs.junit) // Unit testler için
    androidTestImplementation(libs.androidx.junit) // Android Instrument testler için
    androidTestImplementation(libs.androidx.espresso.core) // Espresso UI testleri için
    // UI test bağımlılığı sadece androidTestImplementation kapsamında
    androidTestImplementation(libs.androidx.ui.test.android)
}
// KAPT bloğu (Hilt veya başka KAPT gerektiren bağımlılıklar için gereklidir)
kapt {
    correctErrorTypes = true
}
