package com.anant.fitbuddy.data.backup.mongo

import com.anant.fitbuddy.BuildConfig
import java.util.Base64

/**
 * Best-effort client-side vault for the build-baked Atlas connection URI.
 * Raises the bar vs a plaintext BuildConfig string; reverse-engineering the APK
 * can still recover the URI. Never log or show the resolved value in UI.
 */
object MongoUriVault {

    @Volatile
    private var cachedUri: String? = null

    /** True when this build includes a non-empty obfuscated Atlas URI. */
    fun isAvailable(): Boolean = BuildConfig.MONGO_URI_BLOB.isNotBlank()

    /** Database name from BuildConfig (default `fitbuddy`). */
    fun databaseName(): String =
        BuildConfig.MONGO_DB_NAME.trim().ifBlank { "fitbuddy" }

    /**
     * Decodes the obfuscated URI once and caches it in memory.
     * @throws IllegalStateException when the blob is missing or corrupt.
     */
    fun resolve(): String {
        cachedUri?.let { return it }
        val blob = BuildConfig.MONGO_URI_BLOB
        require(blob.isNotBlank()) { "Cloud backup is not available in this build" }
        val decoded = decode(blob, BuildConfig.MONGO_URI_MASK)
        require(decoded.isNotBlank()) { "Cloud backup URI could not be decoded" }
        cachedUri = decoded
        return decoded
    }

    /** XOR-decode a Base64 blob produced at build time. Visible for unit tests. */
    internal fun decode(base64Blob: String, maskSeed: String): String {
        val masked = Base64.getDecoder().decode(base64Blob)
        val mask = maskSeed.toByteArray(Charsets.UTF_8)
        if (mask.isEmpty()) return String(masked, Charsets.UTF_8)
        val plain = ByteArray(masked.size) { i ->
            (masked[i].toInt() xor mask[i % mask.size].toInt()).toByte()
        }
        return String(plain, Charsets.UTF_8)
    }

    /** XOR-encode for tests (mirrors Gradle `obfuscateForBuildConfig`). */
    internal fun encode(plain: String, maskSeed: String): String {
        val mask = maskSeed.toByteArray(Charsets.UTF_8)
        val plainBytes = plain.toByteArray(Charsets.UTF_8)
        val out = ByteArray(plainBytes.size) { i ->
            (plainBytes[i].toInt() xor mask[i % mask.size].toInt()).toByte()
        }
        return Base64.getEncoder().encodeToString(out)
    }
}
