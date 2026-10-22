package com.anant.fitbuddy.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedHoldExerciseTest {

    @Test
    fun detectsStaticHoldsAndGymnastics() {
        val timed = listOf(
            "Plank",
            "Side Plank",
            "weighted front plank",
            "Bodyweight Incline Side Plank",
            "Wall Sit",
            "Hollow Hold",
            "Dead Hang",
            "Active Hang",
            "L-Sit",
            "L-Sit on Floor",
            "V-Sit",
            "V-Sit on Floor",
            "Handstand Hold",
            "Handstand",
            "Full Planche",
            "Frog Planche",
            "Straddle Planche",
            "Front Lever",
            "Back Lever",
            "Full Maltese",
            "Isometric Chest Squeeze",
            "Band Horizontal Pallof Press",
            "Side Bridge v. 2",
            "Rear Decline Bridge",
            "Hamstring Stretch",
            "Butterfly Yoga Pose",
            "Sphinx",
            "Upward Facing Dog",
            "Flag",
            "Human Flag",
            "Pull-Up Hold",
            "Superman Hold"
        )
        timed.forEach { name ->
            assertTrue("$name should be timed", isTimedHoldExercise(name))
        }
    }

    @Test
    fun detectsCarriesAsTimedWithCarryMode() {
        assertTrue(isTimedCarryExercise("Farmer's Walk"))
        assertTrue(isTimedCarryExercise("Farmers Walk"))
        assertTrue(isTimedCarryExercise("Dumbbell Single Arm Overhead Carry"))
        assertTrue(isTimedCarryExercise("Suitcase Carry"))
        assertEquals(
            ExerciseLoggingMode.TIMED_CARRY,
            exerciseLoggingMode("Farmer's Walk", Equipment.DUMBBELL)
        )
    }

    @Test
    fun excludesDynamicAndRepBasedVariants() {
        val reps = listOf(
            "Push-Up to Side Plank",
            "Front Plank with Twist",
            "Kneeling Plank Tap Shoulder",
            "Dumbbell Side Plank with Rear Fly",
            "Side Plank Hip Adduction",
            "Reverse Plank with Leg Lift",
            "Isometric Wipers",
            "Handstand Push-Up",
            "Full Planche Push-Up",
            "Front Lever Reps",
            "Hanging Leg Raise",
            "Kettlebell Hang Clean",
            "Lever Seated Row",
            "Lever Chest Press",
            "Hold Style Dumbbell Alternating Seated Bicep Curl on Exercise Ball",
            "Dumbbell Reverse Grip Row (Female) Hold",
            "Dumbbell Biceps Curl V Sit on Bosu Ball",
            "Weighted Stretch Lunge",
            "Glute Bridge",
            "Barbell Glute Bridge",
            "Bench Press",
            "Push-Up",
            "Running"
        )
        reps.forEach { name ->
            assertFalse("$name should stay rep-based", isTimedHoldExercise(name))
        }
    }

    @Test
    fun seedTimedEntriesUseHoldLogging() {
        val seedTimed = listOf(
            "Plank", "Side Plank", "Wall Sit", "Hollow Hold", "Dead Hang",
            "L-Sit", "V-Sit", "Handstand Hold", "Farmer's Walk"
        )
        COMMON_EXERCISES_SEED
            .filter { it.name in seedTimed }
            .forEach { ex ->
                assertTrue(ex.name, ex.usesTimedLogging)
            }
    }

    @Test
    fun estimateDuration_usesHoldTimeNotRepHeuristic() {
        val exercises = listOf(
            ExerciseDraft(
                name = "Weighted Front Plank",
                sets = 3,
                reps = 1,
                equipment = Equipment.OTHER,
                durationSeconds = 45,
                weightKg = 10.0
            )
        )
        // 3 × (45 hold + 45 rest) = 270s → 5 min
        assertEquals(5, WorkoutDraft.estimateDurationMinutes(exercises))
    }

    @Test
    fun estimateDuration_cardioPlusHold() {
        val exercises = listOf(
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
        // 20 + ceil(2*(60+45)/60) = 20 + 4 = 24
        assertEquals(24, WorkoutDraft.estimateDurationMinutes(exercises))
    }

    @Test
    fun estimateDuration_farmerWalkUsesCarryTime() {
        val exercises = listOf(
            ExerciseDraft(
                name = "Farmer's Walk",
                sets = 4,
                reps = 1,
                equipment = Equipment.DUMBBELL,
                durationSeconds = 45,
                weightKg = 24.0
            )
        )
        // 4 × (45+45) = 360s → 6 min
        assertEquals(6, WorkoutDraft.estimateDurationMinutes(exercises))
    }
}
