package com.anant.fitbuddy.crash

import android.util.Log
import io.sentry.Hint
import io.sentry.SentryEnvelope
import io.sentry.SentryOptions
import io.sentry.transport.ITransport
import io.sentry.transport.RateLimiter
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Sentry transport that sends every envelope through FitBuddy’s Vercel ingest when available.
 *
 * Wraps the SDK’s HTTP transport ([delegate]) only as a fallback when cloud ingest is
 * unavailable or the POST fails — so the SDK’s disk cache / retry still applies.
 */
internal class SentryCloudTransport(
    private val options: SentryOptions,
    private val delegate: ITransport
) : ITransport {

    override fun send(envelope: SentryEnvelope, hint: Hint) {
        if (SentryCloudIngest.isAvailable()) {
            val bytes = serialize(envelope)
            if (bytes != null && SentryCloudIngest.forwardEnvelope(bytes)) {
                return
            }
            Log.w(TAG, "cloud ingest failed; falling back to direct transport")
        }
        delegate.send(envelope, hint)
    }

    private fun serialize(envelope: SentryEnvelope): ByteArray? =
        runCatching {
            ByteArrayOutputStream().use { out ->
                options.serializer.serialize(envelope, out)
                out.toByteArray()
            }
        }.onFailure { e ->
            Log.w(TAG, "envelope serialize failed", e)
        }.getOrNull()

    override fun isHealthy(): Boolean = delegate.isHealthy

    override fun flush(timeoutMillis: Long) = delegate.flush(timeoutMillis)

    override fun getRateLimiter(): RateLimiter? = delegate.rateLimiter

    @Throws(IOException::class)
    override fun close(isRestarting: Boolean) = delegate.close(isRestarting)

    @Throws(IOException::class)
    override fun close() = delegate.close()

    companion object {
        private const val TAG = "FitBuddyCrash"
    }
}
