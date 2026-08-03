package com.anant.fitbuddy.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BackupContentHasherTest {

    @Test
    fun hash_ignoresExportedAt() {
        fun encode(data: BackupData): String =
            """{"version":${data.version},"exportedAt":${data.exportedAt}}"""
        val a = BackupData(exportedAt = 1L)
        val b = BackupData(exportedAt = 999L)
        assertEquals(
            BackupContentHasher.hash(a, ::encode),
            BackupContentHasher.hash(b, ::encode)
        )
    }

    @Test
    fun hash_changesWhenNormalizedPayloadChanges() {
        fun encode(data: BackupData): String =
            """{"version":${data.version},"exportedAt":${data.exportedAt},"n":${data.measurements.size}}"""
        val a = BackupData(exportedAt = 1L)
        val b = BackupData(
            exportedAt = 1L,
            measurements = listOf(
                com.anant.fitbuddy.data.database.BodyMeasurement(
                    id = 1,
                    dateString = "2026-01-01",
                    weightKg = 70.0,
                    timestamp = 0L
                )
            )
        )
        assertNotEquals(
            BackupContentHasher.hash(a, ::encode),
            BackupContentHasher.hash(b, ::encode)
        )
    }

    @Test
    fun sha256Hex_isStableHex() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            BackupContentHasher.sha256Hex("")
        )
    }
}
