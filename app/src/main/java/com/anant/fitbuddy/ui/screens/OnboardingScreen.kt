package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private val GOAL_OPTIONS = listOf(
    "AUTO" to "Let AI decide",
    "LOSE_WEIGHT" to "Lose weight",
    "GAIN_MUSCLE" to "Gain muscle",
    "RECOMP" to "Body recomposition"
)
private val SEX_OPTIONS = listOf(
    "" to "Prefer not to say",
    "MALE" to "Male",
    "FEMALE" to "Female"
)
private val ACTIVITY_OPTIONS = listOf(
    "SEDENTARY" to "Sedentary",
    "LIGHT" to "Lightly active",
    "MODERATE" to "Moderately active",
    "ACTIVE" to "Active",
    "VERY_ACTIVE" to "Very active"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    isSaving: Boolean,
    onComplete: (
        age: Int,
        weightKg: Double,
        heightCm: Double,
        sex: String?,
        goal: String,
        activityLevel: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(0) }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("RECOMP") }
    var activity by remember { mutableStateOf("MODERATE") }

    val stepOneValid = (age.toIntOrNull() ?: 0) in 10..120 &&
        (height.toDoubleOrNull() ?: 0.0) in 50.0..280.0 &&
        (weight.toDoubleOrNull() ?: 0.0) in 20.0..400.0

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Welcome to FitBuddy",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tell us a bit about yourself so we can personalise calorie targets, " +
                    "workout estimates, and AI insights.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            LinearProgressIndicator(
                progress = { (step + 1) / 2f },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Step ${step + 1} of 2",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (step == 0) {
                        Text(
                            text = "About you",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        OnboardingNumberField("Age", age) { age = it }
                        OnboardingNumberField("Height (cm)", height, decimal = true) { height = it }
                        OnboardingNumberField("Current weight (kg)", weight, decimal = true) { weight = it }
                        OnboardingDropdown("Sex", sex, SEX_OPTIONS) { sex = it }
                    } else {
                        Text(
                            text = "Your lifestyle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "We use this to set sensible daily targets. You can fine-tune " +
                                "calories and macros anytime in Body.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OnboardingDropdown("Activity level", activity, ACTIVITY_OPTIONS) { activity = it }
                        OnboardingDropdown("Goal", goal, GOAL_OPTIONS) { goal = it }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step -= 1 },
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = if (step == 0) stepOneValid && !isSaving else !isSaving,
                    onClick = {
                        if (step == 0) {
                            step = 1
                        } else {
                            onComplete(
                                age.toIntOrNull() ?: 0,
                                weight.toDoubleOrNull() ?: 0.0,
                                height.toDoubleOrNull() ?: 0.0,
                                sex.ifBlank { null },
                                goal,
                                activity
                            )
                        }
                    }
                ) {
                    Text(
                        when {
                            isSaving -> "Setting up…"
                            step == 0 -> "Continue"
                            else -> "Get started"
                        }
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OnboardingNumberField(
    label: String,
    value: String,
    decimal: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() || (decimal && it == '.') }
            onValueChange(filtered)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingDropdown(
    label: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val display = options.firstOrNull { it.first == selectedValue }?.second
        ?: options.firstOrNull()?.second.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
