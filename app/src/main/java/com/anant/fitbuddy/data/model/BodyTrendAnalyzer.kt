package com.anant.fitbuddy.data.model

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

enum class BodyTrendQuality {
    NONE,
    SPARSE,
    INCONSISTENT,
    SUFFICIENT
}

data class BodyTrendReading(
    val epochDay: Long,
    val weightKg: Double,
    val bodyFatPct: Double? = null,
    val muscleMassKg: Double? = null
)

data class BodyTrendEvidence(
    val quality: BodyTrendQuality,
    val windowDays: Int,
    val sampleCount: Int,
    val spanDays: Int,
    val latestAgeDays: Int?,
    val reason: String,
    val weightChangeKg: Double? = null,
    val weightChangePerWeekKg: Double? = null,
    val weightChangePerWeekPct: Double? = null,
    val bodyFatChangePct: Double? = null,
    val muscleMassChangeKg: Double? = null
)

data class NutritionTrendEvidence(
    val windowDays: Int,
    val loggedDays: Int,
    val averageCalories: Int?
) {
    val hasSufficientCoverage: Boolean
        get() = loggedDays >= MIN_LOGGED_DAYS && averageCalories != null

    companion object {
        const val MIN_LOGGED_DAYS = 21
    }
}

object BodyTrendAnalyzer {
    const val DEFAULT_WINDOW_DAYS = 42

    fun analyze(
        readings: List<BodyTrendReading>,
        todayEpochDay: Long,
        windowDays: Int = DEFAULT_WINDOW_DAYS
    ): BodyTrendEvidence {
        val startEpochDay = todayEpochDay - (windowDays - 1)
        val daily = readings
            .asSequence()
            .filter { it.epochDay in startEpochDay..todayEpochDay }
            .filter { it.weightKg.isFinite() && it.weightKg in 25.0..400.0 }
            .groupBy { it.epochDay }
            .toSortedMap()
            .map { (day, values) ->
                DailyReading(
                    epochDay = day,
                    weightKg = median(values.map { it.weightKg }),
                    bodyFatPct = medianOrNull(
                        values.mapNotNull { it.bodyFatPct?.takeIf { value ->
                            value.isFinite() && value in 2.0..75.0
                        } }
                    ),
                    muscleMassKg = medianOrNull(
                        values.mapNotNull { it.muscleMassKg?.takeIf { value ->
                            value.isFinite() && value in 5.0..200.0
                        } }
                    )
                )
            }

        if (daily.isEmpty()) {
            return BodyTrendEvidence(
                quality = BodyTrendQuality.NONE,
                windowDays = windowDays,
                sampleCount = 0,
                spanDays = 0,
                latestAgeDays = null,
                reason = "No recent body readings"
            )
        }

        val sampleCount = daily.size
        val spanDays = (daily.last().epochDay - daily.first().epochDay).toInt()
        val latestAgeDays = (todayEpochDay - daily.last().epochDay).toInt()
        fun sparse(reason: String) = BodyTrendEvidence(
            quality = BodyTrendQuality.SPARSE,
            windowDays = windowDays,
            sampleCount = sampleCount,
            spanDays = spanDays,
            latestAgeDays = latestAgeDays,
            reason = reason
        )

        if (sampleCount < MIN_SAMPLE_DAYS) {
            return sparse("Need at least $MIN_SAMPLE_DAYS measurement days")
        }
        if (spanDays < MIN_SPAN_DAYS) {
            return sparse("Readings must span at least $MIN_SPAN_DAYS days")
        }
        if (latestAgeDays > MAX_LATEST_AGE_DAYS) {
            return sparse("Latest reading is too old for personalization")
        }
        val largestGap = daily.zipWithNext { first, second ->
            (second.epochDay - first.epochDay).toInt()
        }.maxOrNull() ?: 0
        if (largestGap > MAX_GAP_DAYS) {
            return sparse("Readings are too irregular for a reliable trend")
        }

        val weightPoints = daily.map { it.epochDay.toDouble() to it.weightKg }
        val weightSlope = theilSenSlope(weightPoints) ?: return sparse("Weight trend is unavailable")
        val weightIntercept = median(weightPoints.map { (day, weight) -> weight - weightSlope * day })
        val residualMad = median(
            weightPoints.map { (day, weight) -> abs(weight - (weightSlope * day + weightIntercept)) }
        )
        val medianWeight = median(daily.map { it.weightKg })
        val allowedNoiseKg = max(MIN_ALLOWED_WEIGHT_NOISE_KG, medianWeight * MAX_WEIGHT_NOISE_FRACTION)
        if (residualMad > allowedNoiseKg) {
            return BodyTrendEvidence(
                quality = BodyTrendQuality.INCONSISTENT,
                windowDays = windowDays,
                sampleCount = sampleCount,
                spanDays = spanDays,
                latestAgeDays = latestAgeDays,
                reason = "Weight readings vary too much for a reliable trend"
            )
        }

        val weightChangeKg = weightSlope * spanDays
        val weightChangePerWeekKg = weightSlope * 7.0
        val weightChangePerWeekPct = weightChangePerWeekKg / medianWeight * 100.0
        val bodyFatTrend = metricChange(
            daily.mapNotNull { reading ->
                reading.bodyFatPct?.let { reading.epochDay.toDouble() to it }
            },
            todayEpochDay = todayEpochDay,
            maxResidualMad = 2.0
        )
        val muscleTrend = metricChange(
            daily.mapNotNull { reading ->
                reading.muscleMassKg?.let { reading.epochDay.toDouble() to it }
            },
            todayEpochDay = todayEpochDay,
            maxResidualMad = max(0.75, medianWeight * 0.01)
        )

        return BodyTrendEvidence(
            quality = BodyTrendQuality.SUFFICIENT,
            windowDays = windowDays,
            sampleCount = sampleCount,
            spanDays = spanDays,
            latestAgeDays = latestAgeDays,
            reason = "Consistent repeated readings support trend personalization",
            weightChangeKg = roundTo(weightChangeKg, 2),
            weightChangePerWeekKg = roundTo(weightChangePerWeekKg, 2),
            weightChangePerWeekPct = roundTo(weightChangePerWeekPct, 2),
            bodyFatChangePct = bodyFatTrend?.let { roundTo(it, 2) },
            muscleMassChangeKg = muscleTrend?.let { roundTo(it, 2) }
        )
    }

