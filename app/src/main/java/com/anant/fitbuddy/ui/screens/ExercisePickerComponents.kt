package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anant.fitbuddy.data.database.ExerciseUsage
import com.anant.fitbuddy.data.model.CatalogExercise
import com.anant.fitbuddy.ui.components.OutlinedButton
import com.anant.fitbuddy.ui.components.TextButton
import com.anant.fitbuddy.ui.components.pressable

private const val RECENT_LIMIT = 8
private const val FREQUENT_LIMIT = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    exercises: List<CatalogExercise>,
    usages: List<ExerciseUsage>,
    bodyPartFilters: List<String>,
    equipmentFilters: List<String>,
    catalogLoading: Boolean,
    isClassifyingCustom: Boolean,
    isInferringExercises: Boolean,
    isAiOnline: Boolean,
    onPick: (CatalogExercise) -> Unit,
    onClassifyCustom: (rawName: String) -> Unit,
    onInferExercises: (description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedBodyPart by remember { mutableStateOf<String?>(null) }
    var selectedEquipment by remember { mutableStateOf<String?>(null) }
    val isBusy = isClassifyingCustom || isInferringExercises

    val filtered = remember(query, selectedBodyPart, selectedEquipment, exercises) {
        exercises.filter { exercise ->
            val bodyOk = selectedBodyPart == null ||
                exercise.bodyParts.any { it.equals(selectedBodyPart, ignoreCase = true) }
            val equipOk = selectedEquipment == null ||
                exercise.equipments.any { it.equals(selectedEquipment, ignoreCase = true) } ||
                exercise.equipmentTag.equals(selectedEquipment, ignoreCase = true) ||
                exercise.primaryEquipmentLabel.equals(selectedEquipment, ignoreCase = true)
            val queryOk = query.isBlank() ||
                exercise.name.contains(query.trim(), ignoreCase = true)
            bodyOk && equipOk && queryOk
        }
    }

    val byName = remember(filtered) { filtered.associateBy { it.name.lowercase() } }
    val recent = remember(usages, byName, query, selectedBodyPart, selectedEquipment) {
        usages.asSequence()
            .sortedByDescending { it.lastUsedAt }
            .mapNotNull { byName[it.name.lowercase()] }
            .distinctBy { it.name.lowercase() }
            .take(RECENT_LIMIT)
            .toList()
    }
    val recentNames = remember(recent) { recent.map { it.name.lowercase() }.toSet() }
    val frequent = remember(usages, byName, recentNames) {
        usages.asSequence()
            .sortedByDescending { it.useCount }
            .mapNotNull { byName[it.name.lowercase()] }
            .filter { it.name.lowercase() !in recentNames }
            .distinctBy { it.name.lowercase() }
            .take(FREQUENT_LIMIT)
            .toList()
    }
    val featuredNames = remember(recent, frequent) {
        (recent + frequent).map { it.name.lowercase() }.toSet()
    }
    val allRest = remember(filtered, featuredNames) {
        filtered.filter { it.name.lowercase() !in featuredNames }
            .sortedBy { it.name.lowercase() }
    }

    val trimmedQuery = query.trim()
    val showCustomRow = trimmedQuery.isNotBlank() &&
        !trimmedQuery.contains('\n') &&
        filtered.none { it.name.equals(trimmedQuery, ignoreCase = true) }
    val showInferButton = trimmedQuery.isNotBlank()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (imeVisible) Modifier.fillMaxHeight(0.92f) else Modifier)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Add exercise",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (catalogLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                }
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
                if (bodyPartFilters.isNotEmpty()) {
                    FilterChipRow(
                        labels = bodyPartFilters,
                        selected = selectedBodyPart,
                        enabled = !isBusy,
                        onSelect = {
                            selectedBodyPart = if (selectedBodyPart == it) null else it
                        }
                    )
                }
                val equipLabels = equipmentFilters.ifEmpty {
                    listOf(
                        com.anant.fitbuddy.data.model.Equipment.DUMBBELL,
                        com.anant.fitbuddy.data.model.Equipment.BARBELL,
                        com.anant.fitbuddy.data.model.Equipment.BODYWEIGHT,
                        com.anant.fitbuddy.data.model.Equipment.MACHINE,
                        com.anant.fitbuddy.data.model.Equipment.CARDIO,
                        com.anant.fitbuddy.data.model.Equipment.BENCH,
                        com.anant.fitbuddy.data.model.Equipment.OTHER
                    )
                }
                FilterChipRow(
                    labels = equipLabels,
                    selected = selectedEquipment,
                    enabled = !isBusy,
                    onSelect = {
                        selectedEquipment = if (selectedEquipment == it) null else it
                    }
                )
                if (showCustomRow) {
                    CustomExerciseRow(
                        name = trimmedQuery,
                        isLoading = isClassifyingCustom,
                        onPick = { onClassifyCustom(trimmedQuery) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (imeVisible) Modifier.weight(1f)
                        else Modifier.heightIn(max = 420.dp)
                    ),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp)
            ) {
                if (recent.isNotEmpty()) {
                    item(key = "hdr-recent") { SectionHeader("Recent") }
                    items(recent, key = { "r-${it.name}" }) { exercise ->
                        ExercisePickerRow(
                            exercise = exercise,
                            enabled = !isBusy,
                            onPick = { onPick(exercise) }
                        )
                    }
                }
                if (frequent.isNotEmpty()) {
                    item(key = "hdr-frequent") { SectionHeader("Frequent") }
                    items(frequent, key = { "f-${it.name}" }) { exercise ->
                        ExercisePickerRow(
                            exercise = exercise,
                            enabled = !isBusy,
                            onPick = { onPick(exercise) }
                        )
                    }
                }
                item(key = "hdr-all") {
                    SectionHeader(if (recent.isEmpty() && frequent.isEmpty()) "Exercises" else "All")
                }
                items(allRest, key = { "a-${it.name}" }) { exercise ->
                    ExercisePickerRow(
                        exercise = exercise,
                        enabled = !isBusy,
                        onPick = { onPick(exercise) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipRow(
    labels: List<String>,
    selected: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { label ->
            FilterChip(
                selected = selected == label,
                onClick = { onSelect(label) },
                label = { Text(label) },
                enabled = enabled
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun ExercisePickerRow(exercise: CatalogExercise, enabled: Boolean, onPick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(enabled = enabled, onClick = onPick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExerciseGifThumb(url = exercise.gifUrl, modifier = Modifier.size(56.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(exercise.name, style = MaterialTheme.typography.bodyLarge)
            val subtitle = listOfNotNull(
                exercise.primaryBodyPartLabel,
                exercise.primaryEquipmentLabel
            ).joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(Icons.Filled.Add, contentDescription = "Add ${exercise.name}")
    }
}

@Composable
fun CustomExerciseRow(name: String, isLoading: Boolean, onPick: () -> Unit) {
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

@Composable
fun ExerciseDetailDialog(
    exercise: CatalogExercise,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exercise.name) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExerciseGifThumb(
                    url = exercise.gifUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                )
                val meta = listOfNotNull(
                    exercise.primaryBodyPartLabel?.let { "Body: $it" },
                    "Equipment: ${exercise.primaryEquipmentLabel}",
                    exercise.targetMuscles.takeIf { it.isNotEmpty() }
                        ?.joinToString(", ")
                        ?.let { "Target: $it" }
                )
                meta.forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                if (exercise.instructions.isNotEmpty()) {
                    Text("How to", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    exercise.instructions.forEach { step ->
                        Text(step, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue) { Text("Add to workout") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ExerciseGifThumb(url: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.matchParentSize()
        ) {}
        if (url.isNullOrBlank()) {
            Icon(
                Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}
