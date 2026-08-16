plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "com.orlune.app"
    compileSdk = 37
    // Pinned to what's already installed on the SDK (build-tools/36.0.0) — this is also
    // AGP 9.2/9.3's documented default for compileSdk 37, so no extra download is triggered.
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.orlune.app"
        minSdk = 29
        // Deliberately one behind compileSdk: Android 17/API 37 shipped 2026-06-16 and
        // changes default behavior (e.g. orientation-lock handling). Targeting 36 avoids
        // opting into those changes until they're deliberately reviewed.
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // With AGP 9's built-in Kotlin support, the Kotlin compiler's jvmTarget defaults
    // to this targetCompatibility automatically — no separate kotlinOptions/toolchain
    // needed (https://kotl.in/gradle/agp-built-in-kotlin), and no JDK 17 toolchain
    // download is triggered.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// Room schema history (for future migrations) is written here instead of the
// default location so it's easy to find and review under version control.
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
}
