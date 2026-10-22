package com.anant.fitbuddy.crash

import android.util.Log
import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.data.backup.mongo.MongoUriVault
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Forwards Sentry envelopes to FitBuddy’s Vercel ingest (`/api/sentry`), which posts them
 * to Sentry server-side. The app never talks to Sentry ingest hosts directly when this is
 * available — crashes, messages, logs, metrics, and check-ins all go this path.
 *
 * The caller supplies the DSN in `X-Sentry-Dsn` (nothing stored on the server). Auth uses
 * the same build-baked API key as cloud backup.
 */
internal object SentryCloudIngest {

    private const val TAG = "FitBuddyCrash"

    private val ENVELOPE_MEDIA_TYPE = "application/x-sentry-envelope".toMediaType()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }

    /** Parsed Sentry DSN: `https://<publicKey>@<host>[:port]/<projectId>`. */
    private data class Dsn(
        val raw: String,
        val host: String,
        val publicKey: String,
        val projectId: String
    )

    /**
     * True when cloud ingest can run: a base URL, an obfuscated API key, and a parseable
     * Sentry DSN are all present in this build.
     */
    fun isAvailable(): Boolean =
        cloudBaseUrl().isNotBlank() && MongoUriVault.isAvailable() && parseDsn() != null

    /**
     * Forward a serialized Sentry envelope through FitBuddy cloud ingest.
     * Returns true on a 2xx from the Vercel route.
     */
    fun forwardEnvelope(envelopeBytes: ByteArray): Boolean {
        val dsn = parseDsn() ?: return false
        if (envelopeBytes.isEmpty()) return false
        return postEnvelope(dsn, envelopeBytes)
    }

    private fun postEnvelope(dsn: Dsn, envelopeBytes: ByteArray): Boolean {
        val base = cloudBaseUrl()
        if (base.isBlank()) return false
        val apiKey = runCatching { MongoUriVault.resolve() }.getOrNull()
        if (apiKey.isNullOrBlank()) return false

        return runCatching {
            val request = Request.Builder()
                .url("$base/api/sentry")
                .header("Authorization", "Bearer $apiKey")
                .header("X-Sentry-Dsn", dsn.raw)
                .post(envelopeBytes.toRequestBody(ENVELOPE_MEDIA_TYPE))
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.i(TAG, "Sentry envelope forwarded OK: HTTP ${response.code}")
                } else {
                    Log.w(TAG, "Sentry envelope rejected: HTTP ${response.code}")
                }
                response.isSuccessful
            }
        }.onFailure { e ->
            Log.w(TAG, "Sentry envelope forward failed", e)
        }.getOrDefault(false)
    }

    private fun cloudBaseUrl(): String =
        BuildConfig.CLOUD_BACKUP_BASE_URL.trim().trimEnd('/')

    private fun parseDsn(): Dsn? {
        if (BuildConfig.SENTRY_DSN_BLOB.isBlank()) return null
        val raw = runCatching {
            MongoUriVault.decode(BuildConfig.SENTRY_DSN_BLOB, BuildConfig.SENTRY_DSN_MASK).trim()
        }.getOrNull().orEmpty()
        if (raw.isEmpty()) return null
        return runCatching {
            val uri = java.net.URI(raw)
            val publicKey = uri.userInfo?.substringBefore(':').orEmpty()
            val host = uri.host.orEmpty()
            val projectId = uri.path.trim('/').substringAfterLast('/')
            if (publicKey.isBlank() || host.isBlank() || projectId.isBlank()) null
            else Dsn(raw = raw, host = host, publicKey = publicKey, projectId = projectId)
        }.getOrNull()
    }
}
