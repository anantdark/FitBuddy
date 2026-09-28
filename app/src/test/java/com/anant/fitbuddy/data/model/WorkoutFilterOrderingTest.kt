package com.anant.fitbuddy.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutFilterOrderingTest {

    @Test
    fun `displayBodyParts merges upper and lower arms and legs`() {
        val display = WorkoutFilterOrdering.displayBodyParts(
            listOf("chest", "upper arms", "lower arms", "upper legs", "lower legs", "back")
        )
        assertTrue(display.contains("Arms"))
        assertTrue(display.contains("Legs"))
        assertTrue(display.contains("Chest"))
        assertTrue(display.contains("Back"))
        assertFalse(display.any { it.equals("upper arms", ignoreCase = true) })
        assertFalse(display.any { it.equals("lower legs", ignoreCase = true) })
    }

    @Test
    fun `matchesBodyPart accepts either arm region for Arms chip`() {
        assertTrue(WorkoutFilterOrdering.matchesBodyPart(listOf("Upper arms"), "Arms"))
        assertTrue(WorkoutFilterOrdering.matchesBodyPart(listOf("lower arms"), "Arms"))
        assertFalse(WorkoutFilterOrdering.matchesBodyPart(listOf("Chest"), "Arms"))
    }

    @Test
    fun `orderLabels puts pinned first then defaults then leftovers`() {
        val available = listOf("Tire", "Dumbbell", "Body weight", "Band", "Hammer")
        val ordered = WorkoutFilterOrdering.orderLabels(
            available = available,
            defaultOrder = WorkoutFilterOrdering.DEFAULT_EQUIPMENTS,
            pinned = listOf("Tire"),
        )
        assertEquals(
            listOf("Tire", "Body weight", "Dumbbell", "Band", "Hammer"),
            ordered
        )
    }

    @Test
    fun `orderLabels body parts follow chest back legs cardio arms`() {
        val available = WorkoutFilterOrdering.displayBodyParts(
            listOf(
                "neck", "lower arms", "shoulders", "cardio", "upper arms",
                "chest", "lower legs", "back", "upper legs", "waist"
            )
        )
        val ordered = WorkoutFilterOrdering.orderLabels(
            available = available,
            defaultOrder = WorkoutFilterOrdering.DEFAULT_BODY_PARTS,
        )
        assertEquals(
            listOf("Chest", "Back", "Legs", "Cardio", "Arms", "Shoulders", "Waist", "Neck"),
            ordered
        )
    }

    @Test
    fun `promotePin prepends and dedupes`() {
        assertEquals(
            listOf("Band", "Dumbbell"),
            WorkoutFilterOrdering.promotePin(listOf("Dumbbell", "Band"), "Band")
        )
    }
}
