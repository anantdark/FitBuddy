package com.anant.fitbuddy.crash

import io.sentry.Hint
import io.sentry.SentryEnvelope
import io.sentry.SentryOptions
import io.sentry.transport.ITransport
import io.sentry.transport.RateLimiter
import java.io.ByteArrayOutputStream
import java.io.IOException

/** Sentry transport that only talks to the FitBuddy proxy. */
internal class SentryCloudTransport(
    private val options: SentryOptions
) : ITransport {

    override fun send(envelope: SentryEnvelope, hint: Hint) {
        if (!SentryCloudIngest.isAvailable()) return
        val bytes = serialize(envelope) ?: return
        when (SentryCloudIngest.forwardEnvelope(bytes)) {
            IngestResult.Sent, IngestResult.Drop -> Unit
            IngestResult.Retry -> throw IOException("cloud ingest failed")
        }
    }

    private fun serialize(envelope: SentryEnvelope): ByteArray? =
        runCatching {
            ByteArrayOutputStream().use { out ->
                options.serializer.serialize(envelope, out)
                out.toByteArray()
            }
        }.getOrNull()

    override fun isHealthy(): Boolean = true

    override fun flush(timeoutMillis: Long) = Unit

    override fun getRateLimiter(): RateLimiter? = null

    @Throws(IOException::class)
    override fun close(isRestarting: Boolean) = Unit

    @Throws(IOException::class)
    override fun close() = Unit
}
