package com.anant.fitbuddy.data.remote.exercisedb

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExerciseDbExerciseDto(
    val exerciseId: String,
    val name: String,
    val gifUrl: String? = null,
    val targetMuscles: List<String> = emptyList(),
    val bodyParts: List<String> = emptyList(),
    val equipments: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ExerciseDbNamedDto(
    val name: String
)
