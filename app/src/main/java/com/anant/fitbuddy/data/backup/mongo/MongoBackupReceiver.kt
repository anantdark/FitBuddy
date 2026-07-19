package com.anant.fitbuddy.data.backup.mongo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anant.fitbuddy.FitBuddyApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Fires the weekly MongoDB Atlas backup upload and re-arms the next alarm. */
class MongoBackupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_WEEKLY_UPLOAD) return
        val app = context.applicationContext as? FitBuddyApp ?: return
        val pendingResult = goAsync()
        try {
            runBlocking {
                val settings = app.settingsRepository.settings.first()
                if (!settings.isMongoBackupConfigured) {
                    MongoBackupScheduler.cancel(app)
                    return@runBlocking
                }
                runCatching { app.repository.uploadMongoBackup() }
                val updated = app.settingsRepository.settings.first()
                MongoBackupScheduler.scheduleNext(app, updated.mongoLastUploadAt)
            }
        } finally {
            pendingResult.finish()
        }
    }

    companion object {
        const val ACTION_WEEKLY_UPLOAD = "com.anant.fitbuddy.action.MONGO_WEEKLY_BACKUP"
    }
}
