package com.anant.fitbuddy.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * AI-designed daily nutrition plan derived from the user's profile + latest body composition.
 * [recommendedGoal] is one of LOSE_WEIGHT | GAIN_MUSCLE | RECOMP.
 */
@JsonClass(generateAdapter = true)
data class TargetPlanResponse(
    @Json(name = "recommended_goal") val recommendedGoal: String,
    @Json(name = "daily_target_calories") val dailyTargetCalories: Int,
    @Json(name = "target_protein_g") val targetProteinG: Int,
    @Json(name = "target_carbs_g") val targetCarbsG: Int,
    @Json(name = "target_fats_g") val targetFatsG: Int,
    @Json(name = "rationale") val rationale: String
)

/**
 * AI narrative summary of the user's progress plus a short list of actionable recommendations.
 * [bodyScore] is a 0-100 holistic body-composition score (higher is better), or null if there
 * isn't enough data yet to score.
 */
@JsonClass(generateAdapter = true)
data class ProgressInsightResponse(
    @Json(name = "summary") val summary: String,
    @Json(name = "recommendations") val recommendations: List<String> = emptyList(),
    @Json(name = "body_score") val bodyScore: Int? = null
)

/**
 * AI-estimated energy expenditure for a logged workout session, personalised to the user's body
 * factors (weight, age, sex, activity level).
 */
@JsonClass(generateAdapter = true)
data class WorkoutCaloriesResponse(
    @Json(name = "calories_burned") val caloriesBurned: Int,
    @Json(name = "duration_minutes") val durationMinutes: Int,
    @Json(name = "intensity_note") val intensityNote: String? = null
)

/** AI-normalised name + equipment for a user-typed custom exercise. */
@JsonClass(generateAdapter = true)
data class CustomExerciseResponse(
    @Json(name = "canonical_name") val canonicalName: String,
    @Json(name = "equipment") val equipment: String
)

/** One exercise parsed from a free-text workout description. */
@JsonClass(generateAdapter = true)
data class ParsedWorkoutExercise(
    @Json(name = "name") val name: String,
    @Json(name = "equipment") val equipment: String,
    @Json(name = "sets") val sets: Int,
    @Json(name = "reps") val reps: Int,
    @Json(name = "weight_kg") val weightKg: Double? = null,
    @Json(name = "duration_minutes") val durationMinutes: Int? = null,
    @Json(name = "distance_km") val distanceKm: Double? = null
)

/** Full workout parsed from natural-language input. */
@JsonClass(generateAdapter = true)
data class ParsedWorkoutResponse(
    @Json(name = "exercises") val exercises: List<ParsedWorkoutExercise> = emptyList()
)
