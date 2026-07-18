package com.anant.fitbuddy.util

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressMetricsCompressorTest {

    @Test
    fun `compress emits compact profile targets and series headers`() {
        val context = JSONObject()
            .put("age", 30)
            .put("sex", "male")
            .put("height_cm", 175)
            .put("weight_kg", 72.5)
            .put("activity_level", "moderate")
            .put("goal", "lose fat")
            .put("target_calories_rest_day_baseline", 2200)
            .put("target_protein_g", 150)
            .put("target_carbs_g", 200)
            .put("target_fats_g", 70)
            .put("avg_daily_net_calories_recent", 1850.4)
            .put("avg_exercise_calorie_eat_back_ratio", 0.5)
            .put("calorie_model_note", "net = in - burn")
            .put(
                "body_measurements",
                JSONArray().put(
                    JSONObject()
                        .put("date", "2026-07-01")
                        .put("weight_kg", 73.2)
                        .put("body_fat_pct", 18.5)
                        .put("muscle_mass_kg", 55.0)
                        .put("visceral_fat", 8)
                        .put("bmr", 1650)
                        .put("bmi", 23.9)
                )
            )
            .put(
                "body_prior_months",
                JSONArray().put(
                    JSONObject()
                        .put("month", "2026-06")
                        .put("readings", 4)
                        .put("avg_weight_kg", 74.0)
                        .put("start_weight_kg", 75.0)
                        .put("end_weight_kg", 73.5)
                        .put("end_body_fat_pct", 19.0)
                        .put("end_muscle_mass_kg", 54.5)
                        .put("end_visceral_fat", 9)
                        .put("end_bmr", 1660)
                        .put("end_bmi", 24.1)
                )
            )
            .put(
                "nutrition_daily",
                JSONArray().put(
                    JSONObject()
                        .put("date", "2026-07-10")
                        .put("calories", 2100)
                        .put("calories_burned", 300)
                        .put("net_calories", 1800)
                        .put("protein_g", 140)
                        .put("carbs_g", 190)
                        .put("fats_g", 65)
                )
            )
            .put(
                "nutrition_prior_months",
                JSONArray().put(
                    JSONObject()
                        .put("month", "2026-05")
                        .put("days_logged", 20)
                        .put("avg_calories", 2000)
                        .put("avg_calories_burned", 250)
                        .put("avg_net_calories", 1750)
                        .put("avg_protein_g", 130)
                        .put("avg_carbs_g", 180)
                        .put("avg_fats_g", 60)
                        .put("exercise_days", 12)
                )
            )
            .put(
                "exercise_daily",
                JSONArray().put(
                    JSONObject()
                        .put("date", "2026-07-10")
                        .put("calories_burned", 300)
                )
            )
            .put(
                "exercise_prior_months",
                JSONArray().put(
                    JSONObject()
                        .put("month", "2026-05")
                        .put("days_trained", 12)
                        .put("avg_burn_kcal", 280)
                        .put("total_burn_kcal", 3360)
                )
            )

        val out = ProgressMetricsCompressor.compress(context)

        assertTrue(out.contains("profile=age30 sexmale ht175cm wt72.5kg actmoderate"))
        assertTrue(out.contains("goal=lose fat"))
        assertTrue(out.contains("targets=kcal2200 p150 c200 f70"))
        assertTrue(out.contains("avg_net_kcal=1850.4"))
        assertTrue(out.contains("exercise_eat_back_ratio=0.5"))
        assertTrue(out.contains("BODY30 "))
        assertTrue(out.contains("07-01|73.2|18.5|55|8|1650|23.9"))
        assertTrue(out.contains("2026-06|4|74|75|73.5|19|54.5|9|1660|24.1"))
        assertTrue(out.contains("07-10|2100|300|1800|140|190|65"))
        assertTrue(out.contains("2026-05|20|2000|250|1750|130|180|60|12"))
        assertTrue(out.contains("07-10|300"))
        assertTrue(out.contains("2026-05|12|280|3360"))
        assertFalse(out.contains("body_measurements"))
    }

    @Test
    fun `compress empty arrays emit none markers`() {
        val context = JSONObject()
            .put("body_measurements", JSONArray())
            .put("nutrition_daily", JSONArray())
            .put("exercise_daily", JSONArray())

        val out = ProgressMetricsCompressor.compress(context)
        assertTrue(out.contains("BODY30"))
        assertTrue(out.contains("(none)"))
        assertTrue(out.contains("profile=age? sex? ht?cm wt?kg act?"))
    }

    @Test
    fun `compress string overload recovers from invalid json`() {
        val garbage = "not-json{{{}"
        val out = ProgressMetricsCompressor.compress(garbage)
        assertEquals(garbage, out)
    }

    @Test
    fun `missing optional metrics become dashes`() {
        val row = JSONObject()
            .put("date", "2026-07-02")
            .put("weight_kg", 70)
        val context = JSONObject().put("body_measurements", JSONArray().put(row))
        val out = ProgressMetricsCompressor.compress(context)
        assertTrue(out.contains("07-02|70|-|-|-|-|-"))
    }
}
