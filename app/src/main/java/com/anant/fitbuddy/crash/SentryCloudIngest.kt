package com.anant.fitbuddy.crash

import android.util.Log
import com.anant.fitbuddy.data.backup.mongo.CloudProxyAuth
import com.anant.fitbuddy.data.backup.mongo.MongoUriVault
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Forwards Sentry envelopes to the cloud proxy (DSN stays server-side). */
internal object SentryCloudIngest {

    private const val TAG = "FitBuddyCrash"

    private val envelopeMedia = "application/x-sentry-envelope".toMediaType()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }

    fun isAvailable(): Boolean = MongoUriVault.isAvailable()

    fun forwardEnvelope(envelopeBytes: ByteArray): IngestResult {
        if (!isAvailable() || envelopeBytes.isEmpty()) return IngestResult.Drop
        val token = when (val status = runBlocking { CloudProxyAuth.current() }) {
            is CloudProxyAuth.Status.Ready -> status.token
            is CloudProxyAuth.Status.Unavailable ->
                return if (status.retryable) IngestResult.Retry else IngestResult.Drop
        }
        return runCatching {
            val request = envelopeRequest(MongoUriVault.baseUrl(), token, envelopeBytes)
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.i(TAG, "Sentry envelope forwarded OK: HTTP ${response.code}")
                    IngestResult.Sent
                } else if (response.code == 429 || response.code >= 500) {
                    Log.w(TAG, "Sentry envelope retryable: HTTP ${response.code}")
                    IngestResult.Retry
                } else {
                    Log.w(TAG, "Sentry envelope dropped: HTTP ${response.code}")
                    IngestResult.Drop
                }
            }
        }.getOrElse { e ->
            Log.w(TAG, "Sentry envelope forward failed", e)
            IngestResult.Retry
        }
    }

    internal fun envelopeRequest(base: String, token: String, body: ByteArray): Request =
        Request.Builder()
            .url("${base.trimEnd('/')}/api/sentry")
            .header("Authorization", "Bearer $token")
            .post(body.toRequestBody(envelopeMedia))
            .build()
}

internal enum class IngestResult { Sent, Drop, Retry }
