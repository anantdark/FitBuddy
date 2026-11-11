package com.anant.fitbuddy.ui.screens

import com.anant.fitbuddy.R
import androidx.compose.ui.res.stringResource

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.anant.fitbuddy.util.LocalUnitSystem
import com.anant.fitbuddy.util.Units
import com.anant.fitbuddy.data.model.ActivityLevels
import com.anant.fitbuddy.data.model.TargetPlanResponse
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.ui.loading.LoadingAnimationHost
import com.anant.fitbuddy.ui.loading.LoadingAnimationSlot
import com.anant.fitbuddy.ui.viewmodel.DashboardUiState
import com.anant.fitbuddy.ui.viewmodel.TargetPlanUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

private val GOAL_OPTION_RES = listOf(
    "AUTO" to R.string.onboarding_choose_automatically,
    "LOSE_WEIGHT" to R.string.onboarding_lose_weight,
    "GAIN_MUSCLE" to R.string.onboarding_gain_muscle,
    "RECOMP" to R.string.onboarding_body_recomposition
)

@Composable
fun BodyScreen(
    profile: UserProfile?,
    latestMeasurement: BodyMeasurement?,
    measurements: List<BodyMeasurement>,
    targetPlanState: TargetPlanUiState,
    isAiConfigured: Boolean,
    animationChoice: String = AppSettings.LOADING_ANIM_RANDOM,
    forceShowAnimation: Boolean = false,
    onSave: (
        targetWeightKg: Double?,
        goal: String,
        activityLevel: String
    ) -> Unit,
    onSaveTargets: (
        dailyTargetCalories: Int,
        targetProteinG: Int,
        targetCarbsG: Int,
        targetFatsG: Int
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
    modifier: Modifier = Modifier
) {
    val currentWeight = latestMeasurement?.weightKg
        ?.takeIf { it.isFinite() && it > 0.0 }
    val units = LocalUnitSystem.current
    val wUnit = Units.weightUnit(units)
    val targetWeight = remember(profile?.targetWeightKg, units) {
        mutableStateOf(profile?.targetWeightKg?.let { Units.weightText(it, units) } ?: "")
    }
    val goal = remember(profile?.goal) { mutableStateOf(profile?.goal ?: "RECOMP") }
    val parsedTargetWeight = targetWeight.value.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0.0 }
        ?.let { Units.displayToKg(it, units) }
    val targetWeightError = when {
        targetWeight.value.isBlank() -> null
        parsedTargetWeight == null -> stringResource(R.string.profile_enter_valid_weight_milestone)
        currentWeight == null -> stringResource(R.string.profile_add_current_reading_before_setting_weight_milest)
        goal.value == "GAIN_MUSCLE" && parsedTargetWeight < currentWeight ->
            stringResource(R.string.profile_muscle_gain_target_cannot_below_current_weight)
        goal.value == "LOSE_WEIGHT" && parsedTargetWeight > currentWeight ->
            stringResource(R.string.profile_weight_loss_target_cannot_above_current_weight)
        else -> null
    }
    val targetWeightInputValid = targetWeightError == null
    val targetWeightForPlan = when {
        targetWeight.value.isBlank() -> null
        parsedTargetWeight != null -> parsedTargetWeight
        else -> profile?.targetWeightKg
    }
    val activity = remember(profile?.activityLevel) {
        mutableStateOf(profile?.activityLevel ?: "MODERATE")
    }

    val persistedTargetCalories = profile?.dailyTargetCalories
        ?: DashboardUiState.DEFAULT_TARGET_CALORIES
    val persistedTargetProtein = profile?.targetProteinG
        ?: DashboardUiState.DEFAULT_TARGET_PROTEIN
    val persistedTargetCarbs = profile?.targetCarbsG
        ?: DashboardUiState.DEFAULT_TARGET_CARBS
    val persistedTargetFats = profile?.targetFatsG
        ?: DashboardUiState.DEFAULT_TARGET_FATS
    val targetCalories = remember { mutableStateOf(persistedTargetCalories.toString()) }
    val targetProtein = remember { mutableStateOf(persistedTargetProtein.toString()) }
    val targetCarbs = remember { mutableStateOf(persistedTargetCarbs.toString()) }
    val targetFats = remember { mutableStateOf(persistedTargetFats.toString()) }

    LaunchedEffect(
        persistedTargetCalories,
        persistedTargetProtein,
        persistedTargetCarbs,
        persistedTargetFats,
        targetPlanState.appliedRevision
    ) {
        targetCalories.value = persistedTargetCalories.toString()
        targetProtein.value = persistedTargetProtein.toString()
        targetCarbs.value = persistedTargetCarbs.toString()
        targetFats.value = persistedTargetFats.toString()
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
                text = stringResource(R.string.profile_body_goals),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.profile_track_composition_and_set_calorie_and),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DailyTargetsCard(
            targetCalories = targetCalories.value,
            targetProtein = targetProtein.value,
            targetCarbs = targetCarbs.value,
            targetFats = targetFats.value,
            savedTargetCalories = persistedTargetCalories,
            savedTargetProtein = persistedTargetProtein,
            savedTargetCarbs = persistedTargetCarbs,
            savedTargetFats = persistedTargetFats,
            onCaloriesChange = { targetCalories.value = it },
            onProteinChange = { targetProtein.value = it },
            onCarbsChange = { targetCarbs.value = it },
            onFatsChange = { targetFats.value = it },
            onSaveTargets = onSaveTargets,
            planState = targetPlanState,
            isAiConfigured = isAiConfigured,
            animationChoice = animationChoice,
            forceShowAnimation = forceShowAnimation,
            onRequestPlan = {
                onRequestTargetPlan(
                    profile?.age ?: 0,
                    profile?.heightCm ?: 0.0,
                    currentWeight ?: 0.0,
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

        SectionCard(title = stringResource(R.string.profile_body_basics)) {
            OutlinedTextField(
                value = currentWeight?.let { Units.weightText(it, units) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.onboarding_current_weight_kg, wUnit)) },
                supportingText = {
                    Text(
                        if (latestMeasurement != null) {
                            stringResource(R.string.profile_synced_latest_reading, latestMeasurement.dateString)
                        } else {
                            stringResource(R.string.profile_add_reading_update_current_weight)
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            NumberField(stringResource(R.string.profile_weight_milestone_kg_optional, wUnit), targetWeight.value, decimal = true) {
                targetWeight.value = it
            }
            Text(
                text = stringResource(R.string.profile_personalized_targets_set_a_bounded_weight),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val activityContext = LocalContext.current
            val activityOptions = remember(activityContext) {
                ActivityLevels.optionRes.map { (value, res) ->
                    value to activityContext.getString(res)
                }
            }
            val activityDescriptions = remember(activityContext) {
                ActivityLevels.descriptions(activityContext)
            }
            LabeledDropdown(
                stringResource(R.string.onboarding_typical_overall_activity),
                activity.value,
                activityOptions,
                activityDescriptions
            ) { activity.value = it }
            Text(
                text = activityDescriptions[activity.value].orEmpty() +
                    stringResource(R.string.onboarding_include_workouts_daily_movement_physical_work),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val goalOptions = GOAL_OPTION_RES.map { it.first to stringResource(it.second) }
            LabeledDropdown(stringResource(R.string.onboarding_goal), goal.value, goalOptions) { goal.value = it }
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
                    parsedTargetWeight,
                    goal.value,
                    activity.value
                )
            }
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.profile_save_body_data))
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
                currentWeight ?: 0.0,
                targetWeightForPlan,
                profile?.sex,
                activity.value,
                acceptActivityRecommendation
            )
        }
        TargetProposalDialog(
            plan = plan,
            currentWeightKg = currentWeight,
            savedWeightMilestoneKg = profile?.targetWeightKg,
            currentActivityLevel = activity.value,
            onApply = { applyPlan(true) },
            onApplyCurrentActivity = { applyPlan(false) },
            onDismiss = onDismissTargetPlan
        )
    }
}

@Composable
private fun LatestReadingCard(
    latest: BodyMeasurement?,
    onAddReading: () -> Unit
) {
    SectionCard(title = stringResource(R.string.profile_body_composition)) {
        if (latest == null) {
            Text(
                stringResource(R.string.profile_no_readings_yet_add_your_weight),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                stringResource(R.string.profile_latest_reading, latest.dateString),
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
            Text(stringResource(R.string.profile_add_a_reading))
        }
    }
}

private const val METRIC_GRID_COLUMNS = 2

@Composable
private fun MetricGrid(m: BodyMeasurement) {
    val units = LocalUnitSystem.current
    val chips = buildList {
        add(stringResource(R.string.onboarding_weight) to Units.weightLabel(m.weightKg, units))
        m.bodyFatPct?.let { add(stringResource(R.string.analytics_body_fat) to "${Units.trim(it)}%") }
        m.bmr?.let { add(stringResource(R.string.analytics_bmr) to "$it kcal") }
        m.muscleMassKg?.let { add(stringResource(R.string.analytics_muscle_mass) to Units.weightLabel(it, units)) }
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

private val PLAN_CAPTION_RES = listOf(
    R.string.profile_analysing,
    R.string.profile_calculating_targets,
    R.string.profile_checking_profile,
    R.string.profile_weighing_options,
    R.string.profile_almost_ready,
    R.string.loading_consulting_ai,
    R.string.profile_mapping_goals,
    R.string.profile_fine_tuning_numbers
)

@Composable
private fun DailyTargetsCard(
    targetCalories: String,
    targetProtein: String,
    targetCarbs: String,
    targetFats: String,
    savedTargetCalories: Int,
    savedTargetProtein: Int,
    savedTargetCarbs: Int,
    savedTargetFats: Int,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatsChange: (String) -> Unit,
    onSaveTargets: (Int, Int, Int, Int) -> Unit,
    planState: TargetPlanUiState,
    isAiConfigured: Boolean,
    animationChoice: String,
    forceShowAnimation: Boolean = false,
    onRequestPlan: () -> Unit
) {
    val caloriesValue = targetCalories.toIntOrNull()
    val proteinValue = targetProtein.toIntOrNull()
    val carbsValue = targetCarbs.toIntOrNull()
    val fatsValue = targetFats.toIntOrNull()
    val caloriesError = when {
        caloriesValue == null -> stringResource(R.string.onboarding_enter_whole_number)
        caloriesValue <= 0 -> stringResource(R.string.profile_must_greater_than_0)
        else -> null
    }
    val proteinError = when {
        proteinValue == null -> stringResource(R.string.onboarding_enter_whole_number)
        proteinValue <= 0 -> stringResource(R.string.profile_must_greater_than_0)
        else -> null
    }
    val carbsError = when {
        carbsValue == null -> stringResource(R.string.onboarding_enter_whole_number)
        carbsValue < 0 -> stringResource(R.string.profile_cannot_negative)
        else -> null
    }
    val fatsError = when {
        fatsValue == null -> stringResource(R.string.onboarding_enter_whole_number)
        fatsValue <= 0 -> stringResource(R.string.profile_must_greater_than_0)
        else -> null
    }
    val targetsChanged = caloriesValue != savedTargetCalories ||
        proteinValue != savedTargetProtein ||
        carbsValue != savedTargetCarbs ||
        fatsValue != savedTargetFats
    val canSaveTargets = caloriesError == null && proteinError == null &&
        carbsError == null && fatsError == null && targetsChanged

    SectionCard(title = stringResource(R.string.profile_daily_targets)) {
        NumberField(
            stringResource(R.string.profile_calories_kcal),
            targetCalories,
            errorMessage = caloriesError,
            onValueChange = onCaloriesChange
        )
        NumberField(
            stringResource(R.string.profile_protein_g),
            targetProtein,
            errorMessage = proteinError,
            onValueChange = onProteinChange
        )
        NumberField(
            stringResource(R.string.profile_carbs_g),
            targetCarbs,
            errorMessage = carbsError,
            onValueChange = onCarbsChange
        )
        NumberField(
            stringResource(R.string.profile_fats_g),
            targetFats,
            errorMessage = fatsError,
            onValueChange = onFatsChange
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = canSaveTargets,
            onClick = {
                onSaveTargets(
                    requireNotNull(caloriesValue),
                    requireNotNull(proteinValue),
                    requireNotNull(carbsValue),
                    requireNotNull(fatsValue)
                )
            }
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.profile_save_manual_targets))
        }
        Text(
            text = stringResource(R.string.profile_manual_targets_are_applied_across_your),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

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
                    captions = PLAN_CAPTION_RES.map { stringResource(it) },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )
            } else {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.profile_calculate_personalized_targets))
            }
        }
        Text(
            text = if (isAiConfigured) {
                stringResource(R.string.profile_targets_and_safe_trend_adjustments_are)
            } else {
                stringResource(R.string.profile_targets_safe_trend_adjustments_calculated_device_2)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        planState.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ReadingsHistoryCard(
    measurements: List<BodyMeasurement>,
    onDelete: (BodyMeasurement) -> Unit
) {
    val units = LocalUnitSystem.current
    SectionCard(title = stringResource(R.string.profile_reading_history)) {
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
                                append(Units.weightLabel(m.weightKg, units))
                                m.bodyFatPct?.let { append(stringResource(R.string.profile_fat, Units.trim(it))) }
                                m.muscleMassKg?.let {
                                    append(stringResource(R.string.profile_kg_muscle, Units.weightText(it, units), Units.weightUnit(units)))
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDelete(m) }) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = stringResource(R.string.profile_delete_reading),
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
    currentWeightKg: Double?,
    savedWeightMilestoneKg: Double?,
    currentActivityLevel: String,
    onApply: () -> Unit,
    onApplyCurrentActivity: () -> Unit,
    onDismiss: () -> Unit
) {
    val units = LocalUnitSystem.current
    val goalLabel = GOAL_OPTION_RES.firstOrNull { it.first == plan.recommendedGoal }?.let { stringResource(it.second) }
        ?: plan.recommendedGoal
    val proposedTargetWeight = plan.targetWeightKg
        ?.takeIf { it.isFinite() && it > 0.0 }
    val isMaintenanceWeight = proposedTargetWeight != null && currentWeightKg != null &&
        abs(proposedTargetWeight - currentWeightKg) < 0.05
    val recommendedActivity = plan.recommendedActivityLevel
        ?.takeIf { ActivityLevels.definition(it) != null }
    val activityChanged = recommendedActivity != null &&
        !recommendedActivity.equals(currentActivityLevel, ignoreCase = true)
    val activityUsed = recommendedActivity ?: currentActivityLevel
    val activityLabelContext = LocalContext.current
    val activityUsedLabel = ActivityLevels.label(activityLabelContext, activityUsed)
    val currentActivityLabel = ActivityLevels.label(activityLabelContext, currentActivityLevel)
    val weightMilestoneChanged = proposedTargetWeight != null &&
        (savedWeightMilestoneKg == null || abs(proposedTargetWeight - savedWeightMilestoneKg) >= 0.05)
    val canApply = plan.targetsChanged || weightMilestoneChanged || activityChanged
    val rationaleParts = splitPlanRationale(plan.rationale)
    val localRationale = rationaleParts.localRationale.takeIf { it.isNotBlank() }
    val coachingNote = rationaleParts.coachingNote
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
        plan.targetsChanged -> stringResource(R.string.profile_science_based_recommendation)
        weightMilestoneChanged && activityChanged -> stringResource(R.string.profile_health_target_recommendation)
        weightMilestoneChanged -> stringResource(R.string.profile_weight_milestone_recommendation)
        recommendedActivity != null -> stringResource(R.string.profile_activity_recommendation)
        else -> stringResource(R.string.profile_re_track)
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
                RecommendationCard(title = stringResource(R.string.profile_daily_plan)) {
                    Text(
                        text = stringResource(R.string.profile_kcal_day, plan.dailyTargetCalories),
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
                        text = stringResource(
                            R.string.profile_protein_g_carbs_g_fats_g,
                            plan.targetProteinG.toString(),
                            plan.targetCarbsG.toString(),
                            plan.targetFatsG.toString(),
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!plan.targetsChanged) {
                        Text(
                            text = stringResource(R.string.profile_your_saved_nutrition_targets_remain_unchanged),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                RecommendationCard(title = stringResource(R.string.profile_activity_used)) {
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
                                stringResource(
                                    R.string.profile_proposal_uses_workout_suggested_level_not_select,
                                    activityUsedLabel,
                                    currentActivityLabel
                                )
                            recommendedActivity != null ->
                                stringResource(
                                    R.string.profile_workout_based_suggestion_matches_selected_level,
                                    currentActivityLabel
                                )
                            else ->
                                stringResource(R.string.profile_proposal_uses_selected_level, currentActivityLabel)
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    recommendedActivity?.let {
                        Text(
                            text = stringResource(
                                R.string.profile_evidence_workout_days_sessions_about_min_week,
                                plan.activityWorkoutDays ?: 0,
                                plan.activityWorkoutCount ?: 0,
                                plan.activityWeeklyMinutes ?: 0,
                                plan.activityWindowDays ?: 28
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.profile_workout_logs_may_not_include_physical),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                RecommendationCard(title = stringResource(R.string.profile_how_these_were_calculated)) {
                    if (
                        restingCalories != null && activityFactor != null &&
                        maintenanceCalories != null && formulaTarget != null &&
                        goalAdjustment != null
                    ) {
                        Text(
                            text = stringResource(R.string.profile_mifflin_st_jeor_resting_estimate_adjusted),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CalculationRow(stringResource(R.string.profile_resting_estimate), "$restingCalories kcal")
                        CalculationRow(stringResource(R.string.profile_activity_multiplier), "×${formatActivityFactor(activityFactor)}")
                        CalculationRow(stringResource(R.string.profile_estimated_maintenance), "$maintenanceCalories kcal")
                        CalculationRow(stringResource(R.string.profile_goal_adjustment), "${signedInteger(goalAdjustment)} kcal")
                        CalculationRow(stringResource(R.string.profile_formula_target), "$formulaTarget kcal")
                        if (stabilityAdjustment != 0) {
                            CalculationRow(stringResource(R.string.profile_target_stability), "${signedInteger(stabilityAdjustment)} kcal")
                            Text(
                                text = stringResource(R.string.profile_small_differences_from_your_saved_target),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (trendAdjustment != 0) {
                            CalculationRow(stringResource(R.string.profile_body_trend_adjustment), "${signedInteger(trendAdjustment)} kcal")
                        }
                        HorizontalDivider()
                        CalculationRow(
                            label = stringResource(R.string.profile_final_daily_target),
                            value = "${plan.dailyTargetCalories} kcal",
                            emphasized = true
                        )
                    } else {
                        Text(
                            text = localRationale
                                ?: stringResource(R.string.profile_this_target_was_calculated_locally_from),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (
                        localRationale != null &&
                        restingCalories != null && activityFactor != null &&
                        maintenanceCalories != null && formulaTarget != null &&
                        goalAdjustment != null
                    ) {
                        HorizontalDivider()
                        Text(
                            text = localRationale,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (plan.bodyTrendStatus != null) {
                    RecommendationCard(title = stringResource(R.string.profile_body_trend_optional)) {
                        if (plan.bodyTrendStatus == "SUFFICIENT") {
                            Text(
                                text = stringResource(
                                    R.string.profile_measurement_days_over_days,
                                    plan.bodyTrendSampleCount ?: 0,
                                    plan.bodyTrendSpanDays ?: 0
                                ),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(
                                    R.string.profile_weight_kg_week,
                                    signedTwoDecimals(Units.kgToDisplay(plan.bodyTrendWeightChangeKg ?: 0.0, units)),
                                    signedTwoDecimals(plan.bodyTrendWeeklyChangePct ?: 0.0),
                                    Units.weightUnit(units)
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                            val compositionParts = buildList {
                                plan.bodyTrendBodyFatChangePct?.let {
                                    add(
                                        stringResource(
                                            R.string.profile_body_fat_points,
                                            signedTwoDecimals(it)
                                        )
                                    )
                                }
                                plan.bodyTrendMuscleMassChangeKg?.let {
                                    add(
                                        stringResource(
                                            R.string.profile_muscle_kg,
                                            signedTwoDecimals(Units.kgToDisplay(it, units)),
                                            Units.weightUnit(units)
                                        )
                                    )
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
                                    stringResource(R.string.profile_formula_target_retained_after_reviewing_trend)
                                } else {
                                    stringResource(
                                        R.string.profile_kcal_day_bounded_trend_adjustment_applied,
                                        signedInteger(trendAdjustment)
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(
                                    R.string.profile_food_logs_previous_28_completed_days_composition,
                                    plan.bodyTrendFoodDaysLogged ?: 0
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = plan.bodyTrendReason
                                    ?: stringResource(R.string.profile_more_consistent_readings_needed),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = stringResource(R.string.profile_no_trend_adjustment_was_applied_the),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                proposedTargetWeight?.let {
                    RecommendationCard(title = stringResource(R.string.profile_weight_milestone)) {
                        Text(
                            text = "${formatOneDecimal(Units.kgToDisplay(it, units))} ${Units.weightUnit(units)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isMaintenanceWeight) {
                                stringResource(R.string.profile_no_automatic_scale_weight_change_is)
                            } else {
                                stringResource(R.string.profile_initial_milestone_up_5_based_selected_weight)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                coachingNote?.let {
                    RecommendationCard(title = stringResource(R.string.profile_ai_coaching_note)) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.profile_these_are_adult_planning_estimates_not),
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
                            plan.targetsChanged -> stringResource(R.string.profile_apply)
                            weightMilestoneChanged && activityChanged -> stringResource(R.string.profile_apply_recommendations)
                            weightMilestoneChanged -> stringResource(R.string.profile_save_weight_milestone)
                            else -> stringResource(R.string.profile_apply_activity_level)
                        }
                    )
                }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_ok)) }
            }
        },
        dismissButton = {
            if (canApply) {
                Row {
                    if (activityChanged) {
                        TextButton(onClick = onApplyCurrentActivity) { Text(stringResource(R.string.profile_use_current)) }
                    }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_discard)) }
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
private data class MetricSpec(
    val key: String,
    @androidx.annotation.StringRes val labelRes: Int,
    val decimal: Boolean = true
)

private val OPTIONAL_METRICS = listOf(
    MetricSpec("bodyFatPct", R.string.profile_body_fat_2),
    MetricSpec("bmr", R.string.profile_bmr_kcal, decimal = false),
    MetricSpec("muscleMassKg", R.string.profile_muscle_mass_kg),
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
    val units = LocalUnitSystem.current
    var weight by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    val optional = remember { mutableStateMapOf<String, String>() }
    var pulledTimestamp by remember { mutableStateOf(0L) }
    var pulledDateString by remember { mutableStateOf("") }
    var pullBusy by remember { mutableStateOf(false) }
    var showInstallFreeScale by remember { mutableStateOf(false) }
    var pullError by remember { mutableStateOf<String?>(null) }
    var pullStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun applyPulled(m: BodyMeasurement) {
        pulledTimestamp = m.timestamp
        pulledDateString = m.dateString
        weight = fmtOptional(Units.kgToDisplay(m.weightKg, units)) ?: ""
        fun put(key: String, value: Double?) {
            val s = fmtOptional(value)
            if (s != null) optional[key] = s else optional.remove(key)
        }
        m.bodyFatPct?.let { optional["bodyFatPct"] = Units.trim(it) } ?: optional.remove("bodyFatPct")
        m.bmr?.let { optional["bmr"] = it.toString() } ?: optional.remove("bmr")
        put("muscleMassKg", m.muscleMassKg?.let { Units.kgToDisplay(it, units) })
        showAdvanced = optional.isNotEmpty()
        pullStatus = context.getString(R.string.profile_loaded_freescale_reading_review_tap_save)
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
            Text(stringResource(R.string.profile_add_a_reading), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

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
                                pullError = e.message ?: context.getString(R.string.profile_could_not_load_freescale_reading)
                            },
                        )
                    }
                },
            ) {
                Text(if (pullBusy) stringResource(R.string.profile_loading_from_freescale) else stringResource(R.string.profile_pull_latest_from_freescale))
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

            NumberField(context.getString(R.string.workout_weight_kg, Units.weightUnit(units)), weight, decimal = true) { weight = it }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.profile_smart_scale_metrics_optional),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showAdvanced = !showAdvanced }) {
                    Icon(
                        if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (showAdvanced) context.getString(R.string.openrouter_collapse) else stringResource(R.string.openrouter_expand)
                    )
                }
            }

            AnimatedVisibility(visible = showAdvanced) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OPTIONAL_METRICS.forEach { spec ->
                        NumberField(
                            label = stringResource(spec.labelRes, Units.weightUnit(units)),
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
                            weightKg = Units.displayToKg(weight.toDoubleOrNull() ?: 0.0, units),
                            bodyFatPct = d("bodyFatPct")?.let { Units.trim(it).toDouble() },
                            bmr = i("bmr"),
                            muscleMassKg = d("muscleMassKg")?.let { Units.displayToKg(it, units) },
                        )
                    )
                }
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.profile_save_reading))
            }
        }
    }

    if (showInstallFreeScale) {
        AlertDialog(
            onDismissRequest = { showInstallFreeScale = false },
            title = { Text(stringResource(R.string.profile_freescale_not_installed)) },
            text = {
                Text(stringResource(R.string.profile_install_freescale_to_pull_the_latest),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showInstallFreeScale = false
                        uriHandler.openUri(FREESCALE_WEBSITE_URL)
                    },
                ) {
                    Text(stringResource(R.string.profile_get_freescale))
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallFreeScale = false }) {
                    Text(stringResource(R.string.donate_not_now))
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
    errorMessage: String? = null,
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
        isError = errorMessage != null,
        supportingText = errorMessage?.let { message ->
            { Text(message) }
        },
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

