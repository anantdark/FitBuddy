package com.anant.fitbuddy.data.model

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class HealthTargetInput(
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val sex: String?,
    val activityLevel: String,
    val statedGoal: String,
    val currentCalories: Int,
    val currentProteinG: Int,
    val currentCarbsG: Int,
    val currentFatsG: Int,
    val currentTargetsCalculated: Boolean
)

/** Deterministic adult target estimates. AI may explain these values but never changes them. */
object HealthTargetCalculator {
    const val HEALTHY_BMI_MIN = 18.5
    const val HEALTHY_BMI_MAX = 24.9

    fun calculate(input: HealthTargetInput): TargetPlanResponse {
        require(input.age >= 18) {
            "Automated calorie and weight targets are available only for adults 18+. " +
                "For children and teens, ask a qualified health professional."
        }
        require(input.age <= 120) { "Enter an age between 18 and 120" }
        require(input.heightCm in 100.0..250.0) { "Enter a valid height between 100 and 250 cm" }
        require(input.weightKg in 25.0..400.0) { "Enter a valid weight between 25 and 400 kg" }

        val bmi = requireNotNull(bodyMassIndex(input.weightKg, input.heightCm))
        val healthyRange = requireNotNull(healthyWeightRange(input.heightCm))
        val goal = resolveGoal(input.statedGoal, bmi)
        val sexAdjustment = when (input.sex?.trim()?.uppercase()) {
            "MALE" -> 5.0
            "FEMALE" -> -161.0
            else -> -78.0
        }
        val restingCalories = 10.0 * input.weightKg +
            6.25 * input.heightCm -
            5.0 * input.age +
            sexAdjustment
        val activityFactor = activityFactor(input.activityLevel)
        val maintenanceCalories = restingCalories * activityFactor

        val unroundedTarget = when (goal) {
            "LOSE_WEIGHT" -> {
                val deficit = (maintenanceCalories * 0.15).coerceIn(250.0, 500.0)
                maintenanceCalories - deficit
            }
            "GAIN_MUSCLE" -> {
                val surplus = (maintenanceCalories * 0.08).coerceIn(150.0, 300.0)
                maintenanceCalories + surplus
            }
            else -> maintenanceCalories
        }
        val idealCalories = roundToStep(max(MIN_UNSUPERVISED_CALORIES, unroundedTarget), 50)

        val referenceWeight = if (bmi >= 30.0) {
            min(input.weightKg, 30.0 * heightMetresSquared(input.heightCm))
        } else {
            input.weightKg
        }
        val proteinPerKg = proteinPerKg(goal, input.activityLevel)
        val desiredProtein = (referenceWeight * proteinPerKg).roundToInt()
        val proteinMin = ceil(idealCalories * 0.10 / 4.0).toInt()
        val proteinMax = floor(idealCalories * 0.30 / 4.0).toInt()
        val idealProtein = desiredProtein.coerceIn(proteinMin, proteinMax)
        val idealFats = (idealCalories * 0.25 / 9.0).roundToInt()
        val idealCarbs = ((idealCalories - idealProtein * 4 - idealFats * 9) / 4.0)
            .roundToInt()
            .coerceAtLeast(0)

        val targetsChanged = !input.currentTargetsCalculated ||
            !currentMacrosPlausible(input) ||
            input.currentCalories <= 0 ||
            abs(idealCalories - input.currentCalories) >= CALORIE_UPDATE_THRESHOLD ||
            abs(idealProtein - input.currentProteinG) >= PROTEIN_UPDATE_THRESHOLD
        val outputCalories = if (targetsChanged) idealCalories else input.currentCalories
        val outputProtein = if (targetsChanged) idealProtein else input.currentProteinG
        val outputCarbs = if (targetsChanged) idealCarbs else input.currentCarbsG
        val outputFats = if (targetsChanged) idealFats else input.currentFatsG
        val targetWeight = targetWeightMilestone(goal, bmi, input.weightKg, healthyRange)

        return TargetPlanResponse(
            recommendedGoal = goal,
            dailyTargetCalories = outputCalories,
            targetProteinG = outputProtein,
            targetCarbsG = outputCarbs,
            targetFatsG = outputFats,
            rationale = "Evidence-based v1: " + rationale(
                input = input,
                goal = goal,
                bmi = bmi,
                healthyRange = healthyRange,
                restingCalories = restingCalories,
                maintenanceCalories = maintenanceCalories,
                calorieTarget = idealCalories,
                proteinTarget = idealProtein,
                proteinPerKg = proteinPerKg,
                referenceWeight = referenceWeight,
                targetWeight = targetWeight,
                targetsChanged = targetsChanged
            ),
            targetsChanged = targetsChanged,
            targetWeightKg = targetWeight
        )
    }

