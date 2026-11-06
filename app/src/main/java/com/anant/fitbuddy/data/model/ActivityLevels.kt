package com.anant.fitbuddy.data.model

import android.content.Context
import androidx.annotation.StringRes
import com.anant.fitbuddy.R
import kotlin.math.roundToInt

data class ActivityLevelDefinition(
    val value: String,
    @StringRes val labelRes: Int,
    val factor: Double,
    @StringRes val descriptionRes: Int
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
            labelRes = R.string.activity_sedentary,
            factor = 1.2,
            descriptionRes = R.string.activity_mostly_seated_with_little_planned_exercise
        ),
        ActivityLevelDefinition(
            value = "LIGHT",
            labelRes = R.string.activity_lightly_active,
            factor = 1.375,
            descriptionRes = R.string.activity_mostly_seated_plus_light_exercise_about
        ),
        ActivityLevelDefinition(
            value = "MODERATE",
            labelRes = R.string.activity_moderately_active,
            factor = 1.55,
            descriptionRes = R.string.activity_moderate_exercise_about_3_5_days
        ),
        ActivityLevelDefinition(
            value = "ACTIVE",
            labelRes = R.string.activity_active,
            factor = 1.725,
            descriptionRes = R.string.activity_hard_exercise_about_6_7_days
        ),
        ActivityLevelDefinition(
            value = "VERY_ACTIVE",
            labelRes = R.string.activity_very_active,
            factor = 1.9,
            descriptionRes = R.string.activity_a_strenuous_job_plus_hard_training
        )
    )

    /** Value → label string-resource id (resolve with [stringResource] / [Context.getString]). */
    val optionRes: List<Pair<String, Int>> = all.map { it.value to it.labelRes }

    fun definition(value: String): ActivityLevelDefinition? =
        all.firstOrNull { it.value == value.trim().uppercase() }

    fun factor(value: String): Double = definition(value)?.factor ?: 1.375

    fun label(context: Context, value: String): String =
        definition(value)?.let { context.getString(it.labelRes) } ?: value

    fun description(context: Context, value: String): String? {
        val definition = definition(value) ?: return null
        return context.getString(
            R.string.activity_s_resting_needs_s,
            formatFactor(definition.factor),
            context.getString(definition.descriptionRes)
        )
    }

    fun descriptions(context: Context): Map<String, String> =
        all.associate { definition ->
            definition.value to context.getString(
                R.string.activity_s_resting_needs_s,
                formatFactor(definition.factor),
                context.getString(definition.descriptionRes)
            )
        }

    fun isActive(value: String): Boolean =
        value.trim().uppercase() in setOf("MODERATE", "ACTIVE", "VERY_ACTIVE")

    fun formatFactor(value: Double): String =
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
