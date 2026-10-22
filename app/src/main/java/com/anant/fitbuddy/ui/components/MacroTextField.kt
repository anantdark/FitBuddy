package com.anant.fitbuddy.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.anant.fitbuddy.util.macroFieldText
import com.anant.fitbuddy.util.sanitizeMacroInput
import com.anant.fitbuddy.util.toMacroOrNull

/** Text field for a macro value (grams) that accepts one decimal place. */
@Composable
fun MacroTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.sanitizeMacroInput()) },
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

/** [MacroTextField] bound to a Double, keeping partial input like "12." while typing. */
@Composable
fun MacroValueField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var text by remember { mutableStateOf(value.macroFieldText()) }
    LaunchedEffect(value) {
        if ((text.toMacroOrNull() ?: 0.0) != value) text = value.macroFieldText()
    }
    MacroTextField(
        label = label,
        value = text,
        onValueChange = {
            text = it
            onValueChange(it.toMacroOrNull() ?: 0.0)
        },
        modifier = modifier,
        enabled = enabled
    )
}
