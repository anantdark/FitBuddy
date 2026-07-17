package com.anant.fitbuddy.data.model

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

/**
 * One food within a [MealDraft]. A food is made of [ingredients] (e.g. oats + milk); [servings]
 * scales the whole food's macros, like reps/sets scaling an exercise's contribution to a workout.
 */
@Immutable
data class FoodEntryDraft(
    val name: String,
    val servings: Double = 1.0,
    val ingredients: List<IngredientDraft>,
    val presetId: Int? = null,
    val barcode: String? = null
) {
    val baseCalories: Int get() = ingredients.sumOf { it.calories }
    val baseProtein: Int get() = ingredients.sumOf { it.protein }
    val baseCarbs: Int get() = ingredients.sumOf { it.carbs }
    val baseFats: Int get() = ingredients.sumOf { it.fats }
    val baseWeightG: Int get() = ingredients.sumOf { it.weightG }

    val totalCalories: Int get() = (baseCalories * servings).roundToInt()
    val totalProtein: Int get() = (baseProtein * servings).roundToInt()
    val totalCarbs: Int get() = (baseCarbs * servings).roundToInt()
    val totalFats: Int get() = (baseFats * servings).roundToInt()
}

/** A logged (or in-progress) meal containing one or more [FoodEntryDraft] rows. */
@Immutable
data class MealDraft(
    val name: String,
    val timestamp: Long,
    val foods: List<FoodEntryDraft>
) {
    val totalCalories: Int get() = foods.sumOf { it.totalCalories }
    val totalProtein: Int get() = foods.sumOf { it.totalProtein }
    val totalCarbs: Int get() = foods.sumOf { it.totalCarbs }
    val totalFats: Int get() = foods.sumOf { it.totalFats }

    val foodCount: Int get() = foods.size
}

/** Deep copy so builder list clears cannot mutate an in-flight review draft. */
fun MealDraft.snapshot(): MealDraft = copy(
    foods = foods.map { food ->
        food.copy(ingredients = food.ingredients.toList())
    }
)

fun FoodEntryDraft.snapshot(): FoodEntryDraft = copy(ingredients = ingredients.toList())

fun FoodDraft.toFoodEntry(servings: Double = 1.0): FoodEntryDraft = FoodEntryDraft(
    name = dishName,
    servings = servings,
    ingredients = ingredients.toList()
)

/** Wraps a single reviewed food as a one-food meal (e.g. after AI photo analysis). */
fun FoodDraft.toSingleFoodMeal(): MealDraft = MealDraft(
    name = dishName,
    timestamp = timestamp,
    foods = listOf(toFoodEntry())
)

fun FoodDraft.toMealBuilderItems(): List<FoodEntryDraft> = listOf(toFoodEntry())
