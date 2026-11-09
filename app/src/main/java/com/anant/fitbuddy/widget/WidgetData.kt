package com.anant.fitbuddy.widget

/** Snapshot of dashboard + preset data for home-screen widgets. */
data class WidgetData(
    val consumedCalories: Int = 0,
    val burnedCalories: Int = 0,
    val targetCalories: Int = 2000,
    val presets: List<WidgetPresetChip> = emptyList(),
    val waterTrackingEnabled: Boolean = false,
    val waterConsumedMl: Int = 0,
    val waterGoalMl: Int = 2000,
    val waterOnlyMl: Int = 0,
    val coffeeMl: Int = 0,
    val teaMl: Int = 0,
    val waterQuickAddMl: Int = 250,
    val bottleQuickAddMl: Int = 500,
) {
    val netCalories: Int get() = consumedCalories - burnedCalories
    /** Matches dashboard: remaining toward food target (burn shown separately). */
    val remainingCalories: Int get() = targetCalories - consumedCalories
    val calorieProgress: Float
        get() = if (targetCalories <= 0) 0f
        else (consumedCalories.toFloat() / targetCalories).coerceIn(0f, 1f)
    val isOverTarget: Boolean get() = consumedCalories > targetCalories
    val waterProgress: Float
        get() = if (waterGoalMl <= 0) 0f
        else (waterConsumedMl.toFloat() / waterGoalMl).coerceIn(0f, 1f)
}

data class WidgetPresetChip(
    val id: Int,
    val name: String,
    val calories: Int,
    val kind: String,
)
