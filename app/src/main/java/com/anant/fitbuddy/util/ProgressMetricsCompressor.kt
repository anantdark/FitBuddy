package com.anant.fitbuddy.util

import org.json.JSONArray
import org.json.JSONObject

/**
 * Turns the verbose progress JSON from [com.anant.fitbuddy.ui.viewmodel.MainViewModel]
 * into a compact, token-efficient snapshot for the progress-coach chat system prompt.
 */
object ProgressMetricsCompressor {

    fun compress(contextJson: String): String = runCatching {
        compress(JSONObject(contextJson))
    }.getOrElse { contextJson.take(8_000) }

    fun compress(context: JSONObject): String = buildString {
        appendLine("goal=${context.optString("goal", "?")}")
        appendLine(
            "targets=kcal${context.optInt("target_calories_rest_day_baseline")} " +
                "p${context.optInt("target_protein_g")} " +
                "c${context.optInt("target_carbs_g")} " +
                "f${context.optInt("target_fats_g")}"
        )
        context.optDouble("avg_daily_net_calories_recent").takeIf { !it.isNaN() }?.let {
            appendLine("avg_net_kcal=${fmt(it)}")
        }
        context.optDouble("avg_exercise_calorie_eat_back_ratio").takeIf { !it.isNaN() }?.let {
            appendLine("exercise_eat_back_ratio=${fmt(it)}")
        }
        appendLine("calorie_model=${context.optString("calorie_model_note").replace('\n', ' ')}")
        appendLine()

        appendLine("BODY d|kg|bf%|muscle_kg|visceral|bmr (oldest→newest):")
        context.optJSONArray("body_measurements")?.let { appendBody(it) } ?: appendLine("(none)")

        appendLine()
        appendLine("NUT7 d|in|burn|net|p|c|f (weekly chart):")
        context.optJSONArray("nutrition_weekly")?.let { appendNutrition(it) }
            ?: appendLine("(none)")

        appendLine()
        appendLine("NUT30 d|in|burn|net|p|c|f (monthly chart):")
        context.optJSONArray("nutrition_daily")?.let { appendNutrition(it) }
            ?: appendLine("(none)")

        appendLine()
        appendLine("EX7 d|burn_kcal:")
        context.optJSONArray("exercise_weekly")?.let { appendExercise(it) }
            ?: appendLine("(none)")

        appendLine()
        appendLine("EX30 d|burn_kcal:")
        context.optJSONArray("exercise_daily")?.let { appendExercise(it) }
            ?: appendLine("(none)")
    }.trim()

    private fun StringBuilder.appendBody(array: JSONArray) {
        if (array.length() == 0) {
            appendLine("(none)")
            return
        }
        for (i in 0 until array.length()) {
            val m = array.getJSONObject(i)
            appendLine(
                "${shortDate(m.optString("date"))}|" +
                    fmt(m.optDouble("weight_kg")) + "|" +
                    optFmt(m, "body_fat_pct") + "|" +
                    optFmt(m, "muscle_mass_kg") + "|" +
                    optFmt(m, "visceral_fat") + "|" +
                    optFmt(m, "bmr")
            )
        }
    }

    private fun StringBuilder.appendNutrition(array: JSONArray) {
        if (array.length() == 0) {
            appendLine("(none)")
            return
        }
        for (i in 0 until array.length()) {
            val s = array.getJSONObject(i)
            appendLine(
                "${shortDate(s.optString("date"))}|" +
                    s.optInt("calories") + "|" +
                    s.optInt("calories_burned") + "|" +
                    s.optInt("net_calories") + "|" +
                    s.optInt("protein_g") + "|" +
                    s.optInt("carbs_g") + "|" +
                    s.optInt("fats_g")
            )
        }
    }

    private fun StringBuilder.appendExercise(array: JSONArray) {
        if (array.length() == 0) {
            appendLine("(none)")
            return
        }
        for (i in 0 until array.length()) {
            val s = array.getJSONObject(i)
            appendLine("${shortDate(s.optString("date"))}|${s.optInt("calories_burned")}")
        }
    }

    private fun shortDate(isoDate: String): String {
        val parts = isoDate.split('-')
        return if (parts.size >= 3) "${parts[1]}-${parts[2]}" else isoDate
    }

    private fun optFmt(obj: JSONObject, key: String): String {
        if (!obj.has(key) || obj.isNull(key)) return "-"
        return fmt(obj.optDouble(key))
    }

    private fun fmt(value: Double): String {
        if (value.isNaN()) return "-"
        val rounded = (value * 10).toInt() / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }
}