    private fun metricChange(
        points: List<Pair<Double, Double>>,
        todayEpochDay: Long,
        maxResidualMad: Double
    ): Double? {
        if (points.size < MIN_COMPOSITION_SAMPLES) return null
        val sorted = points.sortedBy { it.first }
        val spanDays = sorted.last().first - sorted.first().first
        if (spanDays < MIN_SPAN_DAYS) return null
        if (todayEpochDay - sorted.last().first > MAX_LATEST_AGE_DAYS.toDouble()) return null
        val largestGap = sorted.zipWithNext { first, second -> second.first - first.first }
            .maxOrNull() ?: 0.0
        if (largestGap > MAX_GAP_DAYS.toDouble()) return null
        val slope = theilSenSlope(sorted) ?: return null
        val intercept = median(sorted.map { (day, value) -> value - slope * day })
        val residualMad = median(
            sorted.map { (day, value) -> abs(value - (slope * day + intercept)) }
        )
        return (slope * spanDays).takeIf { residualMad <= maxResidualMad }
    }

    private fun theilSenSlope(points: List<Pair<Double, Double>>): Double? {
        if (points.size < 2) return null
        val slopes = buildList {
            points.forEachIndexed { firstIndex, first ->
                for (secondIndex in firstIndex + 1 until points.size) {
                    val second = points[secondIndex]
                    val dayDelta = second.first - first.first
                    if (dayDelta > 0.0) add((second.second - first.second) / dayDelta)
                }
            }
        }
        return slopes.takeIf { it.isNotEmpty() }?.let(::median)
    }

    private fun medianOrNull(values: List<Double>): Double? =
        values.takeIf { it.isNotEmpty() }?.let(::median)

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }

    private fun roundTo(value: Double, decimals: Int): Double {
        val scale = if (decimals == 2) 100.0 else 10.0
        return round(value * scale) / scale
    }

    private data class DailyReading(
        val epochDay: Long,
        val weightKg: Double,
        val bodyFatPct: Double?,
        val muscleMassKg: Double?
    )

    private const val MIN_SAMPLE_DAYS = 4
    private const val MIN_COMPOSITION_SAMPLES = 3
    private const val MIN_SPAN_DAYS = 14
    private const val MAX_LATEST_AGE_DAYS = 10
    private const val MAX_GAP_DAYS = 14
    private const val MIN_ALLOWED_WEIGHT_NOISE_KG = 0.4
    private const val MAX_WEIGHT_NOISE_FRACTION = 0.006
}

object TargetPlanPersonalizer {
    const val FORMULA_CANDIDATE = "FORMULA"
    const val TREND_CANDIDATE = "TREND_ADJUSTED"

