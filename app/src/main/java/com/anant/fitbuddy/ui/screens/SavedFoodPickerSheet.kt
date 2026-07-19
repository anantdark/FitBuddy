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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.anant.fitbuddy.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.data.database.SavedFood
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
    onDismiss: () -> Unit
) {
    val pickEnabled = mode != SavedFoodSheetMode.MANAGE_LIBRARY
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
                        "Foods you scan or save as preset for reuse when logging or building meals."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            if (foods.isEmpty()) {
                Text(
                    text = "No saved foods yet. Scan a barcode on the Body tab, or save a food as preset after AI review.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(foods, key = { it.id }) { food ->
                        SavedFoodRow(
                            food = food,
                            pickEnabled = pickEnabled,
                            onPick = { onPick(food) },
                            onDelete = { onDelete(food) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedFoodRow(
    food: SavedFood,
    pickEnabled: Boolean,
    onPick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (pickEnabled) Modifier.pressable(onClick = onPick) else Modifier)
            .padding(horizontal = 24.dp, vertical = 12.dp),
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
        Spacer(Modifier.size(16.dp))
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
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete ${food.name}",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
