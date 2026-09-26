package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.clickable
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import coil.size.Size
import com.anant.fitbuddy.util.GifFirstFrameDecoder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anant.fitbuddy.data.database.ExerciseUsage
import com.anant.fitbuddy.data.model.CatalogExercise
import com.anant.fitbuddy.data.model.Equipment
import com.anant.fitbuddy.ui.components.IconButton
import com.anant.fitbuddy.ui.components.OutlinedButton
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
    onToggleFavorite: (CatalogExercise, favorite: Boolean) -> Unit,
    onClassifyCustom: (rawName: String) -> Unit,
    onInferExercises: (description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var selectedBodyPart by remember { mutableStateOf<String?>(null) }
    var selectedEquipment by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<CatalogExercise?>(null) }
    val isBusy = isClassifyingCustom || isInferringExercises

    LaunchedEffect(query) {
        delay(200)
        debouncedQuery = query
    }

    val usageScoreByName = remember(usages) {
        usages.associate { it.name.lowercase() to it.useCount }
    }

    // Rank chips from usage rows only (not a full catalog scan) for snappy first open.
    val rankedBodyParts = remember(bodyPartFilters, usages, exercises) {
        rankFilterLabelsFromUsages(bodyPartFilters, usages, exercises) { it.bodyParts }
    }
    val defaultEquip = listOf(
        Equipment.DUMBBELL,
        Equipment.BARBELL,
        Equipment.BODYWEIGHT,
        Equipment.MACHINE,
        Equipment.CARDIO,
        Equipment.BENCH,
        Equipment.OTHER
    )
    val rankedEquipment = remember(equipmentFilters, usages, exercises) {
        rankFilterLabelsFromUsages(
            labels = equipmentFilters.ifEmpty { defaultEquip },
            usages = usages,
            exercises = exercises
        ) { ex ->
            buildList {
                addAll(ex.equipments)
                add(ex.equipmentTag)
                add(ex.primaryEquipmentLabel)
            }
        }
    }

    val filterQuery = debouncedQuery.trim()
    val filtered = remember(filterQuery, selectedBodyPart, selectedEquipment, exercises) {
        if (filterQuery.isEmpty() && selectedBodyPart == null && selectedEquipment == null) {
            exercises
        } else {
            exercises.filter { exercise ->
                val bodyOk = selectedBodyPart == null ||
                    exercise.bodyParts.any { it.equals(selectedBodyPart, ignoreCase = true) }
                val equipOk = selectedEquipment == null ||
                    exercise.equipments.any { it.equals(selectedEquipment, ignoreCase = true) } ||
                    exercise.equipmentTag.equals(selectedEquipment, ignoreCase = true) ||
                    exercise.primaryEquipmentLabel.equals(selectedEquipment, ignoreCase = true)
                val queryOk = filterQuery.isEmpty() ||
                    exercise.name.contains(filterQuery, ignoreCase = true)
                bodyOk && equipOk && queryOk
            }
        }
    }

    val favoriteNames = remember(usages) {
        usages.filter { it.isFavorite }.map { it.name.lowercase() }.toSet()
    }
    val byNameAll = remember(exercises) { exercises.associateBy { it.name.lowercase() } }
    val favorites = remember(usages, byNameAll, filtered) {
        val filteredKeys = filtered.map { it.name.lowercase() }.toSet()
        usages.asSequence()
            .filter { it.isFavorite }
            .sortedBy { it.name.lowercase() }
            .mapNotNull { byNameAll[it.name.lowercase()] }
            .filter { it.name.lowercase() in filteredKeys }
            .distinctBy { it.name.lowercase() }
            .toList()
    }

    val byName = remember(filtered) { filtered.associateBy { it.name.lowercase() } }
    val recent = remember(usages, byName, favorites) {
        val favKeys = favorites.map { it.name.lowercase() }.toSet()
        usages.asSequence()
            .filter { it.lastUsedAt > 0L }
            .sortedByDescending { it.lastUsedAt }
            .mapNotNull { byName[it.name.lowercase()] }
            .filter { it.name.lowercase() !in favKeys }
            .distinctBy { it.name.lowercase() }
            .take(RECENT_LIMIT)
            .toList()
    }
    val recentNames = remember(recent) { recent.map { it.name.lowercase() }.toSet() }
    val frequent = remember(usages, byName, recentNames, favorites) {
        val favKeys = favorites.map { it.name.lowercase() }.toSet()
        usages.asSequence()
            .filter { it.useCount > 0 }
            .sortedByDescending { it.useCount }
            .mapNotNull { byName[it.name.lowercase()] }
            .filter { it.name.lowercase() !in recentNames && it.name.lowercase() !in favKeys }
            .distinctBy { it.name.lowercase() }
            .take(FREQUENT_LIMIT)
            .toList()
    }
    val featuredNames = remember(favorites, recent, frequent) {
        (favorites + recent + frequent).map { it.name.lowercase() }.toSet()
    }
    val allRest = remember(
        filtered,
        featuredNames,
        filterQuery,
        selectedBodyPart,
        selectedEquipment
    ) {
        val rest = filtered.asSequence()
            .filter { it.name.lowercase() !in featuredNames }
        // Avoid sorting the full 1500-item catalog on every open; only sort when narrowed.
        if (filterQuery.isEmpty() && selectedBodyPart == null && selectedEquipment == null) {
            rest.toList()
        } else {
            rest.sortedBy { it.name.lowercase() }.toList()
        }
    }

    val trimmedQuery = query.trim()
    val showCustomRow = trimmedQuery.isNotBlank() &&
        !trimmedQuery.contains('\n') &&
        filtered.none { it.name.equals(trimmedQuery, ignoreCase = true) }
    val showInferButton = trimmedQuery.isNotBlank()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val activeFilterCount =
        (if (selectedBodyPart != null) 1 else 0) + (if (selectedEquipment != null) 1 else 0)

    ModalBottomSheet(
        onDismissRequest = {
            if (preview != null) preview = null else onDismiss()
        },
        sheetState = sheetState
    ) {
        val selected = preview
        if (selected != null) {
            ExercisePreviewPane(
                exercise = selected,
                isFavorite = selected.name.lowercase() in favoriteNames,
                onBack = { preview = null },
                onToggleFavorite = { fav ->
                    onToggleFavorite(selected, fav)
                },
                onAdd = {
                    onPick(selected)
                    preview = null
                }
            )
        } else {
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
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Choose exercise",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = when {
                                        catalogLoading -> "Updating catalog…"
                                        filtered.size == exercises.size ->
                                            "${exercises.size} exercises"
                                        else -> "${filtered.size} of ${exercises.size}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (catalogLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Search or describe a workout") },
                            minLines = 1,
                            maxLines = 3,
                            enabled = !isBusy,
                            leadingIcon = {
                                Icon(Icons.Filled.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (showInferButton) {
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isBusy && isAiOnline,
                                onClick = { onInferExercises(trimmedQuery) }
                            ) {
                                if (isInferringExercises) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Inferring…")
                                } else {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Infer with AI")
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

                        if (rankedBodyParts.isNotEmpty()) {
                            FilterSection(
                                title = "Body part",
                                labels = rankedBodyParts,
                                selected = selectedBodyPart,
                                enabled = !isBusy,
                                onSelect = {
                                    selectedBodyPart = if (selectedBodyPart == it) null else it
                                }
                            )
                        }
                        FilterSection(
                            title = "Equipment",
                            labels = rankedEquipment,
                            selected = selectedEquipment,
                            enabled = !isBusy,
                            onSelect = {
                                selectedEquipment = if (selectedEquipment == it) null else it
                            }
                        )

                        if (activeFilterCount > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "$activeFilterCount filter${if (activeFilterCount == 1) "" else "s"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                AssistChip(
                                    onClick = {
                                        selectedBodyPart = null
                                        selectedEquipment = null
                                    },
                                    label = { Text("Clear") },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (imeVisible) Modifier.weight(1f)
                                else Modifier.heightIn(max = 440.dp)
                            ),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 4.dp,
                            bottom = 20.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (favorites.isNotEmpty()) {
                            item(key = "hdr-fav") { SectionHeader("Favourites") }
                            items(favorites, key = { "fav-${it.name}" }) { exercise ->
                                ExercisePickerRow(
                                    exercise = exercise,
                                    isFavorite = true,
                                    enabled = !isBusy,
                                    onPick = { preview = exercise },
                                    onToggleFavorite = { onToggleFavorite(exercise, false) }
                                )
                            }
                        }
                        if (recent.isNotEmpty()) {
                            item(key = "hdr-recent") { SectionHeader("Recent") }
                            items(recent, key = { "r-${it.name}" }) { exercise ->
                                ExercisePickerRow(
                                    exercise = exercise,
                                    isFavorite = exercise.name.lowercase() in favoriteNames,
                                    enabled = !isBusy,
                                    onPick = { preview = exercise },
                                    onToggleFavorite = {
                                        onToggleFavorite(
                                            exercise,
                                            exercise.name.lowercase() !in favoriteNames
                                        )
                                    }
                                )
                            }
                        }
                        if (frequent.isNotEmpty()) {
                            item(key = "hdr-frequent") { SectionHeader("Most used") }
                            items(frequent, key = { "f-${it.name}" }) { exercise ->
                                ExercisePickerRow(
                                    exercise = exercise,
                                    isFavorite = exercise.name.lowercase() in favoriteNames,
                                    enabled = !isBusy,
                                    onPick = { preview = exercise },
                                    onToggleFavorite = {
                                        onToggleFavorite(
                                            exercise,
                                            exercise.name.lowercase() !in favoriteNames
                                        )
                                    }
                                )
                            }
                        }
                        item(key = "hdr-all") {
                            SectionHeader(
                                if (favorites.isEmpty() && recent.isEmpty() && frequent.isEmpty()) {
                                    "All exercises"
                                } else {
                                    "Browse all"
                                }
                            )
                        }
                        if (allRest.isEmpty() && favorites.isEmpty() && recent.isEmpty() && frequent.isEmpty()) {
                            item(key = "empty") {
                                Text(
                                    "No exercises match those filters.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 24.dp)
                                )
                            }
                        }
                        items(allRest, key = { "a-${it.name}" }) { exercise ->
                            ExercisePickerRow(
                                exercise = exercise,
                                isFavorite = exercise.name.lowercase() in favoriteNames,
                                enabled = !isBusy,
                                onPick = { preview = exercise },
                                onToggleFavorite = {
                                    onToggleFavorite(
                                        exercise,
                                        exercise.name.lowercase() !in favoriteNames
                                    )
                                }
                            )
                        }
                    }
                }
        }
    }
}

