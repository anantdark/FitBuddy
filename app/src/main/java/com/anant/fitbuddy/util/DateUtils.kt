package com.anant.fitbuddy.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Single source of truth for the "YYYY-MM-DD" date-string format persisted in Room.
 * Both the repository (when writing logs) and the ViewModel (when reading "today")
 * must agree on this format, otherwise date-grouped queries silently return nothing.
 */
object DateUtils {

    private const val DATE_PATTERN = "yyyy-MM-dd"
    private const val DISPLAY_PATTERN = "EEE, MMM d"
    const val ROLLING_WEEK_DAYS = 7

    private fun formatter(): SimpleDateFormat =
        SimpleDateFormat(DATE_PATTERN, Locale.US)

    private fun displayFormatter(): SimpleDateFormat =
        SimpleDateFormat(DISPLAY_PATTERN, Locale.getDefault())

    fun format(timestamp: Long): String = formatter().format(Date(timestamp))

    /**
     * The logical "today" string for the app, respecting [dayChangeHour].
     *
     * When [dayChangeHour] is 0 (default / midnight) this is identical to the plain
     * calendar day. When non-zero (e.g. 3), the calendar day does not roll over until
     * [dayChangeHour] o'clock local time — so a log at 2 AM with offset 3 still lands
     * on the previous day's date string.
     */
    fun today(dayChangeHour: Int = 0): String {
        val offsetMs = dayChangeHour.coerceIn(0, 23) * 60L * 60L * 1000L
        return format(System.currentTimeMillis() - offsetMs)
    }

    fun parse(dateString: String): Date =
        formatter().parse(dateString) ?: error("Invalid date: $dateString")

    /**
     * Rolling window of [ROLLING_WEEK_DAYS] local calendar days ending on [endInclusive]
     * (oldest → newest).
     */
    fun rollingWeekDates(endInclusive: String = today()): List<String> {
        val end = Calendar.getInstance().apply {
            time = parse(endInclusive)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (ROLLING_WEEK_DAYS - 1 downTo 0).map { daysBack ->
            (end.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysBack) }
                .let { format(it.timeInMillis) }
        }
    }

    /** Add [days] (may be negative) to a `yyyy-MM-dd` local calendar day. */
    fun addDays(dateString: String, days: Int): String {
        val cal = Calendar.getInstance().apply {
            time = parse(dateString)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, days)
        }
        return format(cal.timeInMillis)
    }

    /**
     * Compact range for a week strip, e.g. "Jul 6 – 12" or "Jun 30 – Jul 6".
     * Empty [dates] → "".
     */
    fun weekRangeLabel(dates: List<String>): String {
        if (dates.isEmpty()) return ""
        val start = Calendar.getInstance().apply { time = parse(dates.first()) }
        val end = Calendar.getInstance().apply { time = parse(dates.last()) }
        val startLabel = SimpleDateFormat("MMM d", Locale.getDefault()).format(start.time)
        val sameMonth =
            start.get(Calendar.YEAR) == end.get(Calendar.YEAR) &&
                start.get(Calendar.MONTH) == end.get(Calendar.MONTH)
        val endLabel = if (sameMonth) {
            SimpleDateFormat("d", Locale.getDefault()).format(end.time)
        } else {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(end.time)
        }
        return "$startLabel – $endLabel"
    }

    /**
     * Same wall-clock time as [clockMillis], relocated onto the local calendar day of
     * [dateString]. Used so new logs on a past day keep sensible within-day ordering.
     */
    fun timestampOnDate(
        dateString: String,
        clockMillis: Long = System.currentTimeMillis()
    ): Long {
        val clock = Calendar.getInstance().apply { timeInMillis = clockMillis }
        return Calendar.getInstance().apply {
            time = parse(dateString)
            set(Calendar.HOUR_OF_DAY, clock.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, clock.get(Calendar.MINUTE))
            set(Calendar.SECOND, clock.get(Calendar.SECOND))
            set(Calendar.MILLISECOND, clock.get(Calendar.MILLISECOND))
        }.timeInMillis
    }

    /** "Today" / "Yesterday" / "Wed, Jul 16" relative to [realToday]. */
    fun dayTitle(dateString: String, realToday: String = today()): String {
        val yesterday = rollingWeekDates(realToday).getOrNull(ROLLING_WEEK_DAYS - 2)
        return when (dateString) {
            realToday -> "Today"
            yesterday -> "Yesterday"
            else -> displayFormatter().format(parse(dateString))
        }
    }

    fun displayDateSubtitle(dateString: String): String =
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(parse(dateString))

    /** Compact date for the day navigator, e.g. "Fri, Apr 21". */
    fun displayDateCompact(dateString: String): String =
        displayFormatter().format(parse(dateString))

    /** Short weekday for day pills, e.g. "Mon". */
    fun shortWeekday(dateString: String): String =
        SimpleDateFormat("EEE", Locale.getDefault()).format(parse(dateString))

    /** Day-of-month for day pills, e.g. "19". */
    fun dayOfMonth(dateString: String): String =
        SimpleDateFormat("d", Locale.getDefault()).format(parse(dateString))

    /** `yyyy-MM` key for [dateString] (or today). */
    fun yearMonth(dateString: String = today()): String = dateString.take(7)

    /** First and last `yyyy-MM-dd` of the calendar month [yearMonth] (`yyyy-MM`). */
    fun monthBounds(yearMonth: String): Pair<String, String> {
        val start = Calendar.getInstance().apply {
            time = parse("$yearMonth-01")
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        return format(start.timeInMillis) to format(end.timeInMillis)
    }

    /** Shift a `yyyy-MM` key by [deltaMonths] (may be negative). */
    fun addMonths(yearMonth: String, deltaMonths: Int): String {
        val cal = Calendar.getInstance().apply {
            time = parse("$yearMonth-01")
            set(Calendar.HOUR_OF_DAY, 12)
            add(Calendar.MONTH, deltaMonths)
        }
        return SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
    }

    /** Display label for a `yyyy-MM` key, e.g. "Jul 2026". */
    fun monthLabel(yearMonth: String): String =
        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(parse("$yearMonth-01"))

    /** Rolling 30-day bounds ending on [endInclusive]: start = endInclusive − 29 days. */
    fun rolling30DayBounds(endInclusive: String = today()): Pair<String, String> =
        addDays(endInclusive, -29) to endInclusive

    /** Compact label for a 30-day range, e.g. "Jul 3 – Aug 1" or "Jun 2 – Jul 1, 2025". */
    fun rolling30DayLabel(endInclusive: String): String {
        val start = addDays(endInclusive, -29)
        val startDate = parse(start)
        val endDate = parse(endInclusive)
        val startCal = Calendar.getInstance().apply { time = startDate }
        val endCal = Calendar.getInstance().apply { time = endDate }
        val sameYear = startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR)
        val startFmt = if (sameYear) "MMM d" else "MMM d, yyyy"
        val endFmt = "MMM d"
        val startLabel = SimpleDateFormat(startFmt, Locale.getDefault()).format(startDate)
        val endLabel = SimpleDateFormat(endFmt, Locale.getDefault()).format(endDate)
        return "$startLabel – $endLabel"
    }
}
