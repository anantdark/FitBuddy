package com.anant.fitbuddy.data.model

import kotlin.math.roundToInt

data class ActivityLevelDefinition(
    val value: String,
    val label: String,
    val factor: Double,
    val description: String
)

data class ActivityLevelRecommendation(
    val level: String,
    val windowDays: Int,
    val workoutDays: Int,
    val workoutCount: Int,
    val weeklyMinutes: Int
)

object ActivityLevels {
    val all: List<ActivityLevelDefinition> = listOf(
        ActivityLevelDefinition(
            value = "SEDENTARY",
            label = "Sedentary",
            factor = 1.2,
            description = "Mostly seated with little planned exercise"
        ),
        ActivityLevelDefinition(
            value = "LIGHT",
            label = "Lightly active",
            factor = 1.375,
            description = "Mostly seated plus light exercise about 1–3 days/week"
        ),
        ActivityLevelDefinition(
            value = "MODERATE",
            label = "Moderately active",
            factor = 1.55,
            description = "Moderate exercise about 3–5 days/week or an active routine"
        ),
        ActivityLevelDefinition(
            value = "ACTIVE",
            label = "Active",
            factor = 1.725,
            description = "Hard exercise about 6–7 days/week or a physical job"
        ),
        ActivityLevelDefinition(
            value = "VERY_ACTIVE",
            label = "Very active",
            factor = 1.9,
            description = "A strenuous job plus hard training or frequent two-a-days"
        )
    )

    val options: List<Pair<String, String>> = all.map { it.value to it.label }
    val descriptions: Map<String, String> = all.associate { definition ->
        definition.value to
            "${formatFactor(definition.factor)}× resting needs · ${definition.description}"
    }

    fun definition(value: String): ActivityLevelDefinition? =
        all.firstOrNull { it.value == value.trim().uppercase() }

    fun factor(value: String): Double = definition(value)?.factor ?: 1.375

    fun label(value: String): String = definition(value)?.label ?: value

    fun isActive(value: String): Boolean =
        value.trim().uppercase() in setOf("MODERATE", "ACTIVE", "VERY_ACTIVE")

    private fun formatFactor(value: Double): String =
        value.toString().trimEnd('0').trimEnd('.')
}

object ActivityLevelRecommender {
    fun recommend(
        windowDays: Int,
        workoutDays: Int,
        workoutCount: Int,
        totalDurationMinutes: Int
    ): ActivityLevelRecommendation? {
        if (windowDays <= 0 || workoutDays <= 0 || workoutCount <= 0) return null

        val weeklyScale = 7.0 / windowDays
        val weeklyDays = workoutDays * weeklyScale
        val weeklyWorkouts = workoutCount * weeklyScale
        val weeklyMinutes = (totalDurationMinutes.coerceAtLeast(0) * weeklyScale).roundToInt()
        val level = when {
            weeklyDays >= 6.0 && weeklyWorkouts >= 9.0 && weeklyMinutes >= 600 -> "VERY_ACTIVE"
            weeklyDays >= 6.0 || weeklyMinutes >= 300 -> "ACTIVE"
            weeklyDays >= 3.0 || weeklyMinutes >= 150 -> "MODERATE"
            weeklyDays >= 1.0 || weeklyMinutes >= 60 -> "LIGHT"
            else -> "SEDENTARY"
        }
        return ActivityLevelRecommendation(
            level = level,
            windowDays = windowDays,
            workoutDays = workoutDays,
            workoutCount = workoutCount,
            weeklyMinutes = weeklyMinutes
        )
    }
}
