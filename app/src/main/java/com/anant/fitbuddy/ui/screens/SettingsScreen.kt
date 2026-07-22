package com.anant.fitbuddy.ui.screens

import android.Manifest
import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable
import android.widget.ImageView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.viewinterop.AndroidView
import com.anant.fitbuddy.R
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.data.database.UserProfile
import com.anant.fitbuddy.data.model.ModelOption
import com.anant.fitbuddy.data.model.OpenAiCatalog
import com.anant.fitbuddy.data.settings.AiProvider
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.data.settings.isPlausibleModelIdFor
import com.anant.fitbuddy.data.settings.parseApiKeys
import com.anant.fitbuddy.reminders.ReminderReceiver
import com.anant.fitbuddy.reminders.ReminderScheduler
import com.anant.fitbuddy.ui.components.Button
import com.anant.fitbuddy.ui.components.ConfettiOverlay
import com.anant.fitbuddy.ui.components.CraftedWithLoveCredit
import com.anant.fitbuddy.ui.components.IconButton
import com.anant.fitbuddy.ui.components.OpenRouterConnectSection
import com.anant.fitbuddy.ui.components.OutlinedButton
import com.anant.fitbuddy.ui.components.TextButton
import com.anant.fitbuddy.ui.util.dismissKeyboardOnTap
import com.anant.fitbuddy.ui.util.rememberDismissKeyboard
import com.anant.fitbuddy.ui.viewmodel.ModelsUiState
import com.anant.fitbuddy.ui.viewmodel.UpdateUiState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.util.Locale

private const val EASTER_EGG_TAP_TARGET = 31
private const val EASTER_EGG_HINT_START = 25
private const val VERSION_RESET_TAPS = 8
private const val DEVELOPER_UNLOCK_TAPS = 31
// Start showing the "N taps left" hint only when 5 or fewer taps remain.
private const val DEVELOPER_HINT_START = DEVELOPER_UNLOCK_TAPS - 5

