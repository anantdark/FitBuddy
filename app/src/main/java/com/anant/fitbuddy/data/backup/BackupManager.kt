package com.anant.fitbuddy.data.backup

import android.content.Context
import android.net.Uri
import com.anant.fitbuddy.data.database.BodyMeasurementDao
import com.anant.fitbuddy.data.database.ExerciseLogDao
import com.anant.fitbuddy.data.database.FoodLogDao
import com.anant.fitbuddy.data.database.MealFoodDao
import com.anant.fitbuddy.data.database.MealPresetDao
import com.anant.fitbuddy.data.database.SavedFoodDao
import com.anant.fitbuddy.data.database.UserProfileDao
import com.anant.fitbuddy.data.database.WorkoutExerciseDao
import com.anant.fitbuddy.data.database.WorkoutSessionDao
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupManager(
    private val context: Context,
    private val userProfileDao: UserProfileDao,
    private val foodLogDao: FoodLogDao,
    private val mealFoodDao: MealFoodDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val savedFoodDao: SavedFoodDao,
    private val mealPresetDao: MealPresetDao,
    private val bodyMeasurementDao: BodyMeasurementDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val workoutExerciseDao: WorkoutExerciseDao,
    moshi: Moshi
) {
    private val adapter = moshi.adapter(BackupData::class.java).indent("  ")

    suspend fun exportTo(uri: Uri): Int = withContext(Dispatchers.IO) {
        val data = BackupData(
            exportedAt = System.currentTimeMillis(),
            profile = userProfileDao.getProfileOnce(),
            measurements = bodyMeasurementDao.getAllOnce(),
            foodLogs = foodLogDao.getAllOnce(),
            mealFoods = mealFoodDao.getAllOnce(),
            exerciseLogs = exerciseLogDao.getAllOnce(),
            savedFoods = savedFoodDao.getAllOnce(),
            mealPresets = mealPresetDao.getAllOnce(),
            workoutSessions = workoutSessionDao.getAllOnce(),
            workoutExercises = workoutExerciseDao.getAllOnce()
        )
        val json = adapter.toJson(data)
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        } ?: error("Couldn't open the selected file for writing")
        data.measurements.size + data.foodLogs.size + data.mealFoods.size +
            data.exerciseLogs.size + data.savedFoods.size + data.mealPresets.size +
            data.workoutSessions.size + data.workoutExercises.size
    }

    suspend fun importFrom(uri: Uri): Int = withContext(Dispatchers.IO) {
        val json = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: error("Couldn't open the selected file for reading")

        val data = adapter.fromJson(json)
            ?: error("The selected file isn't a valid FitBuddy backup")

        userProfileDao.clearAll()
        bodyMeasurementDao.clearAll()
        foodLogDao.clearAll()
        mealFoodDao.clearAll()
        exerciseLogDao.clearAll()
        savedFoodDao.clearAll()
        mealPresetDao.clearAll()
        workoutExerciseDao.clearAll()
        workoutSessionDao.clearAll()

        data.profile?.let { userProfileDao.insertOrUpdateProfile(it.copy(id = 1)) }
        bodyMeasurementDao.insertAll(data.measurements.map { it.copy(id = 0) })

        val legacyFoods = if (data.savedFoods.isNotEmpty()) data.savedFoods else data.presets
        savedFoodDao.insertAll(legacyFoods.map { it.copy(id = 0) })
        mealPresetDao.insertAll(data.mealPresets.map { it.copy(id = 0) })

        val mealLogIdMap = data.foodLogs.map { it.id }
            .zip(foodLogDao.insertAll(data.foodLogs.map { it.copy(id = 0) }))
            .toMap()

        val remappedMealFoods = data.mealFoods.mapNotNull { food ->
            mealLogIdMap[food.mealLogId]?.let { newMealId ->
                food.copy(id = 0, mealLogId = newMealId.toInt())
            }
        }
        mealFoodDao.insertAll(remappedMealFoods)

        val exerciseLogIdMap = data.exerciseLogs.map { it.id }
            .zip(exerciseLogDao.insertAll(data.exerciseLogs.map { it.copy(id = 0) }))
            .toMap()

        val remappedSessions = data.workoutSessions.map { session ->
            session.copy(
                id = 0,
                exerciseLogId = session.exerciseLogId?.let { exerciseLogIdMap[it]?.toInt() }
            )
        }
        val sessionIdMap = data.workoutSessions.map { it.id }
            .zip(workoutSessionDao.insertAll(remappedSessions))
            .toMap()

        val remappedExercises = data.workoutExercises.mapNotNull { exercise ->
            sessionIdMap[exercise.sessionId]?.let { newSessionId ->
                exercise.copy(id = 0, sessionId = newSessionId.toInt())
            }
        }
        workoutExerciseDao.insertAll(remappedExercises)

        data.measurements.size + data.foodLogs.size + data.mealFoods.size +
            data.exerciseLogs.size + legacyFoods.size + data.mealPresets.size +
            data.workoutSessions.size + data.workoutExercises.size
    }
}
