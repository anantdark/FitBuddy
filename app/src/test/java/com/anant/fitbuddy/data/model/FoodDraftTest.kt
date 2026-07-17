package com.anant.fitbuddy.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodDraftTest {

    @Test
    fun `fromAbsolute single unit stores full weight as one unit`() {
        val egg = IngredientDraft.fromAbsolute(
            name = "Egg", weightG = 50, calories = 78, protein = 6, carbs = 1, fats = 5
        )
        assertEquals(1, egg.quantity)
        assertEquals(50, egg.unitWeightG)
        assertEquals(50, egg.weightG)
        assertEquals(78, egg.calories)
        assertEquals(6, egg.protein)
    }

    @Test
    fun `fromAbsolute with quantity splits weight evenly per unit`() {
        // 2 eggs, 100g / 156kcal total -> 1 egg = 50g / 78kcal
        val eggs = IngredientDraft.fromAbsolute(
            name = "Egg", weightG = 100, calories = 156, protein = 12, carbs = 2, fats = 10, quantity = 2
        )
        assertEquals(2, eggs.quantity)
        assertEquals(50, eggs.unitWeightG)
        assertEquals(100, eggs.weightG)
        assertEquals(156, eggs.calories)
    }

    @Test
    fun `changing quantity scales weight and macros proportionally`() {
        val egg = IngredientDraft.fromAbsolute(
            name = "Egg", weightG = 50, calories = 78, protein = 6, carbs = 1, fats = 5
        )
        val threeEggs = egg.copy(quantity = 3)
        assertEquals(150, threeEggs.weightG)
        assertEquals(234, threeEggs.calories)
        assertEquals(18, threeEggs.protein)
    }

    @Test
    fun `zero weight ingredient does not divide by zero`() {
        val ingredient = IngredientDraft.fromAbsolute(
            name = "Garnish", weightG = 0, calories = 0, protein = 0, carbs = 0, fats = 0
        )
        assertEquals(0, ingredient.weightG)
        assertEquals(0, ingredient.calories)
    }

    @Test
    fun `food draft totals sum all ingredients`() {
        val draft = FoodDraft(
            dishName = "Idli with Sambar",
            timestamp = 0L,
            ingredients = listOf(
                IngredientDraft.fromAbsolute("Idli", 250, 325, 8, 65, 4),
                IngredientDraft.fromAbsolute("Sambar", 150, 110, 4, 15, 4)
            )
        )
        assertEquals(435, draft.totalCalories)
        assertEquals(12, draft.totalProtein)
        assertEquals(80, draft.totalCarbs)
        assertEquals(8, draft.totalFats)
    }
}
