# --- FitBuddy R8 / ProGuard rules ---------------------------------------------------
# Kept intentionally conservative: the release build is minified + resource-shrunk for speed
# and size, while these keeps prevent reflection-based libraries from breaking.

# Keep Kotlin metadata so Moshi's reflective KotlinJsonAdapterFactory (fallback) works.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes Signature,InnerClasses,EnclosingMethod

# --- Data / DTO / model classes (serialized via Moshi) -------------------------------------
# All app DTOs use @JsonClass(generateAdapter = true) codegen, but keep them + generated
# adapters to be safe against any reflective access.
-keep class com.anant.fitbuddy.data.model.** { *; }
-keep class com.anant.fitbuddy.data.remote.dto.** { *; }
-keep class com.anant.fitbuddy.data.database.** { *; }
-keep class com.anant.fitbuddy.**JsonAdapter { *; }

# --- Moshi ---------------------------------------------------------------------------------
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-dontwarn org.jetbrains.annotations.**

# --- Retrofit / OkHttp ---------------------------------------------------------------------
# Retrofit ships its own consumer rules; these cover the interface + generic signatures.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepclasseswithmembers interface com.anant.fitbuddy.data.remote.AiApi { *; }
-keepclasseswithmembers interface com.anant.fitbuddy.data.remote.OpenFoodFactsApi { *; }
# GithubApi's only real caller (UpdateChecker.checkForUpdate) is dead code in the fdroid
# flavor (MainViewModel.checkForUpdates returns early when BuildConfig.IS_FDROID), so R8
# sees the interface as unreachable there and strips/mangles it — but
# FitBuddyApp.updateChecker is still eagerly constructed on both flavors (MainActivity
# always wires it into MainViewModelFactory), so NetworkModule.provideGithubApi() still
# runs retrofit2.Retrofit.create() on it, which then throws ClassCastException building
# the dynamic proxy against the mangled interface. Same failure class as the removed
# ML Kit keep rules: an unreachable-but-still-reflectively-used interface needs an
# explicit, unconditional keep.
-keepclasseswithmembers interface com.anant.fitbuddy.data.remote.GithubApi { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# --- Coroutines ----------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**

# --- kotlinx.serialization (plugin applied; keep in case of @Serializable usage) ------------
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- MongoDB Java driver (Atlas personal backup) --------------------------------------------
-keep class com.mongodb.** { *; }
-keep class org.bson.** { *; }
-keep class javax.security.sasl.** { *; }
-dontwarn com.mongodb.**
-dontwarn org.bson.**
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
-dontwarn javax.security.sasl.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn com.google.common.**

# Compose initializes every accessibility event as non-sensitive. Some API 34 framework
# builds report SDK 34 but omit this API 34 method, so remove the redundant default setter.
-assumenosideeffects class androidx.core.view.accessibility.AccessibilityEventCompat {
    public static void setAccessibilityDataSensitive(android.view.accessibility.AccessibilityEvent, boolean);
}

# WorkManager (used by Glance widgets) instantiates InputMergers reflectively via the no-arg
# constructor. The library's own rule keeps the class but R8 still strips <init>, which makes
# every widget update job fail and leaves home-screen widgets stuck on the loading layout.
-keep class * extends androidx.work.InputMerger { <init>(); }

# Glance instantiates ActionCallbacks reflectively via the no-arg constructor; without this R8
# strips <init> and widget buttons throw NoSuchMethodException (widget then falls back to loading).
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }

# Glance identifies each home-screen widget by GlanceAppWidget's canonical class name
# (GlanceAppWidgetManager.getGlanceIds / DataStore). R8 class merging collapses similar
# subclasses into one name, so updateAll paints every placement with whichever widget
# refreshed last — Action Strip becoming Quick Snap ("Late snack?") after dark. Keep
# original names so the mapping stays stable across installs and updates.
-keep class com.anant.fitbuddy.widget.QuickSnapWidget
-keep class com.anant.fitbuddy.widget.DescribeLogWidget
-keep class com.anant.fitbuddy.widget.CalorieOrbWidget
-keep class com.anant.fitbuddy.widget.PresetLaunchpadWidget
-keep class com.anant.fitbuddy.widget.ActionStripWidget
-keep class com.anant.fitbuddy.widget.WaterIntakeWidget
