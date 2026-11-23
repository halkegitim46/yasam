// AGP 9: Kotlin yerleşiktir ('org.jetbrains.kotlin.android' eklentisi kullanılmaz). Compose eklentisi (sürümü = Kotlin sürümü)
// aynı zamanda derleme sınıf yoluna Kotlin Gradle eklentisini taşır; böylece AGP'nin varsayılan Kotlin'i (2.2.10) yerine 2.4.x kullanılır.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
