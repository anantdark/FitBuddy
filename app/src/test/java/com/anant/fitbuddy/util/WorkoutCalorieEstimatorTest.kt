package com.anant.fitbuddy.util

import com.anant.fitbuddy.data.model.Equipment
import com.anant.fitbuddy.data.model.ExerciseDraft
import com.anant.fitbuddy.data.model.WorkoutDraft
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutCalorieEstimatorTest {

    @Test
    fun strengthSession_usesMet4AndUserDuration() {
        val draft = WorkoutDraft(
            name = "Push",
            durationMinutes = 60,
            exercises = listOf(
                ExerciseDraft("Bench Press", sets = 3, reps = 10, equipment = Equipment.BENCH)
            )
        )
        // 4.0 * 80kg * 1h = 320
        val result = WorkoutCalorieEstimator.estimate(draft, weightKg = 80.0)
        assertEquals(320, result.caloriesBurned)
        assertEquals(60, result.durationMinutes)
        assertEquals("Moderate strength session", result.intensityNote)
    }

    @Test
    fun cardioSession_usesMet7() {
        val draft = WorkoutDraft(
            name = "Run",
            durationMinutes = 30,
            exercises = listOf(
                ExerciseDraft(
                    "Running",
                    sets = 1,
                    reps = 1,
                    equipment = Equipment.CARDIO,
                    durationMinutes = 30
                )
            )
        )
        // 7.0 * 70kg * 0.5h = 245
        val result = WorkoutCalorieEstimator.estimate(draft, weightKg = 70.0)
        assertEquals(245, result.caloriesBurned)
        assertEquals("Cardio session", result.intensityNote)
    }

    @Test
    fun bodyweightOnly_usesMet5() {
        val draft = WorkoutDraft(
            name = "Calisthenics",
            durationMinutes = 40,
            exercises = listOf(
                ExerciseDraft("Push-Up", sets = 3, reps = 15, equipment = Equipment.BODYWEIGHT)
            )
        )
        // 5.0 * 60kg * (40/60) ≈ 200
        val result = WorkoutCalorieEstimator.estimate(draft, weightKg = 60.0)
        assertEquals(200, result.caloriesBurned)
        assertEquals("Bodyweight session", result.intensityNote)
    }

    @Test
    fun missingWeight_defaultsTo70kg() {
        val draft = WorkoutDraft(
            name = "Lift",
            durationMinutes = 60,
            exercises = listOf(
                ExerciseDraft("Squat", sets = 3, reps = 8, equipment = Equipment.BARBELL)
            )
        )
        // 4.0 * 70 * 1 = 280
        val result = WorkoutCalorieEstimator.estimate(draft, weightKg = 0.0)
        assertEquals(280, result.caloriesBurned)
    }
}
