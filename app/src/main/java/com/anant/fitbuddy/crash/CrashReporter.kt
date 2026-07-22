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
}

/**
 * Thin Sentry wrapper: crashes/ANRs, optional daily heartbeat check-ins, no PII.
 * Empty [BuildConfig.SENTRY_DSN_BLOB] keeps the SDK uninitialized (local builds without a key).
 */
object CrashReporter {

    /** Fleet-level cron monitor: any install with reporting on may check in once per UTC day. */
    const val HEARTBEAT_MONITOR_SLUG = "fitbuddy-daily-heartbeat"

    private val ready = AtomicBoolean(false)
    @Volatile
    private var reportingEnabled: Boolean = true

    fun init(app: Application, enabled: Boolean, supportId: String) {
        if (BuildConfig.SENTRY_DSN_BLOB.isBlank()) return
        val dsn = MongoUriVault.decode(BuildConfig.SENTRY_DSN_BLOB, BuildConfig.SENTRY_DSN_MASK).trim()
        if (dsn.isEmpty()) return
        reportingEnabled = enabled
        SentryAndroid.init(app) { options ->
            options.dsn = dsn
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
                    HeartbeatKind.entries.any { it.logMessage == msg }
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

    /**
     * Anonymous heartbeat: Crons check-in (OK) plus Metrics/Logs with device/app/AI
     * attributes for fleet breakdown (Explore → Metrics / Logs — not Issues).
     * Callers gate once-per-day / once-per-update. Returns true if the check-in was sent.
     * Sent regardless of [reportingEnabled] — heartbeats are fleet install/version
     * telemetry, not crash reports, so the crash-reporting opt-out doesn't gate them.
     */
    fun sendHeartbeat(info: HeartbeatInfo, kind: HeartbeatKind = HeartbeatKind.DAILY): Boolean {
        if (!ready.get()) return false
        return runCatching {
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
            emitFleetPulse(info, message = kind.logMessage)
            // Flush so cold-start pulse isn't lost if the process is killed early.
            Sentry.flush(5_000L)
            checkInId != SentryId.EMPTY_ID
        }.onFailure { e ->
            Log.e(TAG, "heartbeat failed", e)
        }.getOrDefault(false)
    }

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
