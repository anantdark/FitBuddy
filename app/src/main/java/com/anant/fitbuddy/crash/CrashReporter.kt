package com.anant.fitbuddy.crash

import android.app.Application
import android.os.Build
import android.util.Log
import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.data.backup.mongo.MongoUriVault
import io.sentry.Breadcrumb
import io.sentry.CheckIn
import io.sentry.CheckInStatus
import io.sentry.MonitorConfig
import io.sentry.MonitorSchedule
import io.sentry.MonitorScheduleUnit
import io.sentry.Sentry
import io.sentry.SentryAttribute
import io.sentry.SentryAttributes
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryLogLevel
import io.sentry.SentryOptions.BeforeSendCallback
import io.sentry.android.core.SentryAndroid
import io.sentry.logger.SentryLogParameters
import io.sentry.metrics.SentryMetricsParameters
import io.sentry.protocol.SentryId
import io.sentry.protocol.User
import java.util.concurrent.atomic.AtomicBoolean

/** Coarse snapshot attached to daily heartbeats (username is device-local display name). */
data class HeartbeatInfo(
    val aiProvider: String,
    val username: String = "",
    val recordCount: Int = 0,
    /** True when Settings developer tools are unlocked on this install. */
    val isDeveloper: Boolean = false,
    val androidSdk: Int = Build.VERSION.SDK_INT,
    val manufacturer: String = Build.MANUFACTURER.orEmpty().take(64),
    val model: String = Build.MODEL.orEmpty().take(64)
)

/** Why a fleet pulse was sent — maps to the Sentry log message string. */
enum class HeartbeatKind {
    DAILY,
    CONFETTI,
    UPDATE;

    val logMessage: String
        get() = when (this) {
            DAILY -> "FitBuddy daily heartbeat"
            CONFETTI -> "FitBuddy confetti heartbeat"
            UPDATE -> "FitBuddy update heartbeat"
        }

    /** Same as [logMessage] but marked as proxied, e.g. "FitBuddy daily heartbeat (proxy)". */
    val proxyLogMessage: String get() = "$logMessage (proxy)"
}

/**
 * Thin Sentry wrapper: crashes/ANRs, optional daily heartbeat check-ins, no PII.
 * Empty [BuildConfig.SENTRY_DSN_BLOB] keeps the SDK uninitialized (local builds without a key).
 */
object CrashReporter {

    /** Fleet-level cron monitor: any install with reporting on may check in once per UTC day. */
    const val HEARTBEAT_MONITOR_SLUG = "fitbuddy-daily-heartbeat"

    /** Groups all custom-region requests into one Issue in Sentry. */
    const val REGION_REQUEST_FINGERPRINT = "fitbuddy-region-request"

    private val ready = AtomicBoolean(false)
    @Volatile
    private var reportingEnabled: Boolean = true

    /**
     * When true, all Sentry traffic is routed through the Vercel proxy (read on the transport's
     * hot path). Set by the daily heartbeat for the rest of a UTC day when Sentry's ingest host
     * is blocked; seeded at init from the persisted per-day flag.
     */
    private val proxyModeActive = AtomicBoolean(false)

    /**
     * True when the developer toggle forced proxy mode on. The daily/auto reachability probe
     * must not override this, so [sendHeartbeat] skips the probe while forced.
     */
    private val forcedProxyMode = AtomicBoolean(false)

    fun init(
        app: Application,
        enabled: Boolean,
        supportId: String,
        proxyModeSeed: Boolean = false,
        forceProxy: Boolean = false
    ) {
        if (BuildConfig.SENTRY_DSN_BLOB.isBlank()) return
        val dsn = MongoUriVault.decode(BuildConfig.SENTRY_DSN_BLOB, BuildConfig.SENTRY_DSN_MASK).trim()
        if (dsn.isEmpty()) return
        reportingEnabled = enabled
        forcedProxyMode.set(forceProxy)
        proxyModeActive.set(forceProxy || proxyModeSeed)
        SentryAndroid.init(app) { options ->
            options.dsn = dsn
            // Route envelopes through the Vercel proxy when proxy mode is active (Sentry ingest
            // blocked for the day); otherwise use the SDK's normal HTTP transport.
            val defaultFactory = io.sentry.AsyncHttpTransportFactory()
            options.setTransportFactory { transportOptions, requestDetails ->
                SentryProxyTransport(
                    options = transportOptions,
                    delegate = defaultFactory.create(transportOptions, requestDetails),
                    proxyModeActive = { proxyModeActive.get() }
                )
            }
            options.isSendDefaultPii = false
            options.tracesSampleRate = 0.0
            options.isEnableUserInteractionTracing = false
            // Disable all automatic/periodic network traffic — we flush manually
            // during heartbeats (UTC midnight) and let crash events through immediately.
            options.isEnableAutoSessionTracking = false
            options.isSendClientReports = false
            // Fleet pulse: Logs (samples) + Metrics (counts by device/version). Not Issues.
            // These only emit data when we explicitly call emitFleetPulse + flush().
            options.logs.isEnabled = true
            options.metrics.isEnabled = true
            options.environment = if (BuildConfig.DEBUG) "debug" else "release"
            options.release =
                "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
            options.setBeforeSend(BeforeSendCallback { event, _ ->
                if (!reportingEnabled) return@BeforeSendCallback null
                // Heartbeats use Logs/Metrics only — never promote them to Issues.
                val msg = event.message?.formatted
                if (event.fingerprints?.contains(HEARTBEAT_MONITOR_SLUG) == true ||
                    HeartbeatKind.entries.any { it.logMessage == msg || it.proxyLogMessage == msg }
                ) {
                    return@BeforeSendCallback null
                }
                scrub(event)
            })
        }
        if (supportId.isNotBlank()) {
            Sentry.setUser(User().apply { id = supportId })
        }
        ready.set(true)
    }

