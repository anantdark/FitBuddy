import java.net.URLEncoder
import java.util.Base64
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
val sentryDsnRaw: String =
    System.getenv("SENTRY_DSN")
        ?: localProperties.getProperty("SENTRY_DSN", "")
val sentryDsnEscaped: String = sentryDsnRaw
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

// Personal Atlas backup (optional). Password from env/local.properties only — never commit it.
// Gradle builds: mongodb+srv://USER:PASSWORD@HOST/?appName=APP
// Empty password = cloud backup unavailable in that build.
val mongoDbUser: String =
    System.getenv("MONGO_DB_USER")
        ?: localProperties.getProperty("MONGO_DB_USER", "anantpatel31")
val mongoDbPassword: String =
    System.getenv("MONGO_DB_PASSWORD")
        ?: localProperties.getProperty("MONGO_DB_PASSWORD", "")
val mongoDbHost: String =
    System.getenv("MONGO_DB_HOST")
        ?: localProperties.getProperty("MONGO_DB_HOST", "cluster0.mzgdsvp.mongodb.net")
val mongoDbAppName: String =
    System.getenv("MONGO_DB_APP_NAME")
        ?: localProperties.getProperty("MONGO_DB_APP_NAME", "Cluster0")
val mongoDbNameRaw: String =
    System.getenv("MONGO_DB_NAME")
        ?: localProperties.getProperty("MONGO_DB_NAME", "fitbuddy")

val mongoDbUriRaw: String = if (mongoDbPassword.isBlank()) {
    // Full URI override still supported (legacy / local smoke).
    System.getenv("MONGO_DB_URI")
        ?: localProperties.getProperty("MONGO_DB_URI", "")
} else {
    val userEnc = URLEncoder.encode(mongoDbUser, Charsets.UTF_8)
    val passEnc = URLEncoder.encode(mongoDbPassword, Charsets.UTF_8)
    "mongodb+srv://$userEnc:$passEnc@$mongoDbHost/?appName=$mongoDbAppName"
}

/** XOR + Base64 so the plaintext URI is not a trivial string literal in the APK. */
fun obfuscateForBuildConfig(plain: String, maskSeed: String): String {
    if (plain.isEmpty()) return ""
    val mask = maskSeed.toByteArray(Charsets.UTF_8)
    val plainBytes = plain.toByteArray(Charsets.UTF_8)
    val out = ByteArray(plainBytes.size) { i ->
        (plainBytes[i].toInt() xor mask[i % mask.size].toInt()).toByte()
    }
    return Base64.getEncoder().encodeToString(out)
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}

val mongoUriMaskSeed = "fitbuddy.mongo.v1"
val mongoUriBlobEscaped = obfuscateForBuildConfig(mongoDbUriRaw, mongoUriMaskSeed)
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
        buildConfigField("String", "SENTRY_DSN", "\"$sentryDsnEscaped\"")
        // Obfuscated Atlas URI blob (empty when MONGO_DB_URI unset). Decoded by MongoUriVault.
        buildConfigField("String", "MONGO_URI_BLOB", "\"$mongoUriBlobEscaped\"")
        buildConfigField("String", "MONGO_URI_MASK", "\"$mongoUriMaskSeed\"")
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
    implementation(libs.mongodb.driver.sync)
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
