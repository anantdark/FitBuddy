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

    @Test
    fun `clearing preferred models from cooldown map leaves others`() {
        val now = LocalDate.of(2026, 7, 19)
            .atTime(LocalTime.of(12, 0))
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()
        val until = now + 60_000L
        val preferred = "google/gemma-4-31b-it:free"
        val other = "google/gemma-4-26b-a4b-it:free"
        val cooled = mutableMapOf(
            ModelCooldown.keyOf(AiProvider.OPENROUTER, preferred) to until,
            ModelCooldown.keyOf(AiProvider.OPENROUTER, other) to until,
            ModelCooldown.keyOf(AiProvider.GEMINI, "gemini-2.5-flash") to until
        )
        // Mirrors SettingsRepository.save: drop cooldowns for the saved preferred ids.
        for (id in setOf(preferred, preferred)) {
            cooled.remove(ModelCooldown.keyOf(AiProvider.OPENROUTER, id))
        }
        assertFalse(cooled.containsKey(ModelCooldown.keyOf(AiProvider.OPENROUTER, preferred)))
        assertTrue(cooled.containsKey(ModelCooldown.keyOf(AiProvider.OPENROUTER, other)))
        assertTrue(cooled.containsKey(ModelCooldown.keyOf(AiProvider.GEMINI, "gemini-2.5-flash")))
        val roundTrip = decodeModelCooldowns(encodeModelCooldowns(cooled), now)
        assertEquals(cooled, roundTrip)
    }
}
