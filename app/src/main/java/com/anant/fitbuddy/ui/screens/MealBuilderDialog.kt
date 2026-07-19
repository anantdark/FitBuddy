package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import com.anant.fitbuddy.ui.components.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.anant.fitbuddy.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import com.anant.fitbuddy.ui.components.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.anant.fitbuddy.ui.components.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anant.fitbuddy.data.model.FoodEntryDraft
import com.anant.fitbuddy.data.model.MealDraft
import com.anant.fitbuddy.data.model.toMealDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealBuilderDialog(
    initialDraft: MealDraft? = null,
    items: SnapshotStateList<FoodEntryDraft>,
    onReview: (MealDraft) -> Unit,
    onCreateFood: () -> Unit,
    onEditFood: (Int) -> Unit,
    onScanProduct: () -> Unit,
    onPickPreset: () -> Unit,
    onDismiss: () -> Unit
) {
    val isEditing = initialDraft != null
    var mealName by remember(initialDraft) { mutableStateOf(initialDraft?.name ?: "Meal") }
    var showManualAdd by remember { mutableStateOf(false) }

    val totalCalories = items.sumOf { it.totalCalories }
    val totalProtein = items.sumOf { it.totalProtein }
    val totalCarbs = items.sumOf { it.totalCarbs }
    val totalFats = items.sumOf { it.totalFats }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(if (isEditing) "Edit meal" else "Build a meal") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        }
                    }
                )
            },
            bottomBar = {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = items.isNotEmpty() && mealName.isNotBlank(),
                    onClick = {
                        onReview(
                            items.toMealDraft(mealName.trim()).copy(
                                timestamp = initialDraft?.timestamp ?: System.currentTimeMillis()
                            )
                        )
                    }
                ) { Text(if (isEditing) "Review changes" else "Review & save meal") }
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
                        value = mealName,
                        onValueChange = { mealName = it },
                        label = { Text("Meal name") },
                        placeholder = { Text("e.g. Breakfast") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    TotalsCard(totalCalories, totalProtein, totalCarbs, totalFats)
                }
                item {
                    Text(
                        text = "Foods in this meal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Each food has its own ingredients — like exercises in a workout.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (items.isEmpty()) {
                    item {
                        Text(
                            "Add saved foods (presets), scan packaged items, create a new food, " +
                                "or enter macros manually.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    itemsIndexed(items, key = { i, _ -> i }) { index, item ->
                        MealFoodCard(
                            item = item,
                            onServingsChange = { servings ->
                                items[index] = item.copy(servings = servings.coerceAtLeast(0.25))
                            },
                            onEdit = { onEditFood(index) },
                            onDelete = { items.removeAt(index) }
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = onScanProduct
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Scan")
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = onPickPreset
                        ) {
                            Icon(Icons.Filled.Restaurant, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Saved food")
                        }
                    }
                }
                item {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onCreateFood
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create new food")
                    }
                }
                item {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showManualAdd = true }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add food manually")
                    }
                }
            }
        }
    }

    if (showManualAdd) {
        ManualFoodEntryDialog(
            onAdd = { item ->
                items.add(item)
                showManualAdd = false
            },
            onDismiss = { showManualAdd = false }
        )
    }
}

@Composable
private fun MealFoodCard(
    item: FoodEntryDraft,
    onServingsChange: (Double) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        "${item.totalCalories} kcal · ${item.ingredients.size} ingredient(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.ingredients.isNotEmpty()) {
                        Text(
                            item.ingredients.joinToString(", ") { it.name },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit ingredients")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Servings:", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { onServingsChange((item.servings - 0.5).coerceAtLeast(0.25)) }) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = formatServings(item.servings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(onClick = { onServingsChange(item.servings + 0.5) }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

private fun formatServings(servings: Double): String =
    if (servings % 1.0 == 0.0) servings.toInt().toString() else servings.toString()

@Composable
private fun ManualFoodEntryDialog(
    onAdd: (FoodEntryDraft) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fats by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add food") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { it.isDigit() }.take(5) },
                    label = { Text("Calories (1 serving)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it.filter { it.isDigit() }.take(4) },
                        label = { Text("Protein") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it.filter { it.isDigit() }.take(4) },
                        label = { Text("Carbs") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fats,
                        onValueChange = { fats = it.filter { it.isDigit() }.take(4) },
                        label = { Text("Fats") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && (calories.toIntOrNull() ?: 0) > 0,
                onClick = {
                    onAdd(
                        com.anant.fitbuddy.data.model.IngredientDraft.fromAbsolute(
                            name = name.trim(),
                            weightG = 100,
                            calories = calories.toIntOrNull() ?: 0,
                            protein = protein.toIntOrNull() ?: 0,
                            carbs = carbs.toIntOrNull() ?: 0,
                            fats = fats.toIntOrNull() ?: 0
                        ).let { ing ->
                            FoodEntryDraft(
                                name = name.trim(),
                                ingredients = listOf(ing)
                            )
                        }
                    )
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Seeds a meal builder list from an existing draft (for editing logged meals). */
fun MealDraft.toFoodEntryList(): SnapshotStateList<FoodEntryDraft> =
    foods.toMutableStateList()
