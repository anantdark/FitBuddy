package com.anant.fitbuddy.bridge

import com.anant.fitbuddy.data.database.BodyMeasurement
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaleBridgeJsonTest {
    @Test
    fun decode_keepsSupportedMetrics_discardsOtherFieldsAndPayload() {
        val json = """
            {
              "format": "anant-scale-bridge",
              "version": 1,
              "timestamp": 1710000000000,
              "dateString": "2024-03-09",
              "weightKg": 70.95,
              "bmi": 23.2,
              "bodyFatPct": 14.6,
              "bmr": 1650,
              "muscleMassKg": 52.4,
              "skeletalMuscleMassKg": 48.6,
              "freescalePayload": {"impedance": 500}
            }
        """.trimIndent()

        val measurement = ScaleBridgeJson.decodeMeasurement(json)
        assertEquals(1_710_000_000_000L, measurement.timestamp)
        assertEquals("2024-03-09", measurement.dateString)
        assertEquals(70.95, measurement.weightKg, 0.001)
        assertEquals(14.6, measurement.bodyFatPct!!, 0.001)
        assertEquals(1650, measurement.bmr)
        assertEquals(52.4, measurement.muscleMassKg!!, 0.001)
        assertNull(measurement.bmi)
        assertNull(measurement.skeletalMuscleMassKg)
        assertNull(measurement.freescalePayloadJson)
    }

    @Test
    fun encodeArray_roundTripsOnlySupportedMetrics() {
        val original = BodyMeasurement(
            timestamp = 1000L,
            dateString = "2024-01-01",
            weightKg = 70.0,
            bmi = 23.0,
            bodyFatPct = 15.0,
            bmr = 1640,
            muscleMassKg = 52.0,
            skeletalMuscleMassKg = 48.0,
        )

        val json = ScaleBridgeJson.encodeArray(listOf(original))
        val encoded = JSONArray(json).getJSONObject(0)
        assertTrue(encoded.has("weightKg"))
        assertTrue(encoded.has("bodyFatPct"))
        assertTrue(encoded.has("bmr"))
        assertTrue(encoded.has("muscleMassKg"))
        assertFalse(encoded.has("bmi"))
        assertFalse(encoded.has("skeletalMuscleMassKg"))

        val decoded = ScaleBridgeJson.decodeMeasurement(encoded.toString())
        assertEquals(original.weightKg, decoded.weightKg, 0.001)
        assertEquals(original.bodyFatPct!!, decoded.bodyFatPct!!, 0.001)
        assertEquals(original.bmr, decoded.bmr)
        assertEquals(original.muscleMassKg!!, decoded.muscleMassKg!!, 0.001)
    }

    @Test
    fun encode_dropsLegacyOpaquePayload() {
        val measurement = BodyMeasurement(
            timestamp = 2L,
            dateString = "2026-01-02",
            weightKg = 71.0,
            freescalePayloadJson = """{"impedance":512}""",
        )

        val encoded = ScaleBridgeJson.encodeObject(measurement)
        assertFalse(encoded.has("freescalePayload"))
    }
}
