package com.anant.fitbuddy.data.model

import androidx.compose.runtime.Immutable
import com.anant.fitbuddy.data.database.MealPreset
import com.anant.fitbuddy.data.database.SavedFood

/** Nutrition for one serving of a packaged product (from barcode lookup or manual entry). */
@Immutable
data class ScannedProduct(
    val barcode: String,
    val name: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatsG: Int,
    val servingGrams: Int? = null
)

fun SavedFood.toFoodEntry(servings: Double = 1.0): FoodEntryDraft {
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
        presetId = id,
        barcode = barcode
    )
}

fun MealPreset.toMealDraft(): MealDraft {
    val entries = foods?.map { it.toFoodEntry() }.orEmpty()
    return MealDraft(
        name = name,
        timestamp = System.currentTimeMillis(),
        foods = entries
    )
}

fun ScannedProduct.toFoodEntry(servings: Double = 1.0): FoodEntryDraft {
    val weight = servingGrams ?: 100
    return FoodEntryDraft(
        name = name,
        servings = servings,
        ingredients = listOf(
            IngredientDraft.fromAbsolute(
                name = name,
                weightG = weight,
                calories = calories,
                protein = proteinG,
                carbs = carbsG,
                fats = fatsG
            )
        ),
        barcode = barcode
    )
}

fun List<FoodEntryDraft>.toMealDraft(mealName: String): MealDraft = MealDraft(
    name = mealName,
    timestamp = System.currentTimeMillis(),
    foods = map { it.snapshot() }
)

fun FoodEntryDraft.toFoodDraft(): FoodDraft = FoodDraft(
    dishName = name,
    timestamp = System.currentTimeMillis(),
    ingredients = ingredients.toList()
)
