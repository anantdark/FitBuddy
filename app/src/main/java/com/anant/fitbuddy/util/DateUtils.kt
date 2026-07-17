package com.anant.fitbuddy.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single source of truth for the "YYYY-MM-DD" date-string format persisted in Room.
 * Both the repository (when writing logs) and the ViewModel (when reading "today")
 * must agree on this format, otherwise date-grouped queries silently return nothing.
 */
object DateUtils {

    private const val DATE_PATTERN = "yyyy-MM-dd"

    private fun formatter(): SimpleDateFormat =
        SimpleDateFormat(DATE_PATTERN, Locale.US)

    fun format(timestamp: Long): String = formatter().format(Date(timestamp))

    fun today(): String = format(System.currentTimeMillis())
}
