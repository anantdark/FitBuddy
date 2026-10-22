package com.anant.fitbuddy.data.model

import androidx.compose.runtime.Immutable

/**
 * One row in the workout exercise picker — either from the ExerciseDB catalog (jsDelivr) or a
 * user/AI custom preset (no gif / instructions).
 */
@Immutable
data class CatalogExercise(
    val name: String,
    /** FitBuddy [Equipment] tag used for cardio UI and burn estimation. */
    val equipmentTag: String,
    val exerciseId: String? = null,
    /** jsDelivr GIF URL when known; null for customs / offline seed. */
    val gifUrl: String? = null,
    val bodyParts: List<String> = emptyList(),
    val equipments: List<String> = emptyList(),
    val targetMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    /**
     * Explicit mode from custom classify / preset. Null → derive from [name] + [equipmentTag].
     */
    val loggingModeOverride: ExerciseLoggingMode? = null,
    /** True for a user-added preset (editable/deletable); false for the live ExerciseDB catalog. */
    val isCustom: Boolean = false,
) {
    val primaryEquipmentLabel: String
        get() = equipments.firstOrNull()?.replaceFirstChar { it.uppercase() }
            ?: equipmentTag

    val primaryBodyPartLabel: String?
        get() = bodyParts.firstOrNull()?.replaceFirstChar { it.uppercase() }

    fun resolvedLoggingMode(): ExerciseLoggingMode =
        resolveLoggingMode(name, equipmentTag, loggingModeOverride)

    /** Sets×seconds UI (holds, carries, stretch holds) vs sets×reps. */
    val usesTimedLogging: Boolean
        get() {
            val mode = resolvedLoggingMode()
            return mode == ExerciseLoggingMode.TIMED_HOLD ||
                mode == ExerciseLoggingMode.TIMED_CARRY
        }
}

/**
 * Maps ExerciseDB equipment / body-part labels onto FitBuddy [Equipment] tags so calorie
 * estimation and cardio sets/reps UI keep working.
 */
fun mapExerciseDbEquipment(
    equipments: List<String>,
    bodyParts: List<String> = emptyList()
): String {
    val eqs = equipments.map { it.trim().lowercase() }
    val parts = bodyParts.map { it.trim().lowercase() }
    if (parts.any { it == "cardio" }) return Equipment.CARDIO
    val cardioEq = setOf(
        "stationary bike",
        "elliptical machine",
        "skierg machine",
        "stepmill machine",
        "upper body ergometer"
    )
    if (eqs.any { it in cardioEq }) return Equipment.CARDIO
    if (eqs.any { it == "dumbbell" }) return Equipment.DUMBBELL
    if (eqs.any {
            it == "barbell" || it == "ez barbell" || it == "olympic barbell" || it == "trap bar"
        }
    ) {
        return Equipment.BARBELL
    }
    if (eqs.any { it == "body weight" }) return Equipment.BODYWEIGHT
    // ExerciseDB tags plate-loaded holds (e.g. weighted front plank) as "weighted"
    if (eqs.any { it == "weighted" }) return Equipment.OTHER
    val machineEq = setOf(
        "cable",
        "leverage machine",
        "smith machine",
        "sled machine",
        "assisted"
    )
    if (eqs.any { it in machineEq }) return Equipment.MACHINE
    return Equipment.OTHER
}

/** Title-cases ExerciseDB filter labels for chips (“upper arms” → “Upper arms”). */
fun titleCaseLabel(raw: String): String =
    raw.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

/**
 * A best-effort GIF match found for a custom exercise, offered to the user to accept or deny
 * before it's persisted on the preset. [source] is shown so the user knows where it came from.
 */
data class GifSuggestion(
    val exerciseName: String,
    val matchedName: String,
    val gifUrl: String,
    val catalogExerciseId: String?,
    val source: GifSuggestionSource,
    val instructions: List<String> = emptyList()
)

enum class GifSuggestionSource { EXERCISE_DB, GYM_GIFS }
