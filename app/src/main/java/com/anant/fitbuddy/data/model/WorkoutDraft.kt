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
    val distanceKm: Double? = null,
    /**
     * Hold time per set in seconds for isometric / static work (plank, wall sit, etc.).
     * Null for cardio and rep-based strength.
     */
    val durationSeconds: Int? = null,
    /**
     * Explicit logging mode from classify / catalog override. When null, derived from name,
     * equipment, and whether [durationSeconds] was logged.
     */
    val loggingModeOverride: ExerciseLoggingMode? = null,
    /** Demo GIF from the exercise catalog when known; null for customs / offline seed. */
    val gifUrl: String? = null,
    /** ExerciseDB body-part labels; drives [LogIconTopic.fromWorkoutDraft]. */
    val bodyParts: List<String> = emptyList()
) {
    fun isCardio(): Boolean =
        resolvedLoggingMode() == ExerciseLoggingMode.CARDIO || equipment == Equipment.CARDIO

    /** Static holds / carries logged by time (sets × seconds), not reps. */
    fun isTimedHold(): Boolean {
        val mode = resolvedLoggingMode()
        return mode == ExerciseLoggingMode.TIMED_HOLD || mode == ExerciseLoggingMode.TIMED_CARRY
    }

    fun resolvedLoggingMode(): ExerciseLoggingMode {
        if (equipment == Equipment.CARDIO) return ExerciseLoggingMode.CARDIO
        loggingModeOverride?.let { return it }
        // Logged hold/carry keeps timed mode even if the custom name is ambiguous.
        if (durationSeconds != null && durationSeconds > 0) {
            return if (isTimedCarryExercise(name, equipment)) {
                ExerciseLoggingMode.TIMED_CARRY
            } else {
                ExerciseLoggingMode.TIMED_HOLD
            }
        }
        return exerciseLoggingMode(name, equipment)
    }
}

/** An in-progress workout session being built in the logging screen, before it's saved. */
@Immutable
data class WorkoutDraft(
    val name: String = DEFAULT_NAME,
    val durationMinutes: Int = DEFAULT_DURATION_MINUTES,
    val exercises: List<ExerciseDraft> = emptyList()
) {
    /** True when the user left the placeholder title (AI naming may replace it on save). */
    fun hasDefaultName(): Boolean = name.trim().equals(DEFAULT_NAME, ignoreCase = true)

    companion object {
        const val DEFAULT_NAME = "Workout"
        const val DEFAULT_DURATION_MINUTES = 45

        /**
         * Estimates total session time for calorie burn from each exercise's logging mode
         * (cardio minutes, hold/carry work+rest, or ~3 min per strength set).
         */
        fun estimateDurationMinutes(exercises: List<ExerciseDraft>): Int {
            if (exercises.isEmpty()) return DEFAULT_DURATION_MINUTES
            return exercises.sumOf { estimatedExerciseMinutes(it) }.coerceAtLeast(5)
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
