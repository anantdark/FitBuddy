package com.anant.fitbuddy.data.backup

import com.anant.fitbuddy.data.database.BodyMeasurement
import com.anant.fitbuddy.data.database.ExerciseLog
import com.anant.fitbuddy.data.database.FoodLog
import com.anant.fitbuddy.data.database.MealFood
import com.anant.fitbuddy.data.database.WorkoutExercise
import com.anant.fitbuddy.data.database.WorkoutSession

/**
 * Builds the mutable tip [BackupData] from a full Room snapshot vs [FrozenBackupIndex], and
 * partitions for rollover (oldest parents first).
 */
object BackupTipBuilder {

    fun buildTip(full: BackupData, frozen: FrozenBackupIndex): BackupData {
        val foodFrozen = frozen.foodLogIds()
        val mealFrozen = frozen.mealFoodIds()
        val exerciseFrozen = frozen.exerciseLogIds()
        val measurementFrozen = frozen.measurementIds()
        val sessionFrozen = frozen.workoutSessionIds()
        val workoutExFrozen = frozen.workoutExerciseIds()

        val tipFoods = selectRows(full.foodLogs, foodFrozen, frozen.foodLogHashes) { it.id to hashFood(it) }
        val tipMeals = selectRows(full.mealFoods, mealFrozen, frozen.mealFoodHashes) { it.id to hashMealFood(it) }
        val tipExercises =
            selectRows(full.exerciseLogs, exerciseFrozen, frozen.exerciseLogHashes) { it.id to hashExercise(it) }
        val tipMeasurements =
            selectRows(full.measurements, measurementFrozen, frozen.measurementHashes) {
                it.id to hashMeasurement(it)
            }
        val tipSessions =
            selectRows(full.workoutSessions, sessionFrozen, frozen.workoutSessionHashes) {
                it.id to hashSession(it)
            }
        val tipWorkoutExercises =
            selectRows(full.workoutExercises, workoutExFrozen, frozen.workoutExerciseHashes) {
                it.id to hashWorkoutExercise(it)
            }

        val roomFoodIds = full.foodLogs.map { it.id }.toSet()
        val roomMealIds = full.mealFoods.map { it.id }.toSet()
        val roomExerciseIds = full.exerciseLogs.map { it.id }.toSet()
        val roomMeasurementIds = full.measurements.map { it.id }.toSet()
        val roomSessionIds = full.workoutSessions.map { it.id }.toSet()
        val roomWorkoutExIds = full.workoutExercises.map { it.id }.toSet()

        return full.copy(
            version = BackupData.CURRENT_VERSION,
            foodLogs = tipFoods,
            mealFoods = tipMeals,
            exerciseLogs = tipExercises,
            measurements = tipMeasurements,
            workoutSessions = tipSessions,
            workoutExercises = tipWorkoutExercises,
            // Presets / profile / settings always live on the tip.
            deletedFoodLogIds = (foodFrozen - roomFoodIds).sorted(),
            deletedMealFoodIds = (mealFrozen - roomMealIds).sorted(),
            deletedExerciseLogIds = (exerciseFrozen - roomExerciseIds).sorted(),
            deletedMeasurementIds = (measurementFrozen - roomMeasurementIds).sorted(),
            deletedWorkoutSessionIds = (sessionFrozen - roomSessionIds).sorted(),
            deletedWorkoutExerciseIds = (workoutExFrozen - roomWorkoutExIds).sorted(),
            deletedSavedFoodIds = emptyList(),
            deletedMealPresetIds = emptyList(),
            deletedExercisePresetIds = emptyList()
        )
    }

    /**
     * Splits [tip] into a frozen segment (oldest parents) and a remainder tip that should seal
     * under [maxSealedBytes] when passed through [sealSize].
     */
    suspend fun partitionForRollover(
        tip: BackupData,
        maxSealedBytes: Int,
        sealSize: suspend (BackupData) -> Int
    ): Pair<BackupData, BackupData> {
        val parents = tip.foodLogs.map { ParentRef("food", it.id, it.dateString) } +
            tip.exerciseLogs.map { ParentRef("exercise", it.id, it.dateString) } +
            tip.measurements.map { ParentRef("measurement", it.id, it.dateString) } +
            tip.workoutSessions.map { ParentRef("session", it.id, it.dateString) }

        val sorted = parents.sortedWith(compareBy({ it.dateString }, { it.kind }, { it.id }))
        if (sorted.isEmpty()) {
            error("Cloud backup tip exceeds size limit without movable log rows")
        }

        var lo = 1
        var hi = sorted.size
        var best = -1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            val freezeIds = sorted.take(mid).groupBy({ it.kind }, { it.id })
            val remainder = stripParents(tip, freezeIds)
            val remainderCore = remainder.copy(
                // Keep tip identity fields; size probe uses remainder only.
                profile = tip.profile,
                settings = tip.settings,
                savedFoods = tip.savedFoods,
                presets = tip.presets,
                mealPresets = tip.mealPresets,
                exercisePresets = tip.exercisePresets,
                deletedFoodLogIds = tip.deletedFoodLogIds,
                deletedMealFoodIds = tip.deletedMealFoodIds,
                deletedExerciseLogIds = tip.deletedExerciseLogIds,
                deletedMeasurementIds = tip.deletedMeasurementIds,
                deletedSavedFoodIds = tip.deletedSavedFoodIds,
                deletedMealPresetIds = tip.deletedMealPresetIds,
                deletedExercisePresetIds = tip.deletedExercisePresetIds,
                deletedWorkoutSessionIds = tip.deletedWorkoutSessionIds,
                deletedWorkoutExerciseIds = tip.deletedWorkoutExerciseIds
            )
            if (sealSize(remainderCore) <= maxSealedBytes) {
                best = mid
                hi = mid - 1
            } else {
                lo = mid + 1
            }
        }
        if (best < 0) {
            error("Cloud backup tip exceeds size limit even after freezing all tip log rows")
        }

