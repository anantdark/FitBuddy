package com.anant.fitbuddy.data.backup

import com.anant.fitbuddy.data.database.FoodLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupChunkMergerTest {

    @Test
    fun merge_laterChunkWinsAndTombstonesDrop() {
        val older = BackupData(
            exportedAt = 1L,
            foodLogs = listOf(
                FoodLog(1, "a", 1L, "2026-01-01", 100, 1, 1, 1),
                FoodLog(2, "b", 2L, "2026-01-02", 200, 2, 2, 2)
            )
        )
        val newer = BackupData(
            exportedAt = 2L,
            foodLogs = listOf(
                FoodLog(2, "b-edited", 3L, "2026-01-02", 250, 2, 2, 2)
            ),
            deletedFoodLogIds = listOf(1)
        )
        val merged = BackupChunkMerger.merge(listOf(older, newer))
        assertEquals(1, merged.foodLogs.size)
        assertEquals(2, merged.foodLogs[0].id)
        assertEquals("b-edited", merged.foodLogs[0].dishName)
        assertTrue(merged.deletedFoodLogIds.isEmpty())
    }
}

class BackupChunkIdsTest {
    @Test
    fun headAndLaterIds() {
        assertEquals("ABC", BackupChunkIds.chunkId("ABC", 0))
        assertEquals("ABC::c::1", BackupChunkIds.chunkId("ABC", 1))
        assertEquals(0, BackupChunkIds.parseChunkIndex("ABC", "ABC"))
        assertEquals(2, BackupChunkIds.parseChunkIndex("ABC::c::2", "ABC"))
        assertFalse(BackupChunkIds.isValidSupportId("x::c::y"))
        assertTrue(BackupChunkIds.isValidSupportId("plain-id"))
    }
}
