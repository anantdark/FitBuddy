package com.anant.fitbuddy.util

import com.anant.fitbuddy.data.model.Equipment
import com.anant.fitbuddy.data.model.ExerciseDraft
import com.anant.fitbuddy.data.model.ExerciseLoggingMode
import com.anant.fitbuddy.data.model.LogIconTopic
import com.anant.fitbuddy.data.model.WorkoutCaloriesResponse
import com.anant.fitbuddy.data.model.WorkoutDraft
import com.anant.fitbuddy.data.model.estimatedExerciseMinutes
import kotlin.math.roundToInt

/**
 * Local MET-based calorie burn for structured workout sessions.
 *
 * Uses a **duration-weighted average MET** across exercises (cardio / carry / isometric hold /
 * bodyweight / strength), then `kcal = avgMET × weight_kg × hours`. Session duration prefers
 * the user-entered value; otherwise the per-exercise estimate (hold seconds included).
 */
object WorkoutCalorieEstimator {

    private const val DEFAULT_WEIGHT_KG = 70.0
    private const val MET_CARDIO = 7.0
    private const val MET_CARRY = 6.0
    private const val MET_BODYWEIGHT = 5.0
    private const val MET_STRENGTH = 4.0
    /** Planks, wall sits, L-sits, stretch holds, gymnastics statics. */
    private const val MET_ISOMETRIC = 3.5

    fun estimate(draft: WorkoutDraft, weightKg: Double): WorkoutCaloriesResponse {
        val weight = weightKg.takeIf { it > 0 } ?: DEFAULT_WEIGHT_KG
        val exercises = draft.exercises
        val perExMinutes = exercises.map { estimatedExerciseMinutes(it).coerceAtLeast(0) }
        val estimatedTotal = perExMinutes.sum().coerceAtLeast(5)
        val duration = draft.durationMinutes.takeIf { it > 0 }
            ?: estimatedTotal.takeIf { exercises.isNotEmpty() }
            ?: WorkoutDraft.DEFAULT_DURATION_MINUTES

        val (avgMet, note) = if (exercises.isEmpty()) {
            MET_STRENGTH to "Moderate strength session"
        } else {
            val metMinuteSum = exercises.zip(perExMinutes).sumOf { (ex, mins) ->
                metFor(ex) * mins.coerceAtLeast(1)
            }
            val weightMinutes = perExMinutes.sumOf { it.coerceAtLeast(1) }
            val avg = metMinuteSum / weightMinutes
            avg to intensityNote(exercises, perExMinutes)
        }

        val calories = (avgMet * weight * (duration / 60.0)).roundToInt().coerceAtLeast(1)
        return WorkoutCaloriesResponse(
            caloriesBurned = calories,
            durationMinutes = duration,
            intensityNote = note,
            iconTopic = LogIconTopic.fromWorkoutDraft(draft)
        )
    }

    internal fun metFor(exercise: ExerciseDraft): Double =
        when (exercise.resolvedLoggingMode()) {
            ExerciseLoggingMode.CARDIO -> MET_CARDIO
            ExerciseLoggingMode.TIMED_CARRY -> MET_CARRY
            ExerciseLoggingMode.TIMED_HOLD -> MET_ISOMETRIC
            ExerciseLoggingMode.REPS -> when (exercise.equipment) {
                Equipment.BODYWEIGHT -> MET_BODYWEIGHT
                else -> MET_STRENGTH
            }
        }

    private fun intensityNote(exercises: List<ExerciseDraft>, minutes: List<Int>): String {
        data class Bucket(val label: String, var mins: Int = 0)
        val cardio = Bucket("Cardio session")
        val carry = Bucket("Carry session")
        val hold = Bucket("Isometric / hold session")
        val bodyweight = Bucket("Bodyweight session")
        val strength = Bucket("Moderate strength session")
        exercises.zip(minutes).forEach { (ex, mins) ->
            val m = mins.coerceAtLeast(1)
            when (ex.resolvedLoggingMode()) {
                ExerciseLoggingMode.CARDIO -> cardio.mins += m
                ExerciseLoggingMode.TIMED_CARRY -> carry.mins += m
                ExerciseLoggingMode.TIMED_HOLD -> hold.mins += m
                ExerciseLoggingMode.REPS -> when (ex.equipment) {
                    Equipment.BODYWEIGHT -> bodyweight.mins += m
                    else -> strength.mins += m
                }
            }
        }
        val dominant = listOf(cardio, carry, hold, bodyweight, strength).maxBy { it.mins }
        val nonZero = listOf(cardio, carry, hold, bodyweight, strength).count { it.mins > 0 }
        return if (nonZero > 1) "Mixed session" else dominant.label
    }
}
