package com.anant.fitbuddy.ui.screens

import com.anant.fitbuddy.R

import androidx.compose.ui.res.stringResource

import com.anant.fitbuddy.util.formatMacro
import com.anant.fitbuddy.util.sanitizeMacroInput
import com.anant.fitbuddy.util.toMacroOrNull
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.data.model.ScannedProduct
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

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
    var protein by remember(product) { mutableStateOf(product.proteinG.formatMacro()) }
    var carbs by remember(product) { mutableStateOf(product.carbsG.formatMacro()) }
    var fats by remember(product) { mutableStateOf(product.fatsG.formatMacro()) }

    fun buildProduct(): ScannedProduct? {
        val cal = calories.toIntOrNull() ?: return null
        return ScannedProduct(
            barcode = product.barcode,
            name = name.trim().ifBlank { product.name },
            calories = cal,
            proteinG = protein.toMacroOrNull() ?: 0.0,
            carbsG = carbs.toMacroOrNull() ?: 0.0,
            fatsG = fats.toMacroOrNull() ?: 0.0,
            servingGrams = product.servingGrams
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(R.string.food_product_found)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.edit_saved_food_barcode, product.barcode))
                product.servingGrams?.let {
                    Text(stringResource(R.string.scanned_product_reference_serving_g_edit_macros_match_packet, it.toString()))
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.food_name)) },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text(stringResource(R.string.food_calories_per_serving)) },
                    singleLine = true,
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { MacroField(stringResource(R.string.dashboard_protein), protein, isSaving) { protein = it } }
                    Box(Modifier.weight(1f)) { MacroField(stringResource(R.string.dashboard_carbs), carbs, isSaving) { carbs = it } }
                    Box(Modifier.weight(1f)) { MacroField(stringResource(R.string.dashboard_fats), fats, isSaving) { fats = it } }
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
                TextButton(onClick = onDismiss, enabled = !isSaving) { Text(stringResource(R.string.common_cancel)) }
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
        onValueChange = { onValueChange(it.sanitizeMacroInput()) },
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}
