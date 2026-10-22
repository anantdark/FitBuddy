package com.anant.fitbuddy.data.model

/**
 * How an exercise should be logged and estimated for calorie burn.
 *
 * - [CARDIO]: minutes (+ optional distance)
 * - [TIMED_HOLD]: sets × hold seconds (plank, wall sit, L-sit, stretch, etc.)
 * - [TIMED_CARRY]: sets × seconds for loaded carries / farmer walks (higher MET)
 * - [REPS]: classic sets × reps (+ optional weight)
 */
enum class ExerciseLoggingMode {
    CARDIO,
    TIMED_HOLD,
    TIMED_CARRY,
    REPS
}

/** Persist / AI wire format for [ExerciseLoggingMode]. */
fun ExerciseLoggingMode.toStorageValue(): String = when (this) {
    ExerciseLoggingMode.CARDIO -> "cardio"
    ExerciseLoggingMode.TIMED_HOLD -> "hold"
    ExerciseLoggingMode.TIMED_CARRY -> "carry"
    ExerciseLoggingMode.REPS -> "reps"
}

fun parseLoggingMode(raw: String?): ExerciseLoggingMode? = when (raw?.trim()?.lowercase()) {
    "cardio" -> ExerciseLoggingMode.CARDIO
    "hold", "timed_hold", "isometric" -> ExerciseLoggingMode.TIMED_HOLD
    "carry", "timed_carry" -> ExerciseLoggingMode.TIMED_CARRY
    "reps", "strength", "rep" -> ExerciseLoggingMode.REPS
    else -> null
}

/**
 * Classifies an exercise by name (and equipment when known). Used by the workout UI,
 * session-duration estimate, AI parse post-processing, and MET calorie burn.
 *
 * Name matching is intentionally catalog-aware: ExerciseDB leverage-machine rows start with
 * "lever …" and must stay rep-based; gymnastic "front/back lever" stays timed.
 */
fun exerciseLoggingMode(
    name: String,
    equipment: String = Equipment.OTHER
): ExerciseLoggingMode {
    if (equipment == Equipment.CARDIO) return ExerciseLoggingMode.CARDIO
    val n = name.trim().lowercase()
    if (n.isEmpty()) return ExerciseLoggingMode.REPS
    if (isTimedCarryName(n)) return ExerciseLoggingMode.TIMED_CARRY
    if (isTimedHoldName(n)) return ExerciseLoggingMode.TIMED_HOLD
    return ExerciseLoggingMode.REPS
}

/**
 * Prefer an explicit override (custom preset / AI classify), else name+equipment heuristics.
 * Cardio equipment always wins so a mis-tagged hold override cannot break run logging.
 */
fun resolveLoggingMode(
    name: String,
    equipment: String,
    override: ExerciseLoggingMode? = null
): ExerciseLoggingMode {
    if (equipment == Equipment.CARDIO) return ExerciseLoggingMode.CARDIO
    if (override == ExerciseLoggingMode.CARDIO) {
        return ExerciseLoggingMode.CARDIO
    }
    return override ?: exerciseLoggingMode(name, equipment)
}

/** True when the UI should collect sets × seconds (holds and carries). */
fun isTimedHoldExercise(name: String, equipment: String = Equipment.OTHER): Boolean {
    val mode = exerciseLoggingMode(name, equipment)
    return mode == ExerciseLoggingMode.TIMED_HOLD || mode == ExerciseLoggingMode.TIMED_CARRY
}

fun isTimedCarryExercise(name: String, equipment: String = Equipment.OTHER): Boolean =
    exerciseLoggingMode(name, equipment) == ExerciseLoggingMode.TIMED_CARRY

