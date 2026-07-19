package com.anant.fitbuddy

import android.app.Application
import com.anant.fitbuddy.crash.CrashReporter
import com.anant.fitbuddy.crash.HeartbeatInfo
import com.anant.fitbuddy.data.backup.BackupManager
import com.anant.fitbuddy.data.database.AppDatabase
import com.anant.fitbuddy.data.remote.NetworkModule
import com.anant.fitbuddy.data.remote.OpenFoodFactsDataSource
import com.anant.fitbuddy.data.remote.RemoteAiDataSource
import com.anant.fitbuddy.data.remote.UpdateChecker
import com.anant.fitbuddy.data.repository.FitnessRepository
import com.anant.fitbuddy.data.settings.SettingsRepository
import com.anant.fitbuddy.reminders.ReminderReceiver
import com.anant.fitbuddy.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Application-scoped service locator. Everything is created lazily and lives for the whole
 * process, which is exactly the lifetime we want for the DB and the repositories.
 */
class FitBuddyApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database by lazy { AppDatabase.getDatabase(this) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    val updateChecker: UpdateChecker by lazy { UpdateChecker(NetworkModule.provideGithubApi()) }

    val repository: FitnessRepository by lazy {
        FitnessRepository(
            userProfileDao = database.userProfileDao(),
            foodLogDao = database.foodLogDao(),
            mealFoodDao = database.mealFoodDao(),
            exerciseLogDao = database.exerciseLogDao(),
            savedFoodDao = database.savedFoodDao(),
            mealPresetDao = database.mealPresetDao(),
            exercisePresetDao = database.exercisePresetDao(),
            bodyMeasurementDao = database.bodyMeasurementDao(),
            workoutSessionDao = database.workoutSessionDao(),
            workoutExerciseDao = database.workoutExerciseDao(),
            remoteAiDataSource = RemoteAiDataSource(
                api = NetworkModule.provideAiApi(),
                moshi = NetworkModule.moshi
            ),
            openFoodFactsDataSource = OpenFoodFactsDataSource(
                api = NetworkModule.provideOpenFoodFactsApi()
            ),
            settingsRepository = settingsRepository,
            backupManager = BackupManager(
                context = this,
                userProfileDao = database.userProfileDao(),
                foodLogDao = database.foodLogDao(),
                mealFoodDao = database.mealFoodDao(),
                exerciseLogDao = database.exerciseLogDao(),
                savedFoodDao = database.savedFoodDao(),
                mealPresetDao = database.mealPresetDao(),
                exercisePresetDao = database.exercisePresetDao(),
                bodyMeasurementDao = database.bodyMeasurementDao(),
                workoutSessionDao = database.workoutSessionDao(),
                workoutExerciseDao = database.workoutExerciseDao(),
                settingsRepository = settingsRepository,
                moshi = NetworkModule.moshi
            )
        )
    }

    override fun onCreate() {
        super.onCreate()
        val settings = runBlocking {
            settingsRepository.ensureSupportId()
            settingsRepository.settings.first()
        }
        CrashReporter.init(
            app = this,
            enabled = settings.crashReportingEnabled,
            supportId = settings.supportId
        )
        NetworkModule.setVerboseHttpLogging(settings.verboseHttpLogging)
        ReminderReceiver.ensureChannel(this)
        ReminderScheduler.applyFromSettings(this, settings)
        appScope.launch {
            settingsRepository.settings
                .map { s ->
                    Triple(
                        s.dailyLogReminderEnabled,
                        s.dailyLogReminderHour to s.dailyLogReminderMinute,
                        s.verboseHttpLogging
                    )
                }
                .distinctUntilChanged()
                .collect { (enabled, time, verboseHttp) ->
                    val (hour, minute) = time
                    NetworkModule.setVerboseHttpLogging(verboseHttp)
                    if (enabled) {
                        ReminderScheduler.scheduleNext(this@FitBuddyApp, hour, minute)
                    } else {
                        ReminderScheduler.cancel(this@FitBuddyApp)
                    }
                }
        }
        if (settings.crashReportingEnabled) {
            Thread({
                runBlocking(Dispatchers.IO) { maybeSendDailyHeartbeat() }
            }, "fitbuddy-heartbeat").start()
        }
    }

    private suspend fun maybeSendDailyHeartbeat() {
        val today = LocalDate.now(ZoneOffset.UTC).toString()
        if (settingsRepository.lastHeartbeatUtcDay() == today) return
        val settings = settingsRepository.settings.first()
        val info = HeartbeatInfo(aiProvider = settings.provider.name)
        if (CrashReporter.sendDailyHeartbeat(info)) {
            settingsRepository.markHeartbeatSent(today)
        }
    }
}
