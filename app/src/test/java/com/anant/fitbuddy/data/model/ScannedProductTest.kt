package com.anant.fitbuddy.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScannedProductTest {

    @Test
    fun `toFoodEntry uses servingGrams when present`() {
        val entry = ScannedProduct(
            barcode = "8901030865422",
            name = "Sample bar",
            calories = 200,
            proteinG = 10.0,
            carbsG = 20.0,
            fatsG = 8.0,
            servingGrams = 40
        ).toFoodEntry(servings = 2.0)

        assertEquals("Sample bar", entry.name)
        assertEquals(2.0, entry.servings, 0.0)
        assertEquals("8901030865422", entry.barcode)
        assertEquals(1, entry.ingredients.size)
        assertEquals(40, entry.ingredients.single().weightG)
        assertEquals(200, entry.ingredients.single().calories)
    }

    @Test
    fun `toFoodEntry defaults weight to 100g when serving unknown`() {
        val entry = ScannedProduct(
            barcode = "0012345678905",
            name = "Unknown serving",
            calories = 150,
            proteinG = 5.0,
            carbsG = 15.0,
            fatsG = 5.0,
            servingGrams = null
        ).toFoodEntry()

        assertEquals(1.0, entry.servings, 0.0)
        assertEquals(100, entry.ingredients.single().weightG)
        assertNull(entry.presetId)
    }
}
