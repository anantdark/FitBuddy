package com.anant.fitbuddy.data.backup

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

/**
 * Local index of parent rows already sealed into frozen cloud chunks, plus content hashes used to
 * detect edits (tip overrides) and absences (tombstones).
 */
@JsonClass(generateAdapter = true)
data class FrozenBackupIndex(
    val nextChunkIndex: Int = 1,
    val tipChunkId: String = "",
    val foodLogHashes: Map<String, String> = emptyMap(),
    val mealFoodHashes: Map<String, String> = emptyMap(),
    val exerciseLogHashes: Map<String, String> = emptyMap(),
    val measurementHashes: Map<String, String> = emptyMap(),
    val workoutSessionHashes: Map<String, String> = emptyMap(),
    val workoutExerciseHashes: Map<String, String> = emptyMap()
) {
    fun foodLogIds(): Set<Int> = foodLogHashes.keys.mapNotNull { it.toIntOrNull() }.toSet()
    fun mealFoodIds(): Set<Int> = mealFoodHashes.keys.mapNotNull { it.toIntOrNull() }.toSet()
    fun exerciseLogIds(): Set<Int> = exerciseLogHashes.keys.mapNotNull { it.toIntOrNull() }.toSet()
    fun measurementIds(): Set<Int> = measurementHashes.keys.mapNotNull { it.toIntOrNull() }.toSet()
    fun workoutSessionIds(): Set<Int> = workoutSessionHashes.keys.mapNotNull { it.toIntOrNull() }.toSet()
    fun workoutExerciseIds(): Set<Int> =
        workoutExerciseHashes.keys.mapNotNull { it.toIntOrNull() }.toSet()

    companion object {
        val EMPTY = FrozenBackupIndex()
    }
}

object FrozenBackupIndexJson {
    fun adapter(moshi: Moshi) = moshi.adapter(FrozenBackupIndex::class.java)

    fun encode(moshi: Moshi, index: FrozenBackupIndex): String = adapter(moshi).toJson(index)

    fun decode(moshi: Moshi, raw: String?): FrozenBackupIndex {
        if (raw.isNullOrBlank()) return FrozenBackupIndex.EMPTY
        return runCatching { adapter(moshi).fromJson(raw) }.getOrNull() ?: FrozenBackupIndex.EMPTY
    }
}
