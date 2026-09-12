package com.anant.fitbuddy.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import com.anant.fitbuddy.ui.components.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.anant.fitbuddy.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import com.anant.fitbuddy.ui.components.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.anant.fitbuddy.ui.components.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.bridge.FreeScaleBridge
import com.anant.fitbuddy.data.database.BodyMeasurement
import com.anant.fitbuddy.data.database.UserProfile
import com.anant.fitbuddy.data.model.ActivityLevels
import com.anant.fitbuddy.data.model.HealthTargetCalculator
import com.anant.fitbuddy.data.model.TargetPlanResponse
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.ui.loading.LoadingAnimationHost
import com.anant.fitbuddy.ui.loading.LoadingAnimationSlot
import com.anant.fitbuddy.ui.viewmodel.DashboardUiState
import com.anant.fitbuddy.ui.viewmodel.TargetPlanUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val GOAL_OPTIONS = listOf(
    "AUTO" to "Choose automatically",
    "LOSE_WEIGHT" to "Lose weight",
    "GAIN_MUSCLE" to "Gain muscle",
    "RECOMP" to "Body recomposition"
)
private val ACTIVITY_OPTIONS = ActivityLevels.options

@Composable
fun BodyScreen(
    profile: UserProfile?,
    latestMeasurement: BodyMeasurement?,
    measurements: List<BodyMeasurement>,
    savedFoodCount: Int,
    targetPlanState: TargetPlanUiState,
    isAiConfigured: Boolean,
    animationChoice: String = AppSettings.LOADING_ANIM_RANDOM,
    forceShowAnimation: Boolean = false,
    onSave: (
        weightKg: Double,
        targetWeightKg: Double?,
        dailyTargetCalories: Int,
        targetProteinG: Int,
        targetCarbsG: Int,
        targetFatsG: Int,
        goal: String,
        activityLevel: String
    ) -> Unit,
    onAddMeasurement: (BodyMeasurement) -> Unit,
    onDeleteMeasurement: (BodyMeasurement) -> Unit,
    onRequestTargetPlan: (
        age: Int,
        heightCm: Double,
        weightKg: Double,
        sex: String?,
        activityLevel: String,
        goal: String
    ) -> Unit,
    onApplyTargetPlan: (
        plan: TargetPlanResponse,
        age: Int,
        heightCm: Double,
        weightKg: Double,
        targetWeightKg: Double?,
        sex: String?,
        activityLevel: String,
        acceptActivityRecommendation: Boolean
    ) -> Unit,
    onDismissTargetPlan: () -> Unit,
    onBuildMeal: () -> Unit,
    onManageSavedFoods: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weight = remember(profile) { mutableStateOf(profile?.weightKg?.toString() ?: "") }
    val targetWeight = remember(profile) {
        mutableStateOf(profile?.targetWeightKg?.toString() ?: "")
    }
    val goal = remember(profile) { mutableStateOf(profile?.goal ?: "RECOMP") }
    val parsedTargetWeight = targetWeight.value.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0.0 }
    val currentWeight = weight.value.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    val adultBmi = if ((profile?.age ?: 0) >= 18) {
        currentWeight?.let { HealthTargetCalculator.bodyMassIndex(it, profile?.heightCm ?: 0.0) }
    } else {
        null
    }
    val healthyWeightRange = if ((profile?.age ?: 0) >= 18) {
        HealthTargetCalculator.healthyWeightRange(profile?.heightCm ?: 0.0)
    } else {
        null
    }
    val targetWeightError = when {
        targetWeight.value.isBlank() -> null
        parsedTargetWeight == null -> "Enter a valid target weight"
        currentWeight == null -> null
        goal.value == "GAIN_MUSCLE" && parsedTargetWeight < currentWeight ->
            "A muscle-gain target cannot be below your current weight"
        goal.value == "LOSE_WEIGHT" && parsedTargetWeight > currentWeight ->
            "A weight-loss target cannot be above your current weight"
        else -> null
    }
    val targetWeightInputValid = targetWeightError == null
    val targetWeightForPlan = when {
        targetWeight.value.isBlank() -> null
        parsedTargetWeight != null -> parsedTargetWeight
        else -> profile?.targetWeightKg
    }
    val activity = remember(profile) { mutableStateOf(profile?.activityLevel ?: "MODERATE") }

    val targetCalories = remember(profile) {
        mutableStateOf((profile?.dailyTargetCalories ?: DashboardUiState.DEFAULT_TARGET_CALORIES).toString())
    }
    val targetProtein = remember(profile) {
        mutableStateOf((profile?.targetProteinG ?: DashboardUiState.DEFAULT_TARGET_PROTEIN).toString())
    }
    val targetCarbs = remember(profile) {
        mutableStateOf((profile?.targetCarbsG ?: DashboardUiState.DEFAULT_TARGET_CARBS).toString())
    }
    val targetFats = remember(profile) {
        mutableStateOf((profile?.targetFatsG ?: DashboardUiState.DEFAULT_TARGET_FATS).toString())
    }

    var showAddReading by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Body & goals",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track composition, set targets, and manage saved foods for meals.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FoodLibraryCard(
            savedFoodCount = savedFoodCount,
            onBuildMeal = onBuildMeal,
            onManage = onManageSavedFoods
        )

        DailyTargetsCard(
            targetCalories = targetCalories.value,
            targetProtein = targetProtein.value,
            targetCarbs = targetCarbs.value,
            targetFats = targetFats.value,
            onCaloriesChange = { targetCalories.value = it },
            onProteinChange = { targetProtein.value = it },
            onCarbsChange = { targetCarbs.value = it },
            onFatsChange = { targetFats.value = it },
            rationale = profile?.goalRationale,
            planState = targetPlanState,
            isAiConfigured = isAiConfigured,
            animationChoice = animationChoice,
            forceShowAnimation = forceShowAnimation,
            onRequestPlan = {
                onRequestTargetPlan(
                    profile?.age ?: 0,
                    profile?.heightCm ?: 0.0,
                    weight.value.toDoubleOrNull() ?: 0.0,
                    profile?.sex,
                    activity.value,
                    goal.value
                )
            }
        )

        LatestReadingCard(
            latest = latestMeasurement,
            onAddReading = { showAddReading = true }
        )

        SectionCard(title = "Body basics") {
            NumberField("Current weight (kg)", weight.value, decimal = true) { weight.value = it }
            NumberField("Target weight (kg, optional)", targetWeight.value, decimal = true) {
                targetWeight.value = it
            }
            LabeledDropdown(
                "Typical overall activity",
                activity.value,
                ACTIVITY_OPTIONS,
                ActivityLevels.descriptions
            ) { activity.value = it }
            Text(
                text = ActivityLevels.descriptions[activity.value].orEmpty() +
                    ". Include workouts, daily movement, and physical work.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LabeledDropdown("Goal", goal.value, GOAL_OPTIONS) { goal.value = it }
            if (adultBmi != null && healthyWeightRange != null) {
                Text(
                    text = "Adult BMI screening: ${formatOneDecimal(adultBmi)} · " +
                        "reference range ${formatOneDecimal(healthyWeightRange.start)}–" +
                        "${formatOneDecimal(healthyWeightRange.endInclusive)} kg. " +
                        "BMI is a screening measure, not an ideal-weight prescription.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            targetWeightError?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = targetWeightInputValid,
            onClick = {
                onSave(
                    weight.value.toDoubleOrNull() ?: 0.0,
                    parsedTargetWeight,
                    targetCalories.value.toIntOrNull() ?: DashboardUiState.DEFAULT_TARGET_CALORIES,
                    targetProtein.value.toIntOrNull() ?: DashboardUiState.DEFAULT_TARGET_PROTEIN,
                    targetCarbs.value.toIntOrNull() ?: DashboardUiState.DEFAULT_TARGET_CARBS,
                    targetFats.value.toIntOrNull() ?: DashboardUiState.DEFAULT_TARGET_FATS,
                    goal.value,
                    activity.value
                )
            }
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Save body data")
        }

        if (measurements.isNotEmpty()) {
            ReadingsHistoryCard(measurements = measurements, onDelete = onDeleteMeasurement)
        }
    }

    if (showAddReading) {
        AddMeasurementSheet(
            onDismiss = { showAddReading = false },
            onSave = { measurement ->
                showAddReading = false
                onAddMeasurement(measurement)
            }
        )
    }

    targetPlanState.plan?.let { plan ->
        val applyPlan: (Boolean) -> Unit = { acceptActivityRecommendation ->
            onApplyTargetPlan(
                plan,
                profile?.age ?: 0,
                profile?.heightCm ?: 0.0,
                weight.value.toDoubleOrNull() ?: 0.0,
                targetWeightForPlan,
                profile?.sex,
                activity.value,
                acceptActivityRecommendation
            )
        }
        TargetProposalDialog(
            plan = plan,
            currentActivityLevel = activity.value,
            onApply = { applyPlan(true) },
            onApplyCurrentActivity = { applyPlan(false) },
            onDismiss = onDismissTargetPlan
        )
    }
}

@Composable
private fun FoodLibraryCard(
    savedFoodCount: Int,
    onBuildMeal: () -> Unit,
    onManage: () -> Unit
) {
    SectionCard(title = "Food library") {
        Text(
            text = if (savedFoodCount == 0) {
                "No saved foods yet — scan barcodes from Log or bookmark foods after AI review."
            } else {
                "$savedFoodCount saved food${if (savedFoodCount == 1) "" else "s"} for meal building."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onBuildMeal
            ) {
                Icon(Icons.Filled.Restaurant, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Build meal")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onManage
            ) {
                Icon(Icons.Filled.RestaurantMenu, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Manage")
            }
        }
    }
}

@Composable
private fun LatestReadingCard(
    latest: BodyMeasurement?,
    onAddReading: () -> Unit
) {
    SectionCard(title = "Body composition") {
        if (latest == null) {
            Text(
                "No readings yet. Add your weight (and optional smart-scale metrics) to start tracking.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "Latest reading · ${latest.dateString}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            MetricGrid(latest)
            Spacer(Modifier.height(4.dp))
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onAddReading
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add a reading")
        }
    }
}

private const val METRIC_GRID_COLUMNS = 2

@Composable
private fun MetricGrid(m: BodyMeasurement) {
    val chips = buildList {
        add("Weight" to "${m.weightKg} kg")
        m.bmi?.let { add("BMI" to it.toString()) }
        m.bodyFatPct?.let { add("Body fat" to "$it%") }
        m.muscleRatePct?.let { add("Muscle rate" to "$it%") }
        m.bodyWaterPct?.let { add("Body water" to "$it%") }
        m.muscleMassKg?.let { add("Muscle mass" to "$it kg") }
        m.fatMassKg?.let { add("Fat mass" to "$it kg") }
        m.boneMassKg?.let { add("Bone mass" to "$it kg") }
        m.bmr?.let { add("BMR" to "$it kcal") }
        m.metabolicAge?.let { add("Metabolic age" to "$it yrs") }
        m.visceralFat?.let { add("Visceral fat" to "$it%") }
        m.subcutaneousFatPct?.let { add("Subcut. fat" to "$it%") }
        m.proteinMassKg?.let { add("Protein" to "$it kg") }
        m.fatFreeMassKg?.let { add("Fat-free" to "$it kg") }
        m.skeletalMuscleMassKg?.let { add("Skeletal muscle" to "$it kg") }
        m.waterWeightKg?.let { add("Water" to "$it kg") }
    }
    // Fixed-column grid (not a wrapping FlowRow) so every chip in a column shares the same width
    // and the last, possibly-partial row still aligns instead of leaving a lone stray chip.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.chunked(METRIC_GRID_COLUMNS).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { (label, value) ->
                    MetricChip(label, value, modifier = Modifier.weight(1f))
                }
                repeat(METRIC_GRID_COLUMNS - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

private val PLAN_CAPTIONS = listOf(
    "Analysing…",
    "Calculating targets…",
    "Checking your profile…",
    "Weighing the options…",
    "Almost ready…",
    "Consulting the AI…",
    "Mapping your goals…",
    "Fine-tuning numbers…"
)

@Composable
private fun DailyTargetsCard(
    targetCalories: String,
    targetProtein: String,
    targetCarbs: String,
    targetFats: String,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatsChange: (String) -> Unit,
    rationale: String?,
    planState: TargetPlanUiState,
    isAiConfigured: Boolean,
    animationChoice: String,
    forceShowAnimation: Boolean = false,
    onRequestPlan: () -> Unit
) {
    SectionCard(title = "Daily targets") {
        NumberField("Calories (kcal)", targetCalories, onValueChange = onCaloriesChange)
        NumberField("Protein (g)", targetProtein, onValueChange = onProteinChange)
        NumberField("Carbs (g)", targetCarbs, onValueChange = onCarbsChange)
        NumberField("Fats (g)", targetFats, onValueChange = onFatsChange)

        val planLoading = planState.isLoading || forceShowAnimation
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !planLoading,
            onClick = onRequestPlan,
            colors = if (planLoading) {
                ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                ButtonDefaults.buttonColors()
            },
            contentPadding = if (planLoading) PaddingValues(0.dp)
                             else PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (planLoading) {
                LoadingAnimationHost(
                    slot = LoadingAnimationSlot.INSIGHT,
                    animationChoice = animationChoice,
                    captions = PLAN_CAPTIONS,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )
            } else {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Calculate personalized targets")
            }
        }
        Text(
            text = if (isAiConfigured) {
                "Targets and safe trend adjustments are calculated on device; AI may choose " +
                    "between those exact options and personalize the explanation."
            } else {
                "Targets and safe trend adjustments are calculated on device — no AI connection required."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        planState.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (!rationale.isNullOrBlank()) {
            val rationaleParts = splitPlanRationale(rationale)
            HorizontalDivider()
            Text(
                "How these were calculated",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (rationaleParts.localRationale.isNotBlank()) {
                Text(
                    rationaleParts.localRationale,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            rationaleParts.coachingNote?.let { coachingNote ->
                Text(
                    "AI coaching note",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    coachingNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReadingsHistoryCard(
    measurements: List<BodyMeasurement>,
    onDelete: (BodyMeasurement) -> Unit
) {
    SectionCard(title = "Reading history") {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
        ) {
            itemsIndexed(measurements, key = { _, m -> m.id }) { index, m ->
                if (index > 0) HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            m.dateString,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            buildString {
                                append("${m.weightKg} kg")
                                m.bodyFatPct?.let { append(" · ${it}% fat") }
                                m.muscleMassKg?.let { append(" · ${it}kg muscle") }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDelete(m) }) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "Delete reading",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetProposalDialog(
    plan: TargetPlanResponse,
    currentActivityLevel: String,
    onApply: () -> Unit,
    onApplyCurrentActivity: () -> Unit,
    onDismiss: () -> Unit
) {
    val goalLabel = GOAL_OPTIONS.firstOrNull { it.first == plan.recommendedGoal }?.second
        ?: plan.recommendedGoal
    val proposedTargetWeight = plan.targetWeightKg
        ?.takeIf { it.isFinite() && it > 0.0 }
    val recommendedActivity = plan.recommendedActivityLevel
        ?.takeIf { ActivityLevels.definition(it) != null }
    val activityChanged = recommendedActivity != null &&
        !recommendedActivity.equals(currentActivityLevel, ignoreCase = true)
    val activityUsed = recommendedActivity ?: currentActivityLevel
    val activityUsedLabel = ActivityLevels.label(activityUsed)
    val currentActivityLabel = ActivityLevels.label(currentActivityLevel)
    val canApply = plan.targetsChanged || proposedTargetWeight != null || activityChanged
    val coachingNote = splitPlanRationale(plan.rationale).coachingNote
    val restingCalories = plan.estimatedRestingCalories
    val activityFactor = plan.activityFactor
    val maintenanceCalories = plan.estimatedMaintenanceCalories
    val formulaTarget = plan.formulaTargetCalories
    val goalAdjustment = plan.goalAdjustmentCalories
    val trendAdjustment = plan.bodyTrendAdjustmentCalories
    val stabilityAdjustment = formulaTarget?.let {
        plan.dailyTargetCalories - it - trendAdjustment
    } ?: 0
    val title = when {
        plan.targetsChanged -> "Science-based recommendation"
        proposedTargetWeight != null && activityChanged -> "Health target recommendation"
        proposedTargetWeight != null -> "Target weight recommendation"
        recommendedActivity != null -> "Activity recommendation"
        else -> "You're on track"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RecommendationCard(title = "Daily plan") {
                    Text(
                        text = "${plan.dailyTargetCalories} kcal/day",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = goalLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Protein ${plan.targetProteinG} g  ·  " +
                            "Carbs ${plan.targetCarbsG} g  ·  Fats ${plan.targetFatsG} g",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!plan.targetsChanged) {
                        Text(
                            text = "Your saved nutrition targets remain unchanged.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                RecommendationCard(title = "Activity used") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(activityUsedLabel, fontWeight = FontWeight.SemiBold)
                        activityFactor?.let {
                            Text(
                                text = "${formatActivityFactor(it)}×",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = when {
                            activityChanged ->
                                "This proposal uses the workout-suggested $activityUsedLabel " +
                                    "level, not your selected $currentActivityLabel level. " +
                                    "Choose Use current to recalculate before saving."
                            recommendedActivity != null ->
                                "The workout-based suggestion matches your selected " +
                                    "$currentActivityLabel level."
                            else ->
                                "This proposal uses your selected $currentActivityLabel level."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    recommendedActivity?.let {
                        Text(
                            text = "Evidence: ${plan.activityWorkoutDays ?: 0} workout days, " +
                                "${plan.activityWorkoutCount ?: 0} sessions, about " +
                                "${plan.activityWeeklyMinutes ?: 0} min/week over " +
                                "${plan.activityWindowDays ?: 28} days.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Workout logs may not include physical work, daily movement, " +
                                "or unlogged exercise.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                RecommendationCard(title = "Calorie calculation") {
                    if (
                        restingCalories != null && activityFactor != null &&
                        maintenanceCalories != null && formulaTarget != null &&
                        goalAdjustment != null
                    ) {
                        Text(
                            text = "Mifflin–St Jeor resting estimate, adjusted for the activity " +
                                "level above and your goal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CalculationRow("Resting estimate", "$restingCalories kcal")
                        CalculationRow("Activity multiplier", "×${formatActivityFactor(activityFactor)}")
                        CalculationRow("Estimated maintenance", "$maintenanceCalories kcal")
                        CalculationRow("Goal adjustment", "${signedInteger(goalAdjustment)} kcal")
                        CalculationRow("Formula target", "$formulaTarget kcal")
                        if (stabilityAdjustment != 0) {
                            CalculationRow("Target stability", "${signedInteger(stabilityAdjustment)} kcal")
                            Text(
                                text = "Small differences from your saved target are retained to " +
                                    "avoid unnecessary changes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (trendAdjustment != 0) {
                            CalculationRow("Body-trend adjustment", "${signedInteger(trendAdjustment)} kcal")
                        }
                        HorizontalDivider()
                        CalculationRow(
                            label = "Final daily target",
                            value = "${plan.dailyTargetCalories} kcal",
                            emphasized = true
                        )
                    } else {
                        Text(
                            text = "This target was calculated locally from your profile, " +
                                "activity level, and goal.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (plan.bodyTrendStatus != null) {
                    RecommendationCard(title = "Body trend (optional)") {
                        if (plan.bodyTrendStatus == "SUFFICIENT") {
                            Text(
                                text = "${plan.bodyTrendSampleCount ?: 0} measurement days over " +
                                    "${plan.bodyTrendSpanDays ?: 0} days",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Weight ${signedTwoDecimals(plan.bodyTrendWeightChangeKg ?: 0.0)} kg " +
                                    "· ${signedTwoDecimals(plan.bodyTrendWeeklyChangePct ?: 0.0)}%/week",
                                style = MaterialTheme.typography.bodySmall
                            )
                            val compositionParts = buildList {
                                plan.bodyTrendBodyFatChangePct?.let {
                                    add("Body fat ${signedTwoDecimals(it)} points")
                                }
                                plan.bodyTrendMuscleMassChangeKg?.let {
                                    add("Muscle ${signedTwoDecimals(it)} kg")
                                }
                            }
                            if (compositionParts.isNotEmpty()) {
                                Text(
                                    text = compositionParts.joinToString("  ·  "),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = if (trendAdjustment == 0) {
                                    "The formula target was retained after reviewing the trend."
                                } else {
                                    "A ${signedInteger(trendAdjustment)} kcal/day bounded trend " +
                                        "adjustment was applied."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Food logs: ${plan.bodyTrendFoodDaysLogged ?: 0} of the " +
                                    "previous 28 completed days. Composition readings are " +
                                    "supporting evidence and can vary with hydration.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = plan.bodyTrendReason
                                    ?: "More consistent readings are needed.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "No trend adjustment was applied; the profile-based " +
                                    "formula remains available without scale history.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                proposedTargetWeight?.let {
                    RecommendationCard(title = "Weight target") {
                        Text(
                            text = "${formatOneDecimal(it)} kg",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "A gradual milestone based on your goal and the healthy BMI " +
                                "screening range.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                coachingNote?.let {
                    RecommendationCard(title = "AI coaching note") {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Text(
                    text = "These are adult planning estimates, not medical advice. Reassess " +
                        "progress regularly and seek professional guidance when needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            if (canApply) {
                TextButton(onClick = onApply) {
                    Text(
                        when {
                            plan.targetsChanged -> "Apply"
                            proposedTargetWeight != null && activityChanged -> "Apply recommendations"
                            proposedTargetWeight != null -> "Save target weight"
                            else -> "Apply activity level"
                        }
                    )
                }
            } else {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        },
        dismissButton = {
            if (canApply) {
                Row {
                    if (activityChanged) {
                        TextButton(onClick = onApplyCurrentActivity) { Text("Use current") }
                    }
                    TextButton(onClick = onDismiss) { Text("Discard") }
                }
            }
        }
    )
}

@Composable
private fun RecommendationCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun CalculationRow(
    label: String,
    value: String,
    emphasized: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = if (emphasized) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.bodySmall
            },
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = value,
            style = if (emphasized) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.bodySmall
            },
            fontWeight = FontWeight.SemiBold,
            color = if (emphasized) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

/** Optional smart-scale fields, rendered generically. First element is the state key. */
private data class MetricSpec(val key: String, val label: String, val decimal: Boolean = true)

private val OPTIONAL_METRICS = listOf(
    MetricSpec("bmi", "BMI"),
    MetricSpec("bodyFatPct", "Body fat (%)"),
    MetricSpec("muscleRatePct", "Muscle rate (%)"),
    MetricSpec("bodyWaterPct", "Body water (%)"),
    MetricSpec("boneMassKg", "Bone mass (kg)"),
    MetricSpec("bmr", "BMR (kcal)", decimal = false),
    MetricSpec("metabolicAge", "Metabolic age (yrs)", decimal = false),
    MetricSpec("visceralFat", "Visceral fat (%)"),
    MetricSpec("subcutaneousFatPct", "Subcutaneous fat (%)"),
    MetricSpec("proteinMassKg", "Protein mass (kg)"),
    MetricSpec("muscleMassKg", "Muscle mass (kg)"),
    MetricSpec("fatFreeMassKg", "Weight without fat (kg)"),
    MetricSpec("skeletalMuscleMassKg", "Skeletal muscle mass (kg)"),
    MetricSpec("waterWeightKg", "Water weight (kg)"),
    MetricSpec("fatMassKg", "Fat mass (kg)")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMeasurementSheet(
    onDismiss: () -> Unit,
    onSave: (BodyMeasurement) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var weight by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    val optional = remember { mutableStateMapOf<String, String>() }
    var pulledTimestamp by remember { mutableStateOf(0L) }
    var pulledDateString by remember { mutableStateOf("") }
    var pulledFreescalePayload by remember { mutableStateOf<String?>(null) }
    var pullBusy by remember { mutableStateOf(false) }
    var showInstallFreeScale by remember { mutableStateOf(false) }
    var pullError by remember { mutableStateOf<String?>(null) }
    var pullStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun applyPulled(m: BodyMeasurement) {
        pulledTimestamp = m.timestamp
        pulledDateString = m.dateString
        pulledFreescalePayload = m.freescalePayloadJson
        weight = fmtOptional(m.weightKg) ?: ""
        fun put(key: String, value: Double?) {
            val s = fmtOptional(value)
            if (s != null) optional[key] = s else optional.remove(key)
        }
        fun putInt(key: String, value: Int?) {
            if (value != null) optional[key] = value.toString() else optional.remove(key)
        }
        put("bmi", m.bmi)
        put("bodyFatPct", m.bodyFatPct)
        put("muscleRatePct", m.muscleRatePct)
        put("bodyWaterPct", m.bodyWaterPct)
        put("boneMassKg", m.boneMassKg)
        putInt("bmr", m.bmr)
        putInt("metabolicAge", m.metabolicAge)
        put("visceralFat", m.visceralFat)
        put("subcutaneousFatPct", m.subcutaneousFatPct)
        put("proteinMassKg", m.proteinMassKg)
        put("muscleMassKg", m.muscleMassKg)
        put("fatFreeMassKg", m.fatFreeMassKg)
        put("skeletalMuscleMassKg", m.skeletalMuscleMassKg)
        put("waterWeightKg", m.waterWeightKg)
        put("fatMassKg", m.fatMassKg)
        showAdvanced = optional.isNotEmpty()
        pullStatus = "Loaded FreeScale reading — review and tap Save."
        pullError = null
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add a reading", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !pullBusy,
                onClick = {
                    pullError = null
                    pullStatus = null
                    if (!FreeScaleBridge.isAvailable(context)) {
                        showInstallFreeScale = true
                        return@OutlinedButton
                    }
                    pullBusy = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            FreeScaleBridge.exportLatest(context)
                        }
                        pullBusy = false
                        result.fold(
                            onSuccess = { applyPulled(it) },
                            onFailure = { e ->
                                pullError = e.message ?: "Could not load FreeScale reading"
                            },
                        )
                    }
                },
            ) {
                Text(if (pullBusy) "Loading from FreeScale…" else "Pull latest from FreeScale")
            }
            pullStatus?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            pullError?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            NumberField("Weight (kg)", weight, decimal = true) { weight = it }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Smart-scale metrics (optional)",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showAdvanced = !showAdvanced }) {
                    Icon(
                        if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (showAdvanced) "Collapse" else "Expand"
                    )
                }
            }

            AnimatedVisibility(visible = showAdvanced) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OPTIONAL_METRICS.forEach { spec ->
                        NumberField(
                            label = spec.label,
                            value = optional[spec.key] ?: "",
                            decimal = spec.decimal
                        ) { optional[spec.key] = it }
                    }
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = (weight.toDoubleOrNull() ?: 0.0) > 0.0,
                onClick = {
                    fun d(key: String) = optional[key]?.toDoubleOrNull()
                    fun i(key: String) = optional[key]?.toIntOrNull()
                    onSave(
                        BodyMeasurement(
                            timestamp = pulledTimestamp,
                            dateString = pulledDateString,
                            weightKg = weight.toDoubleOrNull() ?: 0.0,
                            bmi = d("bmi"),
                            bodyFatPct = d("bodyFatPct"),
                            muscleRatePct = d("muscleRatePct"),
                            bodyWaterPct = d("bodyWaterPct"),
                            boneMassKg = d("boneMassKg"),
                            bmr = i("bmr"),
                            metabolicAge = i("metabolicAge"),
                            visceralFat = d("visceralFat"),
                            subcutaneousFatPct = d("subcutaneousFatPct"),
                            proteinMassKg = d("proteinMassKg"),
                            muscleMassKg = d("muscleMassKg"),
                            fatFreeMassKg = d("fatFreeMassKg"),
                            skeletalMuscleMassKg = d("skeletalMuscleMassKg"),
                            waterWeightKg = d("waterWeightKg"),
                            fatMassKg = d("fatMassKg"),
                            freescalePayloadJson = pulledFreescalePayload,
                        )
                    )
                }
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save reading")
            }
        }
    }

    if (showInstallFreeScale) {
        AlertDialog(
            onDismissRequest = { showInstallFreeScale = false },
            title = { Text("FreeScale not installed") },
            text = {
                Text(
                    "Install FreeScale to pull the latest scale reading into this form. " +
                        "Open the FreeScale page to download it, then come back and try again.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showInstallFreeScale = false
                        uriHandler.openUri(FREESCALE_WEBSITE_URL)
                    },
                ) {
                    Text("Get FreeScale")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallFreeScale = false }) {
                    Text("Not now")
                }
            },
        )
    }
}

private const val FREESCALE_WEBSITE_URL = "https://github.com/anantdark/FreeScale"
private const val COACHING_NOTE_MARKER = " Coaching note: "

private data class PlanRationaleParts(
    val localRationale: String,
    val coachingNote: String?
)

private fun splitPlanRationale(rationale: String): PlanRationaleParts {
    val markerIndex = rationale.indexOf(COACHING_NOTE_MARKER)
    if (markerIndex < 0) return PlanRationaleParts(rationale.trim(), null)
    val coachingNote = rationale
        .substring(markerIndex + COACHING_NOTE_MARKER.length)
        .trim()
        .takeIf { it.isNotEmpty() }
    return PlanRationaleParts(
        localRationale = rationale.substring(0, markerIndex).trim(),
        coachingNote = coachingNote
    )
}

private fun formatActivityFactor(value: Double): String =
    String.format(java.util.Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')

private fun formatOneDecimal(value: Double): String =
    String.format(java.util.Locale.US, "%.1f", value)

private fun signedTwoDecimals(value: Double): String =
    String.format(java.util.Locale.US, "%+.2f", value)

private fun signedInteger(value: Int): String =
    String.format(java.util.Locale.US, "%+d", value)

private fun fmtOptional(value: Double?): String? {
    if (value == null) return null
    return String.format(java.util.Locale.US, "%.2f", value)
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    decimal: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val filtered = buildString {
                input.forEach { char ->
                    when {
                        char.isDigit() -> append(char)
                        decimal && char == '.' && '.' !in this -> append(char)
                    }
                }
            }
            onValueChange(filtered)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledDropdown(
    label: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    descriptions: Map<String, String> = emptyMap(),
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val display = options.firstOrNull { it.first == selectedValue }?.second
        ?: options.firstOrNull()?.second.orEmpty()

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(text)
                            descriptions[value]?.let { description ->
                                Text(
                                    description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

