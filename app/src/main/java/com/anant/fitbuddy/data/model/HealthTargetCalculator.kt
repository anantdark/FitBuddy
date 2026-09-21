package com.anant.fitbuddy.data.model

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
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
    fun calculate(input: HealthTargetInput): TargetPlanResponse {
        require(input.age >= 18) {
            "Automated calorie and weight targets are available only for adults 18+. " +
                "For children and teens, ask a qualified health professional."
        }
        require(input.age <= 120) { "Enter an age between 18 and 120" }
        require(input.heightCm in 100.0..250.0) { "Enter a valid height between 100 and 250 cm" }
        require(input.weightKg in 25.0..400.0) { "Enter a valid weight between 25 and 400 kg" }

        val goal = resolveGoal(input.statedGoal)
        val sexAdjustment = when (input.sex?.trim()?.uppercase()) {
            "MALE" -> 5.0
            "FEMALE" -> -161.0
            else -> -78.0
        }
        val restingCalories = 10.0 * input.weightKg +
            6.25 * input.heightCm -
            5.0 * input.age +
            sexAdjustment
        val roundedRestingCalories = restingCalories.roundToInt()
        val activityFactor = ActivityLevels.factor(input.activityLevel)
        val maintenanceCalories = restingCalories * activityFactor
        val roundedMaintenanceCalories = (roundedRestingCalories * activityFactor).roundToInt()

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

        val proteinPerKg = proteinPerKg(goal, input.activityLevel)
        val macros = macroTargets(
            calories = idealCalories,
            bodyWeightKg = input.weightKg,
            goal = goal,
            activityLevel = input.activityLevel
        )
        val idealProtein = macros.proteinG
        val idealFats = macros.fatsG
        val idealCarbs = macros.carbsG

        val targetsChanged = !input.currentTargetsCalculated ||
            !currentMacrosPlausible(input) ||
            input.currentCalories <= 0 ||
            abs(idealCalories - input.currentCalories) >= CALORIE_UPDATE_THRESHOLD ||
            abs(idealProtein - input.currentProteinG) >= PROTEIN_UPDATE_THRESHOLD
        val outputCalories = if (targetsChanged) idealCalories else input.currentCalories
        val outputProtein = if (targetsChanged) idealProtein else input.currentProteinG
        val outputCarbs = if (targetsChanged) idealCarbs else input.currentCarbsG
        val outputFats = if (targetsChanged) idealFats else input.currentFatsG
        val targetWeight = targetWeightMilestone(goal, input.weightKg)

        return TargetPlanResponse(
            recommendedGoal = goal,
            dailyTargetCalories = outputCalories,
            targetProteinG = outputProtein,
            targetCarbsG = outputCarbs,
            targetFatsG = outputFats,
            rationale = "Evidence-based v3: " + rationale(
                input = input,
                goal = goal,
                restingCalories = restingCalories,
                maintenanceCalories = maintenanceCalories,
                calorieTarget = idealCalories,
                proteinTarget = idealProtein,
                proteinPerKg = proteinPerKg,
                targetWeight = targetWeight,
                targetsChanged = targetsChanged
            ),
            targetsChanged = targetsChanged,
            targetWeightKg = targetWeight,
            estimatedRestingCalories = roundedRestingCalories,
            activityFactor = activityFactor,
            estimatedMaintenanceCalories = roundedMaintenanceCalories,
            formulaTargetCalories = idealCalories,
            goalAdjustmentCalories = idealCalories - roundedMaintenanceCalories
        )
    }

    fun adjustCalories(
        input: HealthTargetInput,
        plan: TargetPlanResponse,
        requestedAdjustmentCalories: Int
    ): TargetPlanResponse {
        val boundedAdjustment = requestedAdjustmentCalories.coerceIn(
            -MAX_TREND_ADJUSTMENT,
            MAX_TREND_ADJUSTMENT
        )
        val adjustedCalories = roundToStep(
            max(
                MIN_UNSUPERVISED_CALORIES,
                (plan.dailyTargetCalories + boundedAdjustment).toDouble()
            ),
            50
        )
        val actualAdjustment = adjustedCalories - plan.dailyTargetCalories
        if (actualAdjustment == 0) return plan

        val macros = macroTargets(
            calories = adjustedCalories,
            bodyWeightKg = input.weightKg,
            goal = plan.recommendedGoal,
            activityLevel = input.activityLevel
        )
        val direction = if (actualAdjustment > 0) "increased" else "reduced"
        return plan.copy(
            dailyTargetCalories = adjustedCalories,
            targetProteinG = macros.proteinG,
            targetCarbsG = macros.carbsG,
            targetFatsG = macros.fatsG,
            rationale = plan.rationale +
                " Trend-adjusted locally with a cautious $direction calorie step from " +
                "consistent body and nutrition history; reassess after another two to four weeks.",
            targetsChanged = true,
            bodyTrendAdjustmentCalories = actualAdjustment
        )
    }

    private fun resolveGoal(statedGoal: String): String {
        val requested = statedGoal.trim().uppercase()
        return requested.takeIf { it in SUPPORTED_GOALS } ?: "RECOMP"
    }

    private fun macroTargets(
        calories: Int,
        bodyWeightKg: Double,
        goal: String,
        activityLevel: String
    ): MacroTargets {
        val desiredProtein = (bodyWeightKg * proteinPerKg(goal, activityLevel)).roundToInt()
        val proteinMin = ceil(calories * 0.10 / 4.0).toInt()
        val proteinMax = floor(calories * 0.30 / 4.0).toInt()
        val protein = desiredProtein.coerceIn(proteinMin, proteinMax)
        val fats = (calories * 0.25 / 9.0).roundToInt()
        val carbs = ((calories - protein * 4 - fats * 9) / 4.0)
            .roundToInt()
            .coerceAtLeast(0)
        return MacroTargets(proteinG = protein, carbsG = carbs, fatsG = fats)
    }

    private fun proteinPerKg(goal: String, activityLevel: String): Double {
        val active = ActivityLevels.isActive(activityLevel)
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

    private fun targetWeightMilestone(goal: String, currentWeightKg: Double): Double {
        if (goal != "LOSE_WEIGHT") return currentWeightKg
        return (ceil(currentWeightKg * 0.95 * 2.0) / 2.0).coerceAtMost(currentWeightKg)
    }

    private fun rationale(
        input: HealthTargetInput,
        goal: String,
        restingCalories: Double,
        maintenanceCalories: Double,
        calorieTarget: Int,
        proteinTarget: Int,
        proteinPerKg: Double,
        targetWeight: Double,
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
        val macroSentence = "Protein is set near ${formatOneDecimal(proteinPerKg)} g/kg of body weight " +
            "($proteinTarget g), with fat near 25% of energy and carbohydrates filling the remainder within adult ranges."
        val milestoneSentence = if (goal == "LOSE_WEIGHT" && abs(targetWeight - input.weightKg) >= 0.05) {
            "The ${formatOneDecimal(targetWeight)} kg target is an initial milestone of up to 5%, not an ideal weight."
        } else {
            "Current weight is retained as a maintenance milestone unless you set a different one."
        }
        return if (targetsChanged) {
            listOf(energySentence, goalSentence, macroSentence, milestoneSentence).joinToString(" ")
        } else {
            listOf(
                energySentence,
                "Your saved targets are already close to the calculated starting point, so they were kept stable.",
                milestoneSentence
            ).joinToString(" ")
        }
    }

    private fun roundToStep(value: Double, step: Int): Int =
        (value / step).roundToInt() * step

    private fun formatOneDecimal(value: Double): String =
        String.format(java.util.Locale.US, "%.1f", value)

    private data class MacroTargets(
        val proteinG: Int,
        val carbsG: Int,
        val fatsG: Int
    )

    private const val MIN_UNSUPERVISED_CALORIES = 1200.0
    private const val MAX_TREND_ADJUSTMENT = 100
    private const val CALORIE_UPDATE_THRESHOLD = 150
    private const val PROTEIN_UPDATE_THRESHOLD = 20
    private val SUPPORTED_GOALS = setOf("LOSE_WEIGHT", "GAIN_MUSCLE", "RECOMP")
}
