package com.anant.fitbuddy.data.backup

import android.content.Context
import android.net.Uri
import com.anant.fitbuddy.data.database.BodyMeasurementDao
import com.anant.fitbuddy.data.database.ExerciseLogDao
import com.anant.fitbuddy.data.database.ExercisePresetDao
import com.anant.fitbuddy.data.database.FoodLogDao
import com.anant.fitbuddy.data.database.MealFoodDao
import com.anant.fitbuddy.data.database.MealPresetDao
import com.anant.fitbuddy.data.database.SavedFoodDao
import com.anant.fitbuddy.data.database.UserProfileDao
import com.anant.fitbuddy.data.database.WorkoutExerciseDao
import com.anant.fitbuddy.data.database.WorkoutSessionDao
import com.anant.fitbuddy.data.settings.SettingsRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class BackupManager(
    private val context: Context,
    private val userProfileDao: UserProfileDao,
    private val foodLogDao: FoodLogDao,
    private val mealFoodDao: MealFoodDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val savedFoodDao: SavedFoodDao,
    private val mealPresetDao: MealPresetDao,
    private val exercisePresetDao: ExercisePresetDao,
    private val bodyMeasurementDao: BodyMeasurementDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val workoutExerciseDao: WorkoutExerciseDao,
    private val settingsRepository: SettingsRepository,
    moshi: Moshi
) {
    private val adapter = moshi.adapter(BackupData::class.java).indent("  ")

    suspend fun buildBackupData(): BackupData = withContext(Dispatchers.IO) {
        snapshot()
    }

    /** Moshi JSON for [data] (same format as file export). */
    fun encode(data: BackupData): String = adapter.toJson(data)

    suspend fun toJson(): String = withContext(Dispatchers.IO) {
        encode(snapshot())
    }

    fun countRecords(data: BackupData, legacyFoodCount: Int? = null): Int {
        val foods = legacyFoodCount ?: data.savedFoods.size
        return data.measurements.size + data.foodLogs.size + data.mealFoods.size +
            data.exerciseLogs.size + foods + data.mealPresets.size +
            data.exercisePresets.size + data.workoutSessions.size +
            data.workoutExercises.size + if (data.settings != null) 1 else 0
    }

    suspend fun exportTo(uri: Uri): Int = withContext(Dispatchers.IO) {
        val data = snapshot()
        val json = adapter.toJson(data)
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        } ?: error("Couldn't open the selected file for writing")
        countRecords(data)
    }

    suspend fun importFrom(uri: Uri): Int = withContext(Dispatchers.IO) {
        val json = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: error("Couldn't open the selected file for reading")
        importFromJsonInternal(json)
    }

    suspend fun importFromJson(json: String): Int = withContext(Dispatchers.IO) {
        importFromJsonInternal(json)
    }

    private suspend fun snapshot(): BackupData {
        val settings = settingsRepository.settings.first()
        return BackupData(
            exportedAt = System.currentTimeMillis(),
            profile = userProfileDao.getProfileOnce(),
            measurements = bodyMeasurementDao.getAllOnce(),
            foodLogs = foodLogDao.getAllOnce(),
            mealFoods = mealFoodDao.getAllOnce(),
            exerciseLogs = exerciseLogDao.getAllOnce(),
            savedFoods = savedFoodDao.getAllOnce(),
            mealPresets = mealPresetDao.getAllOnce(),
            exercisePresets = exercisePresetDao.getAllOnce(),
            workoutSessions = workoutSessionDao.getAllOnce(),
            workoutExercises = workoutExerciseDao.getAllOnce(),
            settings = BackupSettings.from(settings)
        )
    }

    private suspend fun importFromJsonInternal(json: String): Int {
        val data = adapter.fromJson(json)
            ?: error("The selected file isn't a valid FitBuddy backup")

        userProfileDao.clearAll()
        bodyMeasurementDao.clearAll()
        foodLogDao.clearAll()
        mealFoodDao.clearAll()
        exerciseLogDao.clearAll()
        savedFoodDao.clearAll()
        mealPresetDao.clearAll()
        exercisePresetDao.clearAll()
        workoutExerciseDao.clearAll()
        workoutSessionDao.clearAll()

        data.profile?.let { userProfileDao.insertOrUpdateProfile(it.copy(id = 1)) }
        bodyMeasurementDao.insertAll(data.measurements.map { it.copy(id = 0) })

        val legacyFoods = if (data.savedFoods.isNotEmpty()) data.savedFoods else data.presets
        val savedFoodIdMap = BackupIdRemapper.idMap(
            oldIds = legacyFoods.map { it.id },
            newIds = savedFoodDao.insertAll(legacyFoods.map { it.copy(id = 0) })
        )

        mealPresetDao.insertAll(
            data.mealPresets.map { BackupIdRemapper.remapMealPreset(it, savedFoodIdMap) }
        )
        exercisePresetDao.insertAll(data.exercisePresets.map { it.copy(id = 0) })

        val mealLogIdMap = BackupIdRemapper.idMap(
            oldIds = data.foodLogs.map { it.id },
            newIds = foodLogDao.insertAll(data.foodLogs.map { it.copy(id = 0) })
        )

        val remappedMealFoods = data.mealFoods.mapNotNull { food ->
            BackupIdRemapper.remapMealFood(food, mealLogIdMap, savedFoodIdMap)
        }
        mealFoodDao.insertAll(remappedMealFoods)

        val exerciseLogIdMap = BackupIdRemapper.idMap(
            oldIds = data.exerciseLogs.map { it.id },
            newIds = exerciseLogDao.insertAll(data.exerciseLogs.map { it.copy(id = 0) })
        )

        val remappedSessions = data.workoutSessions.map { session ->
            session.copy(
                id = 0,
                exerciseLogId = session.exerciseLogId?.let { exerciseLogIdMap[it] }
            )
        }
        val sessionIdMap = BackupIdRemapper.idMap(
            oldIds = data.workoutSessions.map { it.id },
            newIds = workoutSessionDao.insertAll(remappedSessions)
        )

        val remappedExercises = data.workoutExercises.mapNotNull { exercise ->
            sessionIdMap[exercise.sessionId]?.let { newSessionId ->
                exercise.copy(id = 0, sessionId = newSessionId)
            }
        }
        workoutExerciseDao.insertAll(remappedExercises)

        data.settings?.let { settingsRepository.save(it.toAppSettings()) }

        return countRecords(data, legacyFoodCount = legacyFoods.size)
    }
}
