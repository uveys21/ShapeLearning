// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.navigation.safeargs) apply false
    alias(libs.plugins.hilt) apply false
}


tasks.register("clean", Delete::class) { // Bu blok kalmalı
    delete(rootProject.buildDir)
}

// Eğer hala ext { ... } bloğunu kullanıyorsanız (tavsiye edilmez), burada kalabilir.
// Eğer tamamen Version Catalog'a geçtiyseniz, aşağıdaki blokları silebilirsiniz.
/*
object Versions {
    const val kotlin = "1.8.0" // VEYA GÜNCEL VERSİYON
    // ... diğer versiyonlar ...
    const val hilt = "2.48" // VEYA GÜNCEL VERSİYON
    // ...
}

ext {
    set("kotlin_version", Versions.kotlin)
    // ... diğer set işlemleri ...
    set("hilt_version", Versions.hilt)
     // ...
}
*/