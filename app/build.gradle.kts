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
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Verified safe this session: a full on-device pass (onboarding, Room
            // reads/writes, the blocking overlay + BlockReason display, quiet mode,
            // and the delete-all-data flow) against an R8-minified,
            // resource-shrunk build produced no crashes or missing-class/resource
            // failures with the default AndroidX/Compose/Room/WorkManager consumer
            // ProGuard rules — no custom keep rules were needed. See
            // docs/PROJECT_STATE.md for the verification detail.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // No release signingConfig yet — no signing key exists for this project
            // (see docs/PROJECT_STATE.md / AGENTS.MD "Release rules"). Generating one
            // is a deliberate, hard-to-reverse decision for the user to make, not
            // something to create unprompted; this build type intentionally builds
            // unsigned until that happens.
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
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
