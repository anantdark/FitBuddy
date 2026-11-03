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

/**
 * AI food payload. Prefer [mealName] + [foods]. Legacy single-dish fields ([dishName],
 * [macros], [ingredients]) are still accepted for older model replies / offline sim.
 */
@JsonClass(generateAdapter = true)
data class FoodAnalysis(
    @Json(name = "meal_name") val mealName: String? = null,
    @Json(name = "foods") val foods: List<FoodItemAnalysis>? = null,
    /** Legacy single-dish name; used when [foods] is absent. */
    @Json(name = "dish_name") val dishName: String? = null,
    @Json(name = "macros") val macros: Macros? = null,
    @Json(name = "ingredients") val ingredients: List<Ingredient>? = null,
    /** Coarse [LogIconTopic] id for the dashboard glyph. */
    @Json(name = "icon_topic") val iconTopic: String? = null
)

/** One food within a multi-food meal analysis. Macros live on [ingredients] (per base unit). */
@JsonClass(generateAdapter = true)
data class FoodItemAnalysis(
    @Json(name = "name") val name: String,
    /** Count of this food in the meal (e.g. 2 for "2 burgers"). Base-unit ingredients × this. */
    @Json(name = "servings") val servings: Double = 1.0,
    @Json(name = "ingredients") val ingredients: List<Ingredient>? = null
)

@JsonClass(generateAdapter = true)
data class Ingredient(
    @Json(name = "name") val name: String,
    /** Count of discrete units (e.g. 4 for "4 almonds"). Use 1 for bulk/continuous portions. */
    @Json(name = "quantity") val quantity: Double = 1.0,
    /** Total grams for this ingredient line (all units combined). */
    @Json(name = "weight_g") val weightG: Int,
    @Json(name = "calories") val calories: Int,
    @Json(name = "protein_g") val proteinG: Double,
    @Json(name = "carbs_g") val carbsG: Double,
    @Json(name = "fats_g") val fatsG: Double
)

@JsonClass(generateAdapter = true)
data class Macros(
    @Json(name = "calories") val calories: Int,
    @Json(name = "protein_g") val proteinG: Double,
    @Json(name = "carbs_g") val carbsG: Double,
    @Json(name = "fats_g") val fatsG: Double
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
    @Json(name = "duration_minutes") val durationMinutes: Int? = 30, // Fallback duration in minutes
    /** Coarse [LogIconTopic] id for the dashboard glyph. */
    @Json(name = "icon_topic") val iconTopic: String? = null
)
