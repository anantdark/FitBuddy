package com.anant.fitbuddy.util

import com.anant.fitbuddy.data.model.Equipment
import com.anant.fitbuddy.data.model.ExerciseDraft
import com.anant.fitbuddy.data.model.LogIconTopic
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
                ExerciseDraft(
                    "Bench Press",
                    sets = 3,
                    reps = 10,
                    equipment = Equipment.BENCH,
                    bodyParts = listOf("Chest")
                )
            )
        )
        // 4.0 * 80kg * 1h = 320
        val result = WorkoutCalorieEstimator.estimate(draft, weightKg = 80.0)
        assertEquals(320, result.caloriesBurned)
        assertEquals(60, result.durationMinutes)
        assertEquals("Moderate strength session", result.intensityNote)
        assertEquals(LogIconTopic.CHEST, result.iconTopic)
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
        assertEquals(LogIconTopic.CARDIO, result.iconTopic)
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

    @Test
    fun timedHoldSession_usesIsometricMetAndHoldDuration() {
        val draft = WorkoutDraft(
            name = "Core",
            durationMinutes = 0,
            exercises = listOf(
                ExerciseDraft(
                    name = "Weighted Front Plank",
                    sets = 3,
                    reps = 1,
                    equipment = Equipment.OTHER,
                    durationSeconds = 45,
                    weightKg = 10.0
                )
            )
        )
        // estimateDurationMinutes → 5; MET isometric 3.5 * 70 * (5/60) ≈ 20
        val result = WorkoutCalorieEstimator.estimate(draft, weightKg = 70.0)
        assertEquals(5, result.durationMinutes)
        assertEquals(20, result.caloriesBurned)
        assertEquals("Isometric / hold session", result.intensityNote)
    }

    @Test
    fun carrySession_usesHigherMetThanIsometric() {
        val draft = WorkoutDraft(
            name = "Carries",
            durationMinutes = 6,
            exercises = listOf(
                ExerciseDraft(
                    name = "Farmer's Walk",
                    sets = 4,
                    reps = 1,
                    equipment = Equipment.DUMBBELL,
                    durationSeconds = 45,
                    weightKg = 24.0
                )
            )
        )
        // MET carry 6.0 * 70 * (6/60) = 42
        val result = WorkoutCalorieEstimator.estimate(draft, weightKg = 70.0)
        assertEquals(42, result.caloriesBurned)
        assertEquals("Carry session", result.intensityNote)
    }

    @Test
    fun mixedSession_weightsMetByExerciseMinutes() {
        val draft = WorkoutDraft(
            name = "Mixed",
            durationMinutes = 0,
            exercises = listOf(
                ExerciseDraft(
                    name = "Running",
                    sets = 1,
                    reps = 1,
                    equipment = Equipment.CARDIO,
                    durationMinutes = 20
                ),
                ExerciseDraft(
                    name = "Plank",
                    sets = 2,
                    reps = 1,
                    equipment = Equipment.BODYWEIGHT,
                    durationSeconds = 60
                )
            )
        )
        // minutes 20 + 4 = 24; avg MET = (7*20 + 3.5*4)/24 = 154/24
        // kcal = 154/24 * 70 * 24/60 = 154*70/60 ≈ 180
        val result = WorkoutCalorieEstimator.estimate(draft, weightKg = 70.0)
        assertEquals(24, result.durationMinutes)
        assertEquals(180, result.caloriesBurned)
        assertEquals("Mixed session", result.intensityNote)
    }
}