    fun buildOptions(
        basePlan: TargetPlanResponse,
        input: HealthTargetInput,
        bodyTrend: BodyTrendEvidence,
        nutritionTrend: NutritionTrendEvidence
    ): TargetPlanOptions {
        val formulaPlan = basePlan.withTrendEvidence(
            bodyTrend = bodyTrend,
            nutritionTrend = nutritionTrend,
            candidateId = FORMULA_CANDIDATE,
            adjustmentCalories = 0
        )
        val adjustment = recommendedAdjustment(
            plan = formulaPlan,
            bodyTrend = bodyTrend,
            nutritionTrend = nutritionTrend
        )
        if (adjustment == 0) {
            return TargetPlanOptions(
                candidates = listOf(TargetPlanCandidate(FORMULA_CANDIDATE, formulaPlan)),
                defaultCandidateId = FORMULA_CANDIDATE
            )
        }

        val adjustedBase = HealthTargetCalculator.adjustCalories(
            input = input,
            plan = formulaPlan,
            requestedAdjustmentCalories = adjustment
        )
        val actualAdjustment = adjustedBase.dailyTargetCalories - formulaPlan.dailyTargetCalories
        if (actualAdjustment == 0) {
            return TargetPlanOptions(
                candidates = listOf(TargetPlanCandidate(FORMULA_CANDIDATE, formulaPlan)),
                defaultCandidateId = FORMULA_CANDIDATE
            )
        }
        val adjustedPlan = adjustedBase.copy(
            personalizationCandidateId = TREND_CANDIDATE,
            bodyTrendAdjustmentCalories = actualAdjustment
        )
        return TargetPlanOptions(
            candidates = listOf(
                TargetPlanCandidate(FORMULA_CANDIDATE, formulaPlan),
                TargetPlanCandidate(TREND_CANDIDATE, adjustedPlan)
            ),
            defaultCandidateId = TREND_CANDIDATE
        )
    }

    private fun recommendedAdjustment(
        plan: TargetPlanResponse,
        bodyTrend: BodyTrendEvidence,
        nutritionTrend: NutritionTrendEvidence
    ): Int {
        if (bodyTrend.quality != BodyTrendQuality.SUFFICIENT) return 0
        if (!nutritionTrend.hasSufficientCoverage) return 0
        val averageCalories = nutritionTrend.averageCalories ?: return 0
        val intakeTolerance = max(150.0, plan.dailyTargetCalories * 0.10)
        if (abs(averageCalories - plan.dailyTargetCalories) > intakeTolerance) return 0
        val weeklyRatePct = bodyTrend.weightChangePerWeekPct ?: return 0

        return when (plan.recommendedGoal) {
            "LOSE_WEIGHT" -> when {
                weeklyRatePct >= -0.10 -> -CALORIE_STEP
                weeklyRatePct <= -1.00 -> CALORIE_STEP
                else -> 0
            }
            "GAIN_MUSCLE" -> when {
                weeklyRatePct <= 0.05 -> CALORIE_STEP
                weeklyRatePct >= 0.50 -> -CALORIE_STEP
                else -> 0
            }
            else -> when {
                weeklyRatePct <= -0.50 -> CALORIE_STEP
                weeklyRatePct >= 0.50 -> -CALORIE_STEP
                else -> 0
            }
        }
    }

    private fun TargetPlanResponse.withTrendEvidence(
        bodyTrend: BodyTrendEvidence,
        nutritionTrend: NutritionTrendEvidence,
        candidateId: String,
        adjustmentCalories: Int
    ): TargetPlanResponse = copy(
        personalizationCandidateId = candidateId,
        bodyTrendStatus = bodyTrend.quality.name,
        bodyTrendReason = bodyTrend.reason,
        bodyTrendWindowDays = bodyTrend.windowDays,
        bodyTrendSampleCount = bodyTrend.sampleCount,
        bodyTrendSpanDays = bodyTrend.spanDays,
        bodyTrendWeightChangeKg = bodyTrend.weightChangeKg,
        bodyTrendWeeklyChangePct = bodyTrend.weightChangePerWeekPct,
        bodyTrendBodyFatChangePct = bodyTrend.bodyFatChangePct,
        bodyTrendMuscleMassChangeKg = bodyTrend.muscleMassChangeKg,
        bodyTrendFoodDaysLogged = nutritionTrend.loggedDays,
        bodyTrendAdjustmentCalories = adjustmentCalories
    )

    private const val CALORIE_STEP = 100
}
