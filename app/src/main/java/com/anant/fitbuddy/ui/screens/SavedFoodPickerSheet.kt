package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.data.database.SavedFood
import com.anant.fitbuddy.ui.components.IconButton
import com.anant.fitbuddy.ui.components.pressable

enum class SavedFoodSheetMode { PICK_FOR_MEAL, LOG_TO_DAY, MANAGE_LIBRARY }

/** Bottom sheet for picking a saved food (meal builder or one-tap log), or managing the library. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedFoodPickerSheet(
    foods: List<SavedFood>,
    mode: SavedFoodSheetMode = SavedFoodSheetMode.PICK_FOR_MEAL,
    onPick: (SavedFood) -> Unit = {},
    onDelete: (SavedFood) -> Unit,
    onEdit: (SavedFood) -> Unit = {},
    onMove: (SavedFood, direction: Int) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var editingFood by remember { mutableStateOf<SavedFood?>(null) }
    val pickEnabled = mode != SavedFoodSheetMode.MANAGE_LIBRARY
    val manageActions = mode == SavedFoodSheetMode.MANAGE_LIBRARY
    val filtered = remember(query, foods) {
        val q = query.trim()
        if (q.isEmpty()) foods
        else foods.filter { it.name.contains(q, ignoreCase = true) }
    }
    val canReorder = manageActions && query.isBlank() && foods.size > 1

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = "Saved foods",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Text(
                text = when (mode) {
                    SavedFoodSheetMode.PICK_FOR_MEAL ->
                        "Add a food to this meal. Save foods via barcode scan or Save as preset on food review."
                    SavedFoodSheetMode.LOG_TO_DAY ->
                        "Tap a food to log it to the day you're viewing."
                    SavedFoodSheetMode.MANAGE_LIBRARY ->
                        "Edit, reorder, or delete foods you scan or save as preset."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            if (foods.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    placeholder = { Text("Food name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            when {
                foods.isEmpty() -> {
                    Text(
                        text = "No saved foods yet. Scan a barcode on the Body tab, or save a food as preset after AI review.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
                filtered.isEmpty() -> {
                    Text(
                        text = "No foods match \"$query\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        itemsIndexed(filtered, key = { _, food -> food.id }) { _, food ->
                            val fullIndex = if (canReorder) foods.indexOfFirst { it.id == food.id } else -1
                            SavedFoodRow(
                                food = food,
                                pickEnabled = pickEnabled,
                                showEdit = manageActions,
                                canMoveUp = canReorder && fullIndex > 0,
                                canMoveDown = canReorder && fullIndex in 0 until foods.lastIndex,
                                onPick = { onPick(food) },
                                onEdit = { editingFood = food },
                                onMoveUp = { onMove(food, -1) },
                                onMoveDown = { onMove(food, 1) },
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
    pickEnabled: Boolean,
    showEdit: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPick: () -> Unit,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (pickEnabled) Modifier.pressable(onClick = onPick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${food.calories} kcal · P ${food.proteinG}g · C ${food.carbsG}g · F ${food.fatsG}g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (canMoveUp || canMoveDown) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
                }
            }
        }
        if (showEdit) {
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Edit ${food.name}"
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete ${food.name}",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
