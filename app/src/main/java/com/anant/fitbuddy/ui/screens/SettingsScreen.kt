package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.data.settings.AiProvider
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.ui.viewmodel.ModelsUiState
import kotlinx.coroutines.delay

private const val EASTER_EGG_TAP_TARGET = 31
private const val EASTER_EGG_HINT_START = 25
private const val VERSION_RESET_TAPS = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    modelsState: ModelsUiState,
    textModelsState: ModelsUiState,
    onLoadModels: (AiProvider, String, Boolean) -> Unit,
    onLoadTextModels: (AiProvider, String, Boolean) -> Unit,
    onSave: (AppSettings) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onEasterEggTriggered: () -> Unit,
    onAnantTapHint: (remainingTaps: Int) -> Unit,
    onAnantTapHintDismiss: () -> Unit,
    onAnantTapWhenUnlocked: () -> Unit,
    onResetEasterEggData: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Local editable copies, re-seeded whenever persisted settings change.
    var provider by remember(settings) { mutableStateOf(settings.provider) }
    var openRouterKey by remember(settings) { mutableStateOf(settings.openRouterApiKey) }
    var openRouterModel by remember(settings) { mutableStateOf(settings.openRouterModel) }
    var openRouterTextModel by remember(settings) { mutableStateOf(settings.openRouterTextModel) }
    var geminiKey by remember(settings) { mutableStateOf(settings.geminiApiKey) }
    var geminiModel by remember(settings) { mutableStateOf(settings.geminiModel) }
    var geminiTextModel by remember(settings) { mutableStateOf(settings.geminiTextModel) }
    var ollamaUrl by remember(settings) { mutableStateOf(settings.ollamaBaseUrl) }
    var ollamaModel by remember(settings) { mutableStateOf(settings.ollamaModel) }
    var ollamaTextModel by remember(settings) { mutableStateOf(settings.ollamaTextModel) }

    var anantTapCount by remember { mutableIntStateOf(0) }
    var versionTapCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(anantTapCount) {
        if (anantTapCount in 1 until EASTER_EGG_TAP_TARGET) {
            delay(2_000)
            anantTapCount = 0
            onAnantTapHintDismiss()
        }
    }

    LaunchedEffect(versionTapCount) {
        if (versionTapCount in 1 until VERSION_RESET_TAPS) {
            delay(2_000)
            versionTapCount = 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AiStatusBanner(isConfigured = settings.isConfigured, provider = settings.provider)

        // --- AI provider -----------------------------------------------------------------
        SettingsCard(title = "AI Provider") {
            val options = listOf(
                AiProvider.OPENROUTER to "OpenRouter",
                AiProvider.GEMINI to "Gemini",
                AiProvider.OLLAMA to "Ollama"
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = provider == value,
                        onClick = { provider = value },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size)
                    ) { Text(label) }
                }
            }

            Spacer(Modifier.size(4.dp))

            when (provider) {
                AiProvider.OPENROUTER -> {
                    SettingField(
                        label = "API Key",
                        value = openRouterKey,
                        onValueChange = { openRouterKey = it },
                        isSecret = true
                    )
                    ModelDropdown(
                        label = "Photo model (free + vision)",
                        noun = "free vision",
                        selectedModel = openRouterModel,
                        onModelChange = { openRouterModel = it },
                        modelsState = modelsState,
                        provider = AiProvider.OPENROUTER,
                        apiKey = openRouterKey,
                        onLoad = onLoadModels
                    )
                    Spacer(Modifier.size(4.dp))
                    ModelDropdown(
                        label = "Text model (free)",
                        noun = "free",
                        selectedModel = openRouterTextModel,
                        onModelChange = { openRouterTextModel = it },
                        modelsState = textModelsState,
                        provider = AiProvider.OPENROUTER,
                        apiKey = openRouterKey,
                        onLoad = onLoadTextModels
                    )
                    HintText("Text model is used for typed logs and \"recalculate with AI\" (no photo). Leave blank to reuse the photo model.")
                }

                AiProvider.GEMINI -> {
                    SettingField(
                        label = "API Key",
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        isSecret = true
                    )
                    ModelDropdown(
                        label = "Photo model (vision)",
                        noun = "vision",
                        selectedModel = geminiModel,
                        onModelChange = { geminiModel = it },
                        modelsState = modelsState,
                        provider = AiProvider.GEMINI,
                        apiKey = geminiKey,
                        onLoad = onLoadModels
                    )
                    Spacer(Modifier.size(4.dp))
                    ModelDropdown(
                        label = "Text model (free tier)",
                        noun = "free",
                        selectedModel = geminiTextModel,
                        onModelChange = { geminiTextModel = it },
                        modelsState = textModelsState,
                        provider = AiProvider.GEMINI,
                        apiKey = geminiKey,
                        onLoad = onLoadTextModels
                    )
                    HintText("Get a key from Google AI Studio (aistudio.google.com), enter it above, then pick a vision model like gemini-2.0-flash or gemini-2.5-flash (NOT an image-generation model). Text model (used for typed logs / recalculate) can be a lighter model like gemini-2.0-flash-lite. Free tier is rate-limited (HTTP 429) — wait a bit between requests.")
                }

                AiProvider.OLLAMA -> {
                    SettingField(
                        label = "Server URL",
                        value = ollamaUrl,
                        onValueChange = { ollamaUrl = it },
                        keyboardType = KeyboardType.Uri
                    )
                    SettingField(
                        label = "Photo model (vision)",
                        value = ollamaModel,
                        onValueChange = { ollamaModel = it }
                    )
                    SettingField(
                        label = "Text model",
                        value = ollamaTextModel,
                        onValueChange = { ollamaTextModel = it }
                    )
                    HintText("Reachable from the phone, e.g. http://192.168.1.10:11434. Use a vision model (llava) for food photos; the text model (e.g. llama3) handles typed logs. Leave text model blank to reuse the photo model.")
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onSave(
                        settings.copy(
                            provider = provider,
                            openRouterApiKey = openRouterKey.trim(),
                            openRouterModel = openRouterModel.trim(),
                            openRouterTextModel = openRouterTextModel.trim(),
                            geminiApiKey = geminiKey.trim(),
                            geminiModel = geminiModel.trim(),
                            geminiTextModel = geminiTextModel.trim(),
                            ollamaBaseUrl = ollamaUrl.trim(),
                            ollamaModel = ollamaModel.trim(),
                            ollamaTextModel = ollamaTextModel.trim()
                        )
                    )
                }
            ) { Text("Save AI Settings") }
        }

        // --- Appearance ------------------------------------------------------------------
        SettingsCard(title = "Appearance") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Material You", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Use wallpaper-based dynamic colors (Android 12+)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.dynamicColor,
                    onCheckedChange = onDynamicColorChange
                )
            }
        }

        // --- Backup & Data ---------------------------------------------------------------
        SettingsCard(title = "Backup & Data") {
            Text(
                "Export all your data (profile, readings, food and exercise logs, presets) to a " +
                    "JSON file, or import a backup. Importing replaces everything currently in the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onExport) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Export")
                }
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onImport) {
                    Icon(Icons.Filled.FileUpload, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Import")
                }
            }
        }

        // --- About -----------------------------------------------------------------------
        SettingsCard(title = "About") {
            AboutRow("App", "FitBuddy")
            AboutRow(
                label = "Version",
                value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                onValueClick = {
                    versionTapCount++
                    if (versionTapCount >= VERSION_RESET_TAPS) {
                        versionTapCount = 0
                        anantTapCount = 0
                        onAnantTapHintDismiss()
                        onResetEasterEggData()
                    }
                }
            )
            AboutRow("Package", BuildConfig.APPLICATION_ID)
            AboutRow(
                label = "Created by",
                value = "Anant",
                onValueClick = {
                    if (settings.easterEggDiscovered) {
                        onAnantTapWhenUnlocked()
                        return@AboutRow
                    }
                    anantTapCount++
                    when {
                        anantTapCount >= EASTER_EGG_TAP_TARGET -> {
                        anantTapCount = 0
                        onAnantTapHintDismiss()
                        onEasterEggTriggered()
                    }
                        anantTapCount >= EASTER_EGG_HINT_START -> {
                            onAnantTapHint(EASTER_EGG_TAP_TARGET - anantTapCount)
                        }
                    }
                }
            )
            AboutLinkRow("GitHub", "github.com/anantdark", "https://github.com/anantdark")
            Spacer(Modifier.size(4.dp))
            Text(
                "AI-powered health tracker optimised for Indian diets and lifestyles. Log meals " +
                    "and workouts via photo or loose text; the AI estimates calories and macros.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(4.dp))
            Text(
                "Built with Kotlin, Jetpack Compose, MVVM + Room. Data stays on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Read-only exposed-dropdown listing vision-capable models for the active [provider] (free-only
 * for OpenRouter, all vision models for Gemini). Tapping the field opens the list; a separate
 * field below allows typing a custom model id. Auto-loads when the provider/key changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelDropdown(
    label: String,
    noun: String,
    selectedModel: String,
    onModelChange: (String) -> Unit,
    modelsState: ModelsUiState,
    provider: AiProvider,
    apiKey: String,
    onLoad: (AiProvider, String, Boolean) -> Unit
) {
    // Re-fetch when the provider or key changes (Gemini needs a key to list models).
    LaunchedEffect(provider, apiKey) { onLoad(provider, apiKey, false) }

    var expanded by remember { mutableStateOf(false) }
    val options = modelsState.options

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedModel.ifBlank { "Select a model" },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            trailingIcon = {
                if (modelsState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            // PrimaryNotEditable makes the whole field a tap target that toggles the menu.
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    enabled = false,
                    text = {
                        Text(
                            when {
                                modelsState.isLoading -> "Loading $noun models…"
                                provider == AiProvider.GEMINI && apiKey.isBlank() ->
                                    "Enter your API key first"
                                modelsState.error != null -> "Couldn't load models"
                                else -> "No $noun models found"
                            }
                        )
                    },
                    onClick = {}
                )
            } else {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(option.displayName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    option.id,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onModelChange(option.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        val statusText = when {
            modelsState.isLoading -> "Loading $noun models…"
            modelsState.error != null -> "Couldn't load list: ${modelsState.error}"
            options.isNotEmpty() -> "${options.size} $noun models available"
            provider == AiProvider.GEMINI && apiKey.isBlank() -> "Enter your API key to load models"
            modelsState.loaded -> "No $noun models found; type an id below"
            else -> "Pick an image-capable model"
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = { onLoad(provider, apiKey, true) },
            enabled = !modelsState.isLoading
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Reload models")
        }
    }

    // Manual override / advanced entry — always available, even if the list is empty.
    OutlinedTextField(
        value = selectedModel,
        onValueChange = onModelChange,
        label = { Text("Model id") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AiStatusBanner(isConfigured: Boolean, provider: AiProvider) {
    val container = if (isConfigured) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isConfigured) Icons.Filled.CheckCircle else Icons.Filled.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
                Text(
                    text = if (isConfigured) "AI online" else "AI offline (simulator)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isConfigured) {
                        "Using ${provider.name.lowercase()}"
                    } else {
                        "Add a key/server below to enable live analysis"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun SettingField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isSecret: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isSecret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun HintText(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AboutRow(
    label: String,
    value: String,
    onValueClick: (() -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = if (onValueClick != null) {
                Modifier.clickable(onClick = onValueClick)
            } else {
                Modifier
            }
        )
    }
}

/** Like [AboutRow], but the value is a tappable link that opens [url] in the browser. */
@Composable
private fun AboutLinkRow(label: String, value: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(url) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.Code,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
