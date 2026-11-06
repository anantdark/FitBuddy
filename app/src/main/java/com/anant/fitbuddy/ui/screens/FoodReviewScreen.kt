package com.anant.fitbuddy.ui.screens

import com.anant.fitbuddy.R

import androidx.compose.ui.res.stringResource

import com.anant.fitbuddy.util.formatMacro
import com.anant.fitbuddy.util.qtyFieldText
import com.anant.fitbuddy.util.roundQty
import com.anant.fitbuddy.util.sanitizeQtyInput
import com.anant.fitbuddy.util.toQtyOrNull
import com.anant.fitbuddy.ui.components.MacroTextField
import com.anant.fitbuddy.ui.components.MacroValueField
import com.anant.fitbuddy.util.toMacroOrNull
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
import com.anant.fitbuddy.ui.components.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.anant.fitbuddy.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import com.anant.fitbuddy.ui.components.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.anant.fitbuddy.ui.components.TextButton
import com.anant.fitbuddy.util.SystemToast
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anant.fitbuddy.data.model.FoodDraft
import com.anant.fitbuddy.data.model.IngredientDraft
import com.anant.fitbuddy.ui.components.LogDateField
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

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
    onAskForPortion: (String) -> Unit = {},
    askPortionHint: String? = null,
    /** When false (editing a food inside a meal), the meal owns the log date. */
    showLogDate: Boolean = true,
    /** Primary confirm button label (e.g. Log food vs Save food). */
    confirmLabel: String? = null,
    /** When false, hides the secondary save-as-preset action (library create flow). */
    showSaveAsPreset: Boolean = true,
    onDismiss: () -> Unit
) {
    val resolvedAskPortionHint = askPortionHint
        ?: stringResource(R.string.food_review_know_dish_but_not_grams_estimates_standard)
    val resolvedConfirmLabel = confirmLabel
        ?: stringResource(R.string.food_review_log_food)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Editable working copy, re-seeded if a different draft arrives.
        // Fold food-level quantity into ingredient quantities so one Qty field is enough.
        var dishName by remember(draft) { mutableStateOf(draft.dishName) }
        val ingredients = remember(draft) {
            val foodQty = draft.quantity.roundQty().coerceAtLeast(0.1)
            val seeded = if (foodQty == 1.0) {
                draft.ingredients
            } else {
                draft.ingredients.map { item ->
                    item.copy(quantity = (item.quantity * foodQty).roundQty())
                }
            }
            seeded.toMutableStateList()
        }
        // Keyed on the incoming stamp so food edits from the parent don't wipe a local date pick.
        var logTimestamp by remember(draft.timestamp) { mutableStateOf(draft.timestamp) }
        var showAddDialog by remember { mutableStateOf(false) }

        val context = LocalContext.current

        // Surface reanalyze feedback (clarification/error) via system Toast, then clear it.
        LaunchedEffect(reviewMessage) {
            reviewMessage?.let {
                SystemToast.show(context, it)
                onReviewMessageShown()
            }
        }

        val totalCalories = ingredients.sumOf { it.calories }
        val totalProtein = ingredients.sumOf { it.protein }
        val totalCarbs = ingredients.sumOf { it.carbs }
        val totalFats = ingredients.sumOf { it.fats }
        val totalWeight = ingredients.sumOf { it.weightG }

        fun confirmedDraft(): FoodDraft = draft.copy(
            dishName = dishName.trim(),
            timestamp = logTimestamp,
            ingredients = ingredients.toList(),
            quantity = 1.0
        )

        // Editable total weight. While the field isn't focused it mirrors the ingredient sum;
        // editing it rescales every ingredient proportionally (from a baseline captured on
        // focus, so per-keystroke rounding doesn't drift the proportions).
        var weightText by remember(draft) { mutableStateOf(totalWeight.toString()) }
        var editingWeight by remember { mutableStateOf(false) }
        var scaleBaseline by remember { mutableStateOf<Pair<Int, List<Int>>?>(null) }
        LaunchedEffect(totalWeight, editingWeight) {
            if (!editingWeight) weightText = totalWeight.toString()
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.food_review_food)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_cancel))
                        }
                    }
                )
            },
            bottomBar = {
                val canSave = ingredients.isNotEmpty() && dishName.isNotBlank() &&
                    !isReanalyzing && totalWeight > 0
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showSaveAsPreset) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canSave,
                            onClick = {
                                val confirmed = confirmedDraft()
                                onSaveAsPreset(confirmed)
                                SystemToast.show(
                                    context,
                                    context.getString(R.string.food_review_saved_food_preset, confirmed.dishName)
                                )
                            }
                        ) {
                            Icon(Icons.Filled.BookmarkAdd, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.food_save_as_food_preset))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = onDismiss
                        ) { Text(stringResource(R.string.common_cancel)) }
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = canSave,
                            onClick = { onConfirm(confirmedDraft()) }
                        ) { Text(resolvedConfirmLabel) }
                    }
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

                if (showLogDate) {
                    item {
                        LogDateField(
                            timestamp = logTimestamp,
                            onTimestampChange = { logTimestamp = it },
                            enabled = !isReanalyzing
                        )
                    }
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
                            label = { Text(stringResource(R.string.food_total_weight_g)) },
                            singleLine = true,
                            enabled = !isReanalyzing && ingredients.isNotEmpty(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focus ->
                                    if (focus.isFocused) {
                                        editingWeight = true
                                        scaleBaseline = totalWeight to
                                            ingredients.map { it.unitWeightG }
                                    } else {
                                        editingWeight = false
                                        scaleBaseline = null
                                    }
                                }
                        )
                        Text(
                            text = stringResource(R.string.food_change_this_to_scale_every_ingredient),
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
                            label = { Text(stringResource(R.string.food_food_name)) },
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
                                            contentDescription = stringResource(R.string.food_recalculate_with_ai)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = if (isReanalyzing) {
                                stringResource(R.string.food_review_recalculating_ai)
                            } else {
                                stringResource(R.string.food_review_wrong_item_rename_e_g_pineapple_cake)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { onAskForPortion(dishName) },
                            enabled = dishName.isNotBlank() && !isReanalyzing
                        ) {
                            Text(stringResource(R.string.food_ask_for_portion))
                        }
                        Text(
                            text = resolvedAskPortionHint,
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
                            text = stringResource(R.string.food_ingredients),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.food_adjust_weight_or_edit_macros),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                itemsIndexed(ingredients, key = { i, _ -> i }) { index, item ->
                    IngredientCard(
                        ingredient = item,
                        onQuantityChange = { newQty ->
                            ingredients[index] = item.copy(quantity = newQty.roundQty().coerceAtLeast(0.0))
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
                        Text(stringResource(R.string.food_add_ingredient))
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
fun TotalsCard(calories: Int, protein: Double, carbs: Double, fats: Double) {
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
                text = stringResource(R.string.food_total_for_this_food),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroPill(stringResource(R.string.dashboard_protein), protein)
                MacroPill(stringResource(R.string.dashboard_carbs), carbs)
                MacroPill(stringResource(R.string.dashboard_fats), fats)
            }
        }
    }
}

@Composable
private fun MacroPill(label: String, grams: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${grams.formatMacro()}g",
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
    onQuantityChange: (Double) -> Unit,
    onUnitWeightChange: (Int) -> Unit,
    onMacrosChange: (calories: Int, protein: Double, carbs: Double, fats: Double) -> Unit,
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
                        contentDescription = stringResource(R.string.food_review_remove, ingredient.name),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Keep partial input like "1." while typing (same pattern as MacroValueField).
                var qtyText by remember {
                    mutableStateOf(ingredient.quantity.qtyFieldText())
                }
                LaunchedEffect(ingredient.quantity) {
                    if ((qtyText.toQtyOrNull() ?: 0.0) != ingredient.quantity) {
                        qtyText = ingredient.quantity.qtyFieldText()
                    }
                }
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { input ->
                        val sanitized = input.sanitizeQtyInput(3)
                        qtyText = sanitized
                        onQuantityChange(sanitized.toQtyOrNull() ?: 0.0)
                    },
                    label = { Text(stringResource(R.string.food_qty)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                    label = { Text(stringResource(R.string.food_g_unit)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(110.dp)
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.food_review_g_total, ingredient.weightG.toString()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.food_macros_for_this_ingredient_from_packet),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1.2f)) {
                    MacroEditField(
                        label = stringResource(R.string.food_kcal),
                        value = ingredient.calories,
                        enabled = ingredient.weightG > 0
                    ) { onMacrosChange(it, ingredient.protein, ingredient.carbs, ingredient.fats) }
                }
                Box(Modifier.weight(1f)) {
                    MacroValueField(
                        label = stringResource(R.string.dashboard_protein),
                        value = ingredient.protein,
                        onValueChange = { onMacrosChange(ingredient.calories, it, ingredient.carbs, ingredient.fats) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = ingredient.weightG > 0
                    )
                }
                Box(Modifier.weight(1f)) {
                    MacroValueField(
                        label = stringResource(R.string.dashboard_carbs),
                        value = ingredient.carbs,
                        onValueChange = { onMacrosChange(ingredient.calories, ingredient.protein, it, ingredient.fats) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = ingredient.weightG > 0
                    )
                }
                Box(Modifier.weight(1f)) {
                    MacroValueField(
                        label = stringResource(R.string.dashboard_fats),
                        value = ingredient.fats,
                        onValueChange = { onMacrosChange(ingredient.calories, ingredient.protein, ingredient.carbs, it) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = ingredient.weightG > 0
                    )
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
        title = { Text(stringResource(R.string.food_add_ingredient)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.food_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        QtyField2(stringResource(R.string.food_review_quantity), quantity) { quantity = it }
                    }
                    Box(Modifier.weight(1f)) {
                        NumberField2(stringResource(R.string.food_review_weight_g_unit), weight) { weight = it }
                    }
                }
                Text(
                    text = stringResource(R.string.food_enter_values_for_one_unit_quantity),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NumberField2(stringResource(R.string.food_review_calories_per_unit_kcal), calories) { calories = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { MacroTextField(stringResource(R.string.dashboard_protein), protein, { protein = it }, Modifier.fillMaxWidth()) }
                    Box(Modifier.weight(1f)) { MacroTextField(stringResource(R.string.dashboard_carbs), carbs, { carbs = it }, Modifier.fillMaxWidth()) }
                    Box(Modifier.weight(1f)) { MacroTextField(stringResource(R.string.dashboard_fats), fats, { fats = it }, Modifier.fillMaxWidth()) }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && (weight.toIntOrNull() ?: 0) > 0 &&
                    (quantity.toQtyOrNull() ?: 0.0) > 0.0,
                onClick = {
                    val qty = (quantity.toQtyOrNull() ?: 1.0).roundQty().coerceAtLeast(0.1)
                    val unit = weight.toIntOrNull() ?: 0
                    val totalWeight = (qty * unit).roundToInt()
                    onAdd(
                        IngredientDraft.fromAbsolute(
                            name = name.trim(),
                            weightG = totalWeight,
                            calories = (qty * (calories.toIntOrNull() ?: 0)).roundToInt(),
                            protein = qty * (protein.toMacroOrNull() ?: 0.0),
                            carbs = qty * (carbs.toMacroOrNull() ?: 0.0),
                            fats = qty * (fats.toMacroOrNull() ?: 0.0),
                            quantity = qty
                        )
                    )
                }
            ) { Text(stringResource(R.string.food_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
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

@Composable
private fun QtyField2(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.sanitizeQtyInput(3)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}
