package com.anant.fitbuddy.data.region

/** A regional staple dish/portion with mid-range macros, shared by prompts and the offline sim. */
data class RegionalDish(
    val name: String,
    val weightG: Int,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatsG: Int,
    /** Lowercase keywords that match this dish in offline sim (e.g. "roti", "chapati"). */
    val keywords: List<String> = emptyList()
)
