package com.anant.fitbuddy.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodEntryDraftQuantityTest {

    private fun ingredient(qty: Double = 1.0, unitG: Int = 100) = IngredientDraft(
        name = "Oats",
        quantity = qty,
        unitWeightG = unitG,
        kcalPer100 = 380.0,
        proteinPer100 = 13.0,
        carbsPer100 = 67.0,
        fatsPer100 = 7.0
    )

    @Test
    fun singleIngredient_reviewQuantity_foldsServings() {
        val food = FoodEntryDraft(
            name = "Oats",
            servings = 2.0,
            ingredients = listOf(ingredient(qty = 1.5))
        )
        assertEquals(3.0, food.reviewQuantity, 0.0)
    }

    @Test
    fun singleIngredient_withReviewQuantity_updatesIngredientAndResetsServings() {
        val food = FoodEntryDraft(
            name = "Oats",
            servings = 2.0,
            ingredients = listOf(ingredient(qty = 1.0))
        ).withReviewQuantity(4.5)
        assertEquals(1.0, food.servings, 0.0)
        assertEquals(4.5, food.ingredients.single().quantity, 0.0)
        assertEquals(4.5, food.reviewQuantity, 0.0)
    }

    @Test
    fun multiIngredient_withReviewQuantity_updatesServingsOnly() {
        val milk = ingredient(qty = 1.0, unitG = 200).copy(name = "Milk")
        val food = FoodEntryDraft(
            name = "Oatmeal",
            servings = 1.0,
            ingredients = listOf(ingredient(), milk)
        ).withReviewQuantity(2.5)
        assertEquals(2.5, food.servings, 0.0)
        assertEquals(1.0, food.ingredients[0].quantity, 0.0)
        assertEquals(1.0, food.ingredients[1].quantity, 0.0)
        assertEquals(2.5, food.reviewQuantity, 0.0)
    }

    @Test
    fun asSingleFoodDraftOrNull_oneFood_returnsFoodDraft() {
        val meal = MealDraft(
            name = "Breakfast",
            timestamp = 1L,
            foods = listOf(
                FoodEntryDraft(
                    name = "Oatmeal",
                    servings = 2.0,
                    ingredients = listOf(ingredient(), ingredient().copy(name = "Milk"))
                )
            )
        )
        val food = meal.asSingleFoodDraftOrNull()!!
        assertEquals("Oatmeal", food.dishName)
        assertEquals(2, food.ingredients.size)
        assertEquals(2.0, food.quantity, 0.0)
    }

    @Test
    fun asSingleFoodDraftOrNull_twoSingleIngredientFoods_returnsNull() {
        val meal = MealDraft(
            name = "Snack",
            timestamp = 1L,
            foods = listOf(
                FoodEntryDraft(
                    name = "Banana",
                    servings = 1.0,
                    ingredients = listOf(ingredient().copy(name = "Banana"))
                ),
                FoodEntryDraft(
                    name = "Milk",
                    servings = 1.0,
                    ingredients = listOf(ingredient(unitG = 200).copy(name = "Milk"))
                )
            )
        )
        assertNull(meal.asSingleFoodDraftOrNull())
    }

    @Test
    fun asSingleFoodDraftOrNull_empty_returnsNull() {
        val meal = MealDraft(name = "Empty", timestamp = 1L, foods = emptyList())
        assertNull(meal.asSingleFoodDraftOrNull())
    }
}
