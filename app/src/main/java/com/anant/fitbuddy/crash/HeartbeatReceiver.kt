package com.anant.fitbuddy.crash

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anant.fitbuddy.FitBuddyApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Fires around UTC midnight (inexact alarm). Sends the daily heartbeat if not already
 * sent today, then re-arms the alarm for the next midnight.
 *
 * Work runs on [Dispatchers.IO] via [goAsync] so the daily heartbeat can perform blocking
 * network calls (the direct-ingest reachability probe and the proxy fallback in
 * [SentryHeartbeatProxy]) without touching the main thread.
 */
class HeartbeatReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_HEARTBEAT) return
        val app = context.applicationContext as? FitBuddyApp ?: return
        val settingsRepository = app.settingsRepository
        val appContext = context.applicationContext

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val today = LocalDate.now(ZoneOffset.UTC).toString()
                if (settingsRepository.lastHeartbeatUtcDay() == today) {
                    // Already sent today (e.g. from update heartbeat or love-tap).
                    return@launch
                }
                val settings = settingsRepository.settings.first()
                val recordCount =
                    runCatching { app.repository.getTotalRecordCount() }.getOrDefault(0)
                val info = HeartbeatInfo(
                    aiProvider = settings.provider.name,
                    username = settings.usernameForHeartbeat,
                    recordCount = recordCount,
                    isDeveloper = settings.developerModeUnlocked
                )
                val sent = CrashReporter.sendHeartbeat(info, HeartbeatKind.DAILY)
                // Persist today's transport decision so proxy mode survives process death, and
                // sync the developer toggle to the actual state (the daily auto-check may have
                // flipped it since the last manual toggle).
                val proxied = CrashReporter.isProxyModeActive()
                settingsRepository.setSentryProxyModeDay(if (proxied) today else null)
                if (settings.forceSentryProxyMode != proxied) {
                    settingsRepository.save(settings.copy(forceSentryProxyMode = proxied))
                }
                if (sent) {
                    settingsRepository.markHeartbeatSent(today)
                }
            } finally {
                // Re-arm for next midnight regardless of success — next cold start
                // will also re-arm, so a missed alarm self-heals.
                HeartbeatScheduler.schedule(appContext)
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_HEARTBEAT = "com.anant.fitbuddy.action.SENTRY_HEARTBEAT"
    }
}
