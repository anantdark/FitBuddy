package com.anant.fitbuddy.data.model

import com.squareup.moshi.JsonClass
import kotlin.math.roundToInt

/** One change-point: targets in force from [from] (yyyy-MM-dd) until the next period. */
@JsonClass(generateAdapter = true)
data class NutritionTargetPeriod(
    val from: String,
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatsG: Int
)

object NutritionTargetHistory {
    const val EPOCH_START = "1970-01-01"

    fun period(
        from: String,
        kcal: Int,
        proteinG: Int,
        carbsG: Int,
        fatsG: Int
    ): NutritionTargetPeriod = NutritionTargetPeriod(
        from = from,
        kcal = kcal,
        proteinG = proteinG,
        carbsG = carbsG,
        fatsG = fatsG
    )

    fun seedFromCurrent(
        kcal: Int,
        proteinG: Int,
        carbsG: Int,
        fatsG: Int,
        from: String = EPOCH_START
    ): List<NutritionTargetPeriod> = listOf(
        period(from, kcal, proteinG, carbsG, fatsG)
    )

    /**
     * Ensures history covers the past, then records [new] effective from [effectiveFromDate].
     * Same-day re-apply replaces that day's entry. No-op (returns [history] unchanged) when
     * values already match the period in force on that date.
     */
    fun recordChange(
        history: List<NutritionTargetPeriod>,
        effectiveFromDate: String,
        kcal: Int,
        proteinG: Int,
        carbsG: Int,
        fatsG: Int,
        fallbackCurrent: NutritionTargetPeriod?
    ): List<NutritionTargetPeriod> {
        val base = ensureSeeded(history, fallbackCurrent)
        val current = forDate(base, effectiveFromDate, fallbackCurrent)
        if (
            current != null &&
            current.kcal == kcal &&
            current.proteinG == proteinG &&
            current.carbsG == carbsG &&
            current.fatsG == fatsG
        ) {
            return base
        }
        val updated = base.filterNot { it.from == effectiveFromDate } +
            period(effectiveFromDate, kcal, proteinG, carbsG, fatsG)
        return updated.sortedBy { it.from }
    }

    fun ensureSeeded(
        history: List<NutritionTargetPeriod>,
        fallbackCurrent: NutritionTargetPeriod?
    ): List<NutritionTargetPeriod> {
        if (history.isNotEmpty()) return history.sortedBy { it.from }
        if (fallbackCurrent == null) return emptyList()
        return seedFromCurrent(
            kcal = fallbackCurrent.kcal,
            proteinG = fallbackCurrent.proteinG,
            carbsG = fallbackCurrent.carbsG,
            fatsG = fallbackCurrent.fatsG,
            from = EPOCH_START
        )
    }

    fun forDate(
        history: List<NutritionTargetPeriod>,
        date: String,
        fallback: NutritionTargetPeriod? = null
    ): NutritionTargetPeriod? {
        val sorted = history.sortedBy { it.from }
        val match = sorted.lastOrNull { it.from <= date }
        return match ?: fallback
    }

    fun averageTargetCalories(
        history: List<NutritionTargetPeriod>,
        dates: List<String>,
        fallback: NutritionTargetPeriod?
    ): Int? {
        if (dates.isEmpty()) return null
        val values = dates.mapNotNull { date ->
            forDate(history, date, fallback)?.kcal
        }
        if (values.isEmpty()) return null
        return values.average().roundToInt()
    }
}
