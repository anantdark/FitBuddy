package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import com.anant.fitbuddy.ui.components.FitBuddySnackbarHost
import com.anant.fitbuddy.ui.components.showFitBuddyPill
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anant.fitbuddy.data.model.FoodDraft
import com.anant.fitbuddy.data.model.IngredientDraft

/** Full-screen editable review of an AI-analysed meal, shown before it is saved to the log. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodReviewDialog(
    draft: FoodDraft,
    isReanalyzing: Boolean,
    reviewMessage: String?,
    onReviewMessageShown: () -> Unit,
    onConfirm: (FoodDraft) -> Unit,
    onSaveAsPreset: (FoodDraft) -> Unit,
    onReanalyze: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Editable working copy, re-seeded if a different draft arrives.
        var dishName by remember(draft) { mutableStateOf(draft.dishName) }
        val ingredients = remember(draft) { draft.ingredients.toMutableStateList() }
        var showAddDialog by remember { mutableStateOf(false) }

        // Local snackbar: the app-level one is hidden behind this full-screen dialog.
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        // Surface reanalyze feedback (clarification/error) here, then clear it.
        androidx.compose.runtime.LaunchedEffect(reviewMessage) {
            reviewMessage?.let {
                snackbarHostState.showFitBuddyPill(it)
                onReviewMessageShown()
            }
        }

        val totalCalories = ingredients.sumOf { it.calories }
        val totalProtein = ingredients.sumOf { it.protein }
        val totalCarbs = ingredients.sumOf { it.carbs }
        val totalFats = ingredients.sumOf { it.fats }
        val totalWeight = ingredients.sumOf { it.weightG }

        // Editable total weight. While the field isn't focused it mirrors the ingredient sum;
        // editing it rescales every ingredient proportionally (from a baseline captured on focus,
        // so per-keystroke rounding doesn't drift the proportions).
        var weightText by remember(draft) { mutableStateOf(totalWeight.toString()) }
        var editingWeight by remember { mutableStateOf(false) }
        var scaleBaseline by remember { mutableStateOf<Pair<Int, List<Int>>?>(null) }
        androidx.compose.runtime.LaunchedEffect(totalWeight, editingWeight) {
            if (!editingWeight) weightText = totalWeight.toString()
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { FitBuddySnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Review food") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        IconButton(
                            enabled = ingredients.isNotEmpty() && dishName.isNotBlank() && !isReanalyzing,
                            onClick = {
                                val name = dishName.trim()
                                onSaveAsPreset(
                                    draft.copy(
                                        dishName = name,
                                        ingredients = ingredients.toList()
                                    )
                                )
                                scope.launch {
                                    snackbarHostState.showFitBuddyPill("Saved \"$name\" to food library")
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.BookmarkAdd,
                                contentDescription = "Save to food library"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    ) { Text("Cancel") }
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = ingredients.isNotEmpty() && dishName.isNotBlank() && !isReanalyzing,
                        onClick = {
                            onConfirm(
                                draft.copy(
                                    dishName = dishName.trim(),
                                    ingredients = ingredients.toList()
                                )
                            )
                        }
                    ) { Text("Log food") }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TotalsCard(totalCalories, totalProtein, totalCarbs, totalFats)
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }.take(5)
                                weightText = digits
                                val newTotal = digits.toIntOrNull() ?: 0
                                val base = scaleBaseline
                                if (base != null && base.first > 0 && newTotal > 0 &&
                                    base.second.size == ingredients.size
                                ) {
                                    // Scale each ingredient's per-unit weight proportionally,
                                    // keeping quantities (unit counts) intact.
                                    val (baseTotal, baseUnitWeights) = base
                                    val factor = newTotal.toDouble() / baseTotal
                                    for (i in ingredients.indices) {
                                        val unit = (baseUnitWeights[i] * factor)
                                            .roundToInt().coerceAtLeast(0)
                                        ingredients[i] = ingredients[i].copy(unitWeightG = unit)
                                    }
                                }
                            },
                            label = { Text("Total weight (g)") },
                            singleLine = true,
                            enabled = !isReanalyzing && ingredients.isNotEmpty(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focus ->
                                    if (focus.isFocused) {
                                        editingWeight = true
                                        scaleBaseline = ingredients.sumOf { it.weightG } to
                                            ingredients.map { it.unitWeightG }
                                    } else {
                                        editingWeight = false
                                        scaleBaseline = null
                                    }
                                }
                        )
                        Text(
                            text = "Change this to scale every ingredient's weight (and macros) " +
                                "proportionally.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = dishName,
                            onValueChange = { dishName = it },
                            label = { Text("Dish name") },
                            singleLine = true,
                            enabled = !isReanalyzing,
                            trailingIcon = {
                                if (isReanalyzing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    IconButton(
                                        onClick = { onReanalyze(dishName) },
                                        enabled = dishName.isNotBlank()
                                    ) {
                                        Icon(
                                            Icons.Filled.AutoAwesome,
                                            contentDescription = "Recalculate with AI"
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = if (isReanalyzing) {
                                "Recalculating with AI…"
                            } else {
                                "Wrong item? Rename it (e.g. \"pineapple cake\") and tap ✨ to let " +
                                    "AI recompute calories, macros and ingredients."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ingredients",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "adjust weight or edit macros",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                itemsIndexed(ingredients, key = { i, _ -> i }) { index, item ->
                    IngredientCard(
                        ingredient = item,
                        onQuantityChange = { newQty ->
                            ingredients[index] = item.copy(quantity = newQty.coerceAtLeast(0))
                        },
                        onUnitWeightChange = { newUnit ->
                            ingredients[index] = item.copy(unitWeightG = newUnit.coerceAtLeast(0))
                        },
                        onMacrosChange = { cal, protein, carbs, fats ->
                            ingredients[index] = item.withTotals(cal, protein, carbs, fats)
                        },
                        onDelete = { ingredients.removeAt(index) }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add ingredient")
                    }
                }
            }
        }

        if (showAddDialog) {
            AddIngredientDialog(
                onAdd = { newIngredient ->
                    ingredients.add(newIngredient)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }
    }
}

@Composable
fun TotalsCard(calories: Int, protein: Int, carbs: Int, fats: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$calories kcal",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Total for this meal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroPill("Protein", protein)
                MacroPill("Carbs", carbs)
                MacroPill("Fats", fats)
            }
        }
    }
}

@Composable
private fun MacroPill(label: String, grams: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${grams}g",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun IngredientCard(
    ingredient: IngredientDraft,
    onQuantityChange: (Int) -> Unit,
    onUnitWeightChange: (Int) -> Unit,
    onMacrosChange: (calories: Int, protein: Int, carbs: Int, fats: Int) -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remove ${ingredient.name}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = if (ingredient.quantity == 0) "" else ingredient.quantity.toString(),
                    onValueChange = { input ->
                        onQuantityChange(input.filter { it.isDigit() }.take(3).toIntOrNull() ?: 0)
                    },
                    label = { Text("Qty") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(84.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("×", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = if (ingredient.unitWeightG == 0) "" else ingredient.unitWeightG.toString(),
                    onValueChange = { input ->
                        onUnitWeightChange(input.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0)
                    },
                    label = { Text("g / unit") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(110.dp)
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = "= ${ingredient.weightG} g total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Macros for this ingredient (from packet label)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1.2f)) {
                    MacroEditField(
                        label = "kcal",
                        value = ingredient.calories,
                        enabled = ingredient.weightG > 0
                    ) { onMacrosChange(it, ingredient.protein, ingredient.carbs, ingredient.fats) }
                }
                Box(Modifier.weight(1f)) {
                    MacroEditField(
                        label = "Protein",
                        value = ingredient.protein,
                        enabled = ingredient.weightG > 0
                    ) { onMacrosChange(ingredient.calories, it, ingredient.carbs, ingredient.fats) }
                }
                Box(Modifier.weight(1f)) {
                    MacroEditField(
                        label = "Carbs",
                        value = ingredient.carbs,
                        enabled = ingredient.weightG > 0
                    ) { onMacrosChange(ingredient.calories, ingredient.protein, it, ingredient.fats) }
                }
                Box(Modifier.weight(1f)) {
                    MacroEditField(
                        label = "Fats",
                        value = ingredient.fats,
                        enabled = ingredient.weightG > 0
                    ) { onMacrosChange(ingredient.calories, ingredient.protein, ingredient.carbs, it) }
                }
            }
        }
    }
}

@Composable
private fun MacroEditField(
    label: String,
    value: Int,
    enabled: Boolean,
    onValueChange: (Int) -> Unit
) {
    OutlinedTextField(
        value = if (value == 0) "" else value.toString(),
        onValueChange = { input ->
            onValueChange(input.filter { it.isDigit() }.take(5).toIntOrNull() ?: 0)
        },
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AddIngredientDialog(
    onAdd: (IngredientDraft) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var weight by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fats by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ingredient") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { NumberField2("Quantity", quantity) { quantity = it } }
                    Box(Modifier.weight(1f)) { NumberField2("Weight (g / unit)", weight) { weight = it } }
                }
                Text(
                    text = "Enter values for ONE unit; quantity multiplies them.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NumberField2("Calories per unit (kcal)", calories) { calories = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { NumberField2("Protein", protein) { protein = it } }
                    Box(Modifier.weight(1f)) { NumberField2("Carbs", carbs) { carbs = it } }
                    Box(Modifier.weight(1f)) { NumberField2("Fats", fats) { fats = it } }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && (weight.toIntOrNull() ?: 0) > 0 &&
                    (quantity.toIntOrNull() ?: 0) > 0,
                onClick = {
                    val qty = (quantity.toIntOrNull() ?: 1).coerceAtLeast(1)
                    val unit = weight.toIntOrNull() ?: 0
                    onAdd(
                        IngredientDraft.fromAbsolute(
                            name = name.trim(),
                            weightG = qty * unit,
                            calories = qty * (calories.toIntOrNull() ?: 0),
                            protein = qty * (protein.toIntOrNull() ?: 0),
                            carbs = qty * (carbs.toIntOrNull() ?: 0),
                            fats = qty * (fats.toIntOrNull() ?: 0),
                            quantity = qty
                        )
                    )
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun NumberField2(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() }.take(5)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}
