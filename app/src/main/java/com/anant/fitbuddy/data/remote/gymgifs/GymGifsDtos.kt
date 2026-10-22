package com.anant.fitbuddy.data.remote.gymgifs

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GymGifsExercisesFileDto(
    val count: Int = 0,
    val exercises: List<GymGifsExerciseDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GymGifsExerciseDto(
    val id: String,
    val slug: String = "",
    val name: String,
    val muscle: String = "",
    val bodyPart: String = "",
    val equipment: String = "",
    val category: String = "",
    val secondaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    /** Relative path under the repo root, e.g. `biceps/barbell-curl.gif`. */
    val file: String = "",
    val gifUrl: String? = null,
    val thumbUrl: String? = null
)
