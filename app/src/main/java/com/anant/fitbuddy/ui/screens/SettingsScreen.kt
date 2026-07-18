package com.anant.fitbuddy.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.data.settings.AiProvider
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.data.settings.isPlausibleModelIdFor
import com.anant.fitbuddy.data.settings.parseApiKeys
import com.anant.fitbuddy.ui.viewmodel.ModelsUiState
import com.anant.fitbuddy.ui.viewmodel.UpdateUiState
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
    onLoadModels: (AiProvider, String, Boolean, String) -> Unit,
    onLoadTextModels: (AiProvider, String, Boolean, String) -> Unit,
    onSave: (AppSettings) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onEasterEggTriggered: () -> Unit,
    onAnantTapHint: (remainingTaps: Int) -> Unit,
    onAnantTapHintDismiss: () -> Unit,
    onAnantTapWhenUnlocked: () -> Unit,
    onResetEasterEggData: () -> Unit,
    updateState: UpdateUiState,
    onCheckForUpdates: () -> Unit,
    onAutoCheckUpdatesChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local editable copies, re-seeded whenever persisted settings change.
    var provider by remember(settings) { mutableStateOf(settings.provider) }
    var openRouterKeys by remember(settings) {
        mutableStateOf(settings.keysFor(AiProvider.OPENROUTER))
    }
    var openRouterModel by remember(settings) { mutableStateOf(settings.openRouterModel) }
    var openRouterTextModel by remember(settings) { mutableStateOf(settings.openRouterTextModel) }
    var geminiKeys by remember(settings) {
        mutableStateOf(settings.keysFor(AiProvider.GEMINI))
    }
    var geminiModel by remember(settings) { mutableStateOf(settings.geminiModel) }
    var geminiTextModel by remember(settings) { mutableStateOf(settings.geminiTextModel) }
    var ollamaUrl by remember(settings) { mutableStateOf(settings.ollamaBaseUrl) }
    var ollamaModel by remember(settings) { mutableStateOf(settings.ollamaModel) }
    var ollamaTextModel by remember(settings) { mutableStateOf(settings.ollamaTextModel) }
    var ollamaUseCloud by remember(settings) { mutableStateOf(settings.ollamaUseCloud) }
    var ollamaKeys by remember(settings) {
        mutableStateOf(settings.keysFor(AiProvider.OLLAMA))
    }
    var aiAutoFailover by remember(settings) { mutableStateOf(settings.aiAutoFailover) }

    var anantTapCount by remember { mutableIntStateOf(0) }
    var versionTapCount by remember { mutableIntStateOf(0) }

    val openRouterKey = openRouterKeys.firstOrNull().orEmpty()
    val geminiKey = geminiKeys.firstOrNull().orEmpty()
    val ollamaApiKey = ollamaKeys.firstOrNull().orEmpty()

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
            SettingToggleRow(
                title = "Auto failover",
                checked = aiAutoFailover,
                onCheckedChange = { aiAutoFailover = it },
                hintTitle = "Auto failover",
                hint = "When on, FitBuddy tries other API keys, then other models on the " +
                    "same platform. When off, only your selected model is used, but " +
                    "other API keys are still tried on failure. Change platform " +
                    "manually if everything fails."
            )

            if (aiAutoFailover) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Photo · ${settings.activePhotoModelDisplay()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Text · ${settings.activeTextModelDisplay()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HintIconButton(
                        title = "Active models",
                        message = "Active models update when you save AI settings, and after " +
                            "each successful AI request. Rate-limited models are skipped " +
                            "until the next UTC midnight."
                    )
                }
            }

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

            // When Auto has an active model for this provider, keep local dropdown state in sync.
            // Never copy Gemini Studio ids into OpenRouter/Ollama fields.
            LaunchedEffect(
                aiAutoFailover,
                settings.activeAiProvider,
                settings.activePhotoModel,
                settings.activeTextModel,
                provider
            ) {
                if (!aiAutoFailover) return@LaunchedEffect
                if (settings.activeAiProvider != provider) return@LaunchedEffect
                settings.activePhotoModel.takeIf { isPlausibleModelIdFor(provider, it) }?.let { active ->
                    when (provider) {
                        AiProvider.OPENROUTER -> openRouterModel = active
                        AiProvider.GEMINI -> geminiModel = active
                        AiProvider.OLLAMA -> ollamaModel = active
                    }
                }
                settings.activeTextModel.takeIf { isPlausibleModelIdFor(provider, it) }?.let { active ->
                    when (provider) {
                        AiProvider.OPENROUTER -> openRouterTextModel = active
                        AiProvider.GEMINI -> geminiTextModel = active
                        AiProvider.OLLAMA -> ollamaTextModel = active
                    }
                }
            }

            // One-shot cleanup if a bad Gemini Studio id was previously saved under OpenRouter.
            LaunchedEffect(settings.openRouterModel, settings.openRouterTextModel) {
                if (!isPlausibleModelIdFor(AiProvider.OPENROUTER, openRouterModel) &&
                    openRouterModel.isNotBlank()
                ) {
                    openRouterModel = AppSettings.DEFAULT_OPENROUTER_MODEL
                }
                if (openRouterTextModel.isNotBlank() &&
                    !isPlausibleModelIdFor(AiProvider.OPENROUTER, openRouterTextModel)
                ) {
                    openRouterTextModel = ""
                }
            }

            when (provider) {
                AiProvider.OPENROUTER -> {
                    ApiKeyChipEditor(
                        label = "API keys",
                        keys = openRouterKeys,
                        onKeysChange = { openRouterKeys = it }
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
                    ModelDropdown(
                        label = "Text model (free)",
                        noun = "free",
                        selectedModel = openRouterTextModel,
                        onModelChange = { openRouterTextModel = it },
                        modelsState = textModelsState,
                        provider = AiProvider.OPENROUTER,
                        apiKey = openRouterKey,
                        onLoad = onLoadTextModels,
                        hintTitle = "Text model",
                        hint = "Used for typed logs and \"recalculate with AI\" (no photo). " +
                            "Leave blank to reuse the photo model. Gemma models are listed first."
                    )
                }

                AiProvider.GEMINI -> {
                    ApiKeyChipEditor(
                        label = "API keys",
                        keys = geminiKeys,
                        onKeysChange = { geminiKeys = it }
                    )
                    ModelDropdown(
                        label = "Photo model (free, by intelligence)",
                        noun = "free vision",
                        selectedModel = geminiModel,
                        onModelChange = { geminiModel = it },
                        modelsState = modelsState,
                        provider = AiProvider.GEMINI,
                        apiKey = geminiKey,
                        onLoad = onLoadModels,
                        hintTitle = "Gemini",
                        hint = "Get a key from Google AI Studio (aistudio.google.com). Free " +
                            "Flash models only (smartest-first). With Auto failover on, failed " +
                            "keys then models rotate on Gemini only."
                    )
                    ModelDropdown(
                        label = "Text model (free, by intelligence)",
                        noun = "free",
                        selectedModel = geminiTextModel,
                        onModelChange = { geminiTextModel = it },
                        modelsState = textModelsState,
                        provider = AiProvider.GEMINI,
                        apiKey = geminiKey,
                        onLoad = onLoadTextModels
                    )
                }

                AiProvider.OLLAMA -> {
                    val modeOptions = listOf(false to "Local", true to "Cloud")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        modeOptions.forEachIndexed { index, (useCloud, label) ->
                            SegmentedButton(
                                selected = ollamaUseCloud == useCloud,
                                onClick = { ollamaUseCloud = useCloud },
                                shape = SegmentedButtonDefaults.itemShape(index, modeOptions.size)
                            ) { Text(label) }
                        }
                    }
                    if (ollamaUseCloud) {
                        ApiKeyChipEditor(
                            label = "API keys",
                            keys = ollamaKeys,
                            onKeysChange = { ollamaKeys = it }
                        )
                    } else {
                        SettingField(
                            label = "Server URL",
                            value = ollamaUrl,
                            onValueChange = { ollamaUrl = it },
                            keyboardType = KeyboardType.Uri,
                            trailingHintTitle = "Server URL",
                            trailingHint = "Reachable from the phone, e.g. http://192.168.1.10:11434. " +
                                "Use a vision model (llava/gemma) for food photos."
                        )
                    }
                    val ollamaListKey = if (ollamaUseCloud) ollamaApiKey else ""
                    val ollamaListUrl = if (ollamaUseCloud) {
                        AppSettings.OLLAMA_CLOUD_BASE_URL
                    } else {
                        ollamaUrl
                    }
                    ModelDropdown(
                        label = "Photo model (vision)",
                        noun = "vision",
                        selectedModel = ollamaModel,
                        onModelChange = { ollamaModel = it },
                        modelsState = modelsState,
                        provider = AiProvider.OLLAMA,
                        apiKey = ollamaListKey,
                        baseUrl = ollamaListUrl,
                        onLoad = onLoadModels,
                        hintTitle = if (ollamaUseCloud) "Ollama Cloud" else "Ollama Local",
                        hint = if (ollamaUseCloud) {
                            "Cloud uses https://ollama.com. Create keys at ollama.com/settings/keys. " +
                                "Gemma models are preferred. Leave text model blank to reuse the photo model."
                        } else {
                            "Use a vision model (llava/gemma) for food photos. Leave text model " +
                                "blank to reuse the photo model."
                        }
                    )
                    ModelDropdown(
                        label = "Text model",
                        noun = "Ollama",
                        selectedModel = ollamaTextModel,
                        onModelChange = { ollamaTextModel = it },
                        modelsState = textModelsState,
                        provider = AiProvider.OLLAMA,
                        apiKey = ollamaListKey,
                        baseUrl = ollamaListUrl,
                        onLoad = onLoadTextModels
                    )
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onSave(
                        settings.copy(
                            provider = provider,
                            openRouterApiKeys = openRouterKeys,
                            openRouterApiKey = openRouterKey,
                            openRouterModel = openRouterModel.trim(),
                            openRouterTextModel = openRouterTextModel.trim(),
                            geminiApiKeys = geminiKeys,
                            geminiApiKey = geminiKey,
                            geminiModel = geminiModel.trim(),
                            geminiTextModel = geminiTextModel.trim(),
                            ollamaBaseUrl = ollamaUrl.trim(),
                            ollamaModel = ollamaModel.trim(),
                            ollamaTextModel = ollamaTextModel.trim(),
                            ollamaUseCloud = ollamaUseCloud,
                            ollamaApiKeys = ollamaKeys,
                            ollamaApiKey = ollamaApiKey,
                            aiAutoFailover = aiAutoFailover
                        )
                    )
                }
            ) { Text("Save AI Settings") }
        }

        // --- Updates (below AI provider) -------------------------------------------------
        SettingsCard(title = "Updates", initiallyExpanded = false) {
            Text(
                text = "Installed ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SettingToggleRow(
                title = "Check automatically",
                checked = settings.autoCheckUpdates,
                onCheckedChange = onAutoCheckUpdatesChange,
                hintTitle = "Automatic updates",
                hint = "Looks for a newer GitHub release shortly after startup."
            )
            OutlinedButton(
                onClick = onCheckForUpdates,
                enabled = !updateState.isChecking && !updateState.isDownloading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (updateState.isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                    Text("Checking...")
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Check for Updates")
                }
            }
            updateState.statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (updateState.statusIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        // --- Appearance ------------------------------------------------------------------
        SettingsCard(title = "Appearance", initiallyExpanded = false) {
            SettingToggleRow(
                title = "Material You",
                checked = settings.dynamicColor,
                onCheckedChange = onDynamicColorChange,
                hintTitle = "Material You",
                hint = "Use wallpaper-based dynamic colors (Android 12+)."
            )
        }

        // --- Backup & Data ---------------------------------------------------------------
        SettingsCard(
            title = "Backup & Data",
            initiallyExpanded = false,
            hintTitle = "Backup & Data",
            hint = "Export all your data (profile, readings, food and exercise logs, presets) " +
                "to a JSON file, or import a backup. Importing replaces everything currently " +
                "in the app."
        ) {
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
        SettingsCard(
            title = "About",
            collapsible = false,
            hintTitle = "About FitBuddy",
            hint = "AI-powered health tracker optimised for Indian diets and lifestyles. " +
                "Log meals and workouts via photo or loose text; the AI estimates calories " +
                "and macros.\n\nBuilt with Kotlin, Jetpack Compose, MVVM + Room. Data stays " +
                "on your device."
        ) {
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
        }
    }
}

/** Update / download dialogs — shown from [MainScreen] so startup checks work outside Settings. */
@Composable
fun UpdatePromptDialogs(
    updateState: UpdateUiState,
    onDismissUpdatePrompt: () -> Unit,
    onConfirmUpdate: (downloadUrl: String) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    updateState.updateInfo?.let { info ->
        val highlights = remember(info.releaseNotes) { releaseNoteHighlights(info.releaseNotes) }
        AlertDialog(
            onDismissRequest = onDismissUpdatePrompt,
            icon = {
                Icon(
                    Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Update available") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = info.versionName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Build ${info.versionCode}  ·  you have ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (highlights.isNotEmpty()) {
                        Text(
                            text = "What's new",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            highlights.forEach { line ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("•", color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    if (info.htmlUrl.isNotBlank()) {
                        TextButton(
                            onClick = { uriHandler.openUri(info.htmlUrl) },
                            modifier = Modifier.padding(start = 0.dp)
                        ) {
                            Text("View on GitHub")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onConfirmUpdate(info.downloadUrl) },
                    enabled = !updateState.isDownloading
                ) { Text("Download & install") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismissUpdatePrompt,
                    enabled = !updateState.isDownloading
                ) { Text("Later") }
            }
        )
    }

    if (updateState.isDownloading) {
        AlertDialog(
            onDismissRequest = {},
            icon = {
                CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
            },
            title = { Text("Downloading update") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val progress = updateState.downloadProgress
                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${(progress * 100).toInt()}% complete",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = "Downloading APK…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Keep FitBuddy open until the installer opens.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {}
        )
    }
}

/** Pull commit bullets from CI release notes; drop headers / metadata noise. */
private fun releaseNoteHighlights(raw: String, limit: Int = 6): List<String> =
    raw.lineSequence()
        .map { it.trim() }
        .filter { it.startsWith("- ") }
        .map { line ->
            line.removePrefix("- ")
                .replace(Regex("""\s*\([0-9a-f]{7,40}\)\s*$"""), "")
                .trim()
        }
        .filter { it.isNotBlank() }
        .distinct()
        .take(limit)
        .toList()

/**
 * Read-only exposed-dropdown listing vision-capable models for the active [provider] (free-only
 * for OpenRouter, all vision models for Gemini). Tapping the field opens the list; a Model id
 * field follows for custom entry, then the available-count / reload row. Auto-loads when the
 * provider/key changes.
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
    onLoad: (AiProvider, String, Boolean, String) -> Unit,
    baseUrl: String = "",
    hintTitle: String? = null,
    hint: String? = null
) {
    // Re-fetch when the provider, key, or Ollama URL changes.
    LaunchedEffect(provider, apiKey, baseUrl) { onLoad(provider, apiKey, false, baseUrl) }

    var expanded by remember { mutableStateOf(false) }
    val options = modelsState.options
    val ollamaNeedsUrl = provider == AiProvider.OLLAMA &&
        baseUrl != AppSettings.OLLAMA_CLOUD_BASE_URL &&
        baseUrl.isBlank()
    val ollamaNeedsKey = provider == AiProvider.OLLAMA &&
        baseUrl == AppSettings.OLLAMA_CLOUD_BASE_URL &&
        apiKey.isBlank()

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
                                ollamaNeedsKey -> "Enter your Ollama Cloud API key first"
                                ollamaNeedsUrl -> "Enter your Ollama server URL first"
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

    // Manual override / advanced entry — always available, even if the list is empty.
    OutlinedTextField(
        value = selectedModel,
        onValueChange = onModelChange,
        label = { Text("Model id") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        val statusText = when {
            modelsState.isLoading -> "Loading $noun models…"
            modelsState.error != null -> "Couldn't load list: ${modelsState.error}"
            options.isNotEmpty() -> "${options.size} $noun models available"
            provider == AiProvider.GEMINI && apiKey.isBlank() -> "Enter your API key to load models"
            ollamaNeedsKey -> "Enter your Ollama Cloud API key to load models"
            ollamaNeedsUrl -> "Enter your Ollama server URL to load models"
            modelsState.loaded -> "No $noun models found; type an id above"
            else -> "Pick an image-capable model"
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (hint != null && hintTitle != null) {
            HintIconButton(title = hintTitle, message = hint)
        }
        IconButton(
            onClick = { onLoad(provider, apiKey, true, baseUrl) },
            enabled = !modelsState.isLoading
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Reload models")
        }
    }
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
private fun SettingsCard(
    title: String,
    collapsible: Boolean = true,
    initiallyExpanded: Boolean = true,
    hintTitle: String? = null,
    hint: String? = null,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded || !collapsible) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (collapsible) {
                                Modifier.clickable { expanded = !expanded }
                            } else {
                                Modifier
                            }
                        )
                )
                if (hint != null && hintTitle != null) {
                    HintIconButton(title = hintTitle, message = hint)
                }
                if (collapsible) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                }
            }
            AnimatedVisibility(visible = expanded || !collapsible) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    hintTitle: String? = null,
    hint: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (hint != null && hintTitle != null) {
            HintIconButton(title = hintTitle, message = hint)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isSecret: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingHintTitle: String? = null,
    trailingHint: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isSecret) {
            PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = if (trailingHint != null && trailingHintTitle != null) {
            { HintIconButton(title = trailingHintTitle, message = trailingHint) }
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth()
    )
}

/** Multi-key editor: chips with remove; Add field accepts comma/newline paste. */
@Composable
fun ApiKeyChipEditor(
    label: String,
    keys: List<String>,
    onKeysChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember { mutableStateOf("") }

    fun commitRaw(raw: String): Boolean {
        val added = parseApiKeys(raw)
        if (added.isEmpty()) return false
        onKeysChange((keys + added).distinct())
        return true
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            HintIconButton(
                title = "API keys",
                message = "Paste multiple keys separated by commas or new lines. Keys are " +
                    "masked and can't be copied after adding."
            )
        }
        if (keys.isNotEmpty()) {
            // Column instead of FlowRow — BOM 2024.09 lacks the FlowRowOverflow overload
            // that the compiler was linking, which crashed Settings with NoSuchMethodError.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                keys.forEach { key ->
                    InputChip(
                        selected = false,
                        onClick = { },
                        // Fully masked — never show raw key text that could be copied.
                        label = { Text(maskApiKey(key), maxLines = 1) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove key",
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onKeysChange(keys - key) }
                            )
                        }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Allow paste / select-all; block copy & cut so a key can't leave via the toolbar.
            val parentToolbar = LocalTextToolbar.current
            CompositionLocalProvider(
                LocalTextToolbar provides remember(parentToolbar) {
                    PasteOnlyTextToolbar(parentToolbar)
                }
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { new ->
                        val pasted = new.length - draft.length > 1 ||
                            '\n' in new || ',' in new || ';' in new
                        if (pasted && commitRaw(new)) {
                            draft = ""
                        } else {
                            draft = new
                        }
                    },
                    label = { Text("Add key (paste several)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.weight(1f)
                )
            }
            TextButton(
                onClick = {
                    if (commitRaw(draft)) draft = ""
                }
            ) { Text("Add") }
        }
    }
}

/** Bullets only — no suffix of the real key. */
private fun maskApiKey(key: String): String {
    val n = key.length.coerceIn(8, 16)
    return "•".repeat(n)
}

/**
 * Delegates to the platform text toolbar but omits Copy/Cut so API keys can't be
 * exfiltrated from the draft field. Paste and Select All still work (long-press menu).
 */
private class PasteOnlyTextToolbar(
    private val delegate: TextToolbar
) : TextToolbar {
    override val status: TextToolbarStatus get() = delegate.status
    override fun hide() = delegate.hide()
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        delegate.showMenu(
            rect = rect,
            onCopyRequested = null,
            onPasteRequested = onPasteRequested,
            onCutRequested = null,
            onSelectAllRequested = onSelectAllRequested
        )
    }
}

@Composable
private fun HintIconButton(
    title: String,
    message: String
) {
    var show by remember { mutableStateOf(false) }
    IconButton(
        onClick = { show = true },
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = "About $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            icon = {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(title) },
            text = {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { show = false }) { Text("Got it") }
            }
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
