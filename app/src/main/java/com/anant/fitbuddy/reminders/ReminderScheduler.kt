package com.anant.fitbuddy.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.anant.fitbuddy.data.settings.AppSettings
import java.util.Calendar

/**
 * Schedules the daily log reminder with [AlarmManager.setAlarmClock] (AOSP; no Play Services).
 * Re-arm after each fire and on boot via [ReminderReceiver] / [BootReceiver].
 */
object ReminderScheduler {

    const val REQUEST_CODE = 7101
    /** New id so channel sound/vibration settings apply (Android freezes channel attrs after create). */
    const val CHANNEL_ID = "log_reminders_v2"
    const val NOTIFICATION_ID = 7101

    fun applyFromSettings(context: Context, settings: AppSettings) {
        if (settings.dailyLogReminderEnabled) {
            scheduleNext(context, settings.dailyLogReminderHour, settings.dailyLogReminderMinute)
        } else {
            cancel(context)
        }
    }

    fun scheduleNext(context: Context, hour: Int, minute: Int) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextTriggerMillis(hour, minute)
        val pending = pendingIntent(app)
        val info = AlarmManager.AlarmClockInfo(triggerAt, pending)
        alarmManager.setAlarmClock(info, pending)
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(app))
    }

    fun nextTriggerMillis(hour: Int, minute: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            if (timeInMillis <= nowMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    /** True when the OS may still deliver exact alarm-clock alarms (always true for setAlarmClock). */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }
}
