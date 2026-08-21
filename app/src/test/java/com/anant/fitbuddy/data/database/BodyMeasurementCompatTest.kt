package com.anant.fitbuddy.data.database

import com.anant.fitbuddy.data.remote.NetworkModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ensures freescalePayloadJson is additive: old backups and manual entries stay valid.
 */
class BodyMeasurementCompatTest {

    private val adapter = NetworkModule.moshi.adapter(BodyMeasurement::class.java)

    @Test
    fun moshi_acceptsLegacyJsonWithoutFreescalePayload() {
        val json = """
            {
              "id": 1,
              "timestamp": 1710000000000,
              "dateString": "2024-03-09",
              "weightKg": 70.95,
              "bmi": 23.2,
              "bodyFatPct": 14.6
            }
        """.trimIndent()

        val m = adapter.fromJson(json)!!
        assertEquals(70.95, m.weightKg, 0.001)
        assertEquals(14.6, m.bodyFatPct!!, 0.001)
        assertNull(m.freescalePayloadJson)
    }

    @Test
    fun moshi_omitsNullFreescalePayloadOnEncode() {
        val m = BodyMeasurement(
            id = 1,
            timestamp = 1L,
            dateString = "2026-01-01",
            weightKg = 70.0,
            freescalePayloadJson = null,
        )
        val json = adapter.toJson(m)
        assertFalse(json.contains("freescalePayloadJson"))
    }

    @Test
    fun moshi_roundTripsNonNullFreescalePayload() {
        val m = BodyMeasurement(
            id = 2,
            timestamp = 2L,
            dateString = "2026-01-02",
            weightKg = 71.0,
            freescalePayloadJson = """{"impedance":512}""",
        )
        val parsed = adapter.fromJson(adapter.toJson(m))!!
        assertTrue(parsed.freescalePayloadJson!!.contains("512"))
    }
}
