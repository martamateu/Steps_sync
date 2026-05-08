plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.stepssync"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.stepssync"
        // Health Connect requires minSdk 26 (Android 8.0)
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        // Enable Java 8+ time APIs on older API levels via desugaring
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // WorkManager – background periodic execution
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Health Connect – step data
    implementation("androidx.health.connect:connect-client:1.1.0-alpha07")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // OkHttp – HTTP POST to webhook
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Desugaring for java.time on API < 26 (safety net)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
