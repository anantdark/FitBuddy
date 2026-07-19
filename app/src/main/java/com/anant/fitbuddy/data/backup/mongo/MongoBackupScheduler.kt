package com.anant.fitbuddy.data.backup.mongo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.anant.fitbuddy.data.settings.AppSettings

/**
 * Weekly Atlas upload via [AlarmManager] (AOSP; no Play Services).
 * Next fire is derived from [AppSettings.mongoLastUploadAt] so process restarts do not
 * keep pushing the alarm out by another week.
 */
object MongoBackupScheduler {

    const val REQUEST_CODE = 7201
    const val WEEK_MS = 7L * 24L * 60L * 60L * 1000L

    fun applyFromSettings(context: Context, settings: AppSettings) {
        if (settings.isMongoBackupConfigured) {
            scheduleNext(context, settings.mongoLastUploadAt)
        } else {
            cancel(context)
        }
    }

    /**
     * Schedules the next upload. When [lastUploadAt] is 0, fires in one week from now;
     * otherwise advances in weekly steps from that timestamp until a future time.
     */
    fun scheduleNext(
        context: Context,
        lastUploadAt: Long = 0L,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextTriggerMillis(lastUploadAt, nowMillis)
        val pending = pendingIntent(app)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            @Suppress("DEPRECATION")
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun nextTriggerMillis(lastUploadAt: Long, nowMillis: Long = System.currentTimeMillis()): Long {
        if (lastUploadAt <= 0L) return nowMillis + WEEK_MS
        var trigger = lastUploadAt + WEEK_MS
        while (trigger <= nowMillis) {
            trigger += WEEK_MS
        }
        return trigger
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(app))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MongoBackupReceiver::class.java).apply {
            action = MongoBackupReceiver.ACTION_WEEKLY_UPLOAD
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }
}
