package com.anant.fitbuddy.data.pcsync

import com.anant.fitbuddy.data.backup.BackupData
import com.anant.fitbuddy.data.model.LoggedIngredient
import com.squareup.moshi.JsonClass

/**
 * Wire format shared with the desktop companion (`fitbuddy-desktop`). The phone stays the source
 * of truth: it uploads a full [BackupData] snapshot and pulls queued [PcOp]s created on the PC.
 */
@JsonClass(generateAdapter = true)
data class PcSyncRequest(
    val protocol: Int = PROTOCOL_VERSION,
    val sentAt: Long,
    val appVersion: String,
    /** Op ids the phone has applied; the PC drops them from its queue. */
    val ackedOps: List<String> = emptyList(),
    /** Omitted when unchanged since the last upload ([snapshotHash] lets the PC tell). */
    val snapshot: BackupData? = null,
    val snapshotHash: String? = null
)

@JsonClass(generateAdapter = true)
data class PcSyncResponse(
    val protocol: Int = PROTOCOL_VERSION,
    val ops: List<PcOp> = emptyList(),
    /** True when the PC has no snapshot matching the request's hash (e.g. after a reset). */
    val needSnapshot: Boolean = false
)

@JsonClass(generateAdapter = true)
data class PcOp(
    val id: String,
    /** One of [PcOpType]. Unknown types are acked and skipped so a newer PC never blocks the queue. */
    val type: String,
    val meal: PcMeal? = null,
    val exercise: PcExercise? = null,
    val measurement: PcMeasurement? = null,
    /** Phone-side row id for delete ops. */
    val targetId: Int? = null,
    /**
     * Timestamp of the row the PC saw; ids can be reused (SQLite rowid, backup restore), so a
     * delete only proceeds when the row at [targetId] still has this timestamp.
     */
    val targetTimestamp: Long? = null
)

object PcOpType {
    const val ADD_MEAL = "add_meal"
    const val ADD_EXERCISE = "add_exercise"
    const val ADD_MEASUREMENT = "add_measurement"
    const val DELETE_MEAL = "delete_meal"
    const val DELETE_EXERCISE = "delete_exercise"
    const val DELETE_MEASUREMENT = "delete_measurement"
}

@JsonClass(generateAdapter = true)
data class PcMeal(
    val dishName: String,
    val timestamp: Long,
    val foods: List<PcMealFood>
)

@JsonClass(generateAdapter = true)
data class PcMealFood(
    val name: String,
    val servings: Double = 1.0,
    /** Totals for this food (servings already applied), like [com.anant.fitbuddy.data.database.MealFood]. */
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatsG: Int,
    val ingredients: List<LoggedIngredient>? = null
)

@JsonClass(generateAdapter = true)
data class PcExercise(
    val activityName: String,
    val timestamp: Long,
    val caloriesBurned: Int,
    val durationMinutes: Int
)

@JsonClass(generateAdapter = true)
data class PcMeasurement(
    val timestamp: Long,
    val weightKg: Double,
    val bodyFatPct: Double? = null,
    val muscleMassKg: Double? = null,
    val bmr: Int? = null
)

const val PROTOCOL_VERSION = 1
