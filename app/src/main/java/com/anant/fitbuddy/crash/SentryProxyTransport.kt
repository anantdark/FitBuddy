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
 * A Sentry transport that can divert traffic through the FitBuddy Vercel proxy.
 *
 * It wraps the SDK's real HTTP transport ([delegate]) for the normal, direct path. When
 * [proxyModeActive] returns true — set by the daily heartbeat once it detects Sentry's ingest
 * host is blocked for the day — every envelope the SDK produces (crashes, messages, logs,
 * metrics, check-ins) is serialized and forwarded to the proxy instead. The proxy relays it to
 * Sentry server-side, where the user's DNS/adblock can't interfere.
 *
 * Envelopes forwarded through the proxy are not queued/retried by the SDK's disk cache — if the
 * proxy POST fails we fall back to the direct transport so the SDK's caching/retry still applies.
 */
internal class SentryProxyTransport(
    private val options: SentryOptions,
    private val delegate: ITransport,
    private val proxyModeActive: () -> Boolean
) : ITransport {

    override fun send(envelope: SentryEnvelope, hint: Hint) {
        if (proxyModeActive() && SentryHeartbeatProxy.isAvailable()) {
            val bytes = serialize(envelope)
            if (bytes != null && SentryHeartbeatProxy.forwardEnvelope(bytes)) {
                return
            }
            // Proxy unavailable or the POST failed — fall through to the direct transport so
            // the SDK's own disk cache/retry can take over.
            Log.w(TAG, "proxy send failed; falling back to direct transport")
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
