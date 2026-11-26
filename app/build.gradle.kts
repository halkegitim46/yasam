import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// İmza bilgileri ortam değişkenlerinden okunur. GÜNCEL adlar: KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD.
// Geriye uyumluluk için eski KFT_ önekli adlar da kabul edilir (KFT_KEYSTORE_PATH, ...). Tek yerde tanımlı: ad karışıklığı olmasın.
fun signingEnv(name: String): String? =
    (System.getenv(name) ?: System.getenv("KFT_$name"))?.takeIf { it.isNotBlank() }

val keystorePath: String? = signingEnv("KEYSTORE_PATH")

android {
    namespace = "com.kisisel.finans"
    // compileSdk 37.1: Compose 1.13 (BOM 2026.11.01) en az 37.1 ister (checkDebugAarMetadata). targetSdk ayrıdır:
    // 36 (Android 16) çalışma zamanı davranışını belirler; compileSdk yalnızca derleme zamanı API'leridir.
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.kisisel.finans"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }   // SQLCipher yerel kütüphaneleri
    }

    // Sabit imza anahtarı (CI'da GitHub Secrets'tan gelir). Aynı anahtar = her sürüm eskisinin üstüne veri kaybı olmadan kurulur.
    signingConfigs {
        create("shared") {
            if (keystorePath != null) {
                storeFile = file(requireNotNull(keystorePath))
                storePassword = signingEnv("KEYSTORE_PASSWORD")
                keyAlias = signingEnv("KEY_ALIAS")
                keyPassword = signingEnv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            if (keystorePath != null) signingConfig = signingConfigs.getByName("shared")
        }
        release {
            // R8 küçültme varsayılan açık; sorun çıkarsa -Pkft.minify=false ile kapatılabilir.
            val minify = (project.findProperty("kft.minify") as String?) != "false"
            isMinifyEnabled = minify
            isShrinkResources = minify
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePath != null) signingConfig = signingConfigs.getByName("shared")
        }
    }

    // Lint raporu üretilir ama release derlemesini engellemez (ayrıntı: app/build/reports).
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // AGP 9 yeni DSL: resourceConfigurations yerine androidResources.localeFilters
    androidResources { localeFilters += "tr" }

    buildFeatures { compose = true }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

// AGP 9 yerleşik Kotlin: 'kotlin' uzantısı android {} bloğunun DIŞINDA tanımlanır.
kotlin { jvmToolchain(17) }

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.work.runtime)
    implementation(libs.datastore)
    implementation(libs.biometric)
    implementation(libs.androidx.fragment)
    implementation("net.zetetic:sqlcipher-android:${libs.versions.sqlcipher.get()}@aar")
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)

    // Cihaz/emülatör testleri (gerçek SQLCipher ve Android Keystore): "Cihaz testleri" workflow'u veya connectedDebugAndroidTest
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.coroutines.android)
}

// Başarısız testlerin ayrıntısı (hata mesajı ve yığın) CI log'unda görünsün.
tasks.withType<Test>().configureEach {
    testLogging {
        events("failed")
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
    }
}

// Release derlemesi imza bilgisi olmadan sessizce "unsigned" APK üretmesin: açık bir hata ver.
gradle.taskGraph.whenReady {
    val wantsRelease = allTasks.any { t ->
        t.project == project && (t.name == "assembleRelease" || t.name == "bundleRelease" || t.name == "packageRelease")
    }
    if (wantsRelease && keystorePath == null) {
        throw GradleException(
            "Release imzası yok: KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS ve KEY_PASSWORD ortam değişkenleri tanımlı olmalı " +
                "(CI'da secrets'tan .github/scripts/prepare-signing.sh yazar; ayrıntı: README 'İmza anahtarı'). Debug için assembleDebug kullanın."
        )
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// Robolectric: SDK 34 (Android 14) çalışma zamanı jar'ı (~100 MB) varsa ÇEVRİMDIŞI kullanılır.
// CI, jar'ı testten önce app/build/robolectric-deps içine indirir (yansıma + yeniden deneme + önbellek); GitHub runner'larında
// Maven Central "HTTP 429 Too Many Requests" verip testleri düşürüyordu. Jar yoksa (yerel geliştirme) Robolectric kendisi indirir.
// Robolectric yükseltilince libs.versions.toml'daki robolectricAndroidAll da güncellenmelidir.
// ---------------------------------------------------------------------------------------------------------------------
val robolectricDepsDir = layout.buildDirectory.dir("robolectric-deps").get().asFile
val robolectricJar = robolectricDepsDir.resolve("android-all-instrumented-${libs.versions.robolectricAndroidAll.get()}.jar")

tasks.withType<Test>().configureEach {
    if (robolectricJar.exists()) {
        systemProperty("robolectric.offline", "true")
        systemProperty("robolectric.dependency.dir", robolectricDepsDir.absolutePath)
    }
}
