plugins {
    // Kotlin support is built into AGP 9.0+; org.jetbrains.kotlin.android is no
    // longer applied (see https://kotl.in/gradle/agp-built-in-kotlin).
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
