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
    val settings: BackupSettings? = null
) {
    companion object {
        const val CURRENT_VERSION = 5
    }
}
