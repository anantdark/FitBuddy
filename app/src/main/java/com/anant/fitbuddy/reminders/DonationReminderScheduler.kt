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
 * Schedules the donate reminder notification at 09:00 local on the next due morning.
 *
 * Gap after each completed nudge ([AppSettings.donationLastNudgeAt]) is a stable value in
 * [MIN_INTERVAL_DAYS]..[MAX_INTERVAL_DAYS], derived from that timestamp (not re-rolled on every
 * check) so [isDue] and [nextMorningTriggerMillis] always agree — avoids both spam and
 * never-firing schedules.
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

    const val MIN_INTERVAL_DAYS = 3
    const val MAX_INTERVAL_DAYS = 8
    const val DAY_MS = 24L * 60L * 60L * 1000L

    /** Inclusive span of random-but-stable interval days. */
    private const val INTERVAL_SPAN = MAX_INTERVAL_DAYS - MIN_INTERVAL_DAYS + 1

    /** First-run grace before the donate nudge can fire (matches minimum interval). */
    const val FIRST_RUN_GRACE_MS = MIN_INTERVAL_DAYS * DAY_MS

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

    /**
     * Stable gap in whole days for the cycle that started at [lastNudgeAt].
     * Same input always yields the same day count in [MIN_INTERVAL_DAYS]..[MAX_INTERVAL_DAYS].
     */
    fun intervalDaysFor(lastNudgeAt: Long): Int {
        // Mix bits so nearby timestamps don't all land on the same day bucket.
        val mixed = lastNudgeAt xor (lastNudgeAt ushr 33) xor (lastNudgeAt shl 11)
        val idx = floorMod(mixed, INTERVAL_SPAN)
        return MIN_INTERVAL_DAYS + idx
    }

    fun intervalMsFor(lastNudgeAt: Long): Long = intervalDaysFor(lastNudgeAt) * DAY_MS

    fun isDue(settings: AppSettings, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!settings.donationReminderEnabled) return false
        val last = settings.donationLastNudgeAt
        if (last <= 0L) return false // not seeded yet
        return nowMillis - last >= intervalMsFor(last)
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
        // Already notified in this cycle (before lastNudgeAt advances on dialog dismiss).
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
        // Already showed the dialog in this cycle.
        if (settings.donationLastDialogAt > settings.donationLastNudgeAt) return false
        return true
    }

    /**
     * Next 09:00 on/after [donationLastNudgeAt] + that cycle's interval
     * (or after [FIRST_RUN_GRACE_MS] when not yet seeded).
     */
    fun nextMorningTriggerMillis(
        settings: AppSettings,
        nowMillis: Long = System.currentTimeMillis(),
    ): Long {
        val earliest = if (settings.donationLastNudgeAt > 0L) {
            settings.donationLastNudgeAt + intervalMsFor(settings.donationLastNudgeAt)
        } else {
            nowMillis + FIRST_RUN_GRACE_MS
        }
        val base = maxOf(nowMillis, earliest)
        return nextHourOccurrence(NOTIF_HOUR, base)
    }

    /**
     * Seeds [AppSettings.donationLastNudgeAt] so the first real nudge is after this install's
     * stable interval (at least [FIRST_RUN_GRACE_MS] / [MIN_INTERVAL_DAYS] days).
     *
     * Uses [nowMillis] as the baseline: [isDue] becomes true only after
     * [intervalMsFor] `(nowMillis)` elapses — never sooner than 3 days, never later than 8.
     */
    fun seedFirstRunNudgeAt(nowMillis: Long = System.currentTimeMillis()): Long = nowMillis

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

    private fun floorMod(value: Long, modulus: Int): Int {
        val m = modulus.toLong()
        val r = value % m
        return (if (r < 0) r + m else r).toInt()
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
