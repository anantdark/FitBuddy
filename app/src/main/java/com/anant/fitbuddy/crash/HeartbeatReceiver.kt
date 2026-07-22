package com.anant.fitbuddy.crash

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anant.fitbuddy.FitBuddyApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Fires around UTC midnight (inexact alarm). Sends the daily heartbeat if not already
 * sent today, then re-arms the alarm for the next midnight.
 */
class HeartbeatReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_HEARTBEAT) return
        val app = context.applicationContext as? FitBuddyApp ?: return
        val settingsRepository = app.settingsRepository

        runBlocking {
            val today = LocalDate.now(ZoneOffset.UTC).toString()
            if (settingsRepository.lastHeartbeatUtcDay() == today) {
                // Already sent today (e.g. from update heartbeat or love-tap).
                HeartbeatScheduler.schedule(context)
                return@runBlocking
            }
            val settings = settingsRepository.settings.first()
            val info = HeartbeatInfo(
                aiProvider = settings.provider.name,
                username = settings.usernameForHeartbeat
            )
            if (CrashReporter.sendHeartbeat(info, HeartbeatKind.DAILY)) {
                settingsRepository.markHeartbeatSent(today)
            }
            // Re-arm for next midnight regardless of success — next cold start
            // will also re-arm, so a missed alarm self-heals.
            HeartbeatScheduler.schedule(context)
        }
    }

    companion object {
        const val ACTION_HEARTBEAT = "com.anant.fitbuddy.action.SENTRY_HEARTBEAT"
    }
}
