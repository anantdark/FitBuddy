import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

// Read secrets from local.properties (never checked into VCS) so the OpenRouter/Gemini
// key stays out of source control. Missing key -> app falls back to the offline simulator.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val openRouterApiKey: String = localProperties.getProperty("OPENROUTER_API_KEY", "")
val aiModel: String = localProperties.getProperty("AI_MODEL", "google/gemma-4-31b-it:free")

// Sentry DSN and the cloud-backup proxy API key are committed here obfuscated (XOR + Base64)
// rather than injected via CI secrets — DSNs are meant to be embeddable, and the proxy key
// only grants read/upsert-by-id on the backup endpoint (no delete route exists server-side),
// so a leaked/committed copy is inert the moment the key is rotated in Vercel.
val sentryDsnMaskSeed = "fitbuddy.sentry.v1"
val sentryDsnBlobEscaped = "Dh0AEgZeS1YeF1RWQ0YbH0JXUVlMARYCAEAeQANZFUsYHUMGUlgSWjULUEwfQlJbQkZKG0EAXlhAVlsNCh5LABFAEBdXXRNfEhsNTBwLS00bQlRZQURNGkMBVF1HUUM="

// Personal cloud backup (optional), routed through the fitbuddy-cloud-backup HTTPS proxy
// on Vercel — the app never holds Atlas credentials, only a shared API key.
val cloudBackupBaseUrlRaw: String =
    System.getenv("CLOUD_BACKUP_BASE_URL")
        ?: localProperties.getProperty("CLOUD_BACKUP_BASE_URL", "https://fitbuddy-cloud-backup.vercel.app")
val backupApiKeyMaskSeed = "fitbuddy.backup.v1"
val backupApiKeyBlobEscaped = "VFlMVkBTUkBIVVACWxQSSxUFBwxMV0MAVxobVAcCCBQVT0QEUQ1MB0IFBh0eU1hTChZHGRMEB11HWhECBk4fWg=="
val mongoDbNameRaw: String =
    System.getenv("MONGO_DB_NAME")
        ?: localProperties.getProperty("MONGO_DB_NAME", "fitbuddy")

val cloudBackupBaseUrlEscaped = cloudBackupBaseUrlRaw
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
val mongoDbNameEscaped = mongoDbNameRaw
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

// CI overrides versionCode/versionName per build via -PappVersionCode=<GITHUB_RUN_NUMBER> and
// -PappVersionName=3.0.<GITHUB_RUN_NUMBER> so every commit to main produces an installable
// update (same applicationId + signature + strictly higher versionCode = update, not reinstall);
// local/dev builds keep the fallback.
val ciVersionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull()
val ciVersionName = project.findProperty("appVersionName") as String?

// Release signing — local keystore.properties should point at a local/dev keystore
// (e.g. fitbuddy-local.jks). The Play/CI release key lives only in GitHub RELEASE_* secrets.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.anant.fitbuddy"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.anant.fitbuddy"
        minSdk = 29
        targetSdk = 36
        versionCode = ciVersionCode ?: 1
        versionName = ciVersionName ?: "3.0.0-dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "OPENROUTER_API_KEY", "\"$openRouterApiKey\"")
        buildConfigField("String", "AI_MODEL", "\"$aiModel\"")
        buildConfigField("String", "SENTRY_DSN_BLOB", "\"$sentryDsnBlobEscaped\"")
        buildConfigField("String", "SENTRY_DSN_MASK", "\"$sentryDsnMaskSeed\"")
        // Obfuscated backup API key blob (empty when BACKUP_API_KEY unset). Decoded by MongoUriVault.
        buildConfigField("String", "BACKUP_API_KEY_BLOB", "\"$backupApiKeyBlobEscaped\"")
        buildConfigField("String", "BACKUP_API_KEY_MASK", "\"$backupApiKeyMaskSeed\"")
        buildConfigField("String", "CLOUD_BACKUP_BASE_URL", "\"$cloudBackupBaseUrlEscaped\"")
        buildConfigField("String", "MONGO_DB_NAME", "\"$mongoDbNameEscaped\"")
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            // Side-by-side with release: com.anant.fitbuddy.debug vs com.anant.fitbuddy
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                // No keystore.properties — sign with the Android debug key (local smoke tests).
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Always install into the personal profile (user 0), never the work profile (user 10).
    installation {
        installOptions += listOf("--user", "0")
    }
    // MongoDB driver JARs both ship META-INF/native-image props; Android merge fails otherwise.
    packaging {
        resources {
            excludes += "META-INF/native-image/**"
            excludes += "META-INF/versions/9/previous-compilation-data.bin"
        }
    }
}

// Release APK: FitBuddy-<versionName>.apk (not app-release.apk).
val releaseApkVersionName = ciVersionName ?: "3.0.0-dev"
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("FitBuddy-$releaseApkVersionName.apk")
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.coil.compose)
    implementation(libs.converter.moshi)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.logging.interceptor)
    implementation(libs.material)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.play.services.location)
    implementation(libs.retrofit)
    implementation(libs.sentry.android)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // Real org.json for JVM unit tests (Android stubs throw at runtime).
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    "ksp"(libs.androidx.room.compiler)
    "ksp"(libs.moshi.kotlin.codegen)
}
