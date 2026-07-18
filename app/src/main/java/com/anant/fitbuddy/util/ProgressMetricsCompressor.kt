package com.anant.fitbuddy.util

import org.json.JSONArray
import org.json.JSONObject

/**
 * Turns the verbose progress JSON from [com.anant.fitbuddy.ui.viewmodel.MainViewModel]
 * into a compact, token-efficient snapshot for the progress-coach system prompt.
 *
 * Past-month series stay day-level; older history is already calendar-month averages
 * in the source JSON and is emitted as short prior-month rows.
 */
object ProgressMetricsCompressor {

    fun compress(contextJson: String): String = runCatching {
        compress(JSONObject(contextJson))
    }.getOrElse { contextJson.take(8_000) }

    fun compress(context: JSONObject): String = buildString {
        append("profile=")
        append("age${optScalar(context, "age")} ")
        append("sex${context.optString("sex", "?").ifBlank { "?" }} ")
        append("ht${optScalar(context, "height_cm")}cm ")
        append("wt${optScalar(context, "weight_kg")}kg ")
        append("act${context.optString("activity_level", "?").ifBlank { "?" }}")
        appendLine()
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

        appendLine("BODY30 d|kg|bf%|muscle_kg|visceral|bmr|bmi (past month, oldest→newest):")
        context.optJSONArray("body_measurements")?.let { appendBody(it) } ?: appendLine("(none)")

        appendLine()
        appendLine("BODY_PRIOR month|n|avg_kg|start_kg|end_kg|end_bf%|end_muscle|end_visceral|end_bmr|end_bmi:")
        context.optJSONArray("body_prior_months")?.let { appendPriorBody(it) } ?: appendLine("(none)")

        appendLine()
        appendLine("NUT30 d|in|burn|net|p|c|f (past month, daily):")
        context.optJSONArray("nutrition_daily")?.let { appendNutrition(it) }
            ?: appendLine("(none)")

        appendLine()
        appendLine("NUT_PRIOR month|days|avg_in|avg_burn|avg_net|avg_p|avg_c|avg_f|ex_days:")
        context.optJSONArray("nutrition_prior_months")?.let { appendPriorNutrition(it) }
            ?: appendLine("(none)")

        appendLine()
        appendLine("EX30 d|burn_kcal (past month, daily):")
        context.optJSONArray("exercise_daily")?.let { appendExercise(it) }
            ?: appendLine("(none)")

        appendLine()
        appendLine("EX_PRIOR month|days|avg_burn|total_burn:")
        context.optJSONArray("exercise_prior_months")?.let { appendPriorExercise(it) }
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
                    optFmt(m, "bmr") + "|" +
                    optFmt(m, "bmi")
            )
        }
    }

    private fun StringBuilder.appendPriorBody(array: JSONArray) {
        if (array.length() == 0) {
            appendLine("(none)")
            return
        }
        for (i in 0 until array.length()) {
            val m = array.getJSONObject(i)
            appendLine(
                "${m.optString("month")}|" +
                    m.optInt("readings") + "|" +
                    fmt(m.optDouble("avg_weight_kg")) + "|" +
                    fmt(m.optDouble("start_weight_kg")) + "|" +
                    fmt(m.optDouble("end_weight_kg")) + "|" +
                    optFmt(m, "end_body_fat_pct") + "|" +
                    optFmt(m, "end_muscle_mass_kg") + "|" +
                    optFmt(m, "end_visceral_fat") + "|" +
                    optFmt(m, "end_bmr") + "|" +
                    optFmt(m, "end_bmi")
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

    private fun StringBuilder.appendPriorNutrition(array: JSONArray) {
        if (array.length() == 0) {
            appendLine("(none)")
            return
        }
        for (i in 0 until array.length()) {
            val s = array.getJSONObject(i)
            appendLine(
                "${s.optString("month")}|" +
                    s.optInt("days_logged") + "|" +
                    s.optInt("avg_calories") + "|" +
                    s.optInt("avg_calories_burned") + "|" +
                    s.optInt("avg_net_calories") + "|" +
                    s.optInt("avg_protein_g") + "|" +
                    s.optInt("avg_carbs_g") + "|" +
                    s.optInt("avg_fats_g") + "|" +
                    s.optInt("exercise_days")
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

    private fun StringBuilder.appendPriorExercise(array: JSONArray) {
        if (array.length() == 0) {
            appendLine("(none)")
            return
        }
        for (i in 0 until array.length()) {
            val s = array.getJSONObject(i)
            appendLine(
                "${s.optString("month")}|" +
                    s.optInt("days_trained") + "|" +
                    s.optInt("avg_burn_kcal") + "|" +
                    s.optInt("total_burn_kcal")
            )
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

    private fun optScalar(obj: JSONObject, key: String): String {
        if (!obj.has(key) || obj.isNull(key)) return "?"
        return when (val value = obj.get(key)) {
            is Number -> fmt(value.toDouble())
            else -> value.toString().ifBlank { "?" }
        }
    }

    private fun fmt(value: Double): String {
        if (value.isNaN()) return "-"
        val rounded = (value * 10).toInt() / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }
}
