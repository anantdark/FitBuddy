package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import com.anant.fitbuddy.ui.components.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import com.anant.fitbuddy.ui.components.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.data.settings.AiProvider
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.ui.components.FitBuddySnackbarHost
import com.anant.fitbuddy.ui.components.OpenRouterConnectSection
import com.anant.fitbuddy.ui.components.showFitBuddyPill
import com.anant.fitbuddy.ui.util.dismissKeyboardOnTap

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

/** Per-provider "how to get set up" doc, linked from the onboarding AI step. */
private val AI_SETUP_DOCS: Map<AiProvider, Pair<String, String>> = mapOf(
    AiProvider.OPENROUTER to (
        "How to create an OpenRouter API key" to "https://openrouter.ai/docs/api/reference/authentication"
    ),
    AiProvider.GEMINI to (
        "How to create a Gemini API key" to "https://ai.google.dev/gemini-api/docs/api-key"
    ),
    AiProvider.OLLAMA to (
        "Ollama install & setup guide" to "https://docs.ollama.com/quickstart"
    )
)

private val OLLAMA_CLOUD_KEYS_URL = "https://ollama.com/settings/keys"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    isSaving: Boolean,
    isValidating: Boolean,
    openRouterOAuthBusy: Boolean = false,
    openRouterOAuthKey: String = "",
    userMessage: String? = null,
    onUserMessageConsumed: () -> Unit = {},
    onConnectOpenRouter: (android.content.Context) -> Unit = {},
    onDisconnectOpenRouter: () -> Unit = {},
    onValidateAi: (AppSettings, (Boolean, String?) -> Unit) -> Unit,
    onComplete: (
        age: Int,
        weightKg: Double,
        heightCm: Double,
        sex: String?,
        goal: String,
        activityLevel: String,
        aiSettings: AppSettings
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
    var aiProvider by remember { mutableStateOf(AiProvider.OPENROUTER) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { message ->
            snackbarHostState.showFitBuddyPill(message, displayMillis = 3_000L)
            onUserMessageConsumed()
        }
    }
    var apiKeys by remember { mutableStateOf(emptyList<String>()) }
    var ollamaUrl by remember { mutableStateOf(AppSettings.DEFAULT_OLLAMA_URL) }
    var ollamaUseCloud by remember { mutableStateOf(false) }
    var ollamaKeys by remember { mutableStateOf(emptyList<String>()) }
    var aiValidated by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }

    val openRouterOAuthConnected = openRouterOAuthKey.isNotBlank()
    val stepOneValid = (age.toIntOrNull() ?: 0) in 10..120 &&
        (height.toDoubleOrNull() ?: 0.0) in 50.0..280.0 &&
        (weight.toDoubleOrNull() ?: 0.0) in 20.0..400.0
    val aiConfigValid = when (aiProvider) {
        AiProvider.OPENROUTER -> apiKeys.isNotEmpty() || openRouterOAuthConnected
        AiProvider.GEMINI -> apiKeys.isNotEmpty()
        AiProvider.OLLAMA -> if (ollamaUseCloud) {
            ollamaKeys.isNotEmpty()
        } else {
            ollamaUrl.isNotBlank()
        }
    }

    fun buildAiSettings(): AppSettings {
        val orKeys = if (aiProvider == AiProvider.OPENROUTER) apiKeys else emptyList()
        val gemKeys = if (aiProvider == AiProvider.GEMINI) apiKeys else emptyList()
        val olKeys = if (aiProvider == AiProvider.OLLAMA && ollamaUseCloud) ollamaKeys else emptyList()
        return AppSettings(
            provider = aiProvider,
            openRouterApiKeys = orKeys,
            openRouterApiKey = orKeys.firstOrNull().orEmpty(),
            openRouterOAuthKey = if (aiProvider == AiProvider.OPENROUTER) openRouterOAuthKey else "",
            geminiApiKeys = gemKeys,
            geminiApiKey = gemKeys.firstOrNull().orEmpty(),
            ollamaBaseUrl = if (aiProvider == AiProvider.OLLAMA && !ollamaUseCloud) {
                ollamaUrl.trim()
            } else {
                AppSettings.DEFAULT_OLLAMA_URL
            },
            ollamaUseCloud = aiProvider == AiProvider.OLLAMA && ollamaUseCloud,
            ollamaApiKeys = olKeys,
            ollamaApiKey = olKeys.firstOrNull().orEmpty(),
            aiAutoFailover = true
        )
    }

    fun invalidateAi() {
        aiValidated = false
        aiError = null
    }

    // OAuth key arrives via settings after Custom Tab callback — reset validation once.
    LaunchedEffect(openRouterOAuthKey) {
        if (openRouterOAuthKey.isNotBlank()) {
            aiValidated = false
            aiError = null
        }
    }

    fun finishOnboarding() {
        onComplete(
            age.toIntOrNull() ?: 0,
            weight.toDoubleOrNull() ?: 0.0,
            height.toDoubleOrNull() ?: 0.0,
            sex.ifBlank { null },
            goal,
            activity,
            buildAiSettings()
        )
    }

    fun advanceFromAiStep() {
        if (aiValidated) {
            step = 1
            return
        }
        aiError = null
        onValidateAi(buildAiSettings()) { ok, error ->
            if (ok) {
                aiValidated = true
                aiError = null
                step = 1
            } else {
                aiValidated = false
                aiError = error ?: "Couldn't validate that API key"
            }
        }
    }

    val busy = isSaving || isValidating

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { FitBuddySnackbarHost(snackbarHostState, bottomPadding = 24.dp) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .dismissKeyboardOnTap()
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
                text = "Connect an AI provider first, then tell us about yourself so we can " +
                    "personalise calorie targets, workout estimates, and meal logging.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            LinearProgressIndicator(
                progress = { (step + 1) / 3f },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Step ${step + 1} of 3",
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
                    when (step) {
                        0 -> {
                            Text(
                                text = "Connect an AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "FitBuddy needs an LLM to read food photos, freeform text logs, " +
                                    "and set your initial calorie targets. Connect a provider to continue.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val providerOptions = listOf(
                                AiProvider.OPENROUTER to "OpenRouter",
                                AiProvider.GEMINI to "Gemini",
                                AiProvider.OLLAMA to "Ollama"
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                providerOptions.forEachIndexed { index, (value, label) ->
                                    SegmentedButton(
                                        selected = aiProvider == value,
                                        onClick = {
                                            if (aiProvider != value) {
                                                aiProvider = value
                                                invalidateAi()
                                            }
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index,
                                            providerOptions.size
                                        )
                                    ) { Text(label) }
                                }
                            }

                            when (aiProvider) {
                                AiProvider.OPENROUTER -> {
                                    val context = LocalContext.current
                                    OpenRouterConnectSection(
                                        oauthConnected = openRouterOAuthConnected,
                                        oauthBusy = openRouterOAuthBusy,
                                        hasManualKeys = apiKeys.isNotEmpty(),
                                        onConnect = onConnectOpenRouter,
                                        onDisconnect = {
                                            onDisconnectOpenRouter()
                                            invalidateAi()
                                        },
                                        context = context
                                    ) {
                                        ApiKeyChipEditor(
                                            label = "API keys",
                                            keys = apiKeys,
                                            onKeysChange = {
                                                apiKeys = it
                                                invalidateAi()
                                            }
                                        )
                                    }
                                }

                                AiProvider.GEMINI -> {
                                    AI_SETUP_DOCS[aiProvider]?.let { (label, url) ->
                                        OnboardingDocsLink(label = label, url = url)
                                    }
                                    ApiKeyChipEditor(
                                        label = "API keys",
                                        keys = apiKeys,
                                        onKeysChange = {
                                            apiKeys = it
                                            invalidateAi()
                                        }
                                    )
                                }

                                AiProvider.OLLAMA -> {
                                    AI_SETUP_DOCS[aiProvider]?.let { (label, url) ->
                                        OnboardingDocsLink(label = label, url = url)
                                    }
                                    val modeOptions = listOf(false to "Local", true to "Cloud")
                                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                        modeOptions.forEachIndexed { index, (useCloud, label) ->
                                            SegmentedButton(
                                                selected = ollamaUseCloud == useCloud,
                                                onClick = {
                                                    if (ollamaUseCloud != useCloud) {
                                                        ollamaUseCloud = useCloud
                                                        invalidateAi()
                                                    }
                                                },
                                                shape = SegmentedButtonDefaults.itemShape(
                                                    index,
                                                    modeOptions.size
                                                )
                                            ) { Text(label) }
                                        }
                                    }
                                    if (ollamaUseCloud) {
                                        OnboardingDocsLink(
                                            label = "Create an Ollama Cloud API key",
                                            url = OLLAMA_CLOUD_KEYS_URL
                                        )
                                        ApiKeyChipEditor(
                                            label = "API keys",
                                            keys = ollamaKeys,
                                            onKeysChange = {
                                                ollamaKeys = it
                                                invalidateAi()
                                            }
                                        )
                                    } else {
                                        OnboardingTextField(
                                            label = "Server URL",
                                            value = ollamaUrl,
                                            keyboardType = KeyboardType.Uri
                                        ) {
                                            ollamaUrl = it
                                            invalidateAi()
                                        }
                                    }
                                }
                            }

                            aiError?.let { message ->
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Text(
                                text = "Your credentials stay on-device and only go to the provider you chose.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        1 -> {
                            Text(
                                text = "About you",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            OnboardingNumberField("Age", age) { age = it }
                            OnboardingNumberField("Height (cm)", height, decimal = true) { height = it }
                            OnboardingNumberField("Current weight (kg)", weight, decimal = true) {
                                weight = it
                            }
                            OnboardingDropdown("Sex", sex, SEX_OPTIONS) { sex = it }
                        }

                        else -> {
                            Text(
                                text = "Your lifestyle",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "We'll use this with AI to set your daily calorie and macro " +
                                    "targets when you open the dashboard. You can fine-tune them " +
                                    "anytime in Body.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OnboardingDropdown("Activity level", activity, ACTIVITY_OPTIONS) {
                                activity = it
                            }
                            OnboardingDropdown("Goal", goal, GOAL_OPTIONS) { goal = it }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step -= 1 },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = when (step) {
                        0 -> aiConfigValid && !busy
                        1 -> stepOneValid && !busy
                        else -> !busy
                    },
                    onClick = {
                        when (step) {
                            0 -> advanceFromAiStep()
                            1 -> step = 2
                            else -> finishOnboarding()
                        }
                    }
                ) {
                    Text(
                        when {
                            isValidating -> "Validating…"
                            isSaving -> "Setting up…"
                            step == 2 -> "Finish setup"
                            else -> "Continue"
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

@Composable
private fun OnboardingTextField(
    label: String,
    value: String,
    isSecret: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isSecret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Tappable link to the provider's key-creation/setup docs, opened in the browser. */
@Composable
private fun OnboardingDocsLink(label: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(url) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
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
