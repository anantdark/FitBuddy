package com.anant.fitbuddy.bridge

import com.anant.fitbuddy.data.database.BodyMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaleBridgeJsonTest {
    @Test
    fun decode_keepsOverlappingFields_discardsUnknown() {
        val json = """
            {
              "format": "anant-scale-bridge",
              "version": 1,
              "timestamp": 1710000000000,
              "dateString": "2024-03-09",
              "weightKg": 70.95,
              "bmi": 23.2,
              "bodyFatPct": 14.6,
              "skeletalMuscleMassKg": 48.6,
              "impedance": 500,
              "pkt0Hex": "aabb",
              "algorithm": "WLA25"
            }
        """.trimIndent()

        val m = ScaleBridgeJson.decodeMeasurement(json)
        assertEquals(1_710_000_000_000L, m.timestamp)
        assertEquals("2024-03-09", m.dateString)
        assertEquals(70.95, m.weightKg, 0.001)
        assertEquals(23.2, m.bmi!!, 0.001)
        assertEquals(14.6, m.bodyFatPct!!, 0.001)
        assertEquals(48.6, m.skeletalMuscleMassKg!!, 0.001)
        assertNull(m.bmr)
        assertNull(m.freescalePayloadJson)
    }

    @Test
    fun encodeArray_roundTrip() {
        val original = BodyMeasurement(
            timestamp = 1000L,
            dateString = "2024-01-01",
            weightKg = 70.0,
            bodyFatPct = 15.0,
            skeletalMuscleMassKg = 48.0,
        )
        val json = ScaleBridgeJson.encodeArray(listOf(original))
        assertTrue(json.contains("skeletalMuscleMassKg"))
        val decoded = ScaleBridgeJson.decodeMeasurement(
            org.json.JSONArray(json).getJSONObject(0).toString(),
        )
        assertEquals(original.weightKg, decoded.weightKg, 0.001)
        assertEquals(original.skeletalMuscleMassKg!!, decoded.skeletalMuscleMassKg!!, 0.001)
        assertEquals(original.bodyFatPct!!, decoded.bodyFatPct!!, 0.001)
    }

    @Test
    fun decode_keepsFreescalePayloadOpaque() {
        val json = """
            {
              "format": "anant-scale-bridge",
              "version": 1,
              "timestamp": 1710000000000,
              "dateString": "2024-03-09",
              "weightKg": 70.95,
              "bodyFatPct": 14.6,
              "freescalePayload": {
                "recordedAtEpochMs": 1710000000000,
                "weight": 70.95,
                "impedance": 512.0,
                "pkt0Hex": "deadbeef"
              }
            }
        """.trimIndent()
        val m = ScaleBridgeJson.decodeMeasurement(json)
        assertEquals(70.95, m.weightKg, 0.001)
        assertEquals(14.6, m.bodyFatPct!!, 0.001)
        assertTrue(m.freescalePayloadJson!!.contains("impedance"))
        assertTrue(m.freescalePayloadJson!!.contains("deadbeef"))

        val encoded = ScaleBridgeJson.encodeObject(m)
        assertTrue(encoded.has(ScaleBridgeJson.KEY_FREESCALE_PAYLOAD))
        assertEquals(512.0, encoded.getJSONObject(ScaleBridgeJson.KEY_FREESCALE_PAYLOAD).getDouble("impedance"), 0.0)
    }
}
