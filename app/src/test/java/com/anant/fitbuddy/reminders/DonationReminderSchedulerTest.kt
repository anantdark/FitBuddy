package com.anant.fitbuddy.reminders

import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.reminders.DonationReminderScheduler.DAY_MS
import com.anant.fitbuddy.reminders.DonationReminderScheduler.DIALOG_HOUR
import com.anant.fitbuddy.reminders.DonationReminderScheduler.MAX_INTERVAL_DAYS
import com.anant.fitbuddy.reminders.DonationReminderScheduler.MIN_INTERVAL_DAYS
import com.anant.fitbuddy.reminders.DonationReminderScheduler.MIN_GAP_MS
import com.anant.fitbuddy.reminders.DonationReminderScheduler.NOTIF_HOUR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DonationReminderSchedulerTest {

    @Test
    fun `interval days always in 3 to 8 inclusive`() {
        val samples = listOf(
            0L, 1L, 42L, 1_700_000_000_000L, Long.MAX_VALUE, Long.MIN_VALUE,
            System.currentTimeMillis(),
        ) + (0 until 500).map { it * 86_400_000L + 12_345L }
        for (ts in samples) {
            val days = DonationReminderScheduler.intervalDaysFor(ts)
            assertTrue("days=$days for ts=$ts", days in MIN_INTERVAL_DAYS..MAX_INTERVAL_DAYS)
        }
    }

    @Test
    fun `interval days stable for same last nudge`() {
        val last = 1_720_000_000_000L
        val a = DonationReminderScheduler.intervalDaysFor(last)
        val b = DonationReminderScheduler.intervalDaysFor(last)
        assertEquals(a, b)
        assertEquals(
            DonationReminderScheduler.intervalMsFor(last),
            a * DAY_MS,
        )
    }

    @Test
    fun `interval days covers full span across many timestamps`() {
        val seen = mutableSetOf<Int>()
        var t = 1_700_000_000_000L
        repeat(2_000) {
            seen += DonationReminderScheduler.intervalDaysFor(t)
            t += 3_601_000L
        }
        assertEquals((MIN_INTERVAL_DAYS..MAX_INTERVAL_DAYS).toSet(), seen)
    }

    @Test
    fun `isDue false before interval and true after`() {
        val last = 1_720_000_000_000L
        val gap = DonationReminderScheduler.intervalMsFor(last)
        val settings = AppSettings(
            donationReminderEnabled = true,
            donationLastNudgeAt = last,
        )
        assertFalse(DonationReminderScheduler.isDue(settings, last + gap - 1))
        assertTrue(DonationReminderScheduler.isDue(settings, last + gap))
        assertTrue(DonationReminderScheduler.isDue(settings, last + gap + DAY_MS))
    }

    @Test
    fun `isDue false when disabled or unseeded`() {
        val last = 1_720_000_000_000L
        val gap = DonationReminderScheduler.intervalMsFor(last)
        assertFalse(
            DonationReminderScheduler.isDue(
                AppSettings(donationReminderEnabled = false, donationLastNudgeAt = last),
                last + gap + DAY_MS,
            )
        )
        assertFalse(
            DonationReminderScheduler.isDue(
                AppSettings(donationReminderEnabled = true, donationLastNudgeAt = 0L),
                last + gap + DAY_MS,
            )
        )
    }

    @Test
    fun `nextMorningTrigger uses same interval as isDue`() {
        val last = atLocalHour(1_720_000_000_000L, hour = 12)
        val settings = AppSettings(
            donationReminderEnabled = true,
            donationLastNudgeAt = last,
        )
        val gap = DonationReminderScheduler.intervalMsFor(last)
        val dueAt = last + gap
        val trigger = DonationReminderScheduler.nextMorningTriggerMillis(settings, nowMillis = last)
        assertTrue(trigger >= dueAt)
        assertEquals(NOTIF_HOUR, hourOf(trigger))
        // Before due: not due. At trigger time: due.
        assertFalse(DonationReminderScheduler.isDue(settings, dueAt - 1))
        assertTrue(DonationReminderScheduler.isDue(settings, trigger))
    }

    @Test
    fun `seed first run due only after at least 3 days`() {
        val now = 1_720_000_000_000L
        val seed = DonationReminderScheduler.seedFirstRunNudgeAt(now)
        assertEquals(now, seed)
        val settings = AppSettings(
            donationReminderEnabled = true,
            donationLastNudgeAt = seed,
        )
        val gap = DonationReminderScheduler.intervalMsFor(seed)
        assertTrue(gap in (MIN_INTERVAL_DAYS * DAY_MS)..(MAX_INTERVAL_DAYS * DAY_MS))
        assertFalse(DonationReminderScheduler.isDue(settings, now + MIN_INTERVAL_DAYS * DAY_MS - 1))
        // May still be early if this seed rolled >3 days — only assert due once full gap elapsed.
        assertFalse(DonationReminderScheduler.isDue(settings, now + gap - 1))
        assertTrue(DonationReminderScheduler.isDue(settings, now + gap))
    }

    @Test
    fun `morning notification blocked after already notified this cycle`() {
        val last = 1_700_000_000_000L
        val gap = DonationReminderScheduler.intervalMsFor(last)
        val morning = atLocalHour(last + gap + DAY_MS, hour = NOTIF_HOUR)
        val settings = AppSettings(
            donationReminderEnabled = true,
            donationLastNudgeAt = last,
            donationLastNotifAt = morning - 60_000L,
        )
        assertTrue(DonationReminderScheduler.isDue(settings, morning))
        assertFalse(DonationReminderScheduler.canShowMorningNotification(settings, morning))
    }

    @Test
    fun `evening dialog blocked after already shown this cycle`() {
        val last = 1_700_000_000_000L
        val gap = DonationReminderScheduler.intervalMsFor(last)
        val evening = atLocalHour(last + gap + DAY_MS, hour = DIALOG_HOUR)
        val settings = AppSettings(
            donationReminderEnabled = true,
            donationLastNudgeAt = last,
            donationLastDialogAt = evening - 60_000L,
        )
        assertFalse(DonationReminderScheduler.canShowEveningDialog(settings, evening))
    }

    @Test
    fun `notif and dialog respect min gap`() {
        val last = 1_700_000_000_000L
        val gap = DonationReminderScheduler.intervalMsFor(last)
        val evening = atLocalHour(last + gap + DAY_MS, hour = DIALOG_HOUR)
        val settings = AppSettings(
            donationReminderEnabled = true,
            donationLastNudgeAt = last,
            donationLastNotifAt = evening - (MIN_GAP_MS / 2),
        )
        assertFalse(DonationReminderScheduler.canShowEveningDialog(settings, evening))
    }

    private fun hourOf(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)

    private fun atLocalHour(approxMillis: Long, hour: Int): Long {
        return Calendar.getInstance().apply {
            timeInMillis = approxMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
