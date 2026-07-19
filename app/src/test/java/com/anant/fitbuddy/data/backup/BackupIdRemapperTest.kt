package com.anant.fitbuddy.data.backup

import com.anant.fitbuddy.data.database.MealFood
import com.anant.fitbuddy.data.database.MealPreset
import com.anant.fitbuddy.data.model.PresetMealFood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupIdRemapperTest {

    @Test
    fun idMap_pairsOldIdsToNewInsertIds() {
        val map = BackupIdRemapper.idMap(listOf(10, 20, 30), listOf(1L, 2L, 3L))
        assertEquals(mapOf(10 to 1, 20 to 2, 30 to 3), map)
    }

    @Test
    fun remapMealFood_rewritesMealAndSavedFoodLinks() {
        val food = MealFood(
            id = 99,
            mealLogId = 5,
            name = "Dal",
            servings = 1.0,
            orderIndex = 0,
            calories = 200,
            proteinG = 12,
            carbsG = 30,
            fatsG = 4,
            presetId = 42
        )
        val remapped = BackupIdRemapper.remapMealFood(
            food = food,
            mealLogIdMap = mapOf(5 to 7),
            savedFoodIdMap = mapOf(42 to 3)
        )!!
        assertEquals(0, remapped.id)
        assertEquals(7, remapped.mealLogId)
        assertEquals(3, remapped.presetId)
    }

    @Test
    fun remapMealFood_dropsOrphansAndClearsMissingSavedFood() {
        val food = MealFood(
            id = 1,
            mealLogId = 99,
            name = "Orphan",
            servings = 1.0,
            calories = 100,
            proteinG = 1,
            carbsG = 1,
            fatsG = 1,
            presetId = 7
        )
        assertNull(
            BackupIdRemapper.remapMealFood(food, mealLogIdMap = emptyMap(), savedFoodIdMap = emptyMap())
        )
        val kept = BackupIdRemapper.remapMealFood(
            food = food.copy(mealLogId = 1),
            mealLogIdMap = mapOf(1 to 2),
            savedFoodIdMap = emptyMap()
        )!!
        assertNull(kept.presetId)
    }

    @Test
    fun remapMealPreset_rewritesNestedSavedFoodIds() {
        val preset = MealPreset(
            id = 8,
            name = "Lunch",
            calories = 500,
            proteinG = 30,
            carbsG = 50,
            fatsG = 15,
            createdAt = 1L,
            foods = listOf(
                PresetMealFood(
                    name = "Roti",
                    servings = 2.0,
                    calories = 200,
                    proteinG = 6,
                    carbsG = 40,
                    fatsG = 2,
                    savedFoodId = 11
                ),
                PresetMealFood(
                    name = "Paneer",
                    servings = 1.0,
                    calories = 300,
                    proteinG = 24,
                    carbsG = 10,
                    fatsG = 13,
                    savedFoodId = 22
                )
            )
        )
        val remapped = BackupIdRemapper.remapMealPreset(
            preset,
            savedFoodIdMap = mapOf(11 to 100, 22 to 200)
        )
        assertEquals(0, remapped.id)
        val foods = remapped.foods.orEmpty()
        assertEquals(100, foods[0].savedFoodId)
        assertEquals(200, foods[1].savedFoodId)
    }
}
