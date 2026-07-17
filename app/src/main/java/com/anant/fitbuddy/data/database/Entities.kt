package com.anant.fitbuddy.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anant.fitbuddy.data.model.LoggedIngredient
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val age: Int,
    val weightKg: Double,
    val heightCm: Double,
    val dailyTargetCalories: Int,
    val targetProteinG: Int,
    val targetCarbsG: Int,
    val targetFatsG: Int,
    val lastUpdatedTimestamp: Long,
    // Static-ish attributes used to personalise AI target design.
    val sex: String? = null, // "MALE" | "FEMALE" | null
    val goal: String = "RECOMP", // "LOSE_WEIGHT" | "GAIN_MUSCLE" | "RECOMP" | "AUTO"
    val activityLevel: String = "MODERATE", // "SEDENTARY" | "LIGHT" | "MODERATE" | "ACTIVE" | "VERY_ACTIVE"
    // Latest AI rationale for the recommended goal/targets (shown in Profile).
    val goalRationale: String? = null
) {
    /** True once the user has completed first-run onboarding (age, height, weight). */
    fun hasBasicsConfigured(): Boolean = age > 0 && weightKg > 0 && heightCm > 0
}

@JsonClass(generateAdapter = true)
@Entity(tableName = "food_logs")
data class FoodLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** Meal name (e.g. "Breakfast"). */
    val dishName: String,
    val timestamp: Long,
    val dateString: String, // format: "YYYY-MM-DD"
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatsG: Int
)

/**
 * One food within a logged meal ([FoodLog]), mirroring [WorkoutExercise] inside [WorkoutSession].
 * Each row carries its own ingredient breakdown so edits preserve the food → ingredient hierarchy.
 */
@JsonClass(generateAdapter = true)
@Entity(
    tableName = "meal_foods",
    foreignKeys = [
        ForeignKey(
            entity = FoodLog::class,
            parentColumns = ["id"],
            childColumns = ["mealLogId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mealLogId")]
)
data class MealFood(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mealLogId: Int,
    val name: String,
    val servings: Double,
    val orderIndex: Int = 0,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatsG: Int,
    val ingredients: List<LoggedIngredient>? = null,
    val presetId: Int? = null,
    val barcode: String? = null
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "exercise_logs")
data class ExerciseLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val activityName: String,
    val timestamp: Long,
    val dateString: String, // format: "YYYY-MM-DD"
    val caloriesBurned: Int,
    val durationMinutes: Int
)

/** A reusable single-food library item (ingredients + macros) for meal building only. */
@JsonClass(generateAdapter = true)
@Entity(tableName = "saved_foods")
data class SavedFood(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatsG: Int,
    val createdAt: Long,
    val barcode: String? = null,
    val ingredients: List<LoggedIngredient>? = null
)

/** A reusable meal template (multiple foods) for one-tap logging from the dashboard. */
@JsonClass(generateAdapter = true)
@Entity(tableName = "meal_presets")
data class MealPreset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatsG: Int,
    val createdAt: Long,
    val foods: List<com.anant.fitbuddy.data.model.PresetMealFood>? = null
)

/** A user-added exercise saved for the workout picker after AI (or offline) normalisation. */
@JsonClass(generateAdapter = true)
@Entity(tableName = "exercise_presets")
data class ExercisePreset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val equipment: String,
    val createdAt: Long
)

/**
 * A timestamped body-composition reading. Only [weightKg] is required; the remaining fields come
 * from a smart scale and are optional. Stored as a time series so trends can be charted and fed
 * to the AI for goal/target design and progress insight.
 */
@JsonClass(generateAdapter = true)
@Entity(tableName = "body_measurements")
data class BodyMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val dateString: String, // format: "YYYY-MM-DD"
    val weightKg: Double,
    val bmi: Double? = null,
    val bodyFatPct: Double? = null,
    val muscleRatePct: Double? = null,
    val bodyWaterPct: Double? = null,
    val boneMassKg: Double? = null,
    val bmr: Int? = null,
    val metabolicAge: Int? = null,
    val visceralFat: Double? = null,
    val subcutaneousFatPct: Double? = null,
    val proteinMassKg: Double? = null,
    val muscleMassKg: Double? = null,
    val fatFreeMassKg: Double? = null, // "weight without fat"
    val skeletalMuscleMassKg: Double? = null,
    val waterWeightKg: Double? = null,
    val fatMassKg: Double? = null
)

/**
 * A logged gym/workout session containing one or more [WorkoutExercise] entries. [caloriesBurned]
 * starts at 0 and is filled in once the AI (or the offline estimator) computes it; [exerciseLogId]
 * links to the mirrored [ExerciseLog] row so the burn counts toward daily totals/dashboard/progress
 * without duplicating that logic, and so deleting one cleans up the other.
 */
@JsonClass(generateAdapter = true)
@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val timestamp: Long,
    val dateString: String, // format: "YYYY-MM-DD"
    val durationMinutes: Int,
    val caloriesBurned: Int = 0,
    val exerciseLogId: Int? = null
)

/**
 * One exercise within a [WorkoutSession] (identified by [sessionId]), with sets/reps/weight.
 * [equipment] mirrors one of the `Equipment.*` tags (kept as a plain String literal here to
 * avoid a data.database -> data.model dependency) and is preserved so the workout can be
 * re-estimated correctly if the user edits it later.
 */
@JsonClass(generateAdapter = true)
@Entity(tableName = "workout_exercises")
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val name: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Double? = null, // null for bodyweight exercises
    val orderIndex: Int = 0,
    val equipment: String = "Other",
    val durationMinutes: Int? = null,
    val distanceKm: Double? = null
)
