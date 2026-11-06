package com.anant.fitbuddy.ui.screens

import com.anant.fitbuddy.R

import androidx.compose.ui.res.stringResource

import com.anant.fitbuddy.util.formatMacro
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.anant.fitbuddy.ui.theme.appShape
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import com.anant.fitbuddy.ui.util.LogItemIcons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.data.database.MealPreset
import com.anant.fitbuddy.ui.components.IconButton
import com.anant.fitbuddy.ui.components.OutlinedButton
import com.anant.fitbuddy.ui.components.pressable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

/** Bottom sheet listing saved meals; tap to quick-log the whole meal to the active day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPresetPickerSheet(
    presets: List<MealPreset>,
    onPick: (MealPreset) -> Unit,
    onEdit: (MealPreset) -> Unit,
    onDelete: (MealPreset) -> Unit,
    onCreate: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, presets) {
        val q = query.trim()
        if (q.isEmpty()) presets
        else presets.filter { it.name.contains(q, ignoreCase = true) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.meal_saved_meals),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Text(
                text = stringResource(R.string.meal_tap_a_meal_to_log_it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                onClick = onCreate
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.meal_create_meal))
            }

            if (presets.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.meal_search)) },
                    placeholder = { Text(stringResource(R.string.meal_meal_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            when {
                presets.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.meal_no_saved_meals_yet_create_one),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
                filtered.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.meal_preset_no_meals_match, query),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        items(filtered, key = { it.id }) { preset ->
                            MealPresetRow(
                                preset = preset,
                                onPick = { onPick(preset) },
                                onEdit = { onEdit(preset) },
                                onDelete = { onDelete(preset) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealPresetRow(
    preset: MealPreset,
    onPick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val foodCount = preset.foods?.size ?: 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onPick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = appShape(20.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    LogItemIcons.forFood(preset.iconTopic),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preset.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = buildString {
                    append("${preset.calories} kcal")
                    if (foodCount > 0) append(stringResource(R.string.meal_preset_food_s, foodCount))
                    append(" · P ${preset.proteinG.formatMacro()}g · C ${preset.carbsG.formatMacro()}g · F ${preset.fatsG.formatMacro()}g")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = stringResource(R.string.meal_preset_edit, preset.name)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.meal_preset_delete, preset.name),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
