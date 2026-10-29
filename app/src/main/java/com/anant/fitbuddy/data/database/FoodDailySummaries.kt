package com.anant.fitbuddy.data.database

import com.anant.fitbuddy.util.DateUtils

/**
 * Pads [this] (logged days only) to every calendar day in [[startDate], [endDate]].
 * Returns newest-first to match DAO / chart conventions; missing days are zeros.
 */
fun List<FoodDailySummary>.filledForDateRange(
    startDate: String,
    endDate: String
): List<FoodDailySummary> {
    val byDate = associateBy { it.dateString }
    return DateUtils.datesInclusive(startDate, endDate)
        .asReversed()
        .map { date ->
            byDate[date] ?: FoodDailySummary(
                dateString = date,
                totalCalories = 0,
                totalProtein = 0.0,
                totalCarbs = 0.0,
                totalFats = 0.0
            )
        }
}
