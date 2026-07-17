package com.anant.fitbuddy.data.model

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

/**
 * An editable meal awaiting user confirmation. Nutrients are stored as per-100g rates so that
 * changing an ingredient's weight rescales calories/macros proportionally and live. The effective
 * [weightG] is [quantity] × [unitWeightG], so discrete items (e.g. eggs) can be adjusted by count
 * (unit weight = grams of one item) instead of typing a total weight.
 */
@Immutable
data class IngredientDraft(
    val name: String,
    val quantity: Int,
    val unitWeightG: Int,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatsPer100: Double
) {
    /** Total grams of this ingredient (quantity × per-unit weight); drives macro scaling. */
    val weightG: Int get() = quantity * unitWeightG

    val calories: Int get() = scale(kcalPer100)
    val protein: Int get() = scale(proteinPer100)
    val carbs: Int get() = scale(carbsPer100)
    val fats: Int get() = scale(fatsPer100)

    private fun scale(per100: Double): Int = (per100 * weightG / 100.0).roundToInt()

    /** Recomputes per-100g rates so displayed totals match the given values at current [weightG]. */
    fun withTotals(calories: Int, protein: Int, carbs: Int, fats: Int): IngredientDraft {
        val w = weightG.coerceAtLeast(1)
        val factor = 100.0 / w
        return copy(
            kcalPer100 = calories * factor,
            proteinPer100 = protein * factor,
            carbsPer100 = carbs * factor,
            fatsPer100 = fats * factor
        )
    }

    companion object {
        /**
         * Build from absolute per-ingredient values (as returned by the AI) at a given total
         * [weightG]. Defaults to a single unit whose weight is the whole [weightG]; pass
         * [quantity] to split that total into that many equal units.
         */
        fun fromAbsolute(
            name: String,
            weightG: Int,
            calories: Int,
            protein: Int,
            carbs: Int,
            fats: Int,
            quantity: Int = 1
        ): IngredientDraft {
            val qty = quantity.coerceAtLeast(1)
            val unit = if (weightG > 0) (weightG.toDouble() / qty).roundToInt().coerceAtLeast(1) else 0
            val effectiveWeight = qty * unit
            val factor = if (effectiveWeight > 0) 100.0 / effectiveWeight else 0.0
            return IngredientDraft(
                name = name,
                quantity = qty,
                unitWeightG = unit,
                kcalPer100 = calories * factor,
                proteinPer100 = protein * factor,
                carbsPer100 = carbs * factor,
                fatsPer100 = fats * factor
            )
        }
    }
}

@Immutable
data class FoodDraft(
    val dishName: String,
    val timestamp: Long,
    val ingredients: List<IngredientDraft>
) {
    val totalCalories: Int get() = ingredients.sumOf { it.calories }
    val totalProtein: Int get() = ingredients.sumOf { it.protein }
    val totalCarbs: Int get() = ingredients.sumOf { it.carbs }
    val totalFats: Int get() = ingredients.sumOf { it.fats }
}
