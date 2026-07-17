package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import com.anant.fitbuddy.ui.components.FitBuddySnackbarHost
import com.anant.fitbuddy.ui.components.showFitBuddyPill
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anant.fitbuddy.data.model.FoodEntryDraft
import com.anant.fitbuddy.data.model.MealDraft

/** Final review before persisting a meal (one or more foods), like confirming a workout session. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealReviewDialog(
    draft: MealDraft,
    isEditing: Boolean,
    onConfirm: (MealDraft) -> Unit,
    onSaveAsPreset: (MealDraft) -> Unit,
    onEditFood: (Int, FoodEntryDraft) -> Unit,
    onDismiss: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { FitBuddySnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(if (isEditing) "Edit meal" else "Review meal") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        IconButton(
                            enabled = draft.foods.isNotEmpty(),
                            onClick = {
                                onSaveAsPreset(draft)
                                scope.launch {
                                    snackbarHostState.showFitBuddyPill(
                                        "Saved \"${draft.name}\" as meal preset"
                                    )
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.BookmarkAdd,
                                contentDescription = "Save as meal preset"
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
                        enabled = draft.foods.isNotEmpty(),
                        onClick = { onConfirm(draft) }
                    ) { Text(if (isEditing) "Update meal" else "Save meal") }
                }
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
                    Text(
                        text = draft.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${draft.foodCount} food(s) in this meal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    TotalsCard(
                        draft.totalCalories,
                        draft.totalProtein,
                        draft.totalCarbs,
                        draft.totalFats
                    )
                }
                item {
                    Text(
                        text = "Foods",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (draft.foods.isEmpty()) {
                    item {
                        Text(
                            text = "No foods in this meal. Go back and add at least one food.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                itemsIndexed(draft.foods, key = { index, food -> "${food.name}-$index" }) { index, food ->
                    MealFoodReviewCard(
                        food = food,
                        onEdit = { onEditFood(index, food) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MealFoodReviewCard(
    food: FoodEntryDraft,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                val servingLabel = if (food.servings % 1.0 == 0.0) {
                    food.servings.toInt().toString()
                } else {
                    food.servings.toString()
                }
                Text(
                    text = "$servingLabel serving(s) · ${food.ingredients.size} ingredient(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${food.totalCalories} kcal · P ${food.totalProtein}g · " +
                        "C ${food.totalCarbs}g · F ${food.totalFats}g",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (food.ingredients.isNotEmpty()) {
                    Text(
                        text = food.ingredients.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(Icons.Filled.Edit, contentDescription = "Edit food")
        }
    }
}
