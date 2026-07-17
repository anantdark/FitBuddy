package com.anant.fitbuddy.data.model

import com.squareup.moshi.JsonClass

/** Persisted ingredient row for [com.anant.fitbuddy.data.database.FoodLog]. */
@JsonClass(generateAdapter = true)
data class LoggedIngredient(
    val name: String,
    val quantity: Int,
    val unitWeightG: Int,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatsPer100: Double
) {
    fun toIngredientDraft(): IngredientDraft = IngredientDraft(
        name = name,
        quantity = quantity,
        unitWeightG = unitWeightG,
        kcalPer100 = kcalPer100,
        proteinPer100 = proteinPer100,
        carbsPer100 = carbsPer100,
        fatsPer100 = fatsPer100
    )

    companion object {
        fun fromIngredientDraft(draft: IngredientDraft): LoggedIngredient = LoggedIngredient(
            name = draft.name,
            quantity = draft.quantity,
            unitWeightG = draft.unitWeightG,
            kcalPer100 = draft.kcalPer100,
            proteinPer100 = draft.proteinPer100,
            carbsPer100 = draft.carbsPer100,
            fatsPer100 = draft.fatsPer100
        )
    }
}
