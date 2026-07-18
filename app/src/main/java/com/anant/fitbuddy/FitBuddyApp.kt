package com.anant.fitbuddy

import android.app.Application
import com.anant.fitbuddy.data.backup.BackupManager
import com.anant.fitbuddy.data.database.AppDatabase
import com.anant.fitbuddy.data.remote.NetworkModule
import com.anant.fitbuddy.data.remote.OpenFoodFactsDataSource
import com.anant.fitbuddy.data.remote.RemoteAiDataSource
import com.anant.fitbuddy.data.remote.UpdateChecker
import com.anant.fitbuddy.data.repository.FitnessRepository
import com.anant.fitbuddy.data.settings.SettingsRepository

/**
 * Application-scoped service locator. Everything is created lazily and lives for the whole
 * process, which is exactly the lifetime we want for the DB and the repositories.
 */
class FitBuddyApp : Application() {

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
                bodyMeasurementDao = database.bodyMeasurementDao(),
                workoutSessionDao = database.workoutSessionDao(),
                workoutExerciseDao = database.workoutExerciseDao(),
                moshi = NetworkModule.moshi
            )
        )
    }
}
