package com.anant.fitbuddy.util

import com.anant.fitbuddy.data.model.Equipment
import com.anant.fitbuddy.data.model.WorkoutCaloriesResponse
import com.anant.fitbuddy.data.model.WorkoutDraft
import kotlin.math.roundToInt

/**
 * Local MET-based calorie burn for structured workout sessions.
 * kcal = MET × weight_kg × hours (session-average METs; rest included for strength).
 */
object WorkoutCalorieEstimator {

    private const val DEFAULT_WEIGHT_KG = 70.0
    private const val MET_CARDIO = 7.0
    private const val MET_BODYWEIGHT = 5.0
    private const val MET_STRENGTH = 4.0

    fun estimate(draft: WorkoutDraft, weightKg: Double): WorkoutCaloriesResponse {
        val weight = weightKg.takeIf { it > 0 } ?: DEFAULT_WEIGHT_KG
        val equipmentSet = draft.exercises.map { it.equipment }.toSet()
        val (met, note) = when {
            Equipment.CARDIO in equipmentSet -> MET_CARDIO to "Cardio session"
            Equipment.BODYWEIGHT in equipmentSet && equipmentSet.size == 1 ->
                MET_BODYWEIGHT to "Bodyweight session"
            else -> MET_STRENGTH to "Moderate strength session"
        }
        val duration = draft.durationMinutes.takeIf { it > 0 }
            ?: WorkoutDraft.estimateDurationMinutes(draft.exercises)
                .takeIf { draft.exercises.isNotEmpty() }
            ?: WorkoutDraft.DEFAULT_DURATION_MINUTES
        val calories = (met * weight * (duration / 60.0)).roundToInt().coerceAtLeast(1)
        return WorkoutCaloriesResponse(
            caloriesBurned = calories,
            durationMinutes = duration,
            intensityNote = note
        )
    }
}
