package com.anant.fitbuddy.crash

import android.util.Log
import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.data.backup.mongo.MongoUriVault
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Fallback transport for the **daily heartbeat only**, used when direct Sentry ingest is
 * unreachable (DNS sinkhole, ad/tracker blocker, corporate/VPN filtering, etc.).
 *
 * Sentry's SDK talks to `https://<host>/api/<projectId>/envelope/`. When that host resolves to
 * nothing or is blocked, [CrashReporter.sendHeartbeat] silently loses the check-in. This class
 * re-encodes the same daily check-in as a raw Sentry envelope and POSTs it to the FitBuddy
 * cloud-backup Vercel proxy, which forwards it to Sentry server-side (never blocked by the
 * user's DNS/adblock). Only the daily fleet pulse is proxied — crashes/issues are not.
 *
 * The proxy holds the real ingest host(s); this client sends only the DSN public key so the
 * proxy can route to the correct project (multi-DSN aware).
 */
internal object SentryHeartbeatProxy {

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
    ) {
        /** Direct ingest endpoint the SDK would normally hit. */
        val ingestEnvelopeUrl: String get() = "https://$host/api/$projectId/envelope/"
    }

    /**
     * True when the proxy fallback can run: a proxy base URL, an obfuscated proxy key, and a
     * parseable Sentry DSN are all present in this build.
     */
    fun isAvailable(): Boolean =
        proxyBaseUrl().isNotBlank() && MongoUriVault.isAvailable() && parseDsn() != null

    /**
     * Probe the direct Sentry ingest host. Returns true when a normal connection can be
     * established (i.e. the SDK's own transport should work and no fallback is needed).
     */
    fun isDirectIngestReachable(): Boolean {
        val dsn = parseDsn() ?: return false
        // A HEAD to the envelope endpoint. Any HTTP response (even 4xx/405) proves the host
        // resolves and is reachable; only DNS/connect failures throw, which is exactly the
        // "blocked" signal we're looking for.
        val request = Request.Builder()
            .url(dsn.ingestEnvelopeUrl)
            .head()
            .build()
        return runCatching {
            client.newCall(request).execute().use { true }
        }.getOrDefault(false)
    }

    /**
     * Send the daily heartbeat via the Vercel proxy. Builds a minimal Sentry envelope holding
     * a Crons check-in (OK) plus a structured log line mirroring the SDK's fleet pulse, then
     * POSTs it to `<proxyBaseUrl>/api/sentry`. Returns true on a 2xx from the proxy.
     */
    fun sendDailyHeartbeat(
        info: HeartbeatInfo,
        kind: HeartbeatKind = HeartbeatKind.DAILY
    ): Boolean {
        val dsn = parseDsn() ?: return false
        val envelope =
            runCatching { buildEnvelope(dsn, info, kind.proxyLogMessage) }.getOrNull() ?: return false
        return postEnvelope(dsn, envelope.toByteArray(Charsets.UTF_8))
    }

    /**
     * Forward an already-serialized Sentry envelope (produced by the SDK — crashes, messages,
     * logs, metrics, check-ins, anything) through the Vercel proxy. Used by [SentryProxyTransport]
     * while proxy mode is active for the day. Returns true on a 2xx from the proxy.
     */
    fun forwardEnvelope(envelopeBytes: ByteArray): Boolean {
        val dsn = parseDsn() ?: return false
        if (envelopeBytes.isEmpty()) return false
        return postEnvelope(dsn, envelopeBytes)
    }

    /** Shared POST to `<proxyBaseUrl>/api/sentry` with proxy auth + the target DSN header. */
    private fun postEnvelope(dsn: Dsn, envelopeBytes: ByteArray): Boolean {
        val proxyBase = proxyBaseUrl()
        if (proxyBase.isBlank()) return false
        val proxyKey = runCatching { MongoUriVault.resolve() }.getOrNull()
        if (proxyKey.isNullOrBlank()) return false

        return runCatching {
            val request = Request.Builder()
                .url("$proxyBase/api/sentry")
                .header("Authorization", "Bearer $proxyKey")
                // The proxy forwards to the DSN we supply (no DSN stored server-side).
                .header("X-Sentry-Dsn", dsn.raw)
                .post(envelopeBytes.toRequestBody(ENVELOPE_MEDIA_TYPE))
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.i(TAG, "proxy envelope forwarded OK: HTTP ${response.code} -> $proxyBase/api/sentry")
                } else {
                    Log.w(TAG, "proxy envelope rejected: HTTP ${response.code}")
                }
                response.isSuccessful
            }
        }.onFailure { e ->
            Log.w(TAG, "proxy envelope failed", e)
        }.getOrDefault(false)
    }

    /**
     * Minimal Sentry envelope: header + a `check_in` item + a `log` item. Matches the wire
     * format the SDK would emit for [CrashReporter.emitFleetPulse] so the proxy is a dumb
     * forwarder.
     */
    private fun buildEnvelope(dsn: Dsn, info: HeartbeatInfo, logBody: String): String {
        val sentAt = nowIso()
        val release =
            "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
        val environment = if (BuildConfig.DEBUG) "debug" else "release"

        val envelopeHeader = JSONObject()
            .put("event_id", UUID.randomUUID().toString().replace("-", ""))
            .put("sent_at", sentAt)
            .put(
                "dsn",
                "https://${dsn.publicKey}@${dsn.host}/${dsn.projectId}"
            )
            .toString()

        // --- check-in item ---
        val checkIn = JSONObject()
            .put("check_in_id", UUID.randomUUID().toString().replace("-", ""))
            .put("monitor_slug", CrashReporter.HEARTBEAT_MONITOR_SLUG)
            .put("status", "ok")
            .put("duration", 0.0)
            .put("release", release)
            .put("environment", environment)
            .put(
                "monitor_config",
                JSONObject()
                    .put(
                        "schedule",
                        JSONObject()
                            .put("type", "interval")
                            .put("value", 1)
                            .put("unit", "day")
                    )
                    .put("checkin_margin", 2L * 24L * 60L)
                    .put("max_runtime", 5L)
                    .put("timezone", "UTC")
                    .put("failure_issue_threshold", 10L)
            )
            .toString()
        val checkInHeader = JSONObject().put("type", "check_in").toString()

        // --- log item (structured fleet-pulse attributes) ---
        val logItem = buildLogItem(info, release, environment, logBody)
        val logHeader = JSONObject()
            .put("type", "log")
            .put("item_count", 1)
            .put("content_type", "application/vnd.sentry.items.log+json")
            .toString()

        return buildString {
            append(envelopeHeader).append('\n')
            append(checkInHeader).append('\n')
            append(checkIn).append('\n')
            append(logHeader).append('\n')
            append(logItem).append('\n')
        }
    }

    private fun buildLogItem(
        info: HeartbeatInfo,
        release: String,
        environment: String,
        logBody: String
    ): String {
        val attributes = JSONObject()
            .putStringAttr("heartbeat", "true")
            .putStringAttr("ai_provider", info.aiProvider)
            .putStringAttr("manufacturer", info.manufacturer)
            .putStringAttr("model", info.model)
            .putIntAttr("android_sdk", info.androidSdk.toLong())
            .putStringAttr("app_version", BuildConfig.VERSION_NAME)
            .putStringAttr("app_build", BuildConfig.VERSION_CODE.toString())
            .putStringAttr("app_id", BuildConfig.APPLICATION_ID)
            .putStringAttr("release", release)
            .putStringAttr("environment", environment)
            .putStringAttr("transport", "proxy")
            .apply {
                val username = info.username.trim()
                if (username.isNotEmpty()) {
                    putStringAttr("username", username.take(128))
                }
            }
            .putIntAttr("record_count", info.recordCount.toLong())
            .putBoolAttr("is_developer", info.isDeveloper)

        val log = JSONObject()
            .put("timestamp", System.currentTimeMillis() / 1000.0)
            .put("level", "info")
            .put("body", logBody)
            .put("attributes", attributes)

        return JSONObject().put("items", org.json.JSONArray().put(log)).toString()
    }

    private fun JSONObject.putStringAttr(key: String, value: String): JSONObject =
        put(key, JSONObject().put("value", value).put("type", "string"))

    private fun JSONObject.putIntAttr(key: String, value: Long): JSONObject =
        put(key, JSONObject().put("value", value).put("type", "integer"))

    private fun JSONObject.putBoolAttr(key: String, value: Boolean): JSONObject =
        put(key, JSONObject().put("value", value).put("type", "boolean"))

    private fun nowIso(): String {
        val instant = java.time.Instant.now()
        return java.time.format.DateTimeFormatter.ISO_INSTANT.format(instant)
    }

    private fun proxyBaseUrl(): String =
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
