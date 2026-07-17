package com.anant.fitbuddy.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FitnessTrackerResponse(
    @Json(name = "status") val status: String, // "SUCCESS", "CLARIFICATION_REQUIRED", "EXERCISE_LOGGED"
    @Json(name = "clarification_message") val clarificationMessage: String?,
    @Json(name = "food_analysis") val foodAnalysis: FoodAnalysis?,
    @Json(name = "exercise_analysis") val exerciseAnalysis: ExerciseAnalysis?
)

@JsonClass(generateAdapter = true)
data class FoodAnalysis(
    @Json(name = "dish_name") val dishName: String,
    @Json(name = "macros") val macros: Macros,
    @Json(name = "ingredients") val ingredients: List<Ingredient>? = null
)

@JsonClass(generateAdapter = true)
data class Ingredient(
    @Json(name = "name") val name: String,
    /** Count of discrete units (e.g. 4 for "4 almonds"). Use 1 for bulk/continuous portions. */
    @Json(name = "quantity") val quantity: Int = 1,
    /** Total grams for this ingredient line (all units combined). */
    @Json(name = "weight_g") val weightG: Int,
    @Json(name = "calories") val calories: Int,
    @Json(name = "protein_g") val proteinG: Int,
    @Json(name = "carbs_g") val carbsG: Int,
    @Json(name = "fats_g") val fatsG: Int
)

@JsonClass(generateAdapter = true)
data class Macros(
    @Json(name = "calories") val calories: Int,
    @Json(name = "protein_g") val proteinG: Int,
    @Json(name = "carbs_g") val carbsG: Int,
    @Json(name = "fats_g") val fatsG: Int
)

/**
 * Fields nullable: some models return "exercise_analysis": {"activity_detected": null, ...}
 * instead of null-ing the whole object when the input isn't exercise, which a strict non-null
 * schema would reject.
 */
@JsonClass(generateAdapter = true)
data class ExerciseAnalysis(
    @Json(name = "activity_detected") val activityDetected: String?,
    @Json(name = "calories_burned") val caloriesBurned: Int?,
    @Json(name = "duration_minutes") val durationMinutes: Int? = 30 // Fallback duration in minutes
)
