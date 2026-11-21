package com.anant.fitbuddy.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {

    @Test
    fun `resolve prefers stored unit then US region default`() {
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.resolve("IMPERIAL", "india"))
        assertEquals(UnitSystem.METRIC, UnitSystem.resolve("METRIC", "us"))
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.resolve("", "us"))
        assertEquals(UnitSystem.METRIC, UnitSystem.resolve("", "india"))
        assertEquals(UnitSystem.METRIC, UnitSystem.resolve("bogus", "europe"))
    }

    @Test
    fun `weight and height round-trip between metric and imperial`() {
        val kg = 70.0
        val lb = Units.kgToDisplay(kg, UnitSystem.IMPERIAL)
        assertEquals(kg, Units.displayToKg(lb, UnitSystem.IMPERIAL), 1e-9)
        assertEquals(kg, Units.kgToDisplay(kg, UnitSystem.METRIC), 0.0)

        val cm = 175.0
        val inches = Units.cmToDisplay(cm, UnitSystem.IMPERIAL)
        assertEquals(cm, Units.displayToCm(inches, UnitSystem.IMPERIAL), 1e-9)
    }

    @Test
    fun `trim drops trailing zero decimals`() {
        assertEquals("70", Units.trim(70.0))
        assertEquals("154.3", Units.trim(154.34))
        assertEquals("0", Units.trim(0.04))
    }

    @Test
    fun `volume formats metric ml and imperial fl oz`() {
        assertEquals("250 ml", Units.formatVolume(250, UnitSystem.METRIC))
        assertEquals("0 ml", Units.formatVolume(-5, UnitSystem.METRIC))
        assertEquals("8.5 fl oz", Units.formatVolume(Units.flOzToMl(8.5), UnitSystem.IMPERIAL))
    }
}
