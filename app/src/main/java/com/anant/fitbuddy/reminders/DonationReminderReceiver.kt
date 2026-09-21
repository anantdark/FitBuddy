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

/** Fires the weekly donate reminder notification and re-arms the next alarm. */
class DonationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_FIRE) return
        val app = context.applicationContext
        val settingsRepo = (app as? FitBuddyApp)?.settingsRepository ?: return
        val settings = runBlocking { settingsRepo.settings.first() }

        if (!settings.donationReminderEnabled) {
            DonationReminderScheduler.cancel(app)
            return
        }

        if (DonationReminderScheduler.canShowMorningNotification(settings)) {
            postDonationNotification(app, isTest = false)
            runBlocking {
                val now = System.currentTimeMillis()
                settingsRepo.save(
                    settings.copy(donationLastNotifAt = now)
                )
            }
        }

        val updated = runBlocking { settingsRepo.settings.first() }
        DonationReminderScheduler.scheduleNext(app, updated)
    }

    companion object {
        const val ACTION_FIRE = "com.anant.fitbuddy.action.DONATE_REMINDER"
        private const val TEST_NOTIFICATION_ID = DonationReminderScheduler.NOTIFICATION_ID + 1
        private val VIBRATION_PATTERN = longArrayOf(0, 250, 150, 250)

        fun ensureChannel(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                DonationReminderScheduler.CHANNEL_ID,
                "Donate reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Optional weekly reminder to support FitBuddy development"
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                enableLights(true)
                setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttrs)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }

        fun postDonationNotification(context: Context, isTest: Boolean = false): Boolean {
            val app = context.applicationContext
            val notifier = NotificationManagerCompat.from(app)
            if (!notifier.areNotificationsEnabled()) return false
            ensureChannel(app)
            val message = DonationNotificationCopy.random()
            val title = if (isTest) "Test · ${message.title}" else message.title
            val contentIntent = PendingIntent.getActivity(
                app,
                if (isTest) 3 else 2,
                Intent(app, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(MainActivity.EXTRA_OPEN_DONATE_REMINDER, true)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(app, DonationReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(title)
                .setContentText(message.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build()

            return runCatching {
                notifier.notify(
                    if (isTest) TEST_NOTIFICATION_ID else DonationReminderScheduler.NOTIFICATION_ID,
                    notification
                )
            }.isSuccess
        }
    }
}
