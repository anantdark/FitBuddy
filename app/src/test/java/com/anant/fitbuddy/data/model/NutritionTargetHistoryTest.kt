package com.anant.fitbuddy.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionTargetHistoryTest {

    private val baseline = NutritionTargetHistory.period(
        from = NutritionTargetHistory.EPOCH_START,
        kcal = 2500,
        proteinG = 150,
        carbsG = 250,
        fatsG = 70
    )

    @Test
    fun `forDate uses latest period on or before date`() {
        val history = listOf(
            baseline,
            NutritionTargetHistory.period("2026-03-01", 2800, 160, 260, 75)
        )
        assertEquals(2500, NutritionTargetHistory.forDate(history, "2026-02-28")!!.kcal)
        assertEquals(2800, NutritionTargetHistory.forDate(history, "2026-03-01")!!.kcal)
        assertEquals(2800, NutritionTargetHistory.forDate(history, "2026-09-01")!!.kcal)
    }

    @Test
    fun `recordChange seeds empty history then adds today`() {
        val updated = NutritionTargetHistory.recordChange(
            history = emptyList(),
            effectiveFromDate = "2026-03-01",
            kcal = 2800,
            proteinG = 160,
            carbsG = 260,
            fatsG = 75,
            fallbackCurrent = baseline
        )
        assertEquals(2, updated.size)
        assertEquals(2500, updated[0].kcal)
        assertEquals("1970-01-01", updated[0].from)
        assertEquals(2800, updated[1].kcal)
        assertEquals("2026-03-01", updated[1].from)
    }

    @Test
    fun `same-day reapply overwrites without duplicating`() {
        val first = NutritionTargetHistory.recordChange(
            history = listOf(baseline),
            effectiveFromDate = "2026-03-01",
            kcal = 2800,
            proteinG = 160,
            carbsG = 260,
            fatsG = 75,
            fallbackCurrent = baseline
        )
        val second = NutritionTargetHistory.recordChange(
            history = first,
            effectiveFromDate = "2026-03-01",
            kcal = 2900,
            proteinG = 165,
            carbsG = 270,
            fatsG = 80,
            fallbackCurrent = baseline
        )
        assertEquals(2, second.size)
        assertEquals(2900, second.last().kcal)
    }

    @Test
    fun `unchanged values are a no-op`() {
        val history = listOf(baseline)
        val same = NutritionTargetHistory.recordChange(
            history = history,
            effectiveFromDate = "2026-03-01",
            kcal = 2500,
            proteinG = 150,
            carbsG = 250,
            fatsG = 70,
            fallbackCurrent = baseline
        )
        assertEquals(history, same)
    }

    @Test
    fun `averageTargetCalories respects change points`() {
        val history = listOf(
            baseline,
            NutritionTargetHistory.period("2026-03-01", 2800, 160, 260, 75)
        )
        val avg = NutritionTargetHistory.averageTargetCalories(
            history = history,
            dates = listOf("2026-02-28", "2026-03-01", "2026-03-02"),
            fallback = baseline
        )
        // (2500 + 2800 + 2800) / 3
        assertEquals(2700, avg)
    }
}
