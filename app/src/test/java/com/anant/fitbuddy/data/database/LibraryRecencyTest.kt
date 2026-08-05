package com.anant.fitbuddy.data.database

import com.anant.fitbuddy.data.remote.NetworkModule
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryRecencyTest {

    @Test
    fun `normalize keeps existing lastUsedAt`() {
        val food = SavedFood(
            id = 1,
            name = "Dal",
            calories = 200,
            proteinG = 10,
            carbsG = 20,
            fatsG = 5,
            createdAt = 50L,
            lastUsedAt = 9_999L
        )
        assertEquals(9_999L, LibraryRecency.normalizeSavedFoods(listOf(food)).single().lastUsedAt)
    }

    @Test
    fun `normalize fills lastUsedAt from createdAt when missing`() {
        val food = SavedFood(
            id = 1,
            name = "Oats",
            calories = 150,
            proteinG = 5,
            carbsG = 27,
            fatsG = 3,
            createdAt = 1_000L,
            lastUsedAt = 0L
        )
        assertEquals(1_000L, LibraryRecency.normalizeSavedFoods(listOf(food)).single().lastUsedAt)
    }

    @Test
    fun `moshi reads v12 backup json with sortOrder and without lastUsedAt`() {
        // Unknown sortOrder is ignored; food payload must still parse.
        val json = """
            {
              "id": 7,
              "name": "Oats",
              "calories": 150,
              "proteinG": 5,
              "carbsG": 27,
              "fatsG": 3,
              "createdAt": 1000,
              "barcode": null,
              "ingredients": null,
              "sortOrder": 2
            }
        """.trimIndent()
        val food = NetworkModule.moshi.adapter(SavedFood::class.java).fromJson(json)!!
        assertEquals("Oats", food.name)
        assertEquals(150, food.calories)
        assertEquals(0L, food.lastUsedAt)
        val normalized = LibraryRecency.normalizeSavedFoods(listOf(food)).single()
        assertEquals(1_000L, normalized.lastUsedAt)
        assertEquals("Oats", normalized.name)
    }

    @Test
    fun `moshi round-trips lastUsedAt for new backups`() {
        val food = SavedFood(
            id = 1,
            name = "Dal",
            calories = 200,
            proteinG = 10,
            carbsG = 20,
            fatsG = 5,
            createdAt = 50L,
            lastUsedAt = 9_999L
        )
        val adapter = NetworkModule.moshi.adapter(SavedFood::class.java)
        val parsed = adapter.fromJson(adapter.toJson(food))!!
        assertEquals(9_999L, parsed.lastUsedAt)
    }
}
