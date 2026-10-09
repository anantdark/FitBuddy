package com.anant.fitbuddy.data.donors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DonorLoreTest {

    @Test
    fun supporterNumber_startsAt1001() {
        assertEquals(1001, DonorLore.supporterNumber(0))
        assertEquals(1002, DonorLore.supporterNumber(1))
        assertEquals(1010, DonorLore.supporterNumber(9))
    }

    @Test
    fun lore_stableForSameTier_changesWhenTierChanges() {
        val donor = DonorEntry(
            hash = "abc123",
            name = "Ada",
            tags = listOf("og"),
            donations = listOf(DonationRecord(amountUsd = 2.0, at = "2026-01-01")),
            rosterIndex = 0,
        )
        val ogLore = DonorLore.forDonor(donor, DonorCardStyle.OG)
        assertEquals(ogLore, DonorLore.forDonor(donor, DonorCardStyle.OG))
        assertTrue(DonorLore.poolFor(DonorCardStyle.OG).contains(ogLore))

        val upgraded = donor.copy(
            donations = listOf(DonationRecord(amountUsd = 12.0, at = "2026-02-01")),
        )
        val godlikeStyle = DonorBadgeRules.cardStyle(upgraded)
        assertEquals(DonorCardStyle.GODLIKE, godlikeStyle)
        val newLore = DonorLore.forDonor(upgraded, godlikeStyle)
        assertTrue(DonorLore.poolFor(DonorCardStyle.GODLIKE).contains(newLore))
        assertNotEquals(ogLore, newLore)
    }

    @Test
    fun eachTierHasTenLoreLines() {
        DonorCardStyle.entries.forEach { style ->
            assertEquals(10, DonorLore.poolFor(style).size)
        }
    }
}