/**
 * Ranks filter chips from favourited/used exercises only (not a full 1500-item scan).
 */
private fun rankFilterLabelsFromUsages(
    labels: List<String>,
    usages: List<ExerciseUsage>,
    exercises: List<CatalogExercise>,
    tagsOf: (CatalogExercise) -> List<String>
): List<String> {
    if (labels.isEmpty()) return emptyList()
    if (usages.isEmpty()) return labels
    val byName = exercises.associateBy { it.name.lowercase() }
    val scores = mutableMapOf<String, Int>()
    for (usage in usages) {
        val weight = usage.useCount.coerceAtLeast(if (usage.isFavorite) 1 else 0)
        if (weight <= 0) continue
        val exercise = byName[usage.name.lowercase()] ?: continue
        for (tag in tagsOf(exercise)) {
            val match = labels.firstOrNull { it.equals(tag, ignoreCase = true) } ?: continue
            scores[match] = (scores[match] ?: 0) + weight
        }
    }
    return labels.sortedWith(
        compareByDescending<String> { scores[it] ?: 0 }
            .thenBy { it.lowercase() }
    )
}

@Composable
private fun FilterSection(
    title: String,
    labels: List<String>,
    selected: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                    enabled = enabled,
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun ExercisePreviewPane(
    exercise: CatalogExercise,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to list")
            }
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onToggleFavorite(!isFavorite) }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove favourite" else "Add favourite",
                    tint = if (isFavorite) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExerciseGifThumb(
                url = exercise.gifUrl,
                animated = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                exercise.primaryBodyPartLabel?.let { MetaChip(it) }
                MetaChip(exercise.primaryEquipmentLabel)
                exercise.targetMuscles.take(3).forEach { MetaChip(it) }
            }

            if (exercise.instructions.isNotEmpty()) {
                Text(
                    "How to",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                exercise.instructions.forEachIndexed { index, step ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            cleanInstruction(step),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add to workout")
        }
    }
}

