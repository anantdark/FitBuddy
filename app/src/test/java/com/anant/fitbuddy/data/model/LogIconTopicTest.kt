package com.anant.fitbuddy.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LogIconTopicTest {

    @Test
    fun `normalizeFood falls back to meal`() {
        assertEquals(LogIconTopic.DRINK, LogIconTopic.normalizeFood("Drink"))
        assertEquals(LogIconTopic.MEAL, LogIconTopic.normalizeFood(null))
        assertEquals(LogIconTopic.MEAL, LogIconTopic.normalizeFood("pizza"))
    }

    @Test
    fun `normalizeExercise falls back to strength`() {
        assertEquals(LogIconTopic.RUN, LogIconTopic.normalizeExercise("run"))
        assertEquals(LogIconTopic.CHEST, LogIconTopic.normalizeExercise("chest"))
        assertEquals(LogIconTopic.STRENGTH, LogIconTopic.normalizeExercise(null))
        assertEquals(LogIconTopic.STRENGTH, LogIconTopic.normalizeExercise("deadlift"))
    }

    @Test
    fun `topicFromBodyPart maps ExerciseDB labels`() {
        assertEquals(LogIconTopic.CHEST, LogIconTopic.topicFromBodyPart("Chest"))
        assertEquals(LogIconTopic.LEGS, LogIconTopic.topicFromBodyPart("upper legs"))
        assertEquals(LogIconTopic.ARMS, LogIconTopic.topicFromBodyPart("Lower Arms"))
        assertEquals(LogIconTopic.CORE, LogIconTopic.topicFromBodyPart("waist"))
        assertEquals(null, LogIconTopic.topicFromBodyPart("mystery"))
    }

    @Test
    fun `fromWorkoutDraft majority body part wins`() {
        val chestDay = WorkoutDraft(
            exercises = listOf(
                ExerciseDraft(
                    "Bench Press",
                    sets = 3,
                    reps = 8,
                    equipment = Equipment.BARBELL,
                    bodyParts = listOf("Chest")
                ),
                ExerciseDraft(
                    "Fly",
                    sets = 3,
                    reps = 12,
                    equipment = Equipment.DUMBBELL,
                    bodyParts = listOf("Chest")
                ),
                ExerciseDraft(
                    "Curl",
                    sets = 3,
                    reps = 10,
                    equipment = Equipment.DUMBBELL,
                    bodyParts = listOf("Upper Arms")
                )
            )
        )
        assertEquals(LogIconTopic.CHEST, LogIconTopic.fromWorkoutDraft(chestDay))
    }

    @Test
    fun `fromWorkoutDraft cardio without parts is cardio`() {
        val cardio = WorkoutDraft(
            exercises = listOf(
                ExerciseDraft(
                    "Run",
                    sets = 1,
                    reps = 1,
                    equipment = Equipment.CARDIO,
                    durationMinutes = 20
                ),
                ExerciseDraft(
                    "Bike",
                    sets = 1,
                    reps = 1,
                    equipment = Equipment.CARDIO,
                    durationMinutes = 15
                )
            )
        )
        assertEquals(LogIconTopic.CARDIO, LogIconTopic.fromWorkoutDraft(cardio))
    }

    @Test
    fun `fromWorkoutDraft tied body parts become strength`() {
        val mixed = WorkoutDraft(
            exercises = listOf(
                ExerciseDraft(
                    "Bench",
                    sets = 3,
                    reps = 8,
                    equipment = Equipment.BARBELL,
                    bodyParts = listOf("Chest")
                ),
                ExerciseDraft(
                    "Row",
                    sets = 3,
                    reps = 8,
                    equipment = Equipment.BARBELL,
                    bodyParts = listOf("Back")
                )
            )
        )
        assertEquals(LogIconTopic.STRENGTH, LogIconTopic.fromWorkoutDraft(mixed))
    }
}
