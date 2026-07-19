package com.anant.fitbuddy.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anant.fitbuddy.FitBuddyApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Re-arms the daily log reminder after reboot or app update. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val app = context.applicationContext
        val settings = runBlocking {
            (app as? FitBuddyApp)?.settingsRepository?.settings?.first()
        } ?: return
        ReminderScheduler.applyFromSettings(app, settings)
    }
}
