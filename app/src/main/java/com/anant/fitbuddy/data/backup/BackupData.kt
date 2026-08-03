package com.anant.fitbuddy.data.backup

import com.anant.fitbuddy.data.database.BodyMeasurement
import com.anant.fitbuddy.data.database.ExerciseLog
import com.anant.fitbuddy.data.database.ExercisePreset
import com.anant.fitbuddy.data.database.FoodLog
import com.anant.fitbuddy.data.database.MealFood
import com.anant.fitbuddy.data.database.MealPreset
import com.anant.fitbuddy.data.database.SavedFood
import com.anant.fitbuddy.data.database.UserProfile
import com.anant.fitbuddy.data.database.WorkoutExercise
import com.anant.fitbuddy.data.database.WorkoutSession
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupData(
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long,
    val profile: UserProfile? = null,
    val measurements: List<BodyMeasurement> = emptyList(),
    val foodLogs: List<FoodLog> = emptyList(),
    val mealFoods: List<MealFood> = emptyList(),
    val exerciseLogs: List<ExerciseLog> = emptyList(),
    /** @deprecated v3 single-food presets; imported into [savedFoods] when empty. */
    val presets: List<SavedFood> = emptyList(),
    val savedFoods: List<SavedFood> = emptyList(),
    val mealPresets: List<MealPreset> = emptyList(),
    val exercisePresets: List<ExercisePreset> = emptyList(),
    val workoutSessions: List<WorkoutSession> = emptyList(),
    val workoutExercises: List<WorkoutExercise> = emptyList(),
    /** Null on pre-v5 backups — import leaves current Settings untouched. */
    val settings: BackupSettings? = null,
    /** v6+: ids deleted after an earlier chunk froze them. Missing on v5 → empty. */
    val deletedFoodLogIds: List<Int> = emptyList(),
    val deletedMealFoodIds: List<Int> = emptyList(),
    val deletedExerciseLogIds: List<Int> = emptyList(),
    val deletedMeasurementIds: List<Int> = emptyList(),
    val deletedSavedFoodIds: List<Int> = emptyList(),
    val deletedMealPresetIds: List<Int> = emptyList(),
    val deletedExercisePresetIds: List<Int> = emptyList(),
    val deletedWorkoutSessionIds: List<Int> = emptyList(),
    val deletedWorkoutExerciseIds: List<Int> = emptyList()
) {
    companion object {
        const val CURRENT_VERSION = 6
        /** Largest sealed tip payload before rollover (UTF-8 byte length of envelope JSON). */
        const val MAX_TIP_SEALED_BYTES = 3 * 1024 * 1024
    }
}