        val freezeIds = sorted.take(best).groupBy({ it.kind }, { it.id })
        return splitAtFreezeIds(tip, freezeIds)
    }

    /**
     * Developer/testing helper: freeze every movable tip parent into a segment and leave the
     * remainder tip with profile/settings/presets only. Fails when the tip has no log rows.
     */
    fun forceRollover(tip: BackupData): Pair<BackupData, BackupData> {
        val parents = tip.foodLogs.map { ParentRef("food", it.id, it.dateString) } +
            tip.exerciseLogs.map { ParentRef("exercise", it.id, it.dateString) } +
            tip.measurements.map { ParentRef("measurement", it.id, it.dateString) } +
            tip.workoutSessions.map { ParentRef("session", it.id, it.dateString) }
        val sorted = parents.sortedWith(compareBy({ it.dateString }, { it.kind }, { it.id }))
        if (sorted.isEmpty()) {
            error("Cloud tip has no log rows to freeze into a new chunk")
        }
        val freezeIds = sorted.groupBy({ it.kind }, { it.id })
        return splitAtFreezeIds(tip, freezeIds)
    }

    private fun splitAtFreezeIds(
        tip: BackupData,
        freezeIds: Map<String, List<Int>>
    ): Pair<BackupData, BackupData> {
        val frozenSegment = extractParents(tip, freezeIds).copy(
            profile = null,
            settings = null,
            savedFoods = emptyList(),
            presets = emptyList(),
            mealPresets = emptyList(),
            exercisePresets = emptyList(),
            deletedFoodLogIds = emptyList(),
            deletedMealFoodIds = emptyList(),
            deletedExerciseLogIds = emptyList(),
            deletedMeasurementIds = emptyList(),
            deletedSavedFoodIds = emptyList(),
            deletedMealPresetIds = emptyList(),
            deletedExercisePresetIds = emptyList(),
            deletedWorkoutSessionIds = emptyList(),
            deletedWorkoutExerciseIds = emptyList()
        )
        val newTip = stripParents(tip, freezeIds).copy(
            profile = tip.profile,
            settings = tip.settings,
            savedFoods = tip.savedFoods,
            presets = tip.presets,
            mealPresets = tip.mealPresets,
            exercisePresets = tip.exercisePresets,
            deletedFoodLogIds = tip.deletedFoodLogIds,
            deletedMealFoodIds = tip.deletedMealFoodIds,
            deletedExerciseLogIds = tip.deletedExerciseLogIds,
            deletedMeasurementIds = tip.deletedMeasurementIds,
            deletedSavedFoodIds = tip.deletedSavedFoodIds,
            deletedMealPresetIds = tip.deletedMealPresetIds,
            deletedExercisePresetIds = tip.deletedExercisePresetIds,
            deletedWorkoutSessionIds = tip.deletedWorkoutSessionIds,
            deletedWorkoutExerciseIds = tip.deletedWorkoutExerciseIds
        )
        return frozenSegment to newTip
    }

    fun indexAfterFreezing(
        previous: FrozenBackupIndex,
        frozenSegment: BackupData,
        newTipChunkId: String,
        nextChunkIndex: Int
    ): FrozenBackupIndex {
        fun <T> mergeHashes(
            existing: Map<String, String>,
            rows: List<T>,
            hashOf: (T) -> Pair<Int, String>
        ): Map<String, String> {
            val out = existing.toMutableMap()
            for (row in rows) {
                val (id, hash) = hashOf(row)
                out[id.toString()] = hash
            }
            return out
        }
        return previous.copy(
            nextChunkIndex = nextChunkIndex,
            tipChunkId = newTipChunkId,
            foodLogHashes = mergeHashes(previous.foodLogHashes, frozenSegment.foodLogs, ::hashFoodPair),
            mealFoodHashes = mergeHashes(previous.mealFoodHashes, frozenSegment.mealFoods, ::hashMealPair),
            exerciseLogHashes =
                mergeHashes(previous.exerciseLogHashes, frozenSegment.exerciseLogs, ::hashExercisePair),
            measurementHashes =
                mergeHashes(previous.measurementHashes, frozenSegment.measurements, ::hashMeasurementPair),
            workoutSessionHashes =
                mergeHashes(previous.workoutSessionHashes, frozenSegment.workoutSessions, ::hashSessionPair),
            workoutExerciseHashes =
                mergeHashes(
                    previous.workoutExerciseHashes,
                    frozenSegment.workoutExercises,
                    ::hashWorkoutExercisePair
                )
        )
    }

    private data class ParentRef(val kind: String, val id: Int, val dateString: String)

    private fun <T> selectRows(
        rows: List<T>,
        frozenIds: Set<Int>,
        frozenHashes: Map<String, String>,
        idAndHash: (T) -> Pair<Int, String>
    ): List<T> = rows.filter { row ->
        val (id, hash) = idAndHash(row)
        id !in frozenIds || frozenHashes[id.toString()] != hash
    }

    private fun stripParents(tip: BackupData, freezeIds: Map<String, List<Int>>): BackupData {
        val food = freezeIds["food"]?.toSet().orEmpty()
        val exercise = freezeIds["exercise"]?.toSet().orEmpty()
        val measurement = freezeIds["measurement"]?.toSet().orEmpty()
        val session = freezeIds["session"]?.toSet().orEmpty()
        val keptFood = tip.foodLogs.filterNot { it.id in food }
        val keptFoodIds = keptFood.map { it.id }.toSet()
        val keptSessions = tip.workoutSessions.filterNot { it.id in session }
        val keptSessionIds = keptSessions.map { it.id }.toSet()
        return tip.copy(
            foodLogs = keptFood,
            mealFoods = tip.mealFoods.filter { it.mealLogId in keptFoodIds },
            exerciseLogs = tip.exerciseLogs.filterNot { it.id in exercise },
            measurements = tip.measurements.filterNot { it.id in measurement },
            workoutSessions = keptSessions,
            workoutExercises = tip.workoutExercises.filter { it.sessionId in keptSessionIds }
        )
    }

    private fun extractParents(tip: BackupData, freezeIds: Map<String, List<Int>>): BackupData {
        val food = freezeIds["food"]?.toSet().orEmpty()
        val exercise = freezeIds["exercise"]?.toSet().orEmpty()
        val measurement = freezeIds["measurement"]?.toSet().orEmpty()
        val session = freezeIds["session"]?.toSet().orEmpty()
        val keptFood = tip.foodLogs.filter { it.id in food }
        val keptFoodIds = keptFood.map { it.id }.toSet()
        val keptSessions = tip.workoutSessions.filter { it.id in session }
        val keptSessionIds = keptSessions.map { it.id }.toSet()
        return tip.copy(
            foodLogs = keptFood,
            mealFoods = tip.mealFoods.filter { it.mealLogId in keptFoodIds },
            exerciseLogs = tip.exerciseLogs.filter { it.id in exercise },
            measurements = tip.measurements.filter { it.id in measurement },
            workoutSessions = keptSessions,
            workoutExercises = tip.workoutExercises.filter { it.sessionId in keptSessionIds }
        )
    }

    private fun hashFood(row: FoodLog) = listOf(
        row.id, row.dishName, row.timestamp, row.dateString,
        row.calories, row.proteinG, row.carbsG, row.fatsG
    ).joinToString("|").let(BackupContentHasher::sha256Hex)

    private fun hashMealFood(row: MealFood) = listOf(
        row.id, row.mealLogId, row.name, row.servings, row.orderIndex,
        row.calories, row.proteinG, row.carbsG, row.fatsG, row.presetId, row.barcode
    ).joinToString("|").let(BackupContentHasher::sha256Hex)

    private fun hashExercise(row: ExerciseLog) = listOf(
        row.id, row.activityName, row.timestamp, row.dateString, row.caloriesBurned, row.durationMinutes
    ).joinToString("|").let(BackupContentHasher::sha256Hex)

    private fun hashMeasurement(row: BodyMeasurement) = listOf(
        row.id, row.timestamp, row.dateString, row.weightKg, row.bmi, row.bodyFatPct
    ).joinToString("|").let(BackupContentHasher::sha256Hex)

    private fun hashSession(row: WorkoutSession) = listOf(
        row.id, row.name, row.timestamp, row.dateString, row.caloriesBurned, row.exerciseLogId
    ).joinToString("|").let(BackupContentHasher::sha256Hex)

    private fun hashWorkoutExercise(row: WorkoutExercise) = listOf(
        row.id, row.sessionId, row.name, row.sets, row.reps, row.weightKg, row.orderIndex, row.equipment
    ).joinToString("|").let(BackupContentHasher::sha256Hex)

    private fun hashFoodPair(row: FoodLog) = row.id to hashFood(row)
    private fun hashMealPair(row: MealFood) = row.id to hashMealFood(row)
    private fun hashExercisePair(row: ExerciseLog) = row.id to hashExercise(row)
    private fun hashMeasurementPair(row: BodyMeasurement) = row.id to hashMeasurement(row)
    private fun hashSessionPair(row: WorkoutSession) = row.id to hashSession(row)
    private fun hashWorkoutExercisePair(row: WorkoutExercise) = row.id to hashWorkoutExercise(row)
}
