package com.anant.fitbuddy.data.donors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DonorBadgeRulesTest {

    @Test
    fun moneyBadge_exclusiveHighest() {
        assertNull(DonorBadgeRules.moneyBadge(3.0))
        assertEquals(MoneyBadge.GENEROUS, DonorBadgeRules.moneyBadge(3.01))
        assertEquals(MoneyBadge.GENEROUS, DonorBadgeRules.moneyBadge(5.0))
        assertEquals(MoneyBadge.LEGENDARY, DonorBadgeRules.moneyBadge(5.01))
        assertEquals(MoneyBadge.LEGENDARY, DonorBadgeRules.moneyBadge(10.0))
        assertEquals(MoneyBadge.GODLIKE, DonorBadgeRules.moneyBadge(10.01))
    }

    @Test
    fun badges_stackOgWithMoney_andIgnoreMoneyTags() {
        val donor = DonorEntry(
            hash = "abc",
            name = "Ada",
            tags = listOf("og", "godlike"),
            donations = listOf(DonationRecord(amountUsd = 12.0, at = "2026-01-01")),
        )
        val badges = DonorBadgeRules.badges(donor)
        assertEquals(
            listOf(SupporterBadge.Godlike, SupporterBadge.Og),
            badges,
        )
        assertEquals(DonorCardStyle.GODLIKE, DonorBadgeRules.cardStyle(donor))
    }

    @Test
    fun cardStyle_ogWhenNoMoney() {
        val donor = DonorEntry(
            hash = "abc",
            name = "Ada",
            tags = listOf("og"),
            donations = listOf(DonationRecord(amountUsd = 1.0, at = "2026-01-01")),
        )
        assertEquals(DonorCardStyle.OG, DonorBadgeRules.cardStyle(donor))
    }

    @Test
    fun hasDonationAfter_dateFloor() {
        val donor = DonorEntry(
            hash = "abc",
            name = "Ada",
            donations = listOf(
                DonationRecord(amountUsd = 4.0, at = "2026-01-10"),
                DonationRecord(amountUsd = 4.0, at = "2026-02-01"),
            ),
        )
        assertTrue(DonorBadgeRules.hasDonationAfter(donor, null))
        assertTrue(DonorBadgeRules.hasDonationAfter(donor, "2026-01-10"))
        assertFalse(DonorBadgeRules.hasDonationAfter(donor, "2026-02-01"))
        assertFalse(DonorBadgeRules.hasDonationAfter(donor, "2026-03-01"))
    }

    @Test
    fun newcomerFilter_preservesOrder() {
        val donors = listOf(
            DonorEntry(
                hash = "1",
                name = "A",
                donations = listOf(DonationRecord(amountUsd = 4.0, at = "2026-02-01")),
            ),
            DonorEntry(hash = "2"),
            DonorEntry(
                hash = "3",
                name = "C",
                donations = listOf(DonationRecord(amountUsd = 4.0, at = "2026-01-01")),
            ),
        )
        val floor = "2026-01-15"
        val newcomers = donors.filter {
            it.hasDisplayInfo && DonorBadgeRules.hasDonationAfter(it, floor)
        }
        assertEquals(listOf("A"), newcomers.map { it.displayName })
    }

    @Test
    fun lifetimeSumsDonations() {
        val donor = DonorEntry(
            hash = "x",
            name = "X",
            donations = listOf(
                DonationRecord(amountUsd = 2.0, at = "2026-01-01"),
                DonationRecord(amountUsd = 2.0, at = "2026-02-01"),
            ),
        )
        assertEquals(4.0, DonorBadgeRules.lifetimeUsd(donor), 0.001)
        assertEquals(MoneyBadge.GENEROUS, DonorBadgeRules.moneyBadge(donor))
    }
}
