package com.anant.fitbuddy.util

import com.anant.fitbuddy.data.database.WaterDrinkKind
import com.anant.fitbuddy.data.database.WaterLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaterFormatTest {

    @Test
    fun `formatLiters uses compact metric labels`() {
        assertEquals("2 L", WaterFormat.formatLiters(2000))
        assertEquals("1.15 L", WaterFormat.formatLiters(1150))
        assertEquals("0.25 L", WaterFormat.formatLiters(250))
        assertEquals("0 L", WaterFormat.formatLiters(-10))
        assertEquals("8.5 fl oz", WaterFormat.formatLiters(Units.flOzToMl(8.5), UnitSystem.IMPERIAL))
    }

    @Test
    fun `progress clamps to 0 through 1`() {
        assertEquals(0f, WaterFormat.progress(100, 0), 0f)
        assertEquals(0.5f, WaterFormat.progress(500, 1000), 0f)
        assertEquals(1f, WaterFormat.progress(2000, 1000), 0f)
    }

    @Test
    fun `WaterFillLayers scales shares when over goal`() {
        val layers = WaterFillLayers.fromLogs(
            logs = listOf(
                WaterLog(
                    amountMl = 800,
                    drinkKind = WaterDrinkKind.WATER.name,
                    timestamp = 0L,
                    dateString = "2026-11-21"
                ),
                WaterLog(
                    amountMl = 400,
                    drinkKind = WaterDrinkKind.COFFEE.name,
                    timestamp = 0L,
                    dateString = "2026-11-21"
                ),
                WaterLog(
                    amountMl = 400,
                    drinkKind = WaterDrinkKind.TEA.name,
                    timestamp = 0L,
                    dateString = "2026-11-21"
                ),
            ),
            goalMl = 1000
        )
        assertEquals(1600, layers.totalMl)
        assertEquals(1f, layers.totalShare, 1e-5f)
        assertTrue(layers.waterShare > layers.coffeeShare)
        assertEquals(layers.coffeeShare, layers.teaShare, 1e-5f)
    }
}