    fun bodyMassIndex(weightKg: Double, heightCm: Double): Double? {
        if (!weightKg.isFinite() || !heightCm.isFinite() || weightKg <= 0.0 || heightCm <= 0.0) {
            return null
        }
        return weightKg / heightMetresSquared(heightCm)
    }

    fun healthyWeightRange(heightCm: Double): ClosedFloatingPointRange<Double>? {
        if (!heightCm.isFinite() || heightCm <= 0.0) return null
        val heightSquared = heightMetresSquared(heightCm)
        return (HEALTHY_BMI_MIN * heightSquared)..(HEALTHY_BMI_MAX * heightSquared)
    }

    private fun resolveGoal(statedGoal: String, bmi: Double?): String {
        val requested = statedGoal.trim().uppercase()
        if (bmi != null && bmi < HEALTHY_BMI_MIN && requested == "LOSE_WEIGHT") {
            return "GAIN_MUSCLE"
        }
        if (requested in SUPPORTED_GOALS) return requested
        return when {
            bmi == null -> "RECOMP"
            bmi < HEALTHY_BMI_MIN -> "GAIN_MUSCLE"
            bmi >= 25.0 -> "LOSE_WEIGHT"
            else -> "RECOMP"
        }
    }

    private fun activityFactor(level: String): Double = when (level.trim().uppercase()) {
        "SEDENTARY" -> 1.2
        "LIGHT" -> 1.375
        "MODERATE" -> 1.55
        "ACTIVE" -> 1.725
        "VERY_ACTIVE" -> 1.9
        else -> 1.375
    }

    private fun proteinPerKg(goal: String, activityLevel: String): Double {
        val active = activityLevel.trim().uppercase() in setOf("MODERATE", "ACTIVE", "VERY_ACTIVE")
        return when (goal) {
            "LOSE_WEIGHT" -> if (active) 1.6 else 1.4
            "GAIN_MUSCLE" -> if (active) 1.6 else 1.4
            else -> if (active) 1.6 else 1.2
        }
    }

    private fun currentMacrosPlausible(input: HealthTargetInput): Boolean {
        if (input.currentCalories <= 0 || input.currentProteinG <= 0 ||
            input.currentCarbsG < 0 || input.currentFatsG <= 0
        ) {
            return false
        }
        val macroCalories = input.currentProteinG * 4 + input.currentCarbsG * 4 +
            input.currentFatsG * 9
        val energyTolerance = max(100.0, input.currentCalories * 0.10)
        if (abs(macroCalories - input.currentCalories) > energyTolerance) return false

        val proteinShare = input.currentProteinG * 4.0 / input.currentCalories
        val carbShare = input.currentCarbsG * 4.0 / input.currentCalories
        val fatShare = input.currentFatsG * 9.0 / input.currentCalories
        return proteinShare in 0.10..0.35 &&
            carbShare in 0.45..0.65 &&
            fatShare in 0.20..0.35
    }

