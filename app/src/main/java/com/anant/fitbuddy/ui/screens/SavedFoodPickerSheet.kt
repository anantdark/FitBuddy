package com.anant.fitbuddy.ui.screens

import com.anant.fitbuddy.R

import androidx.compose.ui.res.stringResource

import com.anant.fitbuddy.util.formatMacro
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.anant.fitbuddy.ui.theme.appShape
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.data.database.SavedFood
import com.anant.fitbuddy.ui.components.IconButton
import com.anant.fitbuddy.ui.components.OutlinedButton
import com.anant.fitbuddy.ui.components.pressable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

enum class SavedFoodSheetMode { PICK_FOR_MEAL, LOG_TO_DAY }

/** Bottom sheet for picking a saved food (meal builder or one-tap log). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedFoodPickerSheet(
    foods: List<SavedFood>,
    mode: SavedFoodSheetMode = SavedFoodSheetMode.PICK_FOR_MEAL,
    onPick: (SavedFood) -> Unit = {},
    onDelete: (SavedFood) -> Unit,
    onEdit: (SavedFood) -> Unit = {},
    onCreate: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var editingFood by remember { mutableStateOf<SavedFood?>(null) }
    val filtered = remember(query, foods) {
        val q = query.trim()
        if (q.isEmpty()) foods
        else foods.filter { it.name.contains(q, ignoreCase = true) }
    }
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
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.food_saved_foods),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Text(
                text = when (mode) {
                    SavedFoodSheetMode.PICK_FOR_MEAL ->
                        stringResource(R.string.saved_food_add_food_meal_edit_delete_saved_foods)
                    SavedFoodSheetMode.LOG_TO_DAY ->
                        stringResource(R.string.saved_food_tap_food_log_day_re_viewing_edit)
                },
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
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.food_create_food))
            }

            if (foods.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.meal_search)) },
                    placeholder = { Text(stringResource(R.string.food_food_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            when {
                foods.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.food_no_saved_foods_yet_create_one),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
                filtered.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.saved_food_no_foods_match, query),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (imeVisible) Modifier.weight(1f)
                                else Modifier.heightIn(max = 420.dp)
                            )
                    ) {
                        items(filtered, key = { it.id }) { food ->
                            SavedFoodRow(
                                food = food,
                                onPick = { onPick(food) },
                                onEdit = { editingFood = food },
                                onDelete = { onDelete(food) }
                            )
                        }
                    }
                }
            }
        }
    }

    editingFood?.let { food ->
        EditSavedFoodDialog(
            food = food,
            onSave = {
                onEdit(it)
                editingFood = null
            },
            onDismiss = { editingFood = null }
        )
    }
}

@Composable
private fun SavedFoodRow(
    food: SavedFood,
    onPick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onPick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = appShape(20.dp),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    LogItemIcons.forFood(food.iconTopic),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${food.calories} kcal · P ${food.proteinG.formatMacro()}g · C ${food.carbsG.formatMacro()}g · F ${food.fatsG.formatMacro()}g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = stringResource(R.string.meal_preset_edit, food.name)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.meal_preset_delete, food.name),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