/** Estimated working minutes contributed by one draft row (rest included for non-cardio). */
fun estimatedExerciseMinutes(exercise: ExerciseDraft): Int =
    when (exercise.resolvedLoggingMode()) {
        ExerciseLoggingMode.CARDIO -> (exercise.durationMinutes ?: 0).coerceAtLeast(0)
        ExerciseLoggingMode.TIMED_HOLD,
        ExerciseLoggingMode.TIMED_CARRY -> {
            val holdSec = (exercise.durationSeconds ?: 30).coerceAtLeast(1)
            val sets = exercise.sets.coerceAtLeast(1)
            (sets * (holdSec + HOLD_REST_SECONDS) + 59) / 60
        }
        ExerciseLoggingMode.REPS -> exercise.sets.coerceAtLeast(1) * STRENGTH_MINUTES_PER_SET
    }

internal const val HOLD_REST_SECONDS = 45
internal const val STRENGTH_MINUTES_PER_SET = 3

/**
 * Offline custom-exercise classify when AI is unavailable: Title Case name + equipment +
 * logging_mode from the same heuristics used for catalog holds/carries/cardio.
 */
fun classifyCustomExerciseOffline(rawName: String): CustomExerciseResponse {
    val name = rawName.trim().split("\\s+".toRegex()).joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    val lower = rawName.lowercase()
    val nameMode = resolveLoggingMode(name, Equipment.OTHER)
    val equipment = when {
        nameMode == ExerciseLoggingMode.CARDIO ||
            "treadmill" in lower || "bike" in lower || "rower" in lower ||
            "cardio" in lower || "elliptical" in lower ||
            Regex("""\b(run|jog|running|jogging)\b""").containsMatchIn(lower) ->
            Equipment.CARDIO
        nameMode == ExerciseLoggingMode.TIMED_CARRY ||
            "farmer" in lower || Regex("""\bcarry\b""").containsMatchIn(lower) ->
            when {
                "dumbbell" in lower || lower.startsWith("db ") -> Equipment.DUMBBELL
                else -> Equipment.DUMBBELL
            }
        nameMode == ExerciseLoggingMode.TIMED_HOLD -> when {
            "dumbbell" in lower || lower.startsWith("db ") -> Equipment.DUMBBELL
            "barbell" in lower || "weighted" in lower || "plate" in lower -> Equipment.OTHER
            else -> Equipment.BODYWEIGHT
        }
        "dumbbell" in lower || lower.startsWith("db ") || " db" in lower -> Equipment.DUMBBELL
        "barbell" in lower || lower.startsWith("bb ") -> Equipment.BARBELL
        "pull-up" in lower || "push-up" in lower || "pushup" in lower ||
            "pullup" in lower || "bodyweight" in lower -> Equipment.BODYWEIGHT
        "machine" in lower || "cable" in lower || "pulldown" in lower -> Equipment.MACHINE
        "bench" in lower || "press" in lower -> Equipment.BENCH
        else -> Equipment.OTHER
    }
    val loggingMode = resolveLoggingMode(name, equipment, nameMode)
    return CustomExerciseResponse(
        canonicalName = name,
        equipment = equipment,
        loggingMode = loggingMode.toStorageValue()
    )
}

// ── name heuristics ──────────────────────────────────────────────────────────

private fun isTimedCarryName(n: String): Boolean {
    if (DYNAMIC_EXCLUDE.any { it.containsMatchIn(n) }) return false
    return containsAny(
        n,
        "farmer", "farmers", "farmer's",
        "overhead carry", "suitcase carry", "farmer walk", "farmers walk",
        "yoke carry", "yoke walk", "sandbag carry"
    ) || (n.contains("carry") && !n.contains("support"))
}

private fun isTimedHoldName(n: String): Boolean {
    // Leverage-machine catalog rows ("lever seated row", …) are rep-based.
    if (n.contains("lever") && !containsAny(n, "front lever", "back lever")) return false
    if (containsAny(n, REPS_IN_NAME)) return false
    if (containsAny(n, STYLE_HOLD_EXCLUDE)) return false
    if (DYNAMIC_EXCLUDE.any { it.containsMatchIn(n) }) return false
    // Strength movements that only mention a hold posture in passing (e.g. curl on a v-sit ball).
    if (containsAny(n, STRENGTH_MOVEMENT) &&
        !containsAny(n, "isometric", "pallof", "plank", "planche", "stretch")
    ) {
        return false
    }
    // Dynamic mobility dressed as "stretch" (lunges, walks).
    if (n.contains("stretch") && containsAny(n, "lunge", "walk", "jump", "swing")) return false

    return HOLD_PATTERNS.any { pattern -> pattern.containsMatchIn(n) }
}

