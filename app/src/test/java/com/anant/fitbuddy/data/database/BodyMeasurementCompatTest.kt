package com.anant.fitbuddy.data.database

import com.anant.fitbuddy.data.remote.NetworkModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BodyMeasurementCompatTest {

    private val adapter = NetworkModule.moshi.adapter(BodyMeasurement::class.java)

    @Test
    fun moshi_acceptsLegacyJsonButIgnoresRemovedMetrics() {
        val json = """
            {
              "id": 1,
              "timestamp": 1710000000000,
              "dateString": "2024-03-09",
              "weightKg": 70.95,
              "bmi": 23.2,
              "bodyFatPct": 14.6,
              "bmr": 1650,
              "muscleMassKg": 52.4,
              "visceralFat": 8.0,
              "freescalePayloadJson": "{\"impedance\":512}"
            }
        """.trimIndent()

        val measurement = adapter.fromJson(json)!!
        assertEquals(70.95, measurement.weightKg, 0.001)
        assertEquals(14.6, measurement.bodyFatPct!!, 0.001)
        assertEquals(1650, measurement.bmr)
        assertEquals(52.4, measurement.muscleMassKg!!, 0.001)
        assertNull(measurement.bmi)
        assertNull(measurement.visceralFat)
        assertNull(measurement.freescalePayloadJson)
    }

    @Test
    fun moshi_emitsOnlySupportedMetrics() {
        val measurement = BodyMeasurement(
            id = 1,
            timestamp = 1L,
            dateString = "2026-01-01",
            weightKg = 70.0,
            bmi = 23.0,
            bodyFatPct = 15.0,
            bmr = 1640,
            muscleMassKg = 52.0,
            visceralFat = 8.0,
            freescalePayloadJson = """{"impedance":512}""",
        )

        val json = adapter.toJson(measurement)
        assertFalse(json.contains("bmi"))
        assertFalse(json.contains("visceralFat"))
        assertFalse(json.contains("freescalePayloadJson"))
        assertEquals(measurement.supportedMetricsOnly(), adapter.fromJson(json))
    }
}
