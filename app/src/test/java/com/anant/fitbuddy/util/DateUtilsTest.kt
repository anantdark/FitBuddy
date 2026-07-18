package com.anant.fitbuddy.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class DateUtilsTest {

    @Test
    fun `format and today use yyyy-MM-dd`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 19, 15, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals("2026-07-19", DateUtils.format(cal.timeInMillis))
        assertTrue(DateUtils.today().matches(Regex("""\d{4}-\d{2}-\d{2}""")))
    }

    @Test
    fun `rollingWeekDates returns 7 days oldest to newest ending on given day`() {
        val week = DateUtils.rollingWeekDates("2026-07-19")
        assertEquals(DateUtils.ROLLING_WEEK_DAYS, week.size)
        assertEquals("2026-07-13", week.first())
        assertEquals("2026-07-19", week.last())
        week.zipWithNext().forEach { (a, b) ->
            assertEquals(b, DateUtils.addDays(a, 1))
        }
    }

    @Test
    fun `addDays crosses month and year boundaries`() {
        assertEquals("2026-08-01", DateUtils.addDays("2026-07-31", 1))
        assertEquals("2026-12-31", DateUtils.addDays("2027-01-01", -1))
        assertEquals("2026-07-19", DateUtils.addDays("2026-07-19", 0))
    }

    @Test
    fun `weekRangeLabel same month and cross month`() {
        val prev = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("", DateUtils.weekRangeLabel(emptyList()))
            assertEquals(
                "Jul 13 – 19",
                DateUtils.weekRangeLabel(DateUtils.rollingWeekDates("2026-07-19"))
            )
            assertEquals(
                "Jun 28 – Jul 4",
                DateUtils.weekRangeLabel(
                    listOf("2026-06-28", "2026-06-29", "2026-06-30", "2026-07-01", "2026-07-02", "2026-07-03", "2026-07-04")
                )
            )
        } finally {
            Locale.setDefault(prev)
        }
    }

    @Test
    fun `timestampOnDate relocates wall clock onto target calendar day`() {
        val clock = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 19, 14, 45, 30)
            set(Calendar.MILLISECOND, 123)
        }.timeInMillis

        val relocated = DateUtils.timestampOnDate("2026-07-10", clock)
        assertEquals("2026-07-10", DateUtils.format(relocated))

        val relocatedCal = Calendar.getInstance().apply { timeInMillis = relocated }
        val clockCal = Calendar.getInstance().apply { timeInMillis = clock }
        assertEquals(clockCal.get(Calendar.HOUR_OF_DAY), relocatedCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(clockCal.get(Calendar.MINUTE), relocatedCal.get(Calendar.MINUTE))
        assertEquals(clockCal.get(Calendar.SECOND), relocatedCal.get(Calendar.SECOND))
        assertEquals(clockCal.get(Calendar.MILLISECOND), relocatedCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun `dayTitle today yesterday and older`() {
        val prev = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("Today", DateUtils.dayTitle("2026-07-19", realToday = "2026-07-19"))
            assertEquals("Yesterday", DateUtils.dayTitle("2026-07-18", realToday = "2026-07-19"))
            assertEquals("Mon, Jul 13", DateUtils.dayTitle("2026-07-13", realToday = "2026-07-19"))
        } finally {
            Locale.setDefault(prev)
        }
    }

    @Test
    fun `yearMonth monthBounds and addMonths`() {
        assertEquals("2026-07", DateUtils.yearMonth("2026-07-19"))
        assertEquals("2026-07-01" to "2026-07-31", DateUtils.monthBounds("2026-07"))
        assertEquals("2026-02-01" to "2026-02-28", DateUtils.monthBounds("2026-02"))
        assertEquals("2024-02-01" to "2024-02-29", DateUtils.monthBounds("2024-02"))
        assertEquals("2026-08", DateUtils.addMonths("2026-07", 1))
        assertEquals("2025-12", DateUtils.addMonths("2026-01", -1))
    }

    @Test
    fun `display helpers return expected US locale shapes`() {
        val prev = Locale.getDefault()
        val prevTz = TimeZone.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("Friday, Jul 17", DateUtils.displayDateSubtitle("2026-07-17"))
            assertEquals("Fri, Jul 17", DateUtils.displayDateCompact("2026-07-17"))
            assertEquals("Fri", DateUtils.shortWeekday("2026-07-17"))
            assertEquals("17", DateUtils.dayOfMonth("2026-07-17"))
            assertEquals("Jul 2026", DateUtils.monthLabel("2026-07"))
        } finally {
            Locale.setDefault(prev)
            TimeZone.setDefault(prevTz)
        }
    }
}
