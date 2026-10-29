package com.anant.fitbuddy.data.model

/**
 * Coarse icon topics the AI (or workout estimator) assigns to a food/workout.
 * UI maps these IDs to Material icons — do not keyword-match dish names in the UI.
 */
object LogIconTopic {

    // Food
    const val DRINK = "drink"
    const val DESSERT = "dessert"
    const val BAKERY = "bakery"
    const val BOWL = "bowl"
    const val PROTEIN = "protein"
    const val PRODUCE = "produce"
    const val MEAL = "meal"

    // Exercise — activity
    const val RUN = "run"
    const val WALK = "walk"
    const val CYCLE = "cycle"
    const val SWIM = "swim"
    const val MIND_BODY = "mind_body"
    const val SPORT = "sport"
    const val CARDIO = "cardio"

    // Exercise — body part (gym sessions)
    const val CHEST = "chest"
    const val BACK = "back"
    const val LEGS = "legs"
    const val ARMS = "arms"
    const val SHOULDERS = "shoulders"
    const val CORE = "core"

    /** Mixed / unknown strength work. */
    const val STRENGTH = "strength"

    val FOOD_TOPICS: Set<String> = setOf(
        DRINK, DESSERT, BAKERY, BOWL, PROTEIN, PRODUCE, MEAL
    )

    val EXERCISE_TOPICS: Set<String> = setOf(
        RUN, WALK, CYCLE, SWIM, MIND_BODY, SPORT, CARDIO,
        CHEST, BACK, LEGS, ARMS, SHOULDERS, CORE, STRENGTH
    )

    fun normalizeFood(raw: String?): String =
        raw?.trim()?.lowercase()?.takeIf { it in FOOD_TOPICS } ?: MEAL

    fun normalizeExercise(raw: String?): String =
        raw?.trim()?.lowercase()?.takeIf { it in EXERCISE_TOPICS } ?: STRENGTH

    /**
     * Maps an ExerciseDB / filter body-part label onto an icon topic.
     * Returns null when the label is blank or unrecognized.
     */
    fun topicFromBodyPart(raw: String): String? {
        val part = raw.trim().lowercase()
        if (part.isEmpty()) return null
        return when (part) {
            "chest" -> CHEST
            "back" -> BACK
            "shoulders" -> SHOULDERS
            "waist", "core" -> CORE
            "neck" -> SHOULDERS
            "cardio" -> CARDIO
            "legs", "upper legs", "lower legs" -> LEGS
            "arms", "upper arms", "lower arms" -> ARMS
            else -> null
        }
    }

    /**
     * Picks an icon topic for a structured gym session from exercise body parts
     * (majority vote). Cardio-heavy sessions → [CARDIO]. Ties / unknown → [STRENGTH].
     */
    fun fromWorkoutDraft(draft: WorkoutDraft): String {
        if (draft.exercises.isEmpty()) return STRENGTH

        val topics = draft.exercises.map { topicForExercise(it) }
        val counts = topics.groupingBy { it }.eachCount()
        val max = counts.values.maxOrNull() ?: return STRENGTH
        val leaders = counts.filterValues { it == max }.keys
        return if (leaders.size == 1) leaders.first() else STRENGTH
    }

    private fun topicForExercise(exercise: ExerciseDraft): String {
        val fromParts = exercise.bodyParts
            .asSequence()
            .mapNotNull { topicFromBodyPart(it) }
            .firstOrNull()
        if (fromParts != null) return fromParts
        return if (exercise.isCardio()) CARDIO else STRENGTH
    }
}
