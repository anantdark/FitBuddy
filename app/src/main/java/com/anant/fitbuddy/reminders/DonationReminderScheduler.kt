package com.anant.fitbuddy.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.anant.fitbuddy.data.settings.AppSettings
import java.util.Calendar

/**
 * Schedules the weekly donate reminder notification at 09:00 local on the next due morning.
 */
object DonationReminderScheduler {

    private const val TAG = "DonationReminderSched"

    const val REQUEST_CODE = 7201
    const val CHANNEL_ID = "donate_reminders_v1"
    const val NOTIFICATION_ID = 7201
    const val NOTIF_HOUR = 9
    const val DIALOG_HOUR = 19
    /** Minimum gap between notification and in-app dialog (and vice versa). */
    const val MIN_GAP_MS = 6L * 60L * 60L * 1000L
    const val WEEK_MS = 7L * 24L * 60L * 60L * 1000L
    /** First-run grace before the weekly donate nudge can fire. */
    const val FIRST_RUN_GRACE_MS = 3L * 24L * 60L * 60L * 1000L

    fun applyFromSettings(context: Context, settings: AppSettings) {
        if (settings.donationReminderEnabled) {
            scheduleNext(context, settings)
        } else {
            cancel(context)
        }
    }

    fun scheduleNext(context: Context, settings: AppSettings, nowMillis: Long = System.currentTimeMillis()) {
        val triggerAt = nextMorningTriggerMillis(settings, nowMillis)
        scheduleAt(context, triggerAt)
    }

    fun scheduleAt(context: Context, triggerAtMillis: Long) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(app)
        try {
            if (canScheduleExactAlarms(app)) {
                val info = AlarmManager.AlarmClockInfo(triggerAtMillis, pending)
                alarmManager.setAlarmClock(info, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm denied; scheduling inexact fallback", e)
            runCatching {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            }.onFailure { Log.e(TAG, "Failed to schedule donate reminder", it) }
        }
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(app))
    }

    fun isDue(settings: AppSettings, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!settings.donationReminderEnabled) return false
        val last = settings.donationLastNudgeAt
        if (last <= 0L) return false // not seeded yet
        return nowMillis - last >= WEEK_MS
    }

    fun canShowMorningNotification(
        settings: AppSettings,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        if (!isDue(settings, nowMillis)) return false
        if (hourOfDay(nowMillis) >= DIALOG_HOUR) return false
        if (settings.donationLastDialogAt > 0L &&
            nowMillis - settings.donationLastDialogAt < MIN_GAP_MS
        ) {
            return false
        }
        if (settings.donationLastNotifAt > settings.donationLastNudgeAt) return false
        return true
    }

    fun canShowEveningDialog(
        settings: AppSettings,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        if (!isDue(settings, nowMillis)) return false
        if (hourOfDay(nowMillis) < DIALOG_HOUR) return false
        if (settings.donationLastNotifAt > 0L &&
            nowMillis - settings.donationLastNotifAt < MIN_GAP_MS
        ) {
            return false
        }
        if (settings.donationLastDialogAt > settings.donationLastNudgeAt) return false
        return true
    }

    /** Next 09:00 on/after [donationLastNudgeAt] + 7 days (or next 09:00 if already due). */
    fun nextMorningTriggerMillis(
        settings: AppSettings,
        nowMillis: Long = System.currentTimeMillis(),
    ): Long {
        val earliest = if (settings.donationLastNudgeAt > 0L) {
            settings.donationLastNudgeAt + WEEK_MS
        } else {
            nowMillis + FIRST_RUN_GRACE_MS
        }
        val base = maxOf(nowMillis, earliest)
        return nextHourOccurrence(NOTIF_HOUR, base)
    }

    /**
     * Seeds [AppSettings.donationLastNudgeAt] so the first real nudge is after
     * [FIRST_RUN_GRACE_MS] (not a full week). Call once when the field is still 0.
     */
    fun seedFirstRunNudgeAt(nowMillis: Long = System.currentTimeMillis()): Long =
        nowMillis - WEEK_MS + FIRST_RUN_GRACE_MS

    fun nextHourOccurrence(hour: Int, nowMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, 0)
            if (timeInMillis <= nowMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    private fun hourOfDay(nowMillis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DonationReminderReceiver::class.java).apply {
            action = DonationReminderReceiver.ACTION_FIRE
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }
}