@Composable
private fun MetaChip(label: String) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledLabelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun ExercisePickerRow(
    exercise: CatalogExercise,
    isFavorite: Boolean,
    enabled: Boolean,
    onPick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(enabled = enabled, onClick = onPick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExerciseGifThumb(
                url = exercise.gifUrl,
                animated = false,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = listOfNotNull(
                    exercise.primaryBodyPartLabel,
                    exercise.primaryEquipmentLabel
                ).joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove favourite" else "Add favourite",
                tint = if (isFavorite) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled, onClick = onToggleFavorite)
                    .padding(8.dp)
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "View ${exercise.name}",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomExerciseRow(name: String, isLoading: Boolean, onPick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(enabled = !isLoading, onClick = onPick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                if (isLoading) "Recognising \"$name\"…" else "Add custom \"$name\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ExerciseGifThumb(
    url: String?,
    animated: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNullOrBlank()) {
            Icon(
                Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val sidePx = if (animated) 512 else 128
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .size(Size(sidePx, sidePx))
                    .memoryCacheKey(if (animated) url else "$url-static-$sidePx")
                    .diskCacheKey(if (animated) url else "$url-static-$sidePx")
                    .apply {
                        if (!animated) {
                            setParameter(GifFirstFrameDecoder.PARAM_STATIC_GIF, true)
                        }
                    }
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Strips leading "Step:N" prefixes from ExerciseDB instruction strings. */
private fun cleanInstruction(raw: String): String =
    raw.replace(Regex("""^Step:\s*\d+\s*"""), "").trim().ifBlank { raw }
