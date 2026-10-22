package com.anant.fitbuddy.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomExerciseClassifyTest {

    @Test
    fun offline_plank_isBodyweightHold() {
        val result = classifyCustomExerciseOffline("weighted plank")
        assertEquals("Weighted Plank", result.canonicalName)
        assertEquals(Equipment.OTHER, result.equipment)
        assertEquals("hold", result.loggingMode)
        assertTrue(
            CatalogExercise(
                name = result.canonicalName,
                equipmentTag = result.equipment,
                loggingModeOverride = parseLoggingMode(result.loggingMode)
            ).usesTimedLogging
        )
    }

    @Test
    fun offline_wallSit_isBodyweightHold() {
        val result = classifyCustomExerciseOffline("wall sit")
        assertEquals(Equipment.BODYWEIGHT, result.equipment)
        assertEquals("hold", result.loggingMode)
    }

    @Test
    fun offline_farmersWalk_isCarry() {
        val result = classifyCustomExerciseOffline("farmers walk")
        assertEquals(Equipment.DUMBBELL, result.equipment)
        assertEquals("carry", result.loggingMode)
    }

    @Test
    fun offline_easyJog_isCardio() {
        val result = classifyCustomExerciseOffline("easy jog")
        assertEquals(Equipment.CARDIO, result.equipment)
        assertEquals("cardio", result.loggingMode)
    }

    @Test
    fun offline_benchPress_isReps() {
        val result = classifyCustomExerciseOffline("db bench press")
        assertEquals(Equipment.DUMBBELL, result.equipment)
        assertEquals("reps", result.loggingMode)
    }

    @Test
    fun mergeCustoms_preservesLoggingMode() {
        val merged = mergeCatalogWithCustoms(
            catalog = emptyList(),
            customs = listOf(
                CustomCatalogEntry(
                    name = "Iso Core Crush",
                    equipment = Equipment.BODYWEIGHT,
                    loggingMode = ExerciseLoggingMode.TIMED_HOLD
                )
            )
        )
        assertEquals(1, merged.size)
        assertEquals(ExerciseLoggingMode.TIMED_HOLD, merged[0].resolvedLoggingMode())
        assertTrue(merged[0].usesTimedLogging)
    }

    @Test
    fun draft_keepsHoldModeFromOverrideEvenWithAmbiguousName() {
        val draft = ExerciseDraft(
            name = "Iso Core Crush",
            sets = 3,
            reps = 1,
            equipment = Equipment.BODYWEIGHT,
            durationSeconds = 40,
            loggingModeOverride = ExerciseLoggingMode.TIMED_HOLD
        )
        assertEquals(ExerciseLoggingMode.TIMED_HOLD, draft.resolvedLoggingMode())
        assertTrue(draft.isTimedHold())
    }
}