private fun containsAny(n: String, vararg needles: String): Boolean =
    needles.any { it in n }

private fun containsAny(n: String, needles: Collection<String>): Boolean =
    needles.any { it in n }

/** Word-boundary excludes so "butterfly" ≠ fly, "marching" can still match march, etc. */
private val DYNAMIC_EXCLUDE = listOf(
    Regex("""\bpush[\s-]?ups?\b"""),
    Regex("""\btwist\b"""),
    Regex("""\btaps?\b"""),
    Regex("""\bflys?\b|\bflies\b"""),
    Regex("""\badduction\b|\babduction\b"""),
    Regex("""\bleg[\s-]?lifts?\b"""),
    Regex("""\bknee[\s-]?tucks?\b"""),
    Regex("""\bwipers?\b"""),
    Regex("""\bsit[\s-]?ups?\b"""),
    Regex("""\bhang\s+clean\b|\bfrom\s+the\s+hang\b|\balternating\s+hang\b"""),
    Regex("""\bmountain\s+climber\b"""),
    Regex("""\bmarch\b""")
)

private val STYLE_HOLD_EXCLUDE = listOf(
    "hold style",
    ") hold",
    "grip row (female) hold",
    "grip row hold"
)

private val REPS_IN_NAME = listOf(" reps", "rep ", "repetition")

private val STRENGTH_MOVEMENT = listOf(
    "curl", "press", "row", "raise", "lunge", "squat", "deadlift",
    "clean", "snatch", "swing", "thruster", "extension", "kickback",
    "pulldown", "pull-down", "bench"
)

/**
 * Word-boundary-ish patterns for static holds, gymnastics holds, isometrics, stretches,
 * and yoga poses that are logged by time rather than reps.
 */
private val HOLD_PATTERNS: List<Regex> = listOf(
    Regex("""\bplank\b"""),
    Regex("""\bwall[\s-]?sit\b"""),
    Regex("""\bhollow(\s+body)?(\s+hold)?\b"""),
    Regex("""\b(dead|active)[\s-]?hang\b"""),
    Regex("""\bl[\s-]?sit\b"""),
    Regex("""\bv[\s-]?sit\b"""),
    Regex("""\bisometric\b"""),
    Regex("""\bstatic\s+hold\b"""),
    Regex("""\bglute\s+bridge\s+hold\b"""),
    Regex("""\bhorse\s+stance\b"""),
    Regex("""\bplanche\b"""),
    Regex("""\b(front|back)\s+lever\b"""),
    Regex("""\bhandstand\b"""),
    Regex("""\bmaltese\b"""),
    Regex("""\bpallof\b"""),
    Regex("""\bside\s+bridge\b"""),
    Regex("""\brear\s+decline\s+bridge\b"""),
    Regex("""\bhuman\s+flag\b|\bflag\b"""),
    Regex("""\bsphinx\b"""),
    Regex("""\bupward\s+facing\s+dog\b"""),
    Regex("""\bstretch\b"""),
    Regex("""\byoga\s+pose\b"""),
    Regex("""\bpose\b"""),
    Regex("""\bsuperman\s+hold\b|\bboat\s+pose\b|\bdead\s+bug\s+hold\b"""),
    Regex("""\bchin[\s-]?up\s+hold\b|\bpull[\s-]?up\s+hold\b|\bdip\s+hold\b"""),
    Regex("""\bsqueeze\b"""),
    Regex("""\bhold\b""")
)
