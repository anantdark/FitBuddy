package com.anant.fitbuddy.widget

import java.util.Calendar

internal object WidgetCopy {

    fun timeOfDayPrompt(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> "Morning fuel?"
            in 11..14 -> "Lunch time?"
            in 15..17 -> "Afternoon bite?"
            in 18..21 -> "Dinner plans?"
            else -> "Late snack?"
        }
    }

    fun describePrompt(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> "Log breakfast"
            in 11..14 -> "Log lunch"
            in 15..17 -> "Log a snack"
            in 18..21 -> "Log dinner"
            else -> "Log a late snack"
        }
    }
}
