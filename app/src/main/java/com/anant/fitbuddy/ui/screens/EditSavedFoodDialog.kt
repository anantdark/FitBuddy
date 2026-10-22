package com.anant.fitbuddy.ui.screens

import com.anant.fitbuddy.ui.components.MacroTextField
import com.anant.fitbuddy.util.toMacroOrNull
import com.anant.fitbuddy.util.formatMacro
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.data.database.SavedFood
import com.anant.fitbuddy.data.model.IngredientDraft
import com.anant.fitbuddy.data.model.LoggedIngredient
import com.anant.fitbuddy.ui.components.TextButton
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

@Composable
fun EditSavedFoodDialog(
    food: SavedFood,
    onSave: (SavedFood) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(food.id) { mutableStateOf(food.name) }
    var calories by remember(food.id) { mutableStateOf(food.calories.toString()) }
    var protein by remember(food.id) { mutableStateOf(food.proteinG.formatMacro()) }
    var carbs by remember(food.id) { mutableStateOf(food.carbsG.formatMacro()) }
    var fats by remember(food.id) { mutableStateOf(food.fatsG.formatMacro()) }

    fun buildFood(): SavedFood? {
        val cal = calories.toIntOrNull() ?: return null
        val p = protein.toMacroOrNull() ?: 0.0
        val c = carbs.toMacroOrNull() ?: 0.0
        val f = fats.toMacroOrNull() ?: 0.0
        val trimmed = name.trim().ifBlank { food.name }
        return food.copy(
            name = trimmed,
            calories = cal,
            proteinG = p,
            carbsG = c,
            fatsG = f,
            ingredients = syncIngredients(food.ingredients, trimmed, cal, p, c, f)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit saved food") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                food.barcode?.let { Text("Barcode: $it") }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { ch -> ch.isDigit() }.take(5) },
                    label = { Text("Calories") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        MacroTextField("Protein", protein, { protein = it }, Modifier.fillMaxWidth())
                    }
                    Box(Modifier.weight(1f)) {
                        MacroTextField("Carbs", carbs, { carbs = it }, Modifier.fillMaxWidth())
                    }
                    Box(Modifier.weight(1f)) {
                        MacroTextField("Fats", fats, { fats = it }, Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = buildFood() != null,
                onClick = { buildFood()?.let(onSave) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Keep a single synthetic ingredient aligned with totals; leave multi-ingredient lists untouched
 * (totals still update on the parent [SavedFood]).
 */
private fun syncIngredients(
    ingredients: List<LoggedIngredient>?,
    foodName: String,
    calories: Int,
    protein: Double,
    carbs: Double,
    fats: Double
): List<LoggedIngredient>? {
    if (ingredients != null && ingredients.size > 1) return ingredients
    val weight = ingredients?.singleOrNull()?.let { it.quantity * it.unitWeightG }?.coerceAtLeast(1) ?: 100
    return listOf(
        LoggedIngredient.fromIngredientDraft(
            IngredientDraft.fromAbsolute(
                name = foodName,
                weightG = weight,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fats = fats
            )
        )
    )
}
