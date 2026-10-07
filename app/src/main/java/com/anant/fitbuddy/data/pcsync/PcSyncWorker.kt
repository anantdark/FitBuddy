package com.anant.fitbuddy.data.pcsync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.anant.fitbuddy.FitBuddyApp
import java.util.concurrent.TimeUnit

/** Background PC sync so entries made on either device show up without opening the app. */
class PcSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pcSync = (applicationContext as FitBuddyApp).pcSync
        if (!pcSync.config.value.isUsable) return Result.success()
        // A down PC is normal (laptop asleep); the next period retries, no backoff needed.
        pcSync.syncNow()
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "pc-sync"

        fun apply(context: Context, config: PcSyncConfig) {
            val work = WorkManager.getInstance(context)
            if (!config.isUsable) {
                work.cancelUniqueWork(UNIQUE_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<PcSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            work.enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
