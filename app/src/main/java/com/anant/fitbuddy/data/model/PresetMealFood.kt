package com.anant.fitbuddy.data.model

import com.squareup.moshi.JsonClass

/** One food row stored inside a saved [com.anant.fitbuddy.data.database.MealPreset]. */
@JsonClass(generateAdapter = true)
data class PresetMealFood(
    val name: String,
    val servings: Double,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatsG: Int,
    val ingredients: List<LoggedIngredient>? = null,
    val savedFoodId: Int? = null,
    val barcode: String? = null
) {
    fun toFoodEntry(): FoodEntryDraft {
        val ings = ingredients
            ?.takeIf { it.isNotEmpty() }
            ?.map { it.toIngredientDraft() }
            ?: listOf(
                IngredientDraft.fromAbsolute(
                    name = name,
                    weightG = 250,
                    calories = calories,
                    protein = proteinG,
                    carbs = carbsG,
                    fats = fatsG
                )
            )
        return FoodEntryDraft(
            name = name,
            servings = servings,
            ingredients = ings,
            presetId = savedFoodId,
            barcode = barcode
        )
    }
}

fun FoodEntryDraft.toPresetMealFood(): PresetMealFood = PresetMealFood(
    name = name,
    servings = servings,
    calories = totalCalories,
    proteinG = totalProtein,
    carbsG = totalCarbs,
    fatsG = totalFats,
    ingredients = ingredients.map { LoggedIngredient.fromIngredientDraft(it) },
    savedFoodId = presetId,
    barcode = barcode
)
