package com.anant.fitbuddy.data.backup.mongo

import com.anant.fitbuddy.BuildConfig
import java.util.Base64

/**
 * Best-effort client-side vault for the build-baked cloud-backup proxy API key.
 * Raises the bar vs a plaintext BuildConfig string; reverse-engineering the APK
 * can still recover the key. Never log or show the resolved value in UI.
 *
 * The proxy (not this app) holds the actual Atlas connection string — this key
 * only authorizes calls to that HTTPS proxy.
 */
object MongoUriVault {

    @Volatile
    private var cachedApiKey: String? = null

    /** True when this build includes a non-empty obfuscated backup API key. */
    fun isAvailable(): Boolean = BuildConfig.BACKUP_API_KEY_BLOB.isNotBlank()

    /** Database name from BuildConfig (default `fitbuddy`). */
    fun databaseName(): String =
        BuildConfig.MONGO_DB_NAME.trim().ifBlank { "fitbuddy" }

    /** Base URL of the fitbuddy-cloud-backup HTTPS proxy. */
    fun baseUrl(): String =
        BuildConfig.CLOUD_BACKUP_BASE_URL.trim().trimEnd('/')

    /**
     * Decodes the obfuscated API key once and caches it in memory.
     * @throws IllegalStateException when the blob is missing or corrupt.
     */
    fun resolve(): String {
        cachedApiKey?.let { return it }
        val blob = BuildConfig.BACKUP_API_KEY_BLOB
        require(blob.isNotBlank()) { "Cloud backup is not available in this build" }
        val decoded = decode(blob, BuildConfig.BACKUP_API_KEY_MASK)
        require(decoded.isNotBlank()) { "Cloud backup API key could not be decoded" }
        cachedApiKey = decoded
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