    fun setReportingEnabled(enabled: Boolean) {
        reportingEnabled = enabled
    }

    /**
     * Developer toggle: force all Sentry traffic through the proxy (true) or return to
     * automatic (false). While forced on, the reachability probe won't override it.
     */
    fun setProxyModeActive(active: Boolean) {
        forcedProxyMode.set(active)
        proxyModeActive.set(active)
    }

    fun setSupportId(supportId: String) {
        if (!ready.get() || supportId.isBlank()) return
        Sentry.setUser(User().apply { id = supportId })
    }

    /** Event-name breadcrumb only — never attach meal text, keys, or barcodes. */
    fun breadcrumb(category: String, message: String) {
        if (!ready.get() || !reportingEnabled) return
        Sentry.addBreadcrumb(
            Breadcrumb().apply {
                this.category = category
                this.message = message
                level = SentryLevel.INFO
            }
        )
    }

    /** Outcome of [captureRegionRequest] for UI messaging. */
    enum class RegionRequestResult {
        Sent,
        NotReady,
        ReportingDisabled,
        EmptyRequest,
        Failed;

        val userMessage: String
            get() = when (this) {
                Sent -> "Request sent — thanks!"
                NotReady -> "Crash reporting isn't available in this build."
                ReportingDisabled -> "Enable crash reporting to send a region request."
                EmptyRequest -> "Enter a region name first."
                Failed -> "Couldn't send request. Check your connection and try again."
            }
    }

    /**
     * User-requested region not in the pilot set. Creates a searchable Sentry Issue
     * (INFO [captureMessage]) with fingerprint [REGION_REQUEST_FINGERPRINT].
     * Call only after the in-app consent dialog; gated by crash-reporting opt-in.
     */
    fun captureRegionRequest(
        requestedRegion: String,
        currentRegion: String?,
        supportId: String
    ): RegionRequestResult {
        if (!ready.get()) return RegionRequestResult.NotReady
        if (!reportingEnabled) return RegionRequestResult.ReportingDisabled
        val requested = requestedRegion.trim().take(120)
        if (requested.isBlank()) return RegionRequestResult.EmptyRequest
        return runCatching {
            if (supportId.isNotBlank()) {
                Sentry.setUser(User().apply { id = supportId })
            }
            val eventId = Sentry.captureMessage(
                "Region request: $requested",
                SentryLevel.INFO
            ) { scope ->
                scope.fingerprint = listOf(REGION_REQUEST_FINGERPRINT)
                scope.setTag("fitbuddy.event", "region_request")
                scope.setTag("requested_region", requested)
                scope.setTag(
                    "current_region",
                    currentRegion?.trim()?.take(32)?.ifBlank { null } ?: "unset"
                )
                scope.setExtra("requested_region", requested)
                scope.setExtra(
                    "current_region",
                    currentRegion?.trim()?.ifBlank { null } ?: "unset"
                )
                scope.setExtra("app_version", BuildConfig.VERSION_NAME)
                scope.setExtra("app_build", BuildConfig.VERSION_CODE.toString())
            }
            Sentry.flush(5_000L)
            if (eventId != SentryId.EMPTY_ID) {
                RegionRequestResult.Sent
            } else {
                RegionRequestResult.Failed
            }
        }.onFailure { e ->
            Log.e(TAG, "region request failed", e)
        }.getOrDefault(RegionRequestResult.Failed)
    }

    /**
     * Anonymous heartbeat: Crons check-in (OK) plus Metrics/Logs with device/app/AI
     * attributes for fleet breakdown (Explore → Metrics / Logs — not Issues).
     * Callers gate once-per-day / once-per-update. Returns true if the check-in was sent.
     * Sent regardless of [reportingEnabled] — heartbeats are fleet install/version
     * telemetry, not crash reports, so the crash-reporting opt-out doesn't gate them.
     */
    fun sendHeartbeat(info: HeartbeatInfo, kind: HeartbeatKind = HeartbeatKind.DAILY): Boolean {
        if (!ready.get()) return false

        // Automatic proxy fallback: probe the direct route and, if Sentry's ingest host is
        // blocked, latch proxyModeActive so the custom transport diverts the SDK's *real*
        // envelope through the proxy (same path the developer toggle uses; no hand-built
        // envelope). The developer toggle takes precedence — when it forces proxy mode on,
        // we must NOT run the probe, or a reachable host would flip it back off.
        if (!forcedProxyMode.get() && SentryHeartbeatProxy.isAvailable()) {
            val reachable = SentryHeartbeatProxy.isDirectIngestReachable()
            proxyModeActive.set(!reachable)
            Log.i(TAG, "heartbeat $kind: direct ingest ${if (reachable) "reachable" else "blocked → proxy"}")
        }

        // Send via the SDK. When proxyModeActive is on, SentryProxyTransport forwards the
        // serialized envelope through the Vercel proxy; otherwise it goes direct.
        return sendHeartbeatDirect(info, kind)
    }

