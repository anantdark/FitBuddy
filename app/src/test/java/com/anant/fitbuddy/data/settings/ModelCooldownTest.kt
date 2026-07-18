package com.anant.fitbuddy.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class ModelCooldownTest {

    @Test
    fun `cooldown ends at next UTC midnight`() {
        val middayUtc = LocalDate.of(2026, 7, 18)
            .atTime(LocalTime.of(12, 0))
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()
        val until = ModelCooldownPolicy.cooldownUntilEpochMs(middayUtc)
        val expected = LocalDate.of(2026, 7, 19)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, until)
    }

    @Test
    fun `just before UTC midnight still rolls to next calendar day`() {
        val almostMidnight = LocalDate.of(2026, 7, 18)
            .atTime(LocalTime.of(23, 59, 59))
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()
        val until = ModelCooldownPolicy.cooldownUntilEpochMs(almostMidnight)
        assertEquals(
            LocalDate.of(2026, 7, 19).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            until
        )
    }

    @Test
    fun `decode drops expired entries`() {
        val now = LocalDate.of(2026, 7, 19)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val raw = "GEMINI|gemini-2.5-flash=${now - 1}\nOPENROUTER|m=${now + 1000}"
        val map = decodeModelCooldowns(raw, now)
        assertEquals(setOf("OPENROUTER|m"), map.keys)
    }

    @Test
    fun `rate limit detection`() {
        assertTrue(ModelCooldownPolicy.isRateLimitError(IllegalStateException("Rate limited (HTTP 429)")))
        assertTrue(ModelCooldownPolicy.isRateLimitError(IllegalStateException("quota exceeded")))
        assertFalse(ModelCooldownPolicy.isRateLimitError(IllegalStateException("Network error")))
    }
}
