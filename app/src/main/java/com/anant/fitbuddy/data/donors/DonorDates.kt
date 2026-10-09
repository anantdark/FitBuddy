package com.anant.fitbuddy.data.donors

/**
 * ISO `yyyy-MM-dd` helpers for donor thank-you / last-seen tracking.
 * Lexicographic compare is valid for zero-padded ISO dates.
 */
object DonorDates {
    private val ISO_DATE = Regex("""^\d{4}-\d{2}-\d{2}$""")

    fun isIsoDate(value: String): Boolean = ISO_DATE.matches(value.trim())

    fun normalizeOrNull(value: String?): String? {
        val trimmed = value?.trim().orEmpty()
        return trimmed.takeIf { isIsoDate(it) }
    }

    /** Returns the later of two ISO dates; nulls sort as oldest. */
    fun max(a: String?, b: String?): String? {
        val na = normalizeOrNull(a)
        val nb = normalizeOrNull(b)
        return when {
            na == null -> nb
            nb == null -> na
            na >= nb -> na
            else -> nb
        }
    }
}
