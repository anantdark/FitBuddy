package com.anant.fitbuddy.data.database

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodDailySummariesTest {

    @Test
    fun `filledForDateRange inserts zero days newest first`() {
        val logged = listOf(
            FoodDailySummary("2026-07-15", 500, 20, 40, 10),
            FoodDailySummary("2026-07-13", 400, 15, 30, 8)
        )
        val filled = logged.filledForDateRange("2026-07-13", "2026-07-15")
        assertEquals(
            listOf("2026-07-15", "2026-07-14", "2026-07-13"),
            filled.map { it.dateString }
        )
        assertEquals(500, filled[0].totalCalories)
        assertEquals(0, filled[1].totalCalories)
        assertEquals(400, filled[2].totalCalories)
    }

    @Test
    fun `filledForDateRange works when nothing logged`() {
        val filled = emptyList<FoodDailySummary>().filledForDateRange("2026-07-13", "2026-07-14")
        assertEquals(2, filled.size)
        assertEquals(0, filled.sumOf { it.totalCalories })
    }
}
