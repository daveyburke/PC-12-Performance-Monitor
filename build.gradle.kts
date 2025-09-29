// Top-level build file (build.gradle.kts)
plugins {
    // These aliases come from your libs.versions.toml file
    alias(libs.plugins.android.application) version "8.13.0" apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}
