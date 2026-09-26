package com.anant.fitbuddy.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anant.fitbuddy.data.model.LoggedIngredient
import com.anant.fitbuddy.data.model.NutritionTargetHistory
import com.anant.fitbuddy.data.model.NutritionTargetPeriod
import com.squareup.moshi.Json
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
    // Static-ish attributes used to calculate and explain health targets.
    val sex: String? = null, // "MALE" | "FEMALE" | null
    val goal: String = "RECOMP", // "LOSE_WEIGHT" | "GAIN_MUSCLE" | "RECOMP" | "AUTO"
    val activityLevel: String = "MODERATE", // "SEDENTARY" | "LIGHT" | "MODERATE" | "ACTIVE" | "VERY_ACTIVE"
    // Latest rationale for the calculated goal/targets (shown in Body).
    val goalRationale: String? = null,
    // Optional AI-recommended or manually entered body-weight target.
    val targetWeightKg: Double? = null,
    /** Change-point timeline of nutrition targets; empty until seeded / first real save. */
    val nutritionTargetHistory: List<NutritionTargetPeriod> = emptyList()
) {
    /** True once the user has completed first-run onboarding (age, height, weight). */
    fun hasBasicsConfigured(): Boolean = age > 0 && weightKg > 0 && heightCm > 0

    fun currentTargetPeriod(): NutritionTargetPeriod = NutritionTargetHistory.period(
        from = NutritionTargetHistory.EPOCH_START,
        kcal = dailyTargetCalories,
        proteinG = targetProteinG,
        carbsG = targetCarbsG,
        fatsG = targetFatsG
    )

    fun targetsForDate(date: String): NutritionTargetPeriod =
        NutritionTargetHistory.forDate(
            history = nutritionTargetHistory,
            date = date,
            fallback = currentTargetPeriod()
        ) ?: currentTargetPeriod()

    fun withSeededTargetHistory(): UserProfile {
        if (nutritionTargetHistory.isNotEmpty()) return this
        return copy(
            nutritionTargetHistory = NutritionTargetHistory.seedFromCurrent(
                kcal = dailyTargetCalories,
                proteinG = targetProteinG,
                carbsG = targetCarbsG,
                fatsG = targetFatsG
            )
        )
    }

    fun withoutObsoleteTargetMetadata(): UserProfile {
        val generated = goalRationale?.startsWith("Evidence-based v") == true
        val current = goalRationale?.startsWith("Evidence-based v3:") == true
        return if (generated && !current) copy(goalRationale = null, targetWeightKg = null) else this
    }
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
    val ingredients: List<LoggedIngredient>? = null,
    /** Epoch ms of last pick/log; higher values sort first in the library. */
    val lastUsedAt: Long = 0L
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
    val foods: List<com.anant.fitbuddy.data.model.PresetMealFood>? = null,
    /** Epoch ms of last pick/log; higher values sort first in the library. */
    val lastUsedAt: Long = 0L
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
 * Tracks how often / how recently an exercise was picked into a workout, so the picker can
 * surface Recent and Frequent sections above the full catalog.
 */
@JsonClass(generateAdapter = true)
@Entity(
    tableName = "exercise_usage",
    indices = [Index(value = ["name"], unique = true)]
)
data class ExerciseUsage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    /** ExerciseDB id when the pick came from the catalog; null for customs. */
    val exerciseId: String? = null,
    val lastUsedAt: Long,
    val useCount: Int
)

/**
 * A timestamped body reading. FitBuddy actively supports weight, body-fat percentage, BMR,
 * and muscle mass. Legacy nullable columns remain Room-mapped so existing databases open without
 * destructive migration, but they are ignored by JSON and cleared on every write.
 */
@JsonClass(generateAdapter = true)
@Entity(tableName = "body_measurements")
data class BodyMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val dateString: String, // format: "YYYY-MM-DD"
    val weightKg: Double,
    @Json(ignore = true) val bmi: Double? = null,
    val bodyFatPct: Double? = null,
    @Json(ignore = true) val muscleRatePct: Double? = null,
    @Json(ignore = true) val bodyWaterPct: Double? = null,
    @Json(ignore = true) val boneMassKg: Double? = null,
    val bmr: Int? = null,
    @Json(ignore = true) val metabolicAge: Int? = null,
    @Json(ignore = true) val visceralFat: Double? = null,
    @Json(ignore = true) val subcutaneousFatPct: Double? = null,
    @Json(ignore = true) val proteinMassKg: Double? = null,
    val muscleMassKg: Double? = null,
    @Json(ignore = true) val fatFreeMassKg: Double? = null,
    @Json(ignore = true) val skeletalMuscleMassKg: Double? = null,
    @Json(ignore = true) val waterWeightKg: Double? = null,
    @Json(ignore = true) val fatMassKg: Double? = null,
    @Json(ignore = true) val freescalePayloadJson: String? = null,
) {
    fun supportedMetricsOnly(): BodyMeasurement = copy(
        bmi = null,
        muscleRatePct = null,
        bodyWaterPct = null,
        boneMassKg = null,
        metabolicAge = null,
        visceralFat = null,
        subcutaneousFatPct = null,
        proteinMassKg = null,
        fatFreeMassKg = null,
        skeletalMuscleMassKg = null,
        waterWeightKg = null,
        fatMassKg = null,
        freescalePayloadJson = null,
    )
}

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
