package com.anant.fitbuddy.data.remote

import com.anant.fitbuddy.data.database.FoodLog
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Backups written with decimal macros must stay readable by builds that stored macros as Int. */
class MacroBackupCompatTest {

    private data class LegacyFoodLog(
        val id: Int,
        val dishName: String,
        val timestamp: Long,
        val dateString: String,
        val calories: Int,
        val proteinG: Int,
        val carbsG: Int,
        val fatsG: Int,
        val iconTopic: String? = null
    )

    private val moshi = Moshi.Builder()
        .add(CoercingIntJsonAdapter.FACTORY)
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun wholeMacros_areWrittenWithPointZero() {
        val json = moshi.adapter(FoodLog::class.java)
            .toJson(FoodLog(1, "m", 1L, "2026-01-01", 300, 12.0, 30.0, 5.0))
        assertTrue(json, json.contains("\"proteinG\":12.0"))
        assertTrue(json, json.contains("\"carbsG\":30.0"))
        assertTrue(json, json.contains("\"fatsG\":5.0"))
    }

    @Test
    fun legacyReader_parsesDecimalBackup() {
        val json = moshi.adapter(FoodLog::class.java)
            .toJson(FoodLog(1, "m", 1L, "2026-01-01", 300, 12.0, 30.5, 5.0))
        val legacy = moshi.adapter(LegacyFoodLog::class.java).fromJson(json)!!
        assertEquals(12, legacy.proteinG)
        assertEquals(31, legacy.carbsG)
        assertEquals(5, legacy.fatsG)
    }

    @Test
    fun currentReader_parsesLegacyIntBackup() {
        val json = """{"id":1,"dishName":"m","timestamp":1,"dateString":"2026-01-01","calories":300,"proteinG":12,"carbsG":30,"fatsG":5}"""
        val log = moshi.adapter(FoodLog::class.java).fromJson(json)!!
        assertEquals(12.0, log.proteinG, 0.0)
        assertEquals(30.0, log.carbsG, 0.0)
    }
}
