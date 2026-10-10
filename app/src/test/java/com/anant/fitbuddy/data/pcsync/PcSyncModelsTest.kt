package com.anant.fitbuddy.data.pcsync

import com.anant.fitbuddy.data.backup.BackupData
import com.anant.fitbuddy.data.database.FoodLog
import com.anant.fitbuddy.data.remote.NetworkModule
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PcSyncModelsTest {

    private val moshi = NetworkModule.moshi

    @Test
    fun `parses ops as emitted by FitBuddy Desktop`() {
        // Same shape as fitbuddy_desktop.store.enqueue + model.meal_op_payload.
        val json = """
            {"protocol": 1, "needSnapshot": false, "ops": [
              {"id": "a1", "type": "add_meal", "createdAt": 1,
               "meal": {"dishName": "Cena", "timestamp": 1790000000000,
                        "foods": [{"name": "Pizza", "servings": 1.0, "calories": 850,
                                   "proteinG": 32, "carbsG": 105, "fatsG": 30}]}},
              {"id": "a2", "type": "add_measurement", "createdAt": 2,
               "measurement": {"timestamp": 1790000000000, "weightKg": 70.4}},
              {"id": "a3", "type": "delete_exercise", "createdAt": 3, "targetId": 7, "targetTimestamp": 99},
              {"id": "a4", "type": "something_new", "createdAt": 4}
            ]}
        """.trimIndent()
        val response = moshi.adapter(PcSyncResponse::class.java).fromJson(json)!!
        assertEquals(listOf("a1", "a2", "a3", "a4"), response.ops.map { it.id })
        val meal = response.ops[0].meal!!
        assertEquals("Cena", meal.dishName)
        assertEquals(850, meal.foods.single().calories)
        assertNull(meal.foods.single().ingredients)
        assertEquals(70.4, response.ops[1].measurement!!.weightKg, 0.0)
        assertNull(response.ops[1].measurement!!.bodyFatPct)
        assertEquals(7, response.ops[2].targetId)
        assertEquals(99L, response.ops[2].targetTimestamp)
        assertFalse(response.needSnapshot)
    }

    @Test
    fun `request uses the field names the desktop reads`() {
        val request = PcSyncRequest(
            sentAt = 5,
            appVersion = "3.2.80",
            ackedOps = listOf("a1"),
            snapshot = BackupData(
                exportedAt = 0,
                foodLogs = listOf(FoodLog(1, "Pranzo", 10, "2026-10-07", 700, 40, 80, 20))
            ),
            snapshotHash = "h"
        )
        val obj = JSONObject(moshi.adapter(PcSyncRequest::class.java).toJson(request))
        assertEquals(PROTOCOL_VERSION, obj.getInt("protocol"))
        assertEquals("a1", obj.getJSONArray("ackedOps").getString(0))
        assertEquals("h", obj.getString("snapshotHash"))
        val log = obj.getJSONObject("snapshot").getJSONArray("foodLogs").getJSONObject(0)
        assertEquals("2026-10-07", log.getString("dateString"))
        assertEquals("Pranzo", log.getString("dishName"))
        assertTrue(obj.getJSONObject("snapshot").has("mealFoods"))
    }

    @Test
    fun `base url adds scheme and default port`() {
        assertEquals("http://thinkpad:8765", PcSyncConfig(address = "thinkpad").baseUrl())
        assertEquals("http://thinkpad:9000", PcSyncConfig(address = " thinkpad:9000/ ").baseUrl())
        assertEquals("http://100.64.0.1:8765", PcSyncConfig(address = "http://100.64.0.1").baseUrl())
        assertFalse(PcSyncConfig(enabled = true, address = "x", token = "").isUsable)
    }
}
