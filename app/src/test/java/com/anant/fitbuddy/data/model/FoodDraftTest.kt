package com.anant.fitbuddy.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodDraftTest {

    @Test
    fun `fromAbsolute single unit stores full weight as one unit`() {
        val egg = IngredientDraft.fromAbsolute(
            name = "Egg", weightG = 50, calories = 78, protein = 6.0, carbs = 1.0, fats = 5.0
        )
        assertEquals(1.0, egg.quantity, 0.0)
        assertEquals(50, egg.unitWeightG)
        assertEquals(50, egg.weightG)
        assertEquals(78, egg.calories)
        assertEquals(6.0, egg.protein, 0.0)
    }

    @Test
    fun `fromAbsolute with quantity splits weight evenly per unit`() {
        // 2 eggs, 100g / 156kcal total -> 1 egg = 50g / 78kcal
        val eggs = IngredientDraft.fromAbsolute(
            name = "Egg", weightG = 100, calories = 156, protein = 12.0, carbs = 2.0, fats = 10.0, quantity = 2.0
        )
        assertEquals(2.0, eggs.quantity, 0.0)
        assertEquals(50, eggs.unitWeightG)
        assertEquals(100, eggs.weightG)
        assertEquals(156, eggs.calories)
    }

    @Test
    fun `fromAbsolute supports decimal quantity`() {
        val half = IngredientDraft.fromAbsolute(
            name = "Banana", weightG = 60, calories = 45, protein = 0.5, carbs = 12.0, fats = 0.1,
            quantity = 1.5
        )
        assertEquals(1.5, half.quantity, 0.0)
        assertEquals(40, half.unitWeightG)
        assertEquals(60, half.weightG)
    }

    @Test
    fun `changing quantity scales weight and macros proportionally`() {
        val egg = IngredientDraft.fromAbsolute(
            name = "Egg", weightG = 50, calories = 78, protein = 6.0, carbs = 1.0, fats = 5.0
        )
        val threeEggs = egg.copy(quantity = 3.0)
        assertEquals(150, threeEggs.weightG)
        assertEquals(234, threeEggs.calories)
        assertEquals(18.0, threeEggs.protein, 0.0)
    }

    @Test
    fun `zero weight ingredient does not divide by zero`() {
        val ingredient = IngredientDraft.fromAbsolute(
            name = "Garnish", weightG = 0, calories = 0, protein = 0.0, carbs = 0.0, fats = 0.0
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
                IngredientDraft.fromAbsolute("Idli", 250, 325, 8.0, 65.0, 4.0),
                IngredientDraft.fromAbsolute("Sambar", 150, 110, 4.0, 15.0, 4.0)
            )
        )
        assertEquals(435, draft.totalCalories)
        assertEquals(12.0, draft.totalProtein, 0.0)
        assertEquals(80.0, draft.totalCarbs, 0.0)
        assertEquals(8.0, draft.totalFats, 0.0)
    }

    @Test
    fun `food draft quantity scales totals`() {
        val draft = FoodDraft(
            dishName = "Banana",
            timestamp = 0L,
            quantity = 2.0,
            ingredients = listOf(
                IngredientDraft.fromAbsolute("Banana", 120, 105, 1.3, 27.0, 0.4)
            )
        )
        assertEquals(210, draft.totalCalories)
        assertEquals(2.6, draft.totalProtein, 0.0)
        assertEquals(240, draft.totalWeightG)
    }
}
