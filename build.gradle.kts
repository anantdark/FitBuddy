// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization) apply false
}

buildscript {
    // AGP 9.3.0's transitive deps (ddmlib, layoutlib-api, coroutines-core) need
    // org.jetbrains:annotations 23.0.0, but Gradle's embedded Kotlin stdlib pins
    // it strictly to 13.0 on the buildscript classpath. Force the newer version.
    configurations.classpath {
        resolutionStrategy {
            force("org.jetbrains:annotations:23.0.0")
        }
    }
}