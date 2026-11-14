package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anant.fitbuddy.R
import com.anant.fitbuddy.data.model.FoodEntryDraft
import com.anant.fitbuddy.data.model.MealDraft
import com.anant.fitbuddy.ui.components.AiInferShimmerButton
import com.anant.fitbuddy.ui.components.IconButton
import com.anant.fitbuddy.ui.components.LogDateField
import com.anant.fitbuddy.ui.components.MacroCarbsColor
import com.anant.fitbuddy.ui.components.MacroFatsColor
import com.anant.fitbuddy.ui.components.MacroProteinColor
import com.anant.fitbuddy.ui.components.OutlinedButton
import com.anant.fitbuddy.ui.components.ReviewBottomBar
import com.anant.fitbuddy.ui.components.ReviewTotalsHeader
import com.anant.fitbuddy.ui.theme.appShape
import com.anant.fitbuddy.util.SystemToast
import com.anant.fitbuddy.util.formatMacro
import com.anant.fitbuddy.util.qtyFieldText
import com.anant.fitbuddy.util.sanitizeQtyInput
import com.anant.fitbuddy.util.toQtyOrNull

/**
 * Plate-level review for a meal with two or more foods.
 * Tap a food to open nested Food review; sparkle re-runs AI on the whole meal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealReviewDialog(
    draft: MealDraft,
    isEditing: Boolean,
    saveAsPresetOnly: Boolean = false,
    isReanalyzing: Boolean = false,
    isAiOnline: Boolean = true,
    reviewMessage: String? = null,
    onReviewMessageShown: () -> Unit = {},
    onConfirm: (MealDraft) -> Unit,
    onSaveAsPreset: (MealDraft) -> Unit,
    onEditFood: (Int, FoodEntryDraft) -> Unit,
    onRemoveFood: (Int) -> Unit,
    onQuantityChange: (Int, Double) -> Unit,
    onAddFood: () -> Unit,
    onReanalyze: (String) -> Unit = {},
    onNameChange: (String) -> Unit = {},
    onTimestampChange: (Long) -> Unit = {},
    onDismiss: () -> Unit
) {
    var logTimestamp by remember(draft.timestamp) { mutableStateOf(draft.timestamp) }
    var mealName by remember(draft.name) { mutableStateOf(draft.name) }
    val context = LocalContext.current

    LaunchedEffect(reviewMessage) {
        reviewMessage?.let {
            SystemToast.show(context, it)
            onReviewMessageShown()
        }
    }

    fun confirmedDraft(): MealDraft = draft.copy(
        name = mealName.trim().ifBlank { draft.name },
        timestamp = logTimestamp
    )

    fun savePreset() {
        onSaveAsPreset(confirmedDraft())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (isEditing) stringResource(R.string.meal_review_edit_meal)
                            else stringResource(R.string.meal_review_meal)
                        )
                    },
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
                val canSave = draft.foods.isNotEmpty() && mealName.isNotBlank()
                ReviewBottomBar(
                    primaryLabel = when {
                        isEditing && saveAsPresetOnly ->
                            stringResource(R.string.meal_review_update_meal_preset)
                        isEditing -> stringResource(R.string.meal_review_update_meal)
                        saveAsPresetOnly -> stringResource(R.string.meal_save_as_meal_preset)
                        else -> stringResource(R.string.meal_review_save_meal)
                    },
                    primaryEnabled = canSave,
                    onPrimary = {
                        if (saveAsPresetOnly) savePreset() else onConfirm(confirmedDraft())
                    },
                    secondaryLabel = if (!saveAsPresetOnly && !isEditing) {
                        stringResource(R.string.meal_save_as_meal_preset)
                    } else null,
                    secondaryEnabled = canSave,
                    onSecondary = if (!saveAsPresetOnly && !isEditing) {
                        { savePreset() }
                    } else null,
                    showCancel = false,
                    onCancel = null
                )
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
                    ReviewTotalsHeader(
                        calories = draft.totalCalories,
                        protein = draft.totalProtein,
                        carbs = draft.totalCarbs,
                        fats = draft.totalFats,
                        subtitle = stringResource(
                            R.string.meal_review_total_for_meal,
                            draft.foodCount
                        )
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = mealName,
                            onValueChange = {
                                mealName = it
                                onNameChange(it)
                            },
                            label = { Text(stringResource(R.string.meal_meal_name)) },
                            singleLine = true,
                            enabled = !isReanalyzing,
                            trailingIcon = {
                                AiInferShimmerButton(
                                    enabled = mealName.isNotBlank() && isAiOnline,
                                    loading = isReanalyzing,
                                    onClick = { onReanalyze(mealName) },
                                    contentDescription = stringResource(
                                        R.string.meal_review_recalculate_with_ai
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = if (isReanalyzing) {
                                stringResource(R.string.food_review_recalculating_ai)
                            } else {
                                stringResource(R.string.meal_review_ai_hint)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!saveAsPresetOnly) {
                    item {
                        LogDateField(
                            timestamp = logTimestamp,
                            onTimestampChange = {
                                logTimestamp = it
                                onTimestampChange(it)
                            }
                        )
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.meal_foods),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (draft.foods.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.meal_no_foods_in_this_meal_go),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = appShape(16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                draft.foods.forEachIndexed { index, food ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant
                                                .copy(alpha = 0.5f)
                                        )
                                    }
                                    MealFoodPlateRow(
                                        food = food,
                                        onOpen = { onEditFood(index, food) },
                                        onDelete = { onRemoveFood(index) },
                                        onQuantityChange = { qty ->
                                            onQuantityChange(index, qty)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onAddFood,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.meal_add_food))
                    }
                }
            }
        }
    }
}

@Composable
private fun MealFoodPlateRow(
    food: FoodEntryDraft,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onQuantityChange: (Double) -> Unit
) {
    val reviewQty = food.reviewQuantity
    var qtyText by remember(food.name, food.ingredients.size) {
        mutableStateOf(reviewQty.qtyFieldText())
    }
    LaunchedEffect(reviewQty) {
        if ((qtyText.toQtyOrNull() ?: 0.0) != reviewQty) {
            qtyText = reviewQty.qtyFieldText()
        }
    }
    val singleIngredient = food.ingredients.size == 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            MealFoodMacroStrip(
                calories = food.totalCalories,
                protein = food.totalProtein,
                carbs = food.totalCarbs,
                fats = food.totalFats
            )
            Text(
                text = stringResource(
                    R.string.meal_review_g_ingredient_s,
                    food.totalWeightG.toString(),
                    food.ingredients.size,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        OutlinedTextField(
            value = qtyText,
            onValueChange = { input ->
                val sanitized = input.sanitizeQtyInput(3)
                qtyText = sanitized
                onQuantityChange(sanitized.toQtyOrNull() ?: 0.0)
            },
            label = {
                Text(
                    if (singleIngredient) stringResource(R.string.food_qty)
                    else stringResource(R.string.meal_review_servings)
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .width(72.dp)
                .clickable(enabled = false) {}
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.meal_remove),
                tint = MaterialTheme.colorScheme.error
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.meal_edit_food),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MealFoodMacroStrip(
    calories: Int,
    protein: Double,
    carbs: Double,
    fats: Double
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$calories kcal",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        MacroDot(MacroProteinColor, "P ${protein.formatMacro()}")
        MacroDot(MacroCarbsColor, "C ${carbs.formatMacro()}")
        MacroDot(MacroFatsColor, "F ${fats.formatMacro()}")
    }
}

@Composable
private fun MacroDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
