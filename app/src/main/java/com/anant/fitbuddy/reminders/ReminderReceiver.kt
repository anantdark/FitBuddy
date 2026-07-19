package com.anant.fitbuddy.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.anant.fitbuddy.FitBuddyApp
import com.anant.fitbuddy.MainActivity
import com.anant.fitbuddy.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Fires the daily log reminder notification and re-arms the next alarm. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_FIRE) return
        val app = context.applicationContext
        val settings = runBlocking {
            (app as? FitBuddyApp)?.settingsRepository?.settings?.first()
        } ?: return

        if (!settings.dailyLogReminderEnabled) {
            ReminderScheduler.cancel(app)
            return
        }

        postReminderNotification(app, isTest = false)

        ReminderScheduler.scheduleNext(
            app,
            settings.dailyLogReminderHour,
            settings.dailyLogReminderMinute
        )
    }

    companion object {
        const val ACTION_FIRE = "com.anant.fitbuddy.action.DAILY_LOG_REMINDER"
        private const val TEST_NOTIFICATION_ID = ReminderScheduler.NOTIFICATION_ID + 1

        /** Short vibration pattern: wait, vibrate, pause, vibrate. */
        private val VIBRATION_PATTERN = longArrayOf(0, 250, 150, 250)

        fun ensureChannel(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                ReminderScheduler.CHANNEL_ID,
                "Daily log reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to log meals once a day"
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                enableLights(true)
                setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttrs)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(false)
                }
            }
            manager.createNotificationChannel(channel)
        }

        /**
         * Posts a log-reminder style notification with sound + vibration and a random
         * motivational message. [isTest] uses a distinct id so it does not replace the daily one.
         */
        fun postReminderNotification(context: Context, isTest: Boolean = false): Boolean {
            val app = context.applicationContext
            val notifier = NotificationManagerCompat.from(app)
            if (!notifier.areNotificationsEnabled()) return false
            ensureChannel(app)
            val message = ReminderCopy.random()
            val title = if (isTest) "Test · ${message.title}" else message.title
            val contentIntent = PendingIntent.getActivity(
                app,
                if (isTest) 1 else 0,
                Intent(app, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(MainActivity.EXTRA_OPEN_LOG_HUB, true)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(app, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(title)
                .setContentText(message.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVibrate(VIBRATION_PATTERN)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build()

            return runCatching {
                notifier.notify(
                    if (isTest) TEST_NOTIFICATION_ID else ReminderScheduler.NOTIFICATION_ID,
                    notification
                )
            }.isSuccess
        }
    }
}
