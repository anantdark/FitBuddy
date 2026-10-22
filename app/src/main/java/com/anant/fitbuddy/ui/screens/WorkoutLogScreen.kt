package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.anant.fitbuddy.ui.theme.appShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import com.anant.fitbuddy.ui.components.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import com.anant.fitbuddy.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import com.anant.fitbuddy.ui.components.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.anant.fitbuddy.ui.components.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anant.fitbuddy.data.database.ExerciseUsage
import com.anant.fitbuddy.data.model.CatalogExercise
import com.anant.fitbuddy.data.model.Equipment
import com.anant.fitbuddy.data.model.ExerciseDraft
import com.anant.fitbuddy.data.model.ExerciseLoggingMode
import com.anant.fitbuddy.data.model.WorkoutDraft
import com.anant.fitbuddy.ui.viewmodel.WorkoutLogUiState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

private val SET_PRESETS = listOf("1", "2", "3")
private val REP_PRESETS = listOf("8", "12", "15")
private val HOLD_SEC_PRESETS = listOf("20", "30", "45", "60", "90")
private val CARRY_SEC_PRESETS = listOf("30", "45", "60", "90", "120")
private val WEIGHT_PRESETS = listOf("5", "10", "15", "20")

/**
 * Full-screen session builder: name + exercises (from the common library or custom) + duration.
 * Pass [initialDraft] to pre-fill it for editing a previously logged workout; leave it null to
 * log a brand-new session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutLogDialog(
    state: WorkoutLogUiState,
    initialDraft: WorkoutDraft? = null,
    pickerExercises: List<CatalogExercise>,
    exerciseUsages: List<ExerciseUsage> = emptyList(),
    bodyPartFilters: List<String> = emptyList(),
    equipmentFilters: List<String> = emptyList(),
    pinnedBodyPartFilters: List<String> = emptyList(),
    pinnedEquipmentFilters: List<String> = emptyList(),
    catalogLoading: Boolean = false,
    isClassifyingCustom: Boolean,
    isInferringExercises: Boolean,
    isNamingWorkout: Boolean,
    isAiOnline: Boolean,
    onClassifyCustom: (rawName: String, onResolved: (CatalogExercise) -> Unit) -> Unit,
    onDeleteCustomExercise: (name: String) -> Unit = {},
    onInferExercises: (description: String, onResolved: (List<ExerciseDraft>) -> Unit) -> Unit,
    onSuggestName: (exerciseNames: List<String>, onResolved: (String) -> Unit) -> Unit,
    onRecordPick: (name: String, exerciseId: String?) -> Unit = { _, _ -> },
    onToggleFavorite: (name: String, exerciseId: String?, favorite: Boolean) -> Unit = { _, _, _ -> },
    onPromoteBodyPartFilter: (String) -> Unit = {},
    onPromoteEquipmentFilter: (String) -> Unit = {},
    onSave: (WorkoutDraft) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditing = initialDraft != null
    Dialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var sessionName by remember {
            mutableStateOf(initialDraft?.name ?: WorkoutDraft.DEFAULT_NAME)
        }
        val exercises = remember {
            mutableStateListOf<ExerciseDraft>().apply { initialDraft?.exercises?.let(::addAll) }
        }
        var durationText by remember {
            mutableStateOf((initialDraft?.durationMinutes ?: WorkoutDraft.DEFAULT_DURATION_MINUTES).toString())
        }
        // When editing, respect the already-logged duration instead of re-estimating it from
        // scratch as soon as the pre-filled exercises are loaded in.
        var durationTouched by remember { mutableStateOf(isEditing) }
        var showDurationEdit by remember { mutableStateOf(isEditing) }
        var showPicker by remember { mutableStateOf(false) }
        var pendingExercise by remember { mutableStateOf<CatalogExercise?>(null) }

        val exerciseSnapshot = exercises.toList()
        LaunchedEffect(exerciseSnapshot, durationTouched) {
            if (!durationTouched) {
                durationText = WorkoutDraft.estimateDurationMinutes(exerciseSnapshot).toString()
            }
        }

        LaunchedEffect(state.savedSuccessfully) {
            if (state.savedSuccessfully) onDismiss()
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(if (isEditing) "Edit Workout" else "Log Workout") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, enabled = !state.isSaving) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        }
                    }
                )
            },
            bottomBar = {
                Column(modifier = Modifier.padding(16.dp)) {
                    state.error?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = exercises.isNotEmpty() && sessionName.isNotBlank() && !state.isSaving,
                        onClick = {
                            val duration = (if (durationTouched) durationText.toIntOrNull() else null)
                                ?: WorkoutDraft.estimateDurationMinutes(exercises.toList())
                            onSave(
                                WorkoutDraft(
                                    name = sessionName.trim(),
                                    durationMinutes = duration,
                                    exercises = exercises.toList()
                                )
                            )
                        }
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Estimating calories…")
                        } else {
                            Text(if (isEditing) "Save Changes" else "Save & Estimate Calories")
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = sessionName,
                        onValueChange = { sessionName = it },
                        label = { Text("Session name") },
                        singleLine = true,
                        enabled = !state.isSaving,
                        trailingIcon = {
                            if (isNamingWorkout) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        val names = exercises.map { it.name }
                                        if (names.isNotEmpty()) {
                                            onSuggestName(names) { suggested ->
                                                sessionName = suggested
                                            }
                                        }
                                    },
                                    enabled = exercises.isNotEmpty() && isAiOnline && !state.isSaving
                                ) {
                                    Icon(
                                        Icons.Filled.AutoAwesome,
                                        contentDescription = "Suggest name with AI"
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    if (showDurationEdit) {
                        OutlinedTextField(
                            value = durationText,
                            onValueChange = { input ->
                                durationTouched = true
                                durationText = input.filter { it.isDigit() }.take(3)
                            },
                            label = { Text("Session duration (minutes)") },
                            supportingText = {
                                Text("Used to estimate calories burned — not a substitute for logging reps.")
                            },
                            singleLine = true,
                            enabled = !state.isSaving,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Est. ${durationText.ifBlank { "—" }} min session",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Auto-calculated for calorie burn (cardio time + ~3 min per strength set).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = { showDurationEdit = true },
                                enabled = !state.isSaving
                            ) { Text("Edit") }
                        }
                    }
                }

                item {
                    Text(
                        text = "Exercises",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (exercises.isEmpty()) {
                    item {
                        Text(
                            "No exercises added yet. Tap below to browse the exercise catalog " +
                                "(with demo GIFs), filter by body part or equipment, or add with AI.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    itemsIndexed(exercises, key = { index, _ -> index }) { index, exercise ->
                        ExerciseRow(
                            exercise = exercise,
                            gifUrl = exercise.gifUrl
                                ?: pickerExercises.firstOrNull {
                                    it.name.equals(exercise.name, ignoreCase = true)
                                }?.gifUrl,
                            onEdit = { updated -> exercises[index] = updated },
                            onDelete = { exercises.removeAt(index) }
                        )
                    }
                }

                item {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving,
                        onClick = { showPicker = true }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add exercise")
                    }
                }
            }
        }

        if (showPicker) {
            ExercisePickerSheet(
                exercises = pickerExercises,
                usages = exerciseUsages,
                bodyPartFilters = bodyPartFilters,
                equipmentFilters = equipmentFilters,
                pinnedBodyPartFilters = pinnedBodyPartFilters,
                pinnedEquipmentFilters = pinnedEquipmentFilters,
                catalogLoading = catalogLoading,
                isClassifyingCustom = isClassifyingCustom,
                isInferringExercises = isInferringExercises,
                isAiOnline = isAiOnline,
                onPick = { exercise ->
                    showPicker = false
                    pendingExercise = exercise
                },
                onToggleFavorite = { exercise, favorite ->
                    onToggleFavorite(exercise.name, exercise.exerciseId, favorite)
                },
                onPromoteBodyPartFilter = onPromoteBodyPartFilter,
                onPromoteEquipmentFilter = onPromoteEquipmentFilter,
                onClassifyCustom = { rawName ->
                    onClassifyCustom(rawName) { exercise ->
                        showPicker = false
                        pendingExercise = exercise
                    }
                },
                onDeleteCustomExercise = { name ->
                    onDeleteCustomExercise(name)
                    exercises.removeAll { it.name.equals(name, ignoreCase = true) }
                },
                onInferExercises = { description ->
                    onInferExercises(description) { drafts ->
                        exercises.addAll(drafts)
                        showPicker = false
                    }
                },
                onDismiss = { showPicker = false }
            )
        }

        pendingExercise?.let { exercise ->
            AddExerciseDetailsDialog(
                exercise = exercise,
                onAdd = { draft ->
                    onRecordPick(exercise.name, exercise.exerciseId)
                    exercises.add(
                        draft.copy(
                            gifUrl = exercise.gifUrl,
                            bodyParts = exercise.bodyParts,
                            loggingModeOverride = exercise.resolvedLoggingMode()
                        )
                    )
                    pendingExercise = null
                },
                onDismiss = {
                    // Return to the catalog after canceling sets/reps entry.
                    pendingExercise = null
                    showPicker = true
                }
            )
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: ExerciseDraft,
    gifUrl: String?,
    onEdit: (ExerciseDraft) -> Unit,
    onDelete: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showEditDialog = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExerciseGifThumb(
                url = gifUrl,
                animated = false,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    exerciseSummary(exercise),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Tap to edit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Remove ${exercise.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showEditDialog) {
        EditExerciseDialog(
            exercise = exercise.copy(gifUrl = gifUrl ?: exercise.gifUrl),
            onSave = { updated ->
                onEdit(updated)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }
}

/** Inline editor for an exercise already in the session — edit sets/reps/weight or duration. */
@Composable
private fun EditExerciseDialog(
    exercise: ExerciseDraft,
    onSave: (ExerciseDraft) -> Unit,
    onDismiss: () -> Unit
) {
    val mode = exercise.resolvedLoggingMode()
    val isCardio = mode == ExerciseLoggingMode.CARDIO
    val isHold = mode == ExerciseLoggingMode.TIMED_HOLD || mode == ExerciseLoggingMode.TIMED_CARRY
    val isCarry = mode == ExerciseLoggingMode.TIMED_CARRY
    var sets by remember { mutableStateOf(exercise.sets.toString()) }
    var reps by remember { mutableStateOf(exercise.reps.toString()) }
    var holdSec by remember {
        mutableStateOf((exercise.durationSeconds ?: if (isCarry) 60 else 30).toString())
    }
    var weight by remember { mutableStateOf(exercise.weightKg?.toString() ?: "") }
    var duration by remember { mutableStateOf(exercise.durationMinutes?.toString() ?: "30") }
    var distance by remember { mutableStateOf(exercise.distanceKm?.let { formatDistanceKm(it) } ?: "") }
    val needsWeight = !isCardio && (isCarry || exercise.equipment != Equipment.BODYWEIGHT)
    val timedLabel = if (isCarry) "Time (sec)" else "Hold (sec)"
    val timedPresets = if (isCarry) CARRY_SEC_PRESETS else HOLD_SEC_PRESETS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${exercise.name}") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                ExerciseGifThumb(
                    url = exercise.gifUrl,
                    animated = true,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(appShape(14.dp))
                )
                if (isCardio) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            DetailField("Duration (min)", duration) {
                                duration = it.filter { c -> c.isDigit() }.take(3)
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            DetailField("Distance (km)", distance, decimal = true) {
                                distance = it.filter { c -> c.isDigit() || c == '.' }
                            }
                        }
                    }
                } else if (isHold) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            PresetNumberField(
                                label = "Sets",
                                value = sets,
                                presets = SET_PRESETS,
                                onValueChange = { sets = it.filter { c -> c.isDigit() }.take(2) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            PresetNumberField(
                                label = timedLabel,
                                value = holdSec,
                                presets = timedPresets,
                                onValueChange = { holdSec = it.filter { c -> c.isDigit() }.take(4) }
                            )
                        }
                    }
                    if (needsWeight) {
                        PresetNumberField(
                            label = "Weight (kg)",
                            value = weight,
                            presets = WEIGHT_PRESETS,
                            decimal = true,
                            onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } }
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            PresetNumberField(
                                label = "Sets",
                                value = sets,
                                presets = SET_PRESETS,
                                onValueChange = { sets = it.filter { c -> c.isDigit() }.take(2) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            PresetNumberField(
                                label = "Reps",
                                value = reps,
                                presets = REP_PRESETS,
                                onValueChange = { reps = it.filter { c -> c.isDigit() }.take(3) }
                            )
                        }
                    }
                    if (needsWeight) {
                        PresetNumberField(
                            label = "Weight (kg)",
                            value = weight,
                            presets = WEIGHT_PRESETS,
                            decimal = true,
                            onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = when {
                    isCardio -> (duration.toIntOrNull() ?: 0) > 0
                    isHold -> (sets.toIntOrNull() ?: 0) > 0 && (holdSec.toIntOrNull() ?: 0) > 0
                    else -> (sets.toIntOrNull() ?: 0) > 0 && (reps.toIntOrNull() ?: 0) > 0
                },
                onClick = {
                    when {
                        isCardio -> onSave(
                            exercise.copy(
                                durationMinutes = duration.toIntOrNull() ?: exercise.durationMinutes,
                                distanceKm = distance.toDoubleOrNull()?.takeIf { it > 0 },
                                loggingModeOverride = ExerciseLoggingMode.CARDIO
                            )
                        )
                        isHold -> onSave(
                            exercise.copy(
                                sets = sets.toIntOrNull() ?: exercise.sets,
                                reps = 1,
                                weightKg = weight.toDoubleOrNull(),
                                durationSeconds = holdSec.toIntOrNull() ?: exercise.durationSeconds,
                                loggingModeOverride = if (isCarry) {
                                    ExerciseLoggingMode.TIMED_CARRY
                                } else {
                                    ExerciseLoggingMode.TIMED_HOLD
                                }
                            )
                        )
                        else -> onSave(
                            exercise.copy(
                                sets = sets.toIntOrNull() ?: exercise.sets,
                                reps = reps.toIntOrNull() ?: exercise.reps,
                                weightKg = weight.toDoubleOrNull(),
                                durationSeconds = null,
                                loggingModeOverride = ExerciseLoggingMode.REPS
                            )
                        )
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Sets/reps/weight for strength, hold time for isometrics, or time/distance for cardio. */
@Composable
private fun AddExerciseDetailsDialog(
    exercise: CatalogExercise,
    onAdd: (ExerciseDraft) -> Unit,
    onDismiss: () -> Unit
) {
    val exerciseName = exercise.name
    val equipment = exercise.equipmentTag
    val gifUrl = exercise.gifUrl
    val mode = exercise.resolvedLoggingMode()
    val isCardio = mode == ExerciseLoggingMode.CARDIO
    val isCarry = mode == ExerciseLoggingMode.TIMED_CARRY
    val isHold = mode == ExerciseLoggingMode.TIMED_HOLD || isCarry
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("12") }
    var holdSec by remember { mutableStateOf(if (isCarry) "60" else "30") }
    var weight by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    var distance by remember { mutableStateOf("") }
    val needsWeight = !isCardio && (isCarry || equipment != Equipment.BODYWEIGHT)
    val timedLabel = if (isCarry) "Time (sec)" else "Hold (sec)"
    val timedPresets = if (isCarry) CARRY_SEC_PRESETS else HOLD_SEC_PRESETS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exerciseName) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                ExerciseGifThumb(
                    url = gifUrl,
                    animated = true,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(appShape(14.dp))
                )
                if (isCardio) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            DetailField("Duration (min)", duration) {
                                duration = it.filter { c -> c.isDigit() }.take(3)
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            DetailField("Distance (km, optional)", distance, decimal = true) {
                                distance = it.filter { c -> c.isDigit() || c == '.' }
                            }
                        }
                    }
                } else if (isHold) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            PresetNumberField(
                                label = "Sets",
                                value = sets,
                                presets = SET_PRESETS,
                                onValueChange = { sets = it.filter { c -> c.isDigit() }.take(2) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            PresetNumberField(
                                label = timedLabel,
                                value = holdSec,
                                presets = timedPresets,
                                onValueChange = { holdSec = it.filter { c -> c.isDigit() }.take(4) }
                            )
                        }
                    }
                    if (needsWeight) {
                        PresetNumberField(
                            label = "Weight (kg, optional)",
                            value = weight,
                            presets = WEIGHT_PRESETS,
                            decimal = true,
                            onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } }
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            PresetNumberField(
                                label = "Sets",
                                value = sets,
                                presets = SET_PRESETS,
                                onValueChange = { sets = it.filter { c -> c.isDigit() }.take(2) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            PresetNumberField(
                                label = "Reps",
                                value = reps,
                                presets = REP_PRESETS,
                                onValueChange = { reps = it.filter { c -> c.isDigit() }.take(3) }
                            )
                        }
                    }
                    if (needsWeight) {
                        PresetNumberField(
                            label = "Weight (kg, optional)",
                            value = weight,
                            presets = WEIGHT_PRESETS,
                            decimal = true,
                            onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = when {
                    isCardio -> (duration.toIntOrNull() ?: 0) > 0
                    isHold -> (sets.toIntOrNull() ?: 0) > 0 && (holdSec.toIntOrNull() ?: 0) > 0
                    else -> (sets.toIntOrNull() ?: 0) > 0 && (reps.toIntOrNull() ?: 0) > 0
                },
                onClick = {
                    when {
                        isCardio -> onAdd(
                            ExerciseDraft(
                                name = exerciseName,
                                sets = 1,
                                reps = 1,
                                equipment = Equipment.CARDIO,
                                durationMinutes = duration.toIntOrNull() ?: 1,
                                distanceKm = distance.toDoubleOrNull()?.takeIf { it > 0 },
                                loggingModeOverride = ExerciseLoggingMode.CARDIO,
                                gifUrl = gifUrl
                            )
                        )
                        isHold -> onAdd(
                            ExerciseDraft(
                                name = exerciseName,
                                sets = sets.toIntOrNull() ?: 1,
                                reps = 1,
                                weightKg = weight.toDoubleOrNull(),
                                equipment = equipment,
                                durationSeconds = holdSec.toIntOrNull() ?: 30,
                                loggingModeOverride = if (isCarry) {
                                    ExerciseLoggingMode.TIMED_CARRY
                                } else {
                                    ExerciseLoggingMode.TIMED_HOLD
                                },
                                gifUrl = gifUrl
                            )
                        )
                        else -> onAdd(
                            ExerciseDraft(
                                name = exerciseName,
                                sets = sets.toIntOrNull() ?: 1,
                                reps = reps.toIntOrNull() ?: 1,
                                weightKg = weight.toDoubleOrNull(),
                                equipment = equipment,
                                loggingModeOverride = ExerciseLoggingMode.REPS,
                                gifUrl = gifUrl
                            )
                        )
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun exerciseSummary(exercise: ExerciseDraft): String {
    if (exercise.isCardio()) {
        return buildList {
            exercise.durationMinutes?.let { add("$it min") }
            exercise.distanceKm?.let { km -> add("${formatDistanceKm(km)} km") }
        }.joinToString(" · ").ifBlank { "Cardio" }
    }
    val weightText = exercise.weightKg?.let { " @ ${it}kg" } ?: ""
    if (exercise.isTimedHold()) {
        val sec = exercise.durationSeconds ?: 30
        return "${exercise.sets} sets × ${sec}s$weightText"
    }
    return "${exercise.sets} sets × ${exercise.reps} reps$weightText"
}

private fun formatDistanceKm(km: Double): String =
    if (km % 1.0 == 0.0) km.toInt().toString() else "%.1f".format(km)

@Composable
private fun DetailField(
    label: String,
    value: String,
    decimal: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Number field: tap the field to type; tap the trailing arrow for quick presets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetNumberField(
    label: String,
    value: String,
    presets: List<String>,
    decimal: Boolean = false,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
            ),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            presets.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(if (decimal) "$preset kg" else preset) },
                    onClick = {
                        onValueChange(preset)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
