package com.anant.fitbuddy.data.backup

/**
 * Merges standalone chunk [BackupData] segments (oldest → newest). Later chunks win on the same
 * backup-native id; tombstone lists remove ids after the union.
 */
object BackupChunkMerger {

    fun merge(chunks: List<BackupData>): BackupData {
        require(chunks.isNotEmpty()) { "Cannot merge an empty chunk list" }
        var acc = empty()
        val deletedFood = linkedSetOf<Int>()
        val deletedMealFood = linkedSetOf<Int>()
        val deletedExercise = linkedSetOf<Int>()
        val deletedMeasurement = linkedSetOf<Int>()
        val deletedSavedFood = linkedSetOf<Int>()
        val deletedMealPreset = linkedSetOf<Int>()
        val deletedExercisePreset = linkedSetOf<Int>()
        val deletedSession = linkedSetOf<Int>()
        val deletedWorkoutExercise = linkedSetOf<Int>()

        for (chunk in chunks) {
            acc = union(acc, chunk)
            deletedFood += chunk.deletedFoodLogIds
            deletedMealFood += chunk.deletedMealFoodIds
            deletedExercise += chunk.deletedExerciseLogIds
            deletedMeasurement += chunk.deletedMeasurementIds
            deletedSavedFood += chunk.deletedSavedFoodIds
            deletedMealPreset += chunk.deletedMealPresetIds
            deletedExercisePreset += chunk.deletedExercisePresetIds
            deletedSession += chunk.deletedWorkoutSessionIds
            deletedWorkoutExercise += chunk.deletedWorkoutExerciseIds
        }

        val last = chunks.last()
        return acc.copy(
            version = BackupData.CURRENT_VERSION,
            exportedAt = last.exportedAt,
            profile = chunks.asReversed().firstNotNullOfOrNull { it.profile } ?: acc.profile,
            settings = chunks.asReversed().firstNotNullOfOrNull { it.settings } ?: acc.settings,
            foodLogs = acc.foodLogs.filterNot { it.id in deletedFood },
            mealFoods = acc.mealFoods.filterNot { it.id in deletedMealFood },
            exerciseLogs = acc.exerciseLogs.filterNot { it.id in deletedExercise },
            measurements = acc.measurements.filterNot { it.id in deletedMeasurement },
            savedFoods = acc.savedFoods.filterNot { it.id in deletedSavedFood },
            presets = acc.presets.filterNot { it.id in deletedSavedFood },
            mealPresets = acc.mealPresets.filterNot { it.id in deletedMealPreset },
            exercisePresets = acc.exercisePresets.filterNot { it.id in deletedExercisePreset },
            workoutSessions = acc.workoutSessions.filterNot { it.id in deletedSession },
            workoutExercises = acc.workoutExercises.filterNot { it.id in deletedWorkoutExercise },
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
    }

    private fun empty() = BackupData(exportedAt = 0L)

    private fun union(base: BackupData, next: BackupData): BackupData = base.copy(
        foodLogs = mergeById(base.foodLogs, next.foodLogs) { it.id },
        mealFoods = mergeById(base.mealFoods, next.mealFoods) { it.id },
        exerciseLogs = mergeById(base.exerciseLogs, next.exerciseLogs) { it.id },
        measurements = mergeById(base.measurements, next.measurements) { it.id },
        savedFoods = mergeById(
            if (base.savedFoods.isNotEmpty()) base.savedFoods else base.presets,
            if (next.savedFoods.isNotEmpty()) next.savedFoods else next.presets
        ) { it.id },
        presets = emptyList(),
        mealPresets = mergeById(base.mealPresets, next.mealPresets) { it.id },
        exercisePresets = mergeById(base.exercisePresets, next.exercisePresets) { it.id },
        workoutSessions = mergeById(base.workoutSessions, next.workoutSessions) { it.id },
        workoutExercises = mergeById(base.workoutExercises, next.workoutExercises) { it.id }
    )

    private fun <T> mergeById(older: List<T>, newer: List<T>, idOf: (T) -> Int): List<T> {
        if (older.isEmpty()) return newer
        if (newer.isEmpty()) return older
        val map = LinkedHashMap<Int, T>(older.size + newer.size)
        for (row in older) map[idOf(row)] = row
        for (row in newer) map[idOf(row)] = row
        return map.values.toList()
    }
}
