package com.anant.fitbuddy.data.donors

import com.anant.fitbuddy.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Thank-you banner: new vs last-seen, and hide donations older than 6 months before signup.
 * Uses the same predicates as [DonorListRepository.newDisplayableDonors].
 */
class DonorThankYouFilterTest {

    private fun thankYouFilter(
        donors: List<DonorEntry>,
        lastSeen: String?,
        signupDate: String,
    ): List<DonorEntry> {
        val notBefore = DateUtils.addCalendarMonths(signupDate, -DonorListRepository.THANK_YOU_MAX_AGE_MONTHS)
        return donors.filter { entry ->
            entry.normalizedHash.isNotEmpty() &&
                entry.hasDisplayInfo &&
                DonorBadgeRules.hasDonationAfter(entry, lastSeen) &&
                DonorBadgeRules.hasDonationOnOrAfter(entry, notBefore)
        }
    }

    @Test
    fun thankYou_hidesDonationsOlderThanSixMonthsBeforeSignup() {
        val donors = listOf(
            DonorEntry(
                hash = "old",
                name = "Old",
                donations = listOf(DonationRecord(amountUsd = 4.0, at = "2025-06-01")),
            ),
            DonorEntry(
                hash = "recent",
                name = "Recent",
                donations = listOf(DonationRecord(amountUsd = 4.0, at = "2026-01-10")),
            ),
        )
        // Signup mid-2026 → window starts 2025-12-15; Jan donation stays, June 2025 drops.
        val shown = thankYouFilter(donors, lastSeen = null, signupDate = "2026-06-15")
        assertEquals(listOf("Recent"), shown.map { it.displayName })
    }

    @Test
    fun thankYou_januaryDonationVisibleUntilSixMonthsAfter() {
        val donors = listOf(
            DonorEntry(
                hash = "jan",
                name = "Jan",
                donations = listOf(DonationRecord(amountUsd = 4.0, at = "2026-01-01")),
            ),
        )
        assertEquals(
            listOf("Jan"),
            thankYouFilter(donors, null, "2026-07-01").map { it.displayName },
        )
        assertEquals(
            emptyList<String>(),
            thankYouFilter(donors, null, "2026-07-02").map { it.displayName },
        )
    }

    @Test
    fun thankYou_stillRespectsLastSeenFloor() {
        val donors = listOf(
            DonorEntry(
                hash = "a",
                name = "A",
                donations = listOf(DonationRecord(amountUsd = 4.0, at = "2026-02-01")),
            ),
            DonorEntry(
                hash = "b",
                name = "B",
                donations = listOf(DonationRecord(amountUsd = 4.0, at = "2026-03-01")),
            ),
        )
        val shown = thankYouFilter(donors, lastSeen = "2026-02-15", signupDate = "2026-06-01")
        assertEquals(listOf("B"), shown.map { it.displayName })
    }

    @Test
    fun hasDonationOnOrAfter() {
        val donor = DonorEntry(
            hash = "x",
            name = "X",
            donations = listOf(DonationRecord(amountUsd = 1.0, at = "2026-01-01")),
        )
        assertTrue(DonorBadgeRules.hasDonationOnOrAfter(donor, "2026-01-01"))
        assertFalse(DonorBadgeRules.hasDonationOnOrAfter(donor, "2026-01-02"))
        assertTrue(DonorBadgeRules.hasDonationOnOrAfter(donor, null))
    }
}