    /** True when Sentry traffic is currently being routed through the proxy. */
    fun isProxyModeActive(): Boolean = proxyModeActive.get()

    /** Direct SDK send: Crons check-in + fleet pulse, flushed. True if the check-in was queued. */
    private fun sendHeartbeatDirect(info: HeartbeatInfo, kind: HeartbeatKind): Boolean =
        runCatching {
            val checkIn = CheckIn(HEARTBEAT_MONITOR_SLUG, CheckInStatus.OK).apply {
                release =
                    "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
                environment = if (BuildConfig.DEBUG) "debug" else "release"
                duration = 0.0
                monitorConfig = MonitorConfig(
                    MonitorSchedule.interval(1, MonitorScheduleUnit.DAY)
                ).apply {
                    checkinMargin = 2L * 24L * 60L
                    maxRuntime = 5L
                    timezone = "UTC"
                    failureIssueThreshold = 10L
                }
            }
            val checkInId = Sentry.captureCheckIn(checkIn)
            // If proxy mode is on, this envelope is diverted through the proxy by the custom
            // transport — mark the message so proxied heartbeats read "... (proxy)".
            val message = if (proxyModeActive.get()) kind.proxyLogMessage else kind.logMessage
            emitFleetPulse(info, message = message)
            // Flush so cold-start pulse isn't lost if the process is killed early.
            Sentry.flush(5_000L)
            checkInId != SentryId.EMPTY_ID
        }.onFailure { e ->
            Log.e(TAG, "heartbeat failed", e)
        }.getOrDefault(false)

    /** @see sendHeartbeat */
    fun sendDailyHeartbeat(info: HeartbeatInfo, force: Boolean = false): Boolean =
        sendHeartbeat(info, if (force) HeartbeatKind.CONFETTI else HeartbeatKind.DAILY)

    /**
     * One count per active install/day, tagged for grouping in Explore → Metrics
     * (e.g. group by `model` / `app_version`). Also a structured log for sample rows.
     */
    private fun emitFleetPulse(info: HeartbeatInfo, message: String) {
        val attrList = buildList {
            add(SentryAttribute.stringAttribute("heartbeat", "true"))
            add(SentryAttribute.stringAttribute("ai_provider", info.aiProvider))
            add(SentryAttribute.stringAttribute("manufacturer", info.manufacturer))
            add(SentryAttribute.stringAttribute("model", info.model))
            add(SentryAttribute.integerAttribute("android_sdk", info.androidSdk))
            add(SentryAttribute.stringAttribute("app_version", BuildConfig.VERSION_NAME))
            add(SentryAttribute.stringAttribute("app_build", BuildConfig.VERSION_CODE.toString()))
            add(SentryAttribute.stringAttribute("app_id", BuildConfig.APPLICATION_ID))
            val username = info.username.trim()
            if (username.isNotEmpty()) {
                add(SentryAttribute.stringAttribute("username", username.take(128)))
            }
            add(SentryAttribute.integerAttribute("record_count", info.recordCount))
            add(SentryAttribute.booleanAttribute("is_developer", info.isDeveloper))
        }
        val attrs = SentryAttributes.of(*attrList.toTypedArray())
        Sentry.metrics().count(
            "fitbuddy.daily_active",
            1.0,
            null,
            SentryMetricsParameters.create(attrs)
        )
        Sentry.logger().log(
            SentryLogLevel.INFO,
            SentryLogParameters.create(attrs),
            message
        )
    }

    private const val TAG = "FitBuddyCrash"

    private fun scrub(event: SentryEvent): SentryEvent {
        event.request = null
        event.user?.apply {
            email = null
            username = null
            ipAddress = null
        }
        event.extras?.keys?.toList()?.forEach { key ->
            val value = event.extras?.get(key)?.toString().orEmpty()
            if (looksSecret(value) || looksSecret(key)) {
                event.extras?.remove(key)
            }
        }
        return event
    }

    private fun looksSecret(value: String): Boolean {
        if (value.length < 8) return false
        val lower = value.lowercase()
        return lower.contains("sk-") ||
            lower.contains("aiza") ||
            lower.contains("bearer ") ||
            lower.contains("api_key") ||
            lower.contains("apikey") ||
            Regex("eyJ[A-Za-z0-9_-]{20,}").containsMatchIn(value)
    }
}
