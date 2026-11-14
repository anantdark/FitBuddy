package com.anant.fitbuddy.data.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ProgressChatSessionStoreTest {

    @Test
    fun isExpired_sameUtcDay_notExpired() {
        val day = LocalDate.of(2026, 11, 11)
        val last = day.atTime(15, 30).toInstant(ZoneOffset.UTC).toEpochMilli()
        val now = day.atTime(23, 59).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertFalse(ProgressChatSessionStore.isExpired(last, now))
    }

    @Test
    fun isExpired_nextUtcMidnight_expired() {
        val day = LocalDate.of(2026, 11, 11)
        val last = day.atTime(15, 30).toInstant(ZoneOffset.UTC).toEpochMilli()
        val nextMidnight = day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        assertTrue(ProgressChatSessionStore.isExpired(last, nextMidnight))
    }

    @Test
    fun isExpired_laterUtcDay_expired() {
        val last = LocalDate.of(2026, 11, 11).atTime(1, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val now = LocalDate.of(2026, 11, 12).atTime(0, 1).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertTrue(ProgressChatSessionStore.isExpired(last, now))
    }

    @Test
    fun isExpired_zeroTimestamp_expired() {
        assertTrue(ProgressChatSessionStore.isExpired(0L, System.currentTimeMillis()))
    }
}
