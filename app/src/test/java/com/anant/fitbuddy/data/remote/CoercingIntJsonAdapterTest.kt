package com.anant.fitbuddy.data.remote

import com.anant.fitbuddy.data.backup.BackupData
import com.anant.fitbuddy.data.database.FoodLog
import com.anant.fitbuddy.data.database.MealFood
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoercingIntJsonAdapterTest {

    private val moshi = Moshi.Builder()
        .add(CoercingIntJsonAdapter.FACTORY)
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun foodLog_keepsFractionalMacros() {
        val json = """
            {
              "id": 1,
              "dishName": "Day meals (4)",
              "timestamp": 1781440200000,
              "dateString": "2026-06-14",
              "calories": 1955,
              "proteinG": 83.2,
              "carbsG": 238.9,
              "fatsG": 71.0
            }
        """.trimIndent()

        val log = moshi.adapter(FoodLog::class.java).fromJson(json)!!

        assertEquals(83.2, log.proteinG, 0.0)
        assertEquals(238.9, log.carbsG, 0.0)
        assertEquals(71.0, log.fatsG, 0.0)
        assertEquals(1955, log.calories)
    }

    @Test
    fun mealFood_acceptsNullPresetId() {
        val json = """
            {
              "id": 1,
              "mealLogId": 1,
              "name": "Oats",
              "servings": 1.0,
              "orderIndex": 0,
              "calories": 350,
              "proteinG": 11.9,
              "carbsG": 55.2,
              "fatsG": 8.1,
              "ingredients": null,
              "presetId": null,
              "barcode": null
            }
        """.trimIndent()

        val food = moshi.adapter(MealFood::class.java).fromJson(json)!!

        assertNull(food.presetId)
        assertNull(food.barcode)
        assertEquals(11.9, food.proteinG, 0.0)
    }

    @Test
    fun progressDemoBackup_parsesFully() {
        val candidates = listOf(
            java.io.File("testdata/progress-demo-backup.json"),
            java.io.File("../testdata/progress-demo-backup.json")
        )
        val file = candidates.firstOrNull { it.isFile }
        org.junit.Assume.assumeTrue("testdata fixture present", file != null)

        val data = moshi.adapter(BackupData::class.java).fromJson(file!!.readText())!!

        assertEquals(4, data.version)
        assertEquals(35, data.foodLogs.size)
        assertEquals(130, data.mealFoods.size)
        assertEquals(83.2, data.foodLogs[0].proteinG, 0.0)
        assertNull(data.mealFoods[0].presetId)
    }
}
