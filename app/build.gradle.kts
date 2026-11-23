import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

fun signingEnv(name: String): String? =
    (System.getenv(name) ?: System.getenv("KFT_$name"))?.takeIf { it.isNotBlank() }

val keystorePath: String? = signingEnv("KEYSTORE_PATH")

android {
    namespace = "com.kisisel.finans"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kisisel.finans"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }

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
            val minify = (project.findProperty("kft.minify") as String?) != "false"
            isMinifyEnabled = minify
            isShrinkResources = minify
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePath != null) signingConfig = signingConfigs.getByName("shared")
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources { localeFilters += "tr" }

    buildFeatures { compose = true }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        // Robolectric'in jar'ları Maven Central'dan indirmek yerine yerel dizinden okumasını sağla
        unitTests.all {
            it.systemProperty(
                "robolectric.dependency.dir",
                layout.buildDirectory.dir("robolectric-deps").get().asFile.absolutePath
            )
        }
    }

    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

kotlin { jvmToolchain(17) }

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Robolectric'in ihtiyaç duyduğu android-all-instrumented jar'ını taşımak için ayrı konfigürasyon
val robolectricDeps: Configuration by configurations.creating

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.ui:ui") { version { strictly("1.12.1") } }
    implementation("androidx.compose.foundation:foundation") { version { strictly("1.12.1") } }
    implementation("androidx.compose.foundation:foundation-layout") { version { strictly("1.12.1") } }
    implementation("androidx.compose.animation:animation") { version { strictly("1.12.1") } }
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

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.coroutines.android)

    // Robolectric'in runtime'da indirmeye çalıştığı jar'ı Gradle bağımlılığı olarak tanımla
    // Sürüm numarası, hata günlüğünüzdeki 14-robolectric-10818077-i7 ile eşleşmelidir
    robolectricDeps("org.robolectric:android-all-instrumented:14-robolectric-10818077-i7")
}

// robolectricDeps konfigürasyonundaki jar'ları build/robolectric-deps dizinine kopyala
val prepareRobolectricDeps by tasks.registering(Copy::class) {
    from(robolectricDeps)
    into(layout.buildDirectory.dir("robolectric-deps"))
}

// Tüm Test görevlerinin önce jar'ların kopyalanmasını beklemesini sağla
tasks.withType<Test>().configureEach {
    dependsOn(prepareRobolectricDeps)
    testLogging {
        events("failed")
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
    }
}

gradle.taskGraph.whenReady {
    val wantsRelease = allTasks.any { t ->
        t.project == project && (t.name == "assembleRelease" || t.name == "bundleRelease" || t.name == "packageRelease")
    }
    if (wantsRelease && keystorePath == null) {
        throw GradleException(
            "Release imzası yok: KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS ve KEY_PASSWORD ortam değişkenleri tanımlı olmalı."
        )
    }
}