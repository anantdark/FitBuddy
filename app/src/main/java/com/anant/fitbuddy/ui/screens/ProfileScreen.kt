package com.anant.fitbuddy.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import com.anant.fitbuddy.ui.components.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.data.database.BodyMeasurement
import com.anant.fitbuddy.data.database.UserProfile
import com.anant.fitbuddy.data.model.TargetPlanResponse
import com.anant.fitbuddy.ui.viewmodel.DashboardUiState
import com.anant.fitbuddy.ui.viewmodel.TargetPlanUiState

private val GOAL_OPTIONS = listOf(
    "AUTO" to "Let AI decide",
    "LOSE_WEIGHT" to "Lose weight",
    "GAIN_MUSCLE" to "Gain muscle",
    "RECOMP" to "Body recomposition"
)
private val ACTIVITY_OPTIONS = listOf(
    "SEDENTARY" to "Sedentary",
    "LIGHT" to "Lightly active",
    "MODERATE" to "Moderately active",
    "ACTIVE" to "Active",
    "VERY_ACTIVE" to "Very active"
)

@Composable
fun BodyScreen(
    profile: UserProfile?,
    latestMeasurement: BodyMeasurement?,
    measurements: List<BodyMeasurement>,
    savedFoodCount: Int,
    targetPlanState: TargetPlanUiState,
    isAiConfigured: Boolean,
    onSave: (
        weightKg: Double,
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
        sex: String?,
        activityLevel: String
    ) -> Unit,
    onDismissTargetPlan: () -> Unit,
    onScanSavedFood: () -> Unit,
    onManageSavedFoods: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weight = remember(profile) { mutableStateOf(profile?.weightKg?.toString() ?: "") }
    val goal = remember(profile) { mutableStateOf(profile?.goal ?: "RECOMP") }
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
            onScan = onScanSavedFood,
            onManage = onManageSavedFoods
        )

        AiTargetsCard(
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
            LabeledDropdown("Activity level", activity.value, ACTIVITY_OPTIONS) { activity.value = it }
            LabeledDropdown("Goal", goal.value, GOAL_OPTIONS) { goal.value = it }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(
                    weight.value.toDoubleOrNull() ?: 0.0,
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
        TargetProposalDialog(
            plan = plan,
            onApply = {
                onApplyTargetPlan(
                    plan,
                    profile?.age ?: 0,
                    profile?.heightCm ?: 0.0,
                    weight.value.toDoubleOrNull() ?: 0.0,
                    profile?.sex,
                    activity.value
                )
            },
            onDismiss = onDismissTargetPlan
        )
    }
}

@Composable
private fun FoodLibraryCard(
    savedFoodCount: Int,
    onScan: () -> Unit,
    onManage: () -> Unit
) {
    SectionCard(title = "Food library") {
        Text(
            text = if (savedFoodCount == 0) {
                "No saved foods yet — scan barcodes or bookmark foods after AI review."
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
                onClick = onScan
            ) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Scan barcode")
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

@Composable
private fun AiTargetsCard(
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
    onRequestPlan: () -> Unit
) {
    SectionCard(title = "Daily targets") {
        NumberField("Calories (kcal)", targetCalories, onValueChange = onCaloriesChange)
        NumberField("Protein (g)", targetProtein, onValueChange = onProteinChange)
        NumberField("Carbs (g)", targetCarbs, onValueChange = onCarbsChange)
        NumberField("Fats (g)", targetFats, onValueChange = onFatsChange)

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = isAiConfigured && !planState.isLoading,
            onClick = onRequestPlan
        ) {
            if (planState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.width(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Text(if (planState.isLoading) "Analysing…" else "Recommend with AI")
        }
        if (!isAiConfigured) {
            Text(
                "Connect an AI provider in Settings to get recommendations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        planState.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (!rationale.isNullOrBlank()) {
            HorizontalDivider()
            Text("Why these targets", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(
                rationale,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val goalLabel = GOAL_OPTIONS.firstOrNull { it.first == plan.recommendedGoal }?.second
        ?: plan.recommendedGoal
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
        title = { Text("AI recommendation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Suggested goal: $goalLabel", fontWeight = FontWeight.SemiBold)
                Text("${plan.dailyTargetCalories} kcal / day")
                Text("Protein ${plan.targetProteinG}g · Carbs ${plan.targetCarbsG}g · Fats ${plan.targetFatsG}g")
                HorizontalDivider()
                Text(
                    plan.rationale,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onApply) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Discard") } }
    )
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
    var weight by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    val optional = remember { mutableStateMapOf<String, String>() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add a reading", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            NumberField("Weight (kg)", weight, decimal = true) { weight = it }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
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
                            timestamp = 0L, // set in the ViewModel
                            dateString = "",
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
                            fatMassKg = d("fatMassKg")
                        )
                    )
                }
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save reading")
            }
        }
    }
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
            val filtered = input.filter { it.isDigit() || (decimal && it == '.') }
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
                    text = { Text(text) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
