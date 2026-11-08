package com.anant.fitbuddy.widget

/** Snapshot of dashboard + preset data for home-screen widgets. */
data class WidgetData(
    val consumedCalories: Int = 0,
    val burnedCalories: Int = 0,
    val targetCalories: Int = 2000,
    val presets: List<WidgetPresetChip> = emptyList(),
) {
    val netCalories: Int get() = consumedCalories - burnedCalories
    /** Matches dashboard: remaining toward food target (burn shown separately). */
    val remainingCalories: Int get() = targetCalories - consumedCalories
    val calorieProgress: Float
        get() = if (targetCalories <= 0) 0f
        else (consumedCalories.toFloat() / targetCalories).coerceIn(0f, 1f)
    val isOverTarget: Boolean get() = consumedCalories > targetCalories
}

data class WidgetPresetChip(
    val id: Int,
    val name: String,
    val calories: Int,
    val kind: String,
)
