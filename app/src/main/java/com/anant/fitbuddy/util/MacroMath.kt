package com.anant.fitbuddy.util

import kotlin.math.roundToLong

/** Macros (protein/carbs/fats, grams) are stored and shown to one decimal place. */
fun Double.roundMacro(): Double = (this * 10.0).roundToLong() / 10.0

/** Food / ingredient quantities are stored and shown to one decimal place. */
fun Double.roundQty(): Double = roundMacro()

/** "12" for whole values, "12.5" otherwise. */
fun Double.formatMacro(): String {
    val r = roundMacro()
    return if (r % 1.0 == 0.0) r.toLong().toString() else String.format(java.util.Locale.US, "%.1f", r)
}

/** Same formatting as [formatMacro] — quantities share one-decimal precision. */
fun Double.formatQty(): String = formatMacro()

/** Keeps digits and one decimal point, with at most one fractional digit. */
fun String.sanitizeMacroInput(maxIntDigits: Int = 4): String {
    val s = replace(',', '.')
    val dot = s.indexOf('.')
    if (dot < 0) return s.filter { it.isDigit() }.take(maxIntDigits)
    val whole = s.substring(0, dot).filter { it.isDigit() }.take(maxIntDigits)
    val frac = s.substring(dot + 1).filter { it.isDigit() }.take(1)
    return "$whole.$frac"
}

/** Same sanitizer as macros — qty fields allow one decimal place. */
fun String.sanitizeQtyInput(maxIntDigits: Int = 4): String = sanitizeMacroInput(maxIntDigits)

fun String.toMacroOrNull(): Double? = replace(',', '.').toDoubleOrNull()?.roundMacro()

fun String.toQtyOrNull(): Double? = toMacroOrNull()

/** Text for an editable field: blank for zero, otherwise "12" / "12.5". */
fun Double.macroFieldText(): String = if (this == 0.0) "" else formatMacro()

fun Double.qtyFieldText(): String = macroFieldText()
