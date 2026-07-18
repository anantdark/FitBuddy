package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anant.fitbuddy.data.model.CommonExercise
import com.anant.fitbuddy.data.model.EXERCISE_EQUIPMENT_GROUPS
import com.anant.fitbuddy.data.model.Equipment
import com.anant.fitbuddy.data.model.ExerciseDraft
import com.anant.fitbuddy.data.model.WorkoutDraft
import com.anant.fitbuddy.ui.components.pressable
import com.anant.fitbuddy.ui.viewmodel.WorkoutLogUiState

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
    pickerExercises: List<CommonExercise>,
    isClassifyingCustom: Boolean,
    isInferringExercises: Boolean,
    isAiOnline: Boolean,
    onClassifyCustom: (rawName: String, onResolved: (name: String, equipment: String) -> Unit) -> Unit,
    onInferExercises: (description: String, onResolved: (List<ExerciseDraft>) -> Unit) -> Unit,
    onSave: (WorkoutDraft) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditing = initialDraft != null
    Dialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var sessionName by remember { mutableStateOf(initialDraft?.name ?: "Workout") }
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
        var pendingExercise by remember { mutableStateOf<Pair<String, String>?>(null) }

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
                            "No exercises added yet. Tap below to add one from the common list " +
                                "(dumbbell, bench, and more) or a custom exercise.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    itemsIndexed(exercises, key = { index, _ -> index }) { index, exercise ->
                        ExerciseRow(exercise = exercise, onDelete = { exercises.removeAt(index) })
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
                isClassifyingCustom = isClassifyingCustom,
                isInferringExercises = isInferringExercises,
                isAiOnline = isAiOnline,
                onPick = { name, equipment ->
                    showPicker = false
                    pendingExercise = name to equipment
                },
                onClassifyCustom = { rawName ->
                    onClassifyCustom(rawName) { name, equipment ->
                        showPicker = false
                        pendingExercise = name to equipment
                    }
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

        pendingExercise?.let { (name, equipment) ->
            AddExerciseDetailsDialog(
                exerciseName = name,
                equipment = equipment,
                onAdd = { draft ->
                    exercises.add(draft)
                    pendingExercise = null
                },
                onDismiss = { pendingExercise = null }
            )
        }
    }
}

@Composable
private fun ExerciseRow(exercise: ExerciseDraft, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    exerciseSummary(exercise),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
}

/** Picker listing the common + saved exercise library, plus custom text and AI inference. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    exercises: List<CommonExercise>,
    isClassifyingCustom: Boolean,
    isInferringExercises: Boolean,
    isAiOnline: Boolean,
    onPick: (name: String, equipment: String) -> Unit,
    onClassifyCustom: (rawName: String) -> Unit,
    onInferExercises: (description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedEquipment by remember { mutableStateOf<String?>(null) }
    val isBusy = isClassifyingCustom || isInferringExercises

    val filtered = remember(query, selectedEquipment, exercises) {
        exercises.filter { exercise ->
            (selectedEquipment == null || exercise.equipment == selectedEquipment) &&
                (query.isBlank() || exercise.name.contains(query.trim(), ignoreCase = true))
        }
    }
    val trimmedQuery = query.trim()
    val showCustomRow = trimmedQuery.isNotBlank() &&
        !trimmedQuery.contains('\n') &&
        filtered.none { it.name.equals(trimmedQuery, ignoreCase = true) }
    val showInferButton = trimmedQuery.isNotBlank()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Add exercise",
                    style = MaterialTheme.typography.titleLarge
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search or describe exercises") },
                    placeholder = {
                        Text("e.g. 4×8 bench press, 3×12 lateral raises")
                    },
                    minLines = 2,
                    maxLines = 5,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showInferButton) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBusy && isAiOnline,
                        onClick = { onInferExercises(trimmedQuery) }
                    ) {
                        if (isInferringExercises) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Inferring…")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Infer exercises with AI")
                        }
                    }
                    if (!isAiOnline) {
                        Text(
                            "Connect an AI provider in Settings to infer from text.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EXERCISE_EQUIPMENT_GROUPS.forEach { group ->
                        FilterChip(
                            selected = selectedEquipment == group,
                            onClick = {
                                selectedEquipment = if (selectedEquipment == group) null else group
                            },
                            label = { Text(group) },
                            enabled = !isBusy
                        )
                    }
                }
                if (showCustomRow) {
                    CustomExerciseRow(
                        name = trimmedQuery,
                        isLoading = isClassifyingCustom,
                        onPick = { onClassifyCustom(trimmedQuery) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp)
            ) {
                items(filtered, key = { it.name }) { exercise ->
                    ExercisePickerRow(
                        exercise = exercise,
                        enabled = !isBusy,
                        onPick = { onPick(exercise.name, exercise.equipment) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExercisePickerRow(exercise: CommonExercise, enabled: Boolean, onPick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(enabled = enabled, onClick = onPick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(exercise.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                exercise.equipment,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(Icons.Filled.Add, contentDescription = "Add ${exercise.name}")
    }
}

@Composable
private fun CustomExerciseRow(name: String, isLoading: Boolean, onPick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(enabled = !isLoading, onClick = onPick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                if (isLoading) "Recognising \"$name\"…" else "Add custom exercise \"$name\"",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** Sets/reps/weight for strength, or time/distance for cardio, before adding to the session. */
@Composable
private fun AddExerciseDetailsDialog(
    exerciseName: String,
    equipment: String,
    onAdd: (ExerciseDraft) -> Unit,
    onDismiss: () -> Unit
) {
    val isCardio = equipment == Equipment.CARDIO
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("10") }
    var weight by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    var distance by remember { mutableStateOf("") }
    val needsWeight = !isCardio && equipment != Equipment.BODYWEIGHT

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exerciseName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            DetailField("Sets", sets) { sets = it.filter { c -> c.isDigit() }.take(2) }
                        }
                        Box(Modifier.weight(1f)) {
                            DetailField("Reps", reps) { reps = it.filter { c -> c.isDigit() }.take(3) }
                        }
                    }
                    if (needsWeight) {
                        DetailField("Weight (kg, optional)", weight, decimal = true) {
                            weight = it.filter { c -> c.isDigit() || c == '.' }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = if (isCardio) {
                    (duration.toIntOrNull() ?: 0) > 0
                } else {
                    (sets.toIntOrNull() ?: 0) > 0 && (reps.toIntOrNull() ?: 0) > 0
                },
                onClick = {
                    if (isCardio) {
                        onAdd(
                            ExerciseDraft(
                                name = exerciseName,
                                sets = 1,
                                reps = 1,
                                equipment = equipment,
                                durationMinutes = duration.toIntOrNull() ?: 1,
                                distanceKm = distance.toDoubleOrNull()?.takeIf { it > 0 }
                            )
                        )
                    } else {
                        onAdd(
                            ExerciseDraft(
                                name = exerciseName,
                                sets = sets.toIntOrNull() ?: 1,
                                reps = reps.toIntOrNull() ?: 1,
                                weightKg = weight.toDoubleOrNull(),
                                equipment = equipment
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
