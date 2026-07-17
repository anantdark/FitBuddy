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