    private fun targetWeightMilestone(
        goal: String,
        bmi: Double?,
        currentWeightKg: Double,
        healthyRange: ClosedFloatingPointRange<Double>?
    ): Double? {
        if (bmi == null || healthyRange == null) return null
        val rawTarget = when {
            goal == "LOSE_WEIGHT" && bmi >= 25.0 ->
                max(currentWeightKg * 0.95, healthyRange.endInclusive)
            goal == "GAIN_MUSCLE" && bmi < HEALTHY_BMI_MIN ->
                healthyRange.start
            else -> return null
        }
        val roundedUp = ceil(rawTarget * 2.0) / 2.0
        return if (goal == "LOSE_WEIGHT") min(currentWeightKg, roundedUp) else roundedUp
    }

    private fun rationale(
        input: HealthTargetInput,
        goal: String,
        bmi: Double?,
        healthyRange: ClosedFloatingPointRange<Double>?,
        restingCalories: Double,
        maintenanceCalories: Double,
        calorieTarget: Int,
        proteinTarget: Int,
        proteinPerKg: Double,
        referenceWeight: Double,
        targetWeight: Double?,
        targetsChanged: Boolean
    ): String {
        val sexNote = if (input.sex.isNullOrBlank()) {
            " Because sex was not set, the midpoint of the two Mifflin–St Jeor constants was used."
        } else {
            ""
        }
        val energySentence = "Mifflin–St Jeor estimates about ${restingCalories.roundToInt()} kcal at rest and " +
            "${roundToStep(maintenanceCalories, 50)} kcal/day including your selected activity level.$sexNote"
        val goalSentence = when (goal) {
            "LOSE_WEIGHT" -> "A moderate 15% deficit gives a starting target of $calorieTarget kcal/day."
            "GAIN_MUSCLE" -> "A conservative 8% surplus gives a starting target of $calorieTarget kcal/day."
            else -> "Estimated maintenance gives a starting target of $calorieTarget kcal/day."
        }
        val referenceNote = if (referenceWeight < input.weightKg) " adjusted" else ""
        val macroSentence = "Protein is set near ${formatOneDecimal(proteinPerKg)} g/kg of$referenceNote reference weight " +
            "($proteinTarget g), with fat near 25% of energy and carbohydrates filling the remainder within adult ranges."
        val bmiSentence = when {
            bmi == null || healthyRange == null ->
                "Treat this as a starting estimate and adjust from your 2–4 week trend."
            targetWeight != null && goal == "LOSE_WEIGHT" ->
                "The ${formatOneDecimal(targetWeight)} kg target is an initial health milestone of up to 5%, not an ideal weight; " +
                    "BMI ${formatOneDecimal(bmi)} is only a screening measure."
            targetWeight != null ->
                "The ${formatOneDecimal(targetWeight)} kg milestone moves to the lower adult BMI screening boundary; " +
                    "seek professional guidance for unexplained low weight."
            else ->
                "BMI ${formatOneDecimal(bmi)} and the ${formatOneDecimal(healthyRange.start)}–" +
                    "${formatOneDecimal(healthyRange.endInclusive)} kg screening range are context, not an ideal-weight prescription."
        }
        return if (targetsChanged) {
            listOf(energySentence, goalSentence, macroSentence, bmiSentence).joinToString(" ")
        } else {
            listOf(
                energySentence,
                "Your saved targets are already close to the calculated starting point, so they were kept stable.",
                bmiSentence
            ).joinToString(" ")
        }
    }

    private fun heightMetresSquared(heightCm: Double): Double {
        val metres = heightCm / 100.0
        return metres * metres
    }

    private fun roundToStep(value: Double, step: Int): Int =
        (value / step).roundToInt() * step

    private fun formatOneDecimal(value: Double): String =
        String.format(java.util.Locale.US, "%.1f", value)

    private const val MIN_UNSUPERVISED_CALORIES = 1200.0
    private const val CALORIE_UPDATE_THRESHOLD = 150
    private const val PROTEIN_UPDATE_THRESHOLD = 20
    private val SUPPORTED_GOALS = setOf("LOSE_WEIGHT", "GAIN_MUSCLE", "RECOMP")
}