private val PROFILE_SEX_OPTIONS = listOf(
    "" to "Not set",
    "MALE" to "Male",
    "FEMALE" to "Female"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    profile: UserProfile? = null,
    modelsState: ModelsUiState,
    textModelsState: ModelsUiState,
    onLoadModels: (AiProvider, String, Boolean, String, Boolean) -> Unit,
    onLoadTextModels: (AiProvider, String, Boolean, String, Boolean) -> Unit,
    onSave: (AppSettings) -> Unit,
    /** Persist without a snackbar (e.g. Auto failover toggle). Defaults to [onSave]. */
    onSaveQuiet: (AppSettings) -> Unit = onSave,
    onSavePermanentProfile: (
        firstName: String,
        lastName: String,
        age: Int,
        heightCm: Double,
        sex: String?
    ) -> Unit = { _, _, _, _, _ -> },
    onConnectOpenRouter: (android.content.Context) -> Unit = {},
    onDisconnectOpenRouter: () -> Unit = {},
    openRouterOAuthBusy: Boolean = false,
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
    onCrashReportingChange: (Boolean) -> Unit,
    onSupportIdCopied: () -> Unit = {},
    onDeveloperUnlockHint: (remainingTaps: Int) -> Unit = {},
    onDeveloperUnlockHintDismiss: () -> Unit = {},
    onDeveloperModeToggled: (unlocked: Boolean) -> Unit = {},
    onClearModelCooldowns: () -> Unit = {},
    onShowTestUpdatePrompt: () -> Unit = {},
    onTestNotificationSent: (ok: Boolean) -> Unit = {},
    onPermissionDenied: (message: String) -> Unit = {},
    mongoBackupBusy: Boolean = false,
    onCloudBackupEnabledChange: (Boolean) -> Unit = {},
    onPrepareCloudBackupEnable: () -> Unit = {},
    onEnableCloudBackup: (CharArray?) -> Unit = {},
    onCloudAutoUploadChange: (Boolean) -> Unit = {},
    onMongoUpload: () -> Unit = {},
    onMongoDownload: (supportId: String) -> Unit = {},
    onChangeCloudPassword: (CharArray?) -> Unit = {},
    onRegenerateSupportId: () -> Unit = {},
    /** Settings-only: ♥ double-tap also sends a Sentry heartbeat (when reporting is on). */
    onHeartDoubleTapHeartbeat: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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
    var openAiKeys by remember(settings) {
        mutableStateOf(settings.keysFor(AiProvider.OPENAI))
    }
    var openAiModel by remember(settings) { mutableStateOf(settings.openAiModel) }
    var openAiTextModel by remember(settings) { mutableStateOf(settings.openAiTextModel) }
    var aiAutoFailover by remember(settings) { mutableStateOf(settings.aiAutoFailover) }
    var showPaidModels by remember(settings) { mutableStateOf(settings.showPaidModels) }

    var profileFirstName by remember(settings.firstName) { mutableStateOf(settings.firstName) }
    var profileLastName by remember(settings.lastName) { mutableStateOf(settings.lastName) }
    var profileAge by remember(profile?.age) {
        mutableStateOf(profile?.age?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var profileHeight by remember(profile?.heightCm) {
        mutableStateOf(profile?.heightCm?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var profileSex by remember(profile?.sex) { mutableStateOf(profile?.sex.orEmpty()) }

    var anantTapCount by remember { mutableIntStateOf(0) }
    var versionTapCount by remember { mutableIntStateOf(0) }
    var packageTapCount by remember { mutableIntStateOf(0) }
    val developerUnlocked = settings.developerModeUnlocked
    var confettiKey by remember { mutableIntStateOf(0) }
    var showConfetti by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var pendingEnableReminder by remember { mutableStateOf(false) }
    var pendingTestNotification by remember { mutableStateOf(false) }
    var awaitingExactAlarmSettings by remember { mutableStateOf(false) }
    var showMongoRestoreConfirm by remember { mutableStateOf(false) }
    var mongoRestoreSupportIdDraft by remember(settings.supportId) {
        mutableStateOf(settings.supportId)
    }
    var showCloudBackupEnableConfirm by remember { mutableStateOf(false) }
    var showChangeCloudPasswordDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val cloudVaultAvailable = remember {
        com.anant.fitbuddy.data.backup.mongo.MongoUriVault.isAvailable()
    }

    val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val notificationPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS) { granted ->
        if (granted) {
            if (pendingEnableReminder) {
                pendingEnableReminder = false
                onSave(settings.copy(dailyLogReminderEnabled = true))
                if (!ReminderScheduler.canScheduleExactAlarms(context)) {
                    onPermissionDenied(
                        "Exact alarms not allowed — reminders may be delayed."
                    )
                }
            }
            if (pendingTestNotification) {
                pendingTestNotification = false
                val ok = ReminderReceiver.postReminderNotification(context, isTest = true)
                onTestNotificationSent(ok)
            }
        } else {
            if (pendingEnableReminder) {
                pendingEnableReminder = false
                if (settings.dailyLogReminderEnabled) {
                    onSave(settings.copy(dailyLogReminderEnabled = false))
                }
                onPermissionDenied("Notifications not allowed — reminder turned off.")
            }
            if (pendingTestNotification) {
                pendingTestNotification = false
                onPermissionDenied("Notifications not allowed.")
            }
        }
    }

    val openRouterKey = openRouterKeys.firstOrNull().orEmpty()
        .ifBlank { settings.openRouterOAuthKey }
    val geminiKey = geminiKeys.firstOrNull().orEmpty()
    val ollamaApiKey = ollamaKeys.firstOrNull().orEmpty()
    val openAiKey = openAiKeys.firstOrNull().orEmpty()

    // OpenAI has no free tier: force "Show paid models" on whenever it's the selected
    // provider — the toggle is also locked (disabled) while selected. Auto failover defaults
    // to off for a freshly-selected OpenAI (not coupled/locked — the user can re-enable it).
    LaunchedEffect(provider) {
        if (provider == AiProvider.OPENAI && !showPaidModels) {
            showPaidModels = true
            aiAutoFailover = false
            onSaveQuiet(settings.copy(showPaidModels = true, aiAutoFailover = false))
        }
    }

    LaunchedEffect(confettiKey) {
        if (confettiKey == 0) return@LaunchedEffect
        showConfetti = true
        delay(4_000L)
        showConfetti = false
    }

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

    LaunchedEffect(packageTapCount) {
        if (packageTapCount in 1 until DEVELOPER_UNLOCK_TAPS) {
            delay(2_000)
            packageTapCount = 0
            onDeveloperUnlockHintDismiss()
        }
    }

    // Permission revoked in system settings while reminder was on → force toggle off.
    LaunchedEffect(
        notificationPermission.status.isGranted,
        settings.dailyLogReminderEnabled,
        pendingEnableReminder
    ) {
        if (
            needsNotificationPermission &&
            !notificationPermission.status.isGranted &&
            settings.dailyLogReminderEnabled &&
            !pendingEnableReminder
        ) {
            onSave(settings.copy(dailyLogReminderEnabled = false))
            onPermissionDenied("Notifications not allowed — reminder turned off.")
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (
                event == Lifecycle.Event.ON_RESUME &&
                awaitingExactAlarmSettings
            ) {
                awaitingExactAlarmSettings = false
                if (!ReminderScheduler.canScheduleExactAlarms(context)) {
                    onPermissionDenied(
                        "Exact alarms not allowed — reminders may be delayed."
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .dismissKeyboardOnTap()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AiStatusBanner(isConfigured = settings.isConfigured, provider = settings.provider)

        // --- AI provider -----------------------------------------------------------------
        SettingsCard(title = "AI Provider", initiallyExpanded = false) {
            SettingToggleRow(
                title = "Auto failover",
                checked = aiAutoFailover,
                onCheckedChange = { enabled ->
                    aiAutoFailover = enabled
                    // Persist immediately; leave other draft AI fields for Save AI Settings.
                    onSaveQuiet(settings.copy(aiAutoFailover = enabled))
                },
                hintTitle = "Auto failover",
                hint = "When on, FitBuddy tries other API keys, then other models on the " +
                    "same platform. When off, only your selected model is used, but " +
                    "other API keys are still tried on failure. Change platform " +
                    "manually if everything fails."
            )

            if (provider == AiProvider.OPENROUTER || provider == AiProvider.GEMINI || provider == AiProvider.OPENAI) {
                SettingToggleRow(
                    title = "Show paid models",
                    checked = showPaidModels,
                    enabled = provider != AiProvider.OPENAI,
                    onCheckedChange = { enabled ->
                        showPaidModels = enabled
                        onSaveQuiet(settings.copy(showPaidModels = enabled))
                    },
                    hintTitle = "Paid models",
                    hint = if (provider == AiProvider.OPENAI) {
                        "OpenAI has no free tier, so paid models always stay on for this provider."
                    } else {
                        "Off (default): free models only. On: also list paid models. " +
                            "While paid models are shown, the Refresh button skips reachability " +
                            "checks so paid endpoints are never pinged on their own."
                    }
                )
            }

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
                        message = "Green lines show the models last used successfully. Your " +
                            "dropdown selection stays as the preferred model — Auto tries it " +
                            "first again after rate-limit cooldowns end (next UTC midnight)."
                    )
                }
            }

            val options = listOf(
                AiProvider.OPENROUTER to "OpenRouter",
                AiProvider.GEMINI to "Gemini",
                AiProvider.OLLAMA to "Ollama",
                AiProvider.OPENAI to "OpenAI"
            )
            ProviderSelectorGrid(
                options = options,
                selected = provider,
                onSelect = { provider = it }
            )

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
                    OpenRouterConnectSection(
                        oauthConnected = settings.isOpenRouterOAuthConnected,
                        oauthBusy = openRouterOAuthBusy,
                        hasManualKeys = openRouterKeys.isNotEmpty(),
                        onConnect = onConnectOpenRouter,
                        onDisconnect = onDisconnectOpenRouter,
                        context = context
                    ) {
                        ApiKeyChipEditor(
                            label = "API keys",
                            keys = openRouterKeys,
                            onKeysChange = { openRouterKeys = it }
                        )
                    }
                    ModelDropdown(
                        label = if (showPaidModels) {
                            "Photo model (vision)"
                        } else {
                            "Photo model (free + vision)"
                        },
                        noun = if (showPaidModels) "vision" else "free vision",
                        selectedModel = openRouterModel,
                        onModelChange = { openRouterModel = it },
                        modelsState = modelsState,
                        provider = AiProvider.OPENROUTER,
                        apiKey = openRouterKey,
                        onLoad = onLoadModels,
                        showPaidModels = showPaidModels
                    )
                    ModelDropdown(
                        label = if (showPaidModels) "Text model" else "Text model (free)",
                        noun = if (showPaidModels) "chat" else "free",
                        selectedModel = openRouterTextModel,
                        onModelChange = { openRouterTextModel = it },
                        modelsState = textModelsState,
                        provider = AiProvider.OPENROUTER,
                        apiKey = openRouterKey,
                        onLoad = onLoadTextModels,
                        showPaidModels = showPaidModels,
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
                        label = if (showPaidModels) {
                            "Photo model (by intelligence)"
                        } else {
                            "Photo model (free, by intelligence)"
                        },
                        noun = if (showPaidModels) "vision" else "free vision",
                        selectedModel = geminiModel,
                        onModelChange = { geminiModel = it },
                        modelsState = modelsState,
                        provider = AiProvider.GEMINI,
                        apiKey = geminiKey,
                        onLoad = onLoadModels,
                        showPaidModels = showPaidModels,
                        hintTitle = "Gemini",
                        hint = "Get a key from Google AI Studio (aistudio.google.com). " +
                            (if (showPaidModels) {
                                "Free Flash plus paid Pro (smartest-first). "
                            } else {
                                "Free Flash models only (smartest-first). "
                            }) +
                            "With Auto failover on, failed keys then models rotate on Gemini only."
                    )
                    ModelDropdown(
                        label = if (showPaidModels) {
                            "Text model (by intelligence)"
                        } else {
                            "Text model (free, by intelligence)"
                        },
                        noun = if (showPaidModels) "chat" else "free",
                        selectedModel = geminiTextModel,
                        onModelChange = { geminiTextModel = it },
                        modelsState = textModelsState,
                        provider = AiProvider.GEMINI,
                        apiKey = geminiKey,
                        onLoad = onLoadTextModels,
                        showPaidModels = showPaidModels
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
                        showPaidModels = showPaidModels,
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

                AiProvider.OPENAI -> {
                    ApiKeyChipEditor(
                        label = "API keys",
                        keys = openAiKeys,
                        onKeysChange = { openAiKeys = it }
                    )
                    ModelDropdown(
                        label = "Photo model (vision)",
                        noun = "vision",
                        selectedModel = openAiModel,
                        onModelChange = { openAiModel = it },
                        modelsState = modelsState,
                        provider = AiProvider.OPENAI,
                        apiKey = openAiKey,
                        onLoad = onLoadModels,
                        showPaidModels = true,
                        fallbackOptions = OpenAiCatalog.VISION_MODELS,
                        hintTitle = "OpenAI",
                        hint = "Get a key at platform.openai.com/api-keys. OpenAI is paid — " +
                            "there is no free tier, so Auto failover stays off."
                    )
                    ModelDropdown(
                        label = "Text model",
                        noun = "chat",
                        selectedModel = openAiTextModel,
                        onModelChange = { openAiTextModel = it },
                        modelsState = textModelsState,
                        provider = AiProvider.OPENAI,
                        apiKey = openAiKey,
                        onLoad = onLoadTextModels,
                        showPaidModels = true,
                        fallbackOptions = OpenAiCatalog.TEXT_MODELS
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
                            openAiApiKeys = openAiKeys,
                            openAiApiKey = openAiKey,
                            openAiModel = openAiModel.trim(),
                            openAiTextModel = openAiTextModel.trim(),
                            aiAutoFailover = aiAutoFailover,
                            showPaidModels = showPaidModels
                        )
                    )
                }
            ) { Text("Save AI Settings") }
        }

        // --- Profile (name + permanent body attrs) ---------------------------------------
        SettingsCard(title = "Profile", initiallyExpanded = false) {
            SettingField(
                label = "First name",
                value = profileFirstName,
                onValueChange = { profileFirstName = it }
            )
            SettingField(
                label = "Last name",
                value = profileLastName,
                onValueChange = { profileLastName = it }
            )
            SettingField(
                label = "Age",
                value = profileAge,
                onValueChange = { input ->
                    profileAge = input.filter { it.isDigit() }
                },
                keyboardType = KeyboardType.Number
            )
            SettingField(
                label = "Height (cm)",
                value = profileHeight,
                onValueChange = { input ->
                    profileHeight = input.filter { it.isDigit() || it == '.' }
                },
                keyboardType = KeyboardType.Decimal
            )
            ProfileSexDropdown(
                selectedValue = profileSex,
                onSelected = { profileSex = it }
            )
            val profileValid = profileFirstName.trim().isNotEmpty() &&
                profileLastName.trim().isNotEmpty() &&
                (profileAge.toIntOrNull() ?: 0) in 10..120 &&
                (profileHeight.toDoubleOrNull() ?: 0.0) in 50.0..280.0
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = profileValid,
                onClick = {
                    onSavePermanentProfile(
                        profileFirstName.trim(),
                        profileLastName.trim(),
                        profileAge.toIntOrNull() ?: 0,
                        profileHeight.toDoubleOrNull() ?: 0.0,
                        profileSex.ifBlank { null }
                    )
                }
            ) {
                Text("Save profile")
            }
        }

        // --- Preferences (reminders + appearance) ----------------------------------------
        SettingsCard(title = "Preferences", initiallyExpanded = false) {
            SettingToggleRow(
                title = "Daily log reminder",
                checked = settings.dailyLogReminderEnabled,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        pendingEnableReminder = false
                        onSave(settings.copy(dailyLogReminderEnabled = false))
                        return@SettingToggleRow
                    }
                    if (
                        needsNotificationPermission &&
                        !notificationPermission.status.isGranted
                    ) {
                        pendingEnableReminder = true
                        notificationPermission.launchPermissionRequest()
                    } else {
                        onSave(settings.copy(dailyLogReminderEnabled = true))
                        if (!ReminderScheduler.canScheduleExactAlarms(context)) {
                            onPermissionDenied(
                                "Exact alarms not allowed — reminders may be delayed."
                            )
                        }
                    }
                },
                hintTitle = "Daily log reminder",
                hint = "Local notification once a day (no Google Play Services). " +
                    "Default time is 8:00 PM."
            )
            if (settings.dailyLogReminderEnabled) {
                val hour12 = settings.dailyLogReminderHour % 12
                val displayHour = if (hour12 == 0) 12 else hour12
                val amPm = if (settings.dailyLogReminderHour < 12) "AM" else "PM"
                val timeLabel = String.format(
                    Locale.getDefault(),
                    "%d:%02d %s",
                    displayHour,
                    settings.dailyLogReminderMinute,
                    amPm
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showReminderTimePicker = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reminder time", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            timeLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!ReminderScheduler.canScheduleExactAlarms(context)) {
                    Text(
                        text = "Exact alarms are off for FitBuddy. Reminders may be delayed " +
                            "until you allow Alarms & reminders in system settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(
                        onClick = {
                            awaitingExactAlarmSettings = true
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            } else {
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    ) { Text("Allow exact alarms") }
                }
            }
            SettingToggleRow(
                title = "Material You",
                checked = settings.dynamicColor,
                onCheckedChange = onDynamicColorChange,
                hintTitle = "Material You",
                hint = "Use wallpaper-based dynamic colors (Android 12+)."
            )
        }

        if (showReminderTimePicker) {
            val pickerState = rememberTimePickerState(
                initialHour = settings.dailyLogReminderHour,
                initialMinute = settings.dailyLogReminderMinute,
                is24Hour = false
            )
            AlertDialog(
                onDismissRequest = { showReminderTimePicker = false },
                title = { Text("Reminder time") },
                text = { TimePicker(state = pickerState) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onSave(
                                settings.copy(
                                    dailyLogReminderEnabled = true,
                                    dailyLogReminderHour = pickerState.hour,
                                    dailyLogReminderMinute = pickerState.minute
                                )
                            )
                            showReminderTimePicker = false
                        }
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showReminderTimePicker = false }) { Text("Cancel") }
                }
            )
        }

        // --- Updates & support (updates + crash reports) ---------------------------------
        SettingsCard(title = "Updates & support", initiallyExpanded = false) {
            Text(
                text = "Installed ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (BuildConfig.IS_FDROID) {
                val uriHandler = LocalUriHandler.current
                Text(
                    text = "Updates are handled by F-Droid.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "To get in-app updates, install from GitHub releases",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://github.com/anantdark/FitBuddy/releases")
                    }
                )
            } else {
                SettingToggleRow(
                    title = "Check for updates automatically",
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
            SettingToggleRow(
                title = "Send crash reports",
                checked = settings.crashReportingEnabled,
                onCheckedChange = onCrashReportingChange,
                hintTitle = "Crash reports",
                hint = "Anonymous stack traces help fix bugs. No meals, photos, or API keys. " +
                    "When on, the app may send one anonymous daily heartbeat " +
                    "(Cron, Metrics, and Logs — not Issues). Turn off anytime. " +
                    "Your Support ID (under Backup) identifies reports without personal data."
            )
        }

        // --- Backup ----------------------------------------------------------------------
        SettingsCard(
            title = "Backup",
            initiallyExpanded = false,
            hintTitle = "Backup",
            hint = "Export a JSON file anytime. Cloud uploads use your Support ID as the " +
                "document key — keep that ID safe to restore after reinstalling. Restore from " +
                "cloud or local file is offered during onboarding (import also in Developer tools)."
        ) {
            Text(
                text = "Support ID",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Share this if you report a bug, and keep it safe for cloud restore.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = settings.supportId.ifBlank { "(generating…)" },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val id = settings.supportId
                        if (id.isNotBlank()) {
                            clipboardManager.setText(AnnotatedString(id))
                            onSupportIdCopied()
                        }
                    },
                    enabled = settings.supportId.isNotBlank()
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy support ID")
                }
            }

            Text(
                text = "Local backups",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Save everything to a JSON file on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.FileUpload, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Export")
            }

            Text(
                text = "Cloud",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (!cloudVaultAvailable) {
                Text(
                    text = "Cloud backup is not available in this build.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                SettingToggleRow(
                    title = "Enable cloud backup",
                    checked = settings.cloudBackupEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            onPrepareCloudBackupEnable()
                            showCloudBackupEnableConfirm = true
                        } else {
                            onCloudBackupEnabledChange(false)
                        }
                    },
                    hintTitle = "Enable cloud backup",
                    hint = "When on, you can upload manually or automatically. Uploads use your " +
                        "Support ID as the cloud document key."
                )
                SettingToggleRow(
                    title = "Auto-upload on startup",
                    checked = settings.cloudAutoUploadEnabled,
                    onCheckedChange = onCloudAutoUploadChange,
                    enabled = settings.cloudBackupEnabled,
                    hintTitle = "Auto-upload",
                    hint = "When enabled, FitBuddy uploads on app start if the last upload was " +
                        "more than 12 hours ago. Manual Upload now always runs immediately."
                )
                val statusText = when {
                    !settings.cloudBackupEnabled -> "Cloud backup off"
                    settings.mongoLastUploadAt <= 0L -> "Enabled — no upload yet"
                    settings.mongoLastUploadOk ->
                        "Last upload OK · ${formatMongoUploadTime(settings.mongoLastUploadAt)}"
                    else ->
                        "Last upload failed · ${formatMongoUploadTime(settings.mongoLastUploadAt)}" +
                            settings.mongoLastError.takeIf { it.isNotBlank() }?.let { "\n$it" }
                                .orEmpty()
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (mongoBackupBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                OutlinedButton(
                    onClick = onMongoUpload,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = settings.cloudBackupEnabled && !mongoBackupBusy
                ) {
                    Text("Upload now")
                }
                OutlinedButton(
                    onClick = { showChangeCloudPasswordDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = settings.cloudBackupEnabled && !mongoBackupBusy
                ) {
                    Text("Change cloud backup password")
                }
            }
        }

        // --- About -----------------------------------------------------------------------
        SettingsCard(
            title = "About",
            collapsible = false,
            hintTitle = "About FitBuddy",
            hint = "AI-powered health tracker optimised for North Indian diets and lifestyles. " +
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
            AboutRow(
                label = "Package",
                value = BuildConfig.APPLICATION_ID,
                onValueClick = {
                    packageTapCount++
                    when {
                        packageTapCount >= DEVELOPER_UNLOCK_TAPS -> {
                            packageTapCount = 0
                            onDeveloperUnlockHintDismiss()
                            onDeveloperModeToggled(!developerUnlocked)
                        }
                        packageTapCount >= DEVELOPER_HINT_START -> {
                            onDeveloperUnlockHint(DEVELOPER_UNLOCK_TAPS - packageTapCount)
                        }
                    }
                }
            )
            AboutRow(
                label = "Created by",
                valueContent = {
                    val onAnantClick = {
                        if (settings.easterEggDiscovered) {
                            onAnantTapWhenUnlocked()
                        } else {
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
                    }
                    // Hue cycle stays on permanently (not gated by unlock).
                    RainbowCreditBadge(
                        name = "Anant",
                        onClick = onAnantClick
                    )
                }
            )
            AboutLinkRow("GitHub", "github.com/anantdark", "https://github.com/anantdark")
            AboutLinkRow(
                "Docs",
                "anantdark.github.io/FitBuddy",
                "https://anantdark.github.io/FitBuddy/"
            )
        }

        if (developerUnlocked) {
            SettingsCard(title = "Developer", initiallyExpanded = false) {
                Text(
                    text = "Niche debug / experimental tools. Tap Package 31 times again to hide.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Local backup (debug)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Import a BackupData JSON file. Replaces all local data. " +
                        "Export is under Backup → Local backups.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Import")
                }
                OutlinedButton(
                    onClick = onRegenerateSupportId,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generate new Support ID")
                }
                Text(
                    text = "Creates a new ID for crash reports and future cloud uploads. " +
                        "Existing cloud docs stay under the old ID.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Cloud backup (debug)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Atlas db/collection overrides and restore by Support ID. " +
                        "Defaults: db ${AppSettings.DEFAULT_MONGO_DB_NAME}, " +
                        "collection ${AppSettings.DEFAULT_MONGO_COLLECTION}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                var mongoDbNameDraft by remember(settings.mongoDbName) {
                    mutableStateOf(
                        settings.mongoDbName.ifBlank { AppSettings.DEFAULT_MONGO_DB_NAME }
                    )
                }
                var mongoCollectionDraft by remember(settings.mongoCollectionName) {
                    mutableStateOf(
                        settings.mongoCollectionName.ifBlank {
                            AppSettings.DEFAULT_MONGO_COLLECTION
                        }
                    )
                }
                OutlinedTextField(
                    value = mongoDbNameDraft,
                    onValueChange = { mongoDbNameDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Database name") },
                    singleLine = true,
                    placeholder = { Text(AppSettings.DEFAULT_MONGO_DB_NAME) }
                )
                OutlinedTextField(
                    value = mongoCollectionDraft,
                    onValueChange = { mongoCollectionDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Collection name") },
                    singleLine = true,
                    placeholder = { Text(AppSettings.DEFAULT_MONGO_COLLECTION) }
                )
                OutlinedButton(
                    onClick = {
                        onSaveQuiet(
                            settings.copy(
                                mongoDbName = mongoDbNameDraft.trim()
                                    .ifBlank { AppSettings.DEFAULT_MONGO_DB_NAME },
                                mongoCollectionName = mongoCollectionDraft.trim()
                                    .ifBlank { AppSettings.DEFAULT_MONGO_COLLECTION }
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Atlas db / collection")
                }
                if (mongoBackupBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                OutlinedButton(
                    onClick = {
                        mongoRestoreSupportIdDraft = settings.supportId
                        showMongoRestoreConfirm = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = cloudVaultAvailable && !mongoBackupBusy
                ) {
                    Text("Download & restore from cloud")
                }

                SettingToggleRow(
                    title = "Force offline AI simulator",
                    checked = settings.forceOfflineAiSimulator,
                    onCheckedChange = {
                        onSave(settings.copy(forceOfflineAiSimulator = it))
                    },
                    hintTitle = "Force offline AI",
                    hint = "Bypass live API and use the bundled text simulator " +
                        "(photos still need a real provider)."
                )
                SettingToggleRow(
                    title = "Show raw AI JSON",
                    checked = settings.showRawAiJson,
                    onCheckedChange = {
                        onSave(settings.copy(showRawAiJson = it))
                    },
                    hintTitle = "Raw AI JSON",
                    hint = "After each analysis, show the last raw JSON response."
                )
                SettingToggleRow(
                    title = "Strict clarification",
                    checked = settings.strictClarification,
                    onCheckedChange = {
                        onSave(settings.copy(strictClarification = it))
                    },
                    hintTitle = "Strict clarification",
                    hint = "Experimental: prefer asking for portions when counts are ambiguous."
                )
                SettingToggleRow(
                    title = "Verbose HTTP logging",
                    checked = settings.verboseHttpLogging,
                    onCheckedChange = {
                        onSave(settings.copy(verboseHttpLogging = it))
                    },
                    hintTitle = "Verbose HTTP",
                    hint = "Log full OkHttp request/response bodies (including on release builds). " +
                        "May include API keys in logcat — keep off unless debugging."
                )
                OutlinedButton(
                    onClick = onClearModelCooldowns,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear model cooldowns")
                }
                OutlinedButton(
                    onClick = onShowTestUpdatePrompt,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Show test update prompt")
                }
                Text(
                    text = "Opens the update dialog with fake release info so you can " +
                        "test backup-before-update. Download will fail on purpose.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        if (
                            needsNotificationPermission &&
                            !notificationPermission.status.isGranted
                        ) {
                            pendingTestNotification = true
                            notificationPermission.launchPermissionRequest()
                            return@OutlinedButton
                        }
                        val ok = ReminderReceiver.postReminderNotification(context, isTest = true)
                        onTestNotificationSent(ok)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send test notification")
                }
                OutlinedButton(
                    onClick = {
                        throw RuntimeException("FitBuddy Sentry test crash")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Force test crash")
                }

            }
        }

        CraftedWithLoveCredit(
            onHeartDoubleTap = {
                confettiKey += 1
                onHeartDoubleTapHeartbeat()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp)
        )

        if (showCloudBackupEnableConfirm) {
            EnableCloudBackupDialog(
                supportId = settings.supportId,
                busy = mongoBackupBusy,
                onCopySupportId = {
                    val id = settings.supportId
                    if (id.isNotBlank()) {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(id))
                        onSupportIdCopied()
                    }
                },
                onConfirm = { password ->
                    showCloudBackupEnableConfirm = false
                    onEnableCloudBackup(password)
                },
                onDismiss = { showCloudBackupEnableConfirm = false }
            )
        }

        if (showMongoRestoreConfirm) {
            AlertDialog(
                onDismissRequest = { showMongoRestoreConfirm = false },
                title = { Text("Restore from cloud?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Enter the Support ID of the backup to restore. " +
                                "This replaces all local FitBuddy data (same as file import)."
                        )
                        OutlinedTextField(
                            value = mongoRestoreSupportIdDraft,
                            onValueChange = { mongoRestoreSupportIdDraft = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Support ID") },
                            singleLine = true,
                            placeholder = { Text("xxxxxxxx-xxxx-…") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showMongoRestoreConfirm = false
                            onMongoDownload(mongoRestoreSupportIdDraft.trim())
                        },
                        enabled = mongoRestoreSupportIdDraft.isNotBlank()
                    ) {
                        Text("Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMongoRestoreConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showChangeCloudPasswordDialog) {
            ChangeCloudPasswordDialog(
                busy = mongoBackupBusy,
                onConfirm = { password ->
                    onChangeCloudPassword(password)
                    showChangeCloudPasswordDialog = false
                },
                onDismiss = { showChangeCloudPasswordDialog = false }
            )
        }

    }

        if (showConfetti) {
            key(confettiKey) {
                ConfettiOverlay(
                    modifier = Modifier.fillMaxSize(),
                    durationMillis = 4_000,
                    grand = true
                )
            }
        }
    }
}

private fun formatMongoUploadTime(epochMs: Long): String {
    if (epochMs <= 0L) return "never"
    return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        .format(java.util.Date(epochMs))
}

/** Update / download dialogs — shown from [MainScreen] so startup checks work outside Settings. */
@Composable
fun UpdatePromptDialogs(
    updateState: UpdateUiState,
    cloudBackupEnabled: Boolean,
    onDismissUpdatePrompt: () -> Unit,
    onExportBackupAndUpdate: (downloadUrl: String) -> Unit,
    onSkipBackupAndUpdate: (downloadUrl: String) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    updateState.updateInfo?.let { info ->
        val highlights = remember(info.releaseNotes) { releaseNoteHighlights(info.releaseNotes) }
        val busy = updateState.isDownloading ||
            updateState.isExportingBackup ||
            updateState.isAwaitingBackupFilePick
        var skipCountdownSec by remember(info.versionCode, info.downloadUrl) { mutableIntStateOf(5) }
        LaunchedEffect(info.versionCode, info.downloadUrl) {
            skipCountdownSec = 5
            while (skipCountdownSec > 0) {
                delay(1_000)
                skipCountdownSec--
            }
        }
        val skipEnabled = !busy && skipCountdownSec == 0
        AlertDialog(
            onDismissRequest = { if (!busy) onDismissUpdatePrompt() },
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
                    Text(
                        text = if (cloudBackupEnabled) {
                            "Back up your data to the cloud before updating."
                        } else {
                            "Export a local backup before updating."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    updateState.backupStatusMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (updateState.backupStatusIsError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                    if (updateState.isExportingBackup) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onExportBackupAndUpdate(info.downloadUrl) },
                            enabled = !busy && !updateState.backupCompleted,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Export backup & update") }
                        OutlinedButton(
                            onClick = { onSkipBackupAndUpdate(info.downloadUrl) },
                            enabled = skipEnabled,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (skipCountdownSec > 0) {
                                    "Skip backup & update ($skipCountdownSec)"
                                } else {
                                    "Skip backup & update"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
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
    onLoad: (AiProvider, String, Boolean, String, Boolean) -> Unit,
    baseUrl: String = "",
    showPaidModels: Boolean = false,
    fallbackOptions: List<ModelOption> = emptyList(),
    hintTitle: String? = null,
    hint: String? = null
) {
    // Listing models is a free/cheap metadata call (unlike the Refresh reachability probe,
    // which sends real chat completions) — auto-fetch on input change regardless of platform.
    LaunchedEffect(provider, apiKey, baseUrl, showPaidModels) {
        onLoad(provider, apiKey, false, baseUrl, showPaidModels)
    }

    var expanded by remember { mutableStateOf(false) }
    // Fall back to curated options (e.g. OpenAI defaults) so the list is never empty before
    // the live fetch resolves (or if it fails).
    val options = modelsState.options.ifEmpty { fallbackOptions }
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
            modelsState.loaded -> "No reachable $noun models; type an id above"
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
            onClick = {
                onLoad(provider, apiKey, true, baseUrl, showPaidModels)
            },
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
    hint: String? = null,
    enabled: Boolean = true
) {
    val dismiss = rememberDismissKeyboard()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
        )
        if (hint != null && hintTitle != null) {
            HintIconButton(title = hintTitle, message = hint)
        }
        Switch(
            checked = checked,
            onCheckedChange = { dismiss(); onCheckedChange(it) },
            enabled = enabled
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSexDropdown(
    selectedValue: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val display = PROFILE_SEX_OPTIONS.firstOrNull { it.first == selectedValue }?.second
        ?: PROFILE_SEX_OPTIONS.first().second

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text("Sex") },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PROFILE_SEX_OPTIONS.forEach { (value, text) ->
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

private val rainbowAnimationSpec = infiniteRepeatable<Float>(
    animation = tween(durationMillis = 2800, easing = LinearEasing),
    repeatMode = RepeatMode.Restart
)

@Composable
private fun rememberCyclingHue(): Float {
    val transition = rememberInfiniteTransition(label = "createdByRainbow")
    val hue by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = rainbowAnimationSpec,
        label = "hue"
    )
    return hue
}

private fun hsvRainbow(hue: Float): Color =
    Color.hsv(hue = ((hue % 360f) + 360f) % 360f, saturation = 0.85f, value = 0.95f)

/** Rainbow text where each letter is hue-offset so color travels across the word. */
private fun cyclingRainbowText(text: String, hue: Float): AnnotatedString {
    val stepDegrees = 360f / text.length.coerceAtLeast(1)
    return buildAnnotatedString {
        text.forEachIndexed { index, char ->
            withStyle(SpanStyle(color = hsvRainbow(hue + index * stepDegrees))) {
                append(char)
            }
        }
    }
}

/** Sweeping rainbow border brush — hues travel around the box like the letter wave. */
private fun rainbowBorderBrush(hue: Float): Brush {
    val stops = 8
    val colors = List(stops + 1) { i ->
        hsvRainbow(hue + i * (360f / stops))
    }
    return Brush.sweepGradient(colors = colors)
}

/** Name + party parrot inside a hue-cycling rainbow border. */
@Composable
private fun RainbowCreditBadge(name: String, onClick: () -> Unit) {
    val hue = rememberCyclingHue()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .border(
                width = 1.5.dp,
                brush = rainbowBorderBrush(hue),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = cyclingRainbowText(name, hue),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(4.dp))
        PartyParrot()
    }
}

/** Classic animated party parrot (cultofthepartyparrot.com). */
@Composable
private fun PartyParrot(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                contentDescription = "Party parrot"
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                val drawable = ImageDecoder.decodeDrawable(
                    ImageDecoder.createSource(context.resources, R.raw.party_parrot)
                )
                setImageDrawable(drawable)
                if (drawable is Animatable) drawable.start()
            }
        },
        modifier = modifier.size(20.dp)
    )
}

@Composable
private fun AboutRow(
    label: String,
    value: String,
    onValueClick: (() -> Unit)? = null
) {
    AboutRow(
        label = label,
        valueContent = {
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
    )
}

@Composable
private fun AboutRow(
    label: String,
    valueContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        valueContent()
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

/**
 * Shown when the user turns on cloud backup (Requirement 12). Explains that backups are encrypted
 * on-device before upload (12.3), lets the user copy their Support ID, and offers an optional
 * password (blank → Support ID default, 12.4). Non-blank passwords are validated 8–128 (12.5).
 * Cancelling leaves cloud backup off and performs no upload (12.6).
 */
@Composable
private fun EnableCloudBackupDialog(
    supportId: String,
    busy: Boolean,
    onCopySupportId: () -> Unit,
    onConfirm: (CharArray?) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val isBlank = password.isEmpty() || password.all { it.isWhitespace() }
    val passwordsMatch = password == confirmPassword
    val lengthValid = isBlank || password.length in 8..128

    val validationError = when {
        !lengthValid && password.length < 8 -> "Password must be at least 8 characters"
        !lengthValid -> "Password must be 128 characters or fewer"
        !isBlank && confirmPassword.isNotEmpty() && !passwordsMatch -> "Passwords do not match"
        else -> null
    }
    val canConfirm = !busy && (isBlank || (lengthValid && passwordsMatch))

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Enable cloud backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Your backup is encrypted on this device before it's uploaded — the cloud only " +
                        "ever stores encrypted data."
                )
                Text(
                    "Cloud backups are keyed by your Support ID. Copy it now and store it safely — " +
                        "you'll need it to restore after reinstalling."
                )
                Text(
                    text = supportId.ifBlank { "(generating…)" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Optionally set a password. Leave blank to use default (Support ID) protection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (optional)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = validationError != null && confirmPassword.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isBlank) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (validationError != null) {
                    Text(
                        text = validationError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCopySupportId()
                    onConfirm(if (isBlank) null else password.toCharArray())
                },
                enabled = canConfirm
            ) { Text("Copy ID & enable") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
        }
    )
}

@Composable
private fun ChangeCloudPasswordDialog(
    busy: Boolean,
    onConfirm: (CharArray?) -> Unit,
    onDismiss: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val isBlank = newPassword.isEmpty() || newPassword.all { it.isWhitespace() }
    val passwordsMatch = newPassword == confirmPassword
    val lengthValid = isBlank || newPassword.length in 8..128

    // Derive validation error
    val validationError = when {
        !lengthValid && newPassword.length < 8 -> "Password must be at least 8 characters"
        !lengthValid -> "Password must be 128 characters or fewer"
        !isBlank && !passwordsMatch && confirmPassword.isNotEmpty() -> "Passwords do not match"
        else -> null
    }
    val canConfirm = !busy && (isBlank || (lengthValid && passwordsMatch))

    AlertDialog(
        onDismissRequest = {
            if (!busy) onDismiss()
        },
        title = { Text("Change cloud backup password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Set a new password to protect your cloud backup. " +
                        "Leave blank to use default (Support ID) protection.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = validationError != null && confirmPassword.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = !isBlank && confirmPassword.isNotEmpty() && !passwordsMatch,
                    modifier = Modifier.fillMaxWidth()
                )
                if (validationError != null) {
                    Text(
                        text = validationError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val password = if (isBlank) null else newPassword.toCharArray()
                    onConfirm(password)
                },
                enabled = canConfirm
            ) { Text("Change") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !busy
            ) { Text("Cancel") }
        }
    )
}

/**
 * Provider selector rendered as one connected grid ([columns] per row) with a single rounded
 * outer border and hairline dividers between cells — no gaps between rows. Selected cell uses the
 * segmented-button look (secondary container + check). Shared by the Settings and Onboarding
 * selectors (same package). Keep a separate control (with its own gap) for Local/Cloud.
 */
@Composable
internal fun <T> ProviderSelectorGrid(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 2
) {
    val shape = RoundedCornerShape(20.dp)
    val outline = MaterialTheme.colorScheme.outline
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, outline, shape)
    ) {
        options.chunked(columns).forEachIndexed { rowIndex, rowOptions ->
            if (rowIndex > 0) {
                HorizontalDivider(thickness = 1.dp, color = outline)
            }
            Row(modifier = Modifier.height(48.dp)) {
                rowOptions.forEachIndexed { colIndex, (value, label) ->
                    if (colIndex > 0) {
                        VerticalDivider(thickness = 1.dp, color = outline)
                    }
                    val isSelected = value == selected
                    val contentColor =
                        if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                else Color.Transparent
                            )
                            .clickable { onSelect(value) }
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = label,
                            color = contentColor,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
