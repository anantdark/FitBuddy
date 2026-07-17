package com.anant.fitbuddy.data.model

/** A selectable AI model surfaced in Settings (id used on the wire, name shown to the user). */
data class ModelOption(
    val id: String,
    val displayName: String
)
