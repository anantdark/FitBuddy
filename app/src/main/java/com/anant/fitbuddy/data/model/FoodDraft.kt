package com.anant.fitbuddy.data.model

import androidx.compose.runtime.Immutable
import com.anant.fitbuddy.util.roundMacro
import com.anant.fitbuddy.util.roundQty
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
    val quantity: Double,
    val unitWeightG: Int,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatsPer100: Double
) {
    /** Total grams of this ingredient (quantity × per-unit weight); drives macro scaling. */
    val weightG: Int get() = (quantity * unitWeightG).roundToInt()

    val calories: Int get() = scale(kcalPer100)
    val protein: Double get() = scaleMacro(proteinPer100)
    val carbs: Double get() = scaleMacro(carbsPer100)
    val fats: Double get() = scaleMacro(fatsPer100)

    private fun scale(per100: Double): Int = (per100 * weightG / 100.0).roundToInt()

    private fun scaleMacro(per100: Double): Double = (per100 * weightG / 100.0).roundMacro()

    /** Recomputes per-100g rates so displayed totals match the given values at current [weightG]. */
    fun withTotals(calories: Int, protein: Double, carbs: Double, fats: Double): IngredientDraft {
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
            protein: Double,
            carbs: Double,
            fats: Double,
            quantity: Double = 1.0
        ): IngredientDraft {
            val qty = quantity.roundQty().coerceAtLeast(0.1)
            val unit = if (weightG > 0) (weightG.toDouble() / qty).roundToInt().coerceAtLeast(1) else 0
            val effectiveWeight = (qty * unit).roundToInt()
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
    val ingredients: List<IngredientDraft>,
    /** Food-level count (e.g. 2 bananas). Ingredient macros are for one unit; totals × this. */
    val quantity: Double = 1.0,
    /** AI [LogIconTopic] id; carried through review → save. */
    val iconTopic: String? = null
) {
    private val qty: Double get() = quantity.roundQty().coerceAtLeast(0.0)

    val baseCalories: Int get() = ingredients.sumOf { it.calories }
    val baseProtein: Double get() = ingredients.sumOf { it.protein }.roundMacro()
    val baseCarbs: Double get() = ingredients.sumOf { it.carbs }.roundMacro()
    val baseFats: Double get() = ingredients.sumOf { it.fats }.roundMacro()
    val baseWeightG: Int get() = ingredients.sumOf { it.weightG }

    val totalCalories: Int get() = (baseCalories * qty).roundToInt()
    val totalProtein: Double get() = (baseProtein * qty).roundMacro()
    val totalCarbs: Double get() = (baseCarbs * qty).roundMacro()
    val totalFats: Double get() = (baseFats * qty).roundMacro()
    val totalWeightG: Int get() = (baseWeightG * qty).roundToInt()
}
