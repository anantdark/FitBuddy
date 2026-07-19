package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.anant.fitbuddy.ui.components.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.data.model.ScannedProduct

@Composable
fun ScannedProductDialog(
    product: ScannedProduct,
    isSaving: Boolean,
    primaryLabel: String,
    onPrimary: (ScannedProduct) -> Unit,
    onDismiss: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: ((ScannedProduct) -> Unit)? = null
) {
    var name by remember(product) { mutableStateOf(product.name) }
    var calories by remember(product) { mutableStateOf(product.calories.toString()) }
    var protein by remember(product) { mutableStateOf(product.proteinG.toString()) }
    var carbs by remember(product) { mutableStateOf(product.carbsG.toString()) }
    var fats by remember(product) { mutableStateOf(product.fatsG.toString()) }

    fun buildProduct(): ScannedProduct? {
        val cal = calories.toIntOrNull() ?: return null
        return ScannedProduct(
            barcode = product.barcode,
            name = name.trim().ifBlank { product.name },
            calories = cal,
            proteinG = protein.toIntOrNull() ?: 0,
            carbsG = carbs.toIntOrNull() ?: 0,
            fatsG = fats.toIntOrNull() ?: 0,
            servingGrams = product.servingGrams
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Product found") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Barcode: ${product.barcode}")
                product.servingGrams?.let {
                    Text("Reference serving: ${it}g (edit macros to match your packet)")
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("Calories per serving") },
                    singleLine = true,
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { MacroField("Protein", protein, isSaving) { protein = it } }
                    Box(Modifier.weight(1f)) { MacroField("Carbs", carbs, isSaving) { carbs = it } }
                    Box(Modifier.weight(1f)) { MacroField("Fats", fats, isSaving) { fats = it } }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving && buildProduct() != null,
                onClick = { buildProduct()?.let(onPrimary) }
            ) { Text(primaryLabel) }
        },
        dismissButton = {
            Row {
                if (secondaryLabel != null && onSecondary != null) {
                    TextButton(
                        enabled = !isSaving && buildProduct() != null,
                        onClick = { buildProduct()?.let(onSecondary) }
                    ) { Text(secondaryLabel) }
                }
                TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun MacroField(
    label: String,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(4)) },
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}
