package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anant.fitbuddy.R
import com.anant.fitbuddy.data.model.FoodDraft
import com.anant.fitbuddy.data.model.IngredientDraft
import com.anant.fitbuddy.ui.components.AiInferShimmerButton
import com.anant.fitbuddy.ui.components.IconButton
import com.anant.fitbuddy.ui.components.LogDateField
import com.anant.fitbuddy.ui.components.MacroTextField
import com.anant.fitbuddy.ui.components.MacroValueField
import com.anant.fitbuddy.ui.components.OutlinedButton
import com.anant.fitbuddy.ui.components.ReviewBottomBar
import com.anant.fitbuddy.ui.components.ReviewTotalsHeader
import com.anant.fitbuddy.ui.components.TextButton
import com.anant.fitbuddy.ui.theme.appShape
import com.anant.fitbuddy.util.SystemToast
import com.anant.fitbuddy.util.formatMacro
import com.anant.fitbuddy.util.qtyFieldText
import com.anant.fitbuddy.util.roundQty
import com.anant.fitbuddy.util.sanitizeQtyInput
import com.anant.fitbuddy.util.toMacroOrNull
import com.anant.fitbuddy.util.toQtyOrNull
import kotlin.math.roundToInt

/**
 * Full-screen review of a single food (one dish, any number of ingredients) before log / save.
 */
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
    title: String? = null,
    showLogDate: Boolean = true,
    /** Ask-for-portion above ingredients (standalone log only). */
    showAskForPortion: Boolean = true,
    isAiOnline: Boolean = true,
    confirmLabel: String? = null,
    showSaveAsPreset: Boolean = true,
    onDismiss: () -> Unit
) {
    val resolvedConfirmLabel = confirmLabel
        ?: stringResource(R.string.food_review_log_food)
    val resolvedTitle = title ?: stringResource(R.string.food_review_food)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
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
        var logTimestamp by remember(draft.timestamp) { mutableStateOf(draft.timestamp) }
        var showAddDialog by remember { mutableStateOf(false) }
        val context = LocalContext.current

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

        var weightText by remember(draft) { mutableStateOf(totalWeight.toString()) }
        var editingWeight by remember { mutableStateOf(false) }
        var scaleBaseline by remember { mutableStateOf<Pair<Int, List<Int>>?>(null) }
        LaunchedEffect(totalWeight, editingWeight) {
            if (!editingWeight) weightText = totalWeight.toString()
        }

        val canSave = ingredients.isNotEmpty() && dishName.isNotBlank() &&
            !isReanalyzing && totalWeight > 0

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(resolvedTitle) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.common_cancel)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                ReviewBottomBar(
                    primaryLabel = resolvedConfirmLabel,
                    primaryEnabled = canSave,
                    onPrimary = { onConfirm(confirmedDraft()) },
                    secondaryLabel = if (showSaveAsPreset) {
                        stringResource(R.string.food_save_as_food_preset)
                    } else null,
                    secondaryEnabled = canSave,
                    onSecondary = if (showSaveAsPreset) {
                        {
                            val confirmed = confirmedDraft()
                            onSaveAsPreset(confirmed)
                            SystemToast.show(
                                context,
                                context.getString(
                                    R.string.food_review_saved_food_preset,
                                    confirmed.dishName
                                )
                            )
                        }
                    } else null,
                    showCancel = true,
                    onCancel = onDismiss
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    ReviewTotalsHeader(
                        calories = totalCalories,
                        protein = totalProtein,
                        carbs = totalCarbs,
                        fats = totalFats,
                        subtitle = stringResource(
                            R.string.food_review_totals_subtitle,
                            totalWeight
                        )
                    )
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
                                AiInferShimmerButton(
                                    enabled = dishName.isNotBlank() && isAiOnline,
                                    loading = isReanalyzing,
                                    onClick = { onReanalyze(dishName) },
                                    contentDescription = stringResource(
                                        R.string.food_recalculate_with_ai
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isReanalyzing) {
                            Text(
                                text = stringResource(R.string.food_review_recalculating_ai),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                }

                if (showAskForPortion) {
                    item {
                        OutlinedButton(
                            onClick = { onAskForPortion(dishName) },
                            enabled = dishName.isNotBlank() && !isReanalyzing && isAiOnline,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.food_ask_for_portion))
                        }
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.food_ingredients),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = appShape(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ingredients.forEachIndexed { index, item ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant
                                            .copy(alpha = 0.5f)
                                    )
                                }
                                IngredientReviewRow(
                                    ingredient = item,
                                    enabled = !isReanalyzing,
                                    onQuantityChange = { newQty ->
                                        ingredients[index] = item.copy(
                                            quantity = newQty.roundQty().coerceAtLeast(0.0)
                                        )
                                    },
                                    onUnitWeightChange = { newUnit ->
                                        ingredients[index] = item.copy(
                                            unitWeightG = newUnit.coerceAtLeast(0)
                                        )
                                    },
                                    onMacrosChange = { cal, protein, carbs, fats ->
                                        ingredients[index] =
                                            item.withTotals(cal, protein, carbs, fats)
                                    },
                                    onDelete = { ingredients.removeAt(index) }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        enabled = !isReanalyzing,
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

/** Kept for MealBuilder; delegates to [ReviewTotalsHeader]. */
@Composable
fun TotalsCard(
    calories: Int,
    protein: Double,
    carbs: Double,
    fats: Double,
    subtitle: String = stringResource(R.string.food_total_for_this_food),
) {
    ReviewTotalsHeader(
        calories = calories,
        protein = protein,
        carbs = carbs,
        fats = fats,
        subtitle = subtitle
    )
}

@Composable
private fun IngredientReviewRow(
    ingredient: IngredientDraft,
    enabled: Boolean,
    onQuantityChange: (Double) -> Unit,
    onUnitWeightChange: (Int) -> Unit,
    onMacrosChange: (calories: Int, protein: Double, carbs: Double, fats: Double) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var qtyText by remember {
        mutableStateOf(ingredient.quantity.qtyFieldText())
    }
    LaunchedEffect(ingredient.quantity) {
        if ((qtyText.toQtyOrNull() ?: 0.0) != ingredient.quantity) {
            qtyText = ingredient.quantity.qtyFieldText()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(
                        R.string.food_review_ingredient_summary,
                        ingredient.weightG,
                        ingredient.calories,
                        ingredient.protein.formatMacro()
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { expanded = !expanded }, enabled = enabled) {
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(R.string.food_review_edit_macros)
                )
            }
            IconButton(onClick = onDelete, enabled = enabled) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(
                        R.string.food_review_remove,
                        ingredient.name
                    ),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = qtyText,
                onValueChange = { input ->
                    val sanitized = input.sanitizeQtyInput(3)
                    qtyText = sanitized
                    onQuantityChange(sanitized.toQtyOrNull() ?: 0.0)
                },
                label = { Text(stringResource(R.string.food_qty)) },
                singleLine = true,
                enabled = enabled,
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
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(110.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.food_review_g_total, ingredient.weightG.toString()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (expanded) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1.2f)) {
                    MacroEditField(
                        label = stringResource(R.string.food_kcal),
                        value = ingredient.calories,
                        enabled = enabled && ingredient.weightG > 0
                    ) { onMacrosChange(it, ingredient.protein, ingredient.carbs, ingredient.fats) }
                }
                Box(Modifier.weight(1f)) {
                    MacroValueField(
                        label = stringResource(R.string.dashboard_protein),
                        value = ingredient.protein,
                        onValueChange = {
                            onMacrosChange(ingredient.calories, it, ingredient.carbs, ingredient.fats)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled && ingredient.weightG > 0
                    )
                }
                Box(Modifier.weight(1f)) {
                    MacroValueField(
                        label = stringResource(R.string.dashboard_carbs),
                        value = ingredient.carbs,
                        onValueChange = {
                            onMacrosChange(ingredient.calories, ingredient.protein, it, ingredient.fats)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled && ingredient.weightG > 0
                    )
                }
                Box(Modifier.weight(1f)) {
                    MacroValueField(
                        label = stringResource(R.string.dashboard_fats),
                        value = ingredient.fats,
                        onValueChange = {
                            onMacrosChange(
                                ingredient.calories,
                                ingredient.protein,
                                ingredient.carbs,
                                it
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled && ingredient.weightG > 0
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
                        QtyField2(stringResource(R.string.food_review_quantity), quantity) {
                            quantity = it
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        NumberField2(stringResource(R.string.food_review_weight_g_unit), weight) {
                            weight = it
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.food_enter_values_for_one_unit_quantity),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NumberField2(
                    stringResource(R.string.food_review_calories_per_unit_kcal),
                    calories
                ) { calories = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        MacroTextField(
                            stringResource(R.string.dashboard_protein),
                            protein,
                            { protein = it },
                            Modifier.fillMaxWidth()
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        MacroTextField(
                            stringResource(R.string.dashboard_carbs),
                            carbs,
                            { carbs = it },
                            Modifier.fillMaxWidth()
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        MacroTextField(
                            stringResource(R.string.dashboard_fats),
                            fats,
                            { fats = it },
                            Modifier.fillMaxWidth()
                        )
                    }
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
