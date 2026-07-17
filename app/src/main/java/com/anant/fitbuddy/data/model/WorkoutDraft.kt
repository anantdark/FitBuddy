package com.anant.fitbuddy.data.model

import androidx.compose.runtime.Immutable

/** One exercise being added to a not-yet-saved [WorkoutDraft]; mirrors the persisted entity. */
@Immutable
data class ExerciseDraft(
    val name: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Double? = null,
    val equipment: String = Equipment.OTHER,
    /** Populated for cardio (run/jog/bike, etc.); null for rep-based strength work. */
    val durationMinutes: Int? = null,
    val distanceKm: Double? = null
) {
    fun isCardio(): Boolean = equipment == Equipment.CARDIO
}

/** An in-progress workout session being built in the logging screen, before it's saved. */
@Immutable
data class WorkoutDraft(
    val name: String = "Workout",
    val durationMinutes: Int = DEFAULT_DURATION_MINUTES,
    val exercises: List<ExerciseDraft> = emptyList()
) {
    companion object {
        const val DEFAULT_DURATION_MINUTES = 45

        /**
         * Estimates total session time for calorie burn: sums cardio durations plus ~3 min per
         * strength set (rest included). Rep counts alone don't encode elapsed time.
         */
        fun estimateDurationMinutes(exercises: List<ExerciseDraft>): Int {
            if (exercises.isEmpty()) return DEFAULT_DURATION_MINUTES
            val cardioMinutes = exercises.filter { it.isCardio() }.sumOf { it.durationMinutes ?: 0 }
            val strengthMinutes = exercises.filter { !it.isCardio() }.sumOf { it.sets * 3 }
            val total = cardioMinutes + strengthMinutes
            return total.coerceAtLeast(5)
        }
    }
}

/** Equipment tags used to group the common-exercise picker and label draft rows. */
object Equipment {
    const val DUMBBELL = "Dumbbell"
    const val BENCH = "Bench"
    const val BARBELL = "Barbell"
    const val BODYWEIGHT = "Bodyweight"
    const val MACHINE = "Machine"
    const val CARDIO = "Cardio"
    const val OTHER = "Other"
}
