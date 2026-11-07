package com.anant.fitbuddy.ui.screens
import androidx.compose.ui.res.stringResource

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
import com.anant.fitbuddy.ui.theme.appShape
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
import android.os.SystemClock
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
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.util.SystemToast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.data.remote.UpdateChannel
import com.anant.fitbuddy.util.DiagnosticLogger
import com.anant.fitbuddy.util.ReleaseNotes
import com.anant.fitbuddy.data.database.UserProfile
import com.anant.fitbuddy.data.model.ModelOption
import com.anant.fitbuddy.data.model.OpenAiCatalog
import com.anant.fitbuddy.data.region.AppRegion
import com.anant.fitbuddy.data.settings.AiProvider
import com.anant.fitbuddy.data.settings.AppColorTheme
import com.anant.fitbuddy.data.settings.AppComponentStyle
import com.anant.fitbuddy.data.settings.AppFontOption
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.ui.loading.LoadingAnimationRegistry
import com.anant.fitbuddy.ui.loading.LoadingAnimationSlot
import com.anant.fitbuddy.data.settings.isPlausibleModelIdFor
import com.anant.fitbuddy.data.settings.parseApiKeys
import com.anant.fitbuddy.reminders.ReminderReceiver
import com.anant.fitbuddy.reminders.ReminderScheduler
import com.anant.fitbuddy.ui.components.AppSwitch
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.activity.compose.rememberLauncherForActivityResult
import com.anant.fitbuddy.FitBuddyApp
import com.anant.fitbuddy.data.healthconnect.HealthConnectMetricAvailability
import com.anant.fitbuddy.data.healthconnect.HealthConnectMetricPrefs
import com.anant.fitbuddy.data.healthconnect.sanitized
import com.anant.fitbuddy.data.healthconnect.HealthConnectSdkStatus
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private const val EASTER_EGG_TAP_TARGET = 31
private const val EASTER_EGG_HINT_START = 25
private const val VERSION_RESET_TAPS = 8
private const val DEVELOPER_UNLOCK_TAPS = 31
// Start showing the "N taps left" hint only when 5 or fewer taps remain.
private const val DEVELOPER_HINT_START = DEVELOPER_UNLOCK_TAPS - 5

private val PROFILE_SEX_OPTION_RES = listOf(
    "" to R.string.settings_not_set,
    "MALE" to R.string.onboarding_male,
    "FEMALE" to R.string.onboarding_female
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
    onRegionChange: (AppRegion) -> Unit = {},
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
    onHealthConnectEnabledChange: (Boolean) -> Unit = {},
    onHealthConnectAutoSyncChange: (Boolean) -> Unit = {},
    onHealthConnectMetricsChange: (HealthConnectMetricPrefs) -> Unit = {},
    onHealthConnectSync: () -> Unit = {},
    healthConnectSyncBusy: Boolean = false,
    onStartDiagnosticLogging: () -> Unit = {},
    onStopAndExportDiagnosticLog: () -> Unit = {},
    onSupportIdCopied: () -> Unit = {},
    onDeveloperUnlockHint: (remainingTaps: Int) -> Unit = {},
    onDeveloperUnlockHintDismiss: () -> Unit = {},
    onDeveloperModeToggled: (unlocked: Boolean) -> Unit = {},
    onClearModelCooldowns: () -> Unit = {},
    onApplyBuiltInModelDefaults: () -> Unit = {},
    onShowTestUpdatePrompt: () -> Unit = {},
    onShowTestDonateNotification: () -> Unit = {},
    onShowTestDonateReminderDialog: () -> Unit = {},
    onShowTestNewDonorsThankYou: () -> Unit = {},
    onShowTestDonorGallery: () -> Unit = {},
    onShowTestDonationDialog: () -> Unit = {},
    lastSeenDonorDonationDate: String? = null,
    onRefreshLastSeenDonorDonationDate: () -> Unit = {},
    onClearLastSeenDonorDonationDate: () -> Unit = {},
    onSetLastSeenDonorDonationDateToLatest: () -> Unit = {},
    onSetLastSeenDonorDonationDate: (isoDate: String) -> Unit = {},
    onResetDonateReminderTimers: () -> Unit = {},
    onRestartOnboarding: () -> Unit = {},
    onTestNotificationSent: (ok: Boolean) -> Unit = {},
    onPermissionDenied: (message: String) -> Unit = {},
    mongoBackupBusy: Boolean = false,
    onCloudBackupEnabledChange: (Boolean) -> Unit = {},
    onPrepareCloudBackupEnable: () -> Unit = {},
    onEnableCloudBackup: (CharArray?) -> Unit = {},
    onCloudAutoUploadChange: (Boolean) -> Unit = {},
    onMongoUpload: () -> Unit = {},
    onForceNewCloudChunk: () -> Unit = {},
    onMongoDownload: (supportId: String) -> Unit = {},
    onChangeCloudPassword: (CharArray?) -> Unit = {},
    onRegenerateSupportId: () -> Unit = {},
    /** Settings-only: ♥ double-tap side effect (crash reporting on). */
    onHeartDoubleTapHeartbeat: () -> Unit = {},
    /** Themes section unlock — true when this install's Support ID is on the donors list. */
    isSupporter: Boolean = false,
    onDonate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val healthConnectManager = remember(context) {
        (context.applicationContext as FitBuddyApp).healthConnectManager
    }
    var healthConnectSdkStatus by remember {
        mutableStateOf(healthConnectManager.sdkStatus())
    }
    /** Desired metrics after the permission sheet; aligned to grants on result. */
    var pendingHealthConnectMetrics by remember {
        mutableStateOf<HealthConnectMetricPrefs?>(null)
    }
    var pendingEnableHealthConnect by remember { mutableStateOf(false) }
    var pendingSyncAfterPermission by remember { mutableStateOf(false) }
    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = healthConnectManager.permissionContract()
    ) { resultGranted ->
        val desired = pendingHealthConnectMetrics ?: settings.healthConnectMetrics
        pendingHealthConnectMetrics = null
        val wantEnable = pendingEnableHealthConnect
        pendingEnableHealthConnect = false
        val runSync = pendingSyncAfterPermission
        pendingSyncAfterPermission = false
        coroutineScope.launch {
            val fromApi = healthConnectManager.grantedPermissions()
            val previous = settings.healthConnectMetrics
            val aligned = healthConnectManager.applyPermissionResult(
                previous = previous,
                desired = desired,
                resultGranted = resultGranted,
                allGranted = fromApi
            )
            onHealthConnectMetricsChange(aligned)
            val anyOn = aligned.anyImport() || aligned.anyExport()
            when {
                wantEnable -> {
                    if (anyOn) {
                        onHealthConnectEnabledChange(true)
                        if (runSync) onHealthConnectSync()
                    } else {
                        onHealthConnectEnabledChange(false)
                        onPermissionDenied(
                            context.getString(
                                R.string.settings_health_connect_permissions_denied
                            )
                        )
                    }
                }
                runSync -> {
                    if (anyOn) onHealthConnectSync()
                    else onPermissionDenied(
                        context.getString(R.string.settings_health_connect_permissions_denied)
                    )
                }
                // Single-metric toggle: stay silent; denied metrics simply remain off.
            }
        }
    }

    // When HC is first turned on, drop any toggles that lack a grant. Do not key on
    // metrics themselves — getGrantedPermissions() can briefly return empty right after
    // the permission sheet and would otherwise snap granted toggles back off.
    LaunchedEffect(settings.healthConnectEnabled) {
        if (!settings.healthConnectEnabled) return@LaunchedEffect
        if (healthConnectManager.sdkStatus() != HealthConnectSdkStatus.Available) return@LaunchedEffect
        val granted = healthConnectManager.grantedPermissions()
        if (granted.isEmpty()) return@LaunchedEffect
        val current = settings.healthConnectMetrics
        val aligned = healthConnectManager.alignToGranted(current, granted)
        if (aligned != current) onHealthConnectMetricsChange(aligned)
    }
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
    var customUrl by remember(settings) { mutableStateOf(settings.customBaseUrl) }
    var customModel by remember(settings) { mutableStateOf(settings.customModel) }
    var customTextModel by remember(settings) { mutableStateOf(settings.customTextModel) }
    var customKeys by remember(settings) {
        mutableStateOf(settings.keysFor(AiProvider.CUSTOM))
    }
    var aiAutoFailover by remember(settings, provider) { mutableStateOf(settings.autoFailoverFor(provider)) }
    var showPaidModels by remember(settings, provider) { mutableStateOf(settings.showPaidFor(provider)) }

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
    LaunchedEffect(developerUnlocked) {
        if (developerUnlocked) onRefreshLastSeenDonorDonationDate()
    }
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
                    onPermissionDenied(context.getString(R.string.settings_exact_alarms_not_allowed_reminders_may_delayed))
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
                onPermissionDenied(context.getString(R.string.settings_notifications_not_allowed_reminder_turned_off))
            }
            if (pendingTestNotification) {
                pendingTestNotification = false
                onPermissionDenied(context.getString(R.string.settings_notifications_not_allowed))
            }
        }
    }

    val openRouterKey = openRouterKeys.firstOrNull().orEmpty()
        .ifBlank { settings.openRouterOAuthKey }
    val geminiKey = geminiKeys.firstOrNull().orEmpty()
    val ollamaApiKey = ollamaKeys.firstOrNull().orEmpty()
    val openAiKey = openAiKeys.firstOrNull().orEmpty()
    val customApiKey = customKeys.firstOrNull().orEmpty()

    // Prefill official OpenAI URL when opening OpenAI-compatible with an empty base URL.
    LaunchedEffect(provider) {
        if (provider == AiProvider.CUSTOM && customUrl.isBlank()) {
            customUrl = AppSettings.DEFAULT_CUSTOM_BASE_URL
        }
    }

    // Paid catalogs are manual pick only — keep Auto off (and the switch greyed out) for the
    // whole time Show paid models is on.
    LaunchedEffect(provider, showPaidModels) {
        if (showPaidModels && aiAutoFailover) {
            aiAutoFailover = false
            onSaveQuiet(settings.withAutoFailover(provider, false))
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
            onPermissionDenied(context.getString(R.string.settings_notifications_not_allowed_reminder_turned_off))
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
                    onPermissionDenied(context.getString(R.string.settings_exact_alarms_not_allowed_reminders_may_delayed))
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
        SettingsCard(title = stringResource(R.string.settings_ai_provider), initiallyExpanded = false) {
            SettingToggleRow(
                title = stringResource(R.string.settings_auto_failover),
                checked = aiAutoFailover,
                enabled = !showPaidModels,
                onCheckedChange = { enabled ->
                    aiAutoFailover = enabled
                    // Persist immediately; leave other draft AI fields for Save AI Settings.
                    onSaveQuiet(settings.withAutoFailover(provider, enabled))
                },
                hintTitle = stringResource(R.string.settings_auto_failover),
                hint = if (showPaidModels) {
                    stringResource(R.string.settings_unavailable_while_show_paid_models_pick_one)
                } else {
                    stringResource(R.string.settings_when_fitbuddy_tries_other_api_keys_then)
                }
            )

            if (provider == AiProvider.OPENROUTER || provider == AiProvider.GEMINI) {
                SettingToggleRow(
                    title = stringResource(R.string.settings_show_paid_models),
                    checked = showPaidModels,
                    enabled = true,
                    onCheckedChange = { enabled ->
                        showPaidModels = enabled
                        if (enabled) {
                            // Paid catalogs are manual pick only — turn Auto off for this provider.
                            aiAutoFailover = false
                            onSaveQuiet(
                                settings
                                    .withShowPaid(provider, true)
                                    .withAutoFailover(provider, false)
                            )
                        } else {
                            aiAutoFailover = true
                            onSaveQuiet(
                                settings
                                    .withShowPaid(provider, false)
                                    .withAutoFailover(provider, true)
                            )
                        }
                    },
                    hintTitle = stringResource(R.string.settings_paid_models),
                    hint = stringResource(R.string.settings_off_default_free_models_only_also_list)
                )
            }

            if (aiAutoFailover) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_photo, settings.activePhotoModelDisplay(context)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.settings_text, settings.activeTextModelDisplay(context)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HintIconButton(
                        title = stringResource(R.string.settings_active_models),
                        message = stringResource(R.string.settings_green_lines_show_the_models_last)
                    )
                }
            }

            val options = listOf(
                AiProvider.OPENROUTER to stringResource(R.string.settings_openrouter),
                AiProvider.GEMINI to stringResource(R.string.settings_gemini),
                AiProvider.OLLAMA to stringResource(R.string.settings_ollama),
                AiProvider.CUSTOM to stringResource(R.string.settings_openai_compatible)
            )
            ProviderSelectorGrid(
                options = options,
                selected = if (provider == AiProvider.OPENAI) AiProvider.CUSTOM else provider,
                onSelect = { selected ->
                    provider = selected
                    if (selected == AiProvider.CUSTOM && customUrl.isBlank()) {
                        customUrl = AppSettings.DEFAULT_CUSTOM_BASE_URL
                    }
                    if (selected == AiProvider.CUSTOM && customModel.isBlank()) {
                        customModel = AppSettings.DEFAULT_CUSTOM_MODEL
                    }
                }
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
                            label = stringResource(R.string.onboarding_api_keys),
                            keys = openRouterKeys,
                            onKeysChange = { openRouterKeys = it }
                        )
                    }
                    ModelDropdown(
                        label = if (showPaidModels) {
                            stringResource(R.string.settings_photo_model_vision)
                        } else {
                            stringResource(R.string.settings_photo_model_free_vision)
                        },
                        noun = if (showPaidModels) stringResource(R.string.settings_vision) else stringResource(R.string.settings_free_vision),
                        selectedModel = openRouterModel,
                        onModelChange = { openRouterModel = it },
                        modelsState = modelsState,
                        provider = AiProvider.OPENROUTER,
                        apiKey = openRouterKey,
                        onLoad = onLoadModels,
                        showPaidModels = showPaidModels
                    )
                    ModelDropdown(
                        label = if (showPaidModels) stringResource(R.string.settings_text_model) else stringResource(R.string.settings_text_model_free),
                        noun = if (showPaidModels) stringResource(R.string.settings_chat) else stringResource(R.string.settings_free),
                        selectedModel = openRouterTextModel,
                        onModelChange = { openRouterTextModel = it },
                        modelsState = textModelsState,
                        provider = AiProvider.OPENROUTER,
                        apiKey = openRouterKey,
                        onLoad = onLoadTextModels,
                        showPaidModels = showPaidModels,
                        hintTitle = stringResource(R.string.settings_text_model),
                        hint = stringResource(R.string.settings_used_for_typed_logs_and_recalculate)
                    )
                }

                AiProvider.GEMINI -> {
                    ApiKeyChipEditor(
                        label = stringResource(R.string.onboarding_api_keys),
                        keys = geminiKeys,
                        onKeysChange = { geminiKeys = it }
                    )
                    ModelDropdown(
                        label = if (showPaidModels) {
                            stringResource(R.string.settings_photo_model_vision)
                        } else {
                            stringResource(R.string.settings_photo_model_free_vision)
                        },
                        noun = if (showPaidModels) stringResource(R.string.settings_vision) else stringResource(R.string.settings_free_vision),
                        selectedModel = geminiModel,
                        onModelChange = { geminiModel = it },
                        modelsState = modelsState,
                        provider = AiProvider.GEMINI,
                        apiKey = geminiKey,
                        onLoad = onLoadModels,
                        showPaidModels = showPaidModels,
                        hintTitle = stringResource(R.string.settings_gemini),
                        hint = stringResource(R.string.settings_get_key_google_ai_studio_aistudio_google) +
                            (if (showPaidModels) {
                                stringResource(R.string.settings_free_flash_plus_paid_pro)
                            } else {
                                stringResource(R.string.settings_free_flash_models_only)
                            }) +
                            stringResource(R.string.settings_dropdown_order_follows_approved_failover_ladder)
                    )
                    ModelDropdown(
                        label = if (showPaidModels) {
                            stringResource(R.string.settings_text_model)
                        } else {
                            stringResource(R.string.settings_text_model_free)
                        },
                        noun = if (showPaidModels) stringResource(R.string.settings_chat) else stringResource(R.string.settings_free),
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
                    val modeOptions = listOf(false to stringResource(R.string.onboarding_local), true to stringResource(R.string.settings_cloud))
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
                            label = stringResource(R.string.onboarding_api_keys),
                            keys = ollamaKeys,
                            onKeysChange = { ollamaKeys = it }
                        )
                    } else {
                        SettingField(
                            label = stringResource(R.string.onboarding_server_url),
                            value = ollamaUrl,
                            onValueChange = { ollamaUrl = it },
                            keyboardType = KeyboardType.Uri,
                            trailingHintTitle = stringResource(R.string.onboarding_server_url),
                            trailingHint = stringResource(R.string.settings_reachable_phone_e_g_http_192_168) +
                                stringResource(R.string.settings_use_vision_model_llava_gemma_food_photos)
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
                        noun = stringResource(R.string.settings_vision),
                        selectedModel = ollamaModel,
                        onModelChange = { ollamaModel = it },
                        modelsState = modelsState,
                        provider = AiProvider.OLLAMA,
                        apiKey = ollamaListKey,
                        baseUrl = ollamaListUrl,
                        onLoad = onLoadModels,
                        showPaidModels = showPaidModels,
                        hintTitle = if (ollamaUseCloud) stringResource(R.string.settings_ollama_cloud) else stringResource(R.string.settings_ollama_local),
                        hint = if (ollamaUseCloud) {
                            stringResource(R.string.settings_cloud_uses_https_ollama_com_create_keys) +
                                stringResource(R.string.settings_gemma_models_preferred_leave_text_model_blank)
                        } else {
                            stringResource(R.string.settings_use_vision_model_llava_gemma_food_photos_2)
                        }
                    )
                    ModelDropdown(
                        label = "Text model",
                        noun = stringResource(R.string.settings_ollama),
                        selectedModel = ollamaTextModel,
                        onModelChange = { ollamaTextModel = it },
                        modelsState = textModelsState,
                        provider = AiProvider.OLLAMA,
                        apiKey = ollamaListKey,
                        baseUrl = ollamaListUrl,
                        onLoad = onLoadTextModels
                    )
                }

                AiProvider.CUSTOM, AiProvider.OPENAI -> {
                    Text(
                        text = stringResource(R.string.settings_works_with_openai_and_any_openai),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SettingField(
                        label = stringResource(R.string.onboarding_base_url),
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        keyboardType = KeyboardType.Uri,
                        trailingHintTitle = stringResource(R.string.onboarding_base_url),
                        trailingHint = stringResource(R.string.settings_official_openai_https_api_openai_com_local) +
                            stringResource(R.string.settings_server_try_http_192_168_x_x) +
                            stringResource(R.string.settings_openai_layer_enter_host_only_no_v1) +
                            stringResource(R.string.settings_base_v1_chat_completions_base_v1_models)
                    )
                    ApiKeyChipEditor(
                        label = if (AppSettings.isOfficialOpenAiBaseUrl(customUrl)) {
                            stringResource(R.string.onboarding_api_keys)
                        } else {
                            stringResource(R.string.settings_api_keys_optional_local_servers)
                        },
                        keys = customKeys,
                        onKeysChange = { customKeys = it }
                    )
                    val openAiFallbacks = AppSettings.isOfficialOpenAiBaseUrl(customUrl)
                    ModelDropdown(
                        label = "Photo model (vision)",
                        noun = stringResource(R.string.settings_vision),
                        selectedModel = customModel,
                        onModelChange = { customModel = it },
                        modelsState = modelsState,
                        provider = AiProvider.CUSTOM,
                        apiKey = customApiKey,
                        baseUrl = customUrl,
                        onLoad = onLoadModels,
                        showPaidModels = showPaidModels,
                        fallbackOptions = if (openAiFallbacks) OpenAiCatalog.VISION_MODELS else emptyList(),
                        hintTitle = stringResource(R.string.settings_openai_compatible),
                        hint = stringResource(R.string.settings_paste_openai_key_api_openai_com_paid)
                    )
                    ModelDropdown(
                        label = "Text model",
                        noun = stringResource(R.string.settings_chat),
                        selectedModel = customTextModel,
                        onModelChange = { customTextModel = it },
                        modelsState = textModelsState,
                        provider = AiProvider.CUSTOM,
                        apiKey = customApiKey,
                        baseUrl = customUrl,
                        onLoad = onLoadTextModels,
                        showPaidModels = showPaidModels,
                        fallbackOptions = if (openAiFallbacks) OpenAiCatalog.TEXT_MODELS else emptyList()
                    )
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onSave(
                        settings.copy(
                            provider = if (provider == AiProvider.OPENAI) AiProvider.CUSTOM else provider,
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
                            customBaseUrl = customUrl.trim(),
                            customModel = customModel.trim(),
                            customTextModel = customTextModel.trim(),
                            customApiKeys = customKeys,
                            customApiKey = customApiKey,
                            aiAutoFailoverByProvider = settings.aiAutoFailoverByProvider
                                + (provider to aiAutoFailover),
                            showPaidModelsByProvider = settings.showPaidModelsByProvider
                                + (provider to showPaidModels)
                        )
                    )
                }
            ) { Text(stringResource(R.string.settings_save_ai_settings)) }
        }

        // --- Profile (name + permanent body attrs) ---------------------------------------
        SettingsCard(title = stringResource(R.string.settings_profile), initiallyExpanded = false) {
            SettingField(
                label = "First name",
                value = profileFirstName,
                onValueChange = { profileFirstName = it }
            )
            SettingField(
                label = "Last name (optional)",
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
                (profileAge.toIntOrNull() ?: 0) in 18..120 &&
                (profileHeight.toDoubleOrNull() ?: 0.0) in 100.0..250.0
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
                Text(stringResource(R.string.settings_save_profile))
            }
        }

        // --- Themes (colors + fonts) — supporters only ------------------------------------
        var themesLockToastAt by remember { mutableLongStateOf(0L) }
        var showThemesSupportDialog by remember { mutableStateOf(false) }
        val themesUnlocked = isSupporter || settings.developerUnlockThemes
        SettingsCard(
            title = stringResource(R.string.settings_themes),
            titleBadge = {
                SupporterExclusiveBadge(muted = themesUnlocked)
            },
            initiallyExpanded = false,
            // Developer stringResource(R.string.settings_surface_lab) toggle unlocks for local testing; shipping UI stays supporter-gated.
            unlocked = themesUnlocked,
            onLockedExpandAttempt = {
                val now = SystemClock.elapsedRealtime()
                if (themesLockToastAt > 0L && now - themesLockToastAt < 5_000L) {
                    showThemesSupportDialog = true
                } else {
                    themesLockToastAt = now
                    SystemToast.show(context, context.getString(R.string.settings_available_for_supporters))
                }
            },
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_material_you),
                checked = settings.dynamicColor,
                onCheckedChange = onDynamicColorChange,
                hintTitle = stringResource(R.string.settings_material_you),
                hint = stringResource(R.string.settings_use_wallpaper_based_dynamic_colors_android_12)
            )
            if (!settings.dynamicColor) {
                var themeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = themeExpanded,
                    onExpandedChange = { themeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = stringResource(settings.colorTheme.displayNameRes()),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.settings_color_theme)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = themeExpanded,
                        onDismissRequest = { themeExpanded = false }
                    ) {
                        AppColorTheme.entries.forEach { theme ->
                            DropdownMenuItem(
                                text = { Text(stringResource(theme.displayNameRes())) },
                                onClick = {
                                    onSaveQuiet(settings.copy(colorTheme = theme))
                                    themeExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.settings_fitbuddy_follows_system_light_dark_catppuccin),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
            Text(
                text = stringResource(R.string.settings_components),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            var componentExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = componentExpanded,
                onExpandedChange = { componentExpanded = it }
            ) {
                OutlinedTextField(
                    value = stringResource(settings.componentStyle.displayNameRes()),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_component_style)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = componentExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = componentExpanded,
                    onDismissRequest = { componentExpanded = false }
                ) {
                    AppComponentStyle.entries.forEach { style ->
                        DropdownMenuItem(
                            text = { Text(stringResource(style.displayNameRes())) },
                            onClick = {
                                onSaveQuiet(settings.copy(componentStyle = style))
                                componentExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.settings_material_keeps_soft_rounded_corners_mako),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            Text(
                text = stringResource(R.string.settings_font),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            var fontExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = fontExpanded,
                onExpandedChange = { fontExpanded = it }
            ) {
                OutlinedTextField(
                    value = stringResource(settings.fontOption.displayNameRes()),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_app_font)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = fontExpanded,
                    onDismissRequest = { fontExpanded = false }
                ) {
                    AppFontOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.displayNameRes())) },
                            onClick = {
                                onSaveQuiet(settings.copy(fontOption = option))
                                fontExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.settings_applies_app_wide_the_mako_font),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showThemesSupportDialog) {
            AlertDialog(
                onDismissRequest = { showThemesSupportDialog = false },
                title = { Text(stringResource(R.string.settings_themes)) },
                text = {
                    Text(
                        stringResource(R.string.settings_themes_are_available_exclusively_for_supporters),
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showThemesSupportDialog = false
                            onDonate()
                        },
                    ) {
                        Text(stringResource(R.string.settings_donate))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showThemesSupportDialog = false }) {
                        Text(stringResource(R.string.donate_not_now))
                    }
                },
            )
        }

        // --- Preferences (reminders + animations + region) -------------------------------
        SettingsCard(title = stringResource(R.string.settings_preferences), initiallyExpanded = false) {
            SettingToggleRow(
                title = stringResource(R.string.settings_daily_log_reminder),
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
                            onPermissionDenied(context.getString(R.string.settings_exact_alarms_not_allowed_reminders_may_delayed))
                        }
                    }
                },
                hintTitle = stringResource(R.string.settings_daily_log_reminder),
                hint = stringResource(R.string.settings_local_notification_once_day_no_google_play)
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
                        Text(stringResource(R.string.settings_reminder_time), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            timeLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!ReminderScheduler.canScheduleExactAlarms(context)) {
                    Text(
                        text = stringResource(R.string.settings_exact_alarms_are_off_for_fitbuddy),
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
                    ) { Text(stringResource(R.string.settings_allow_exact_alarms)) }
                }
            }
            SettingToggleRow(
                title = stringResource(R.string.settings_loading_animations),
                checked = settings.animationsEnabled,
                onCheckedChange = { enabled ->
                    onSaveQuiet(settings.withAnimationsEnabled(enabled))
                },
                hintTitle = stringResource(R.string.settings_loading_animations),
                hint = stringResource(R.string.settings_play_wait_animations_while_ai_analyses_food)
            )
            if (settings.animationsEnabled) {
                LoadingAnimationChoiceDropdown(
                    label = "Analyzing banner",
                    selected = settings.analyzingAnimationChoice,
                    slot = LoadingAnimationSlot.ANALYZING,
                    onSelect = { onSaveQuiet(settings.withAnalyzingAnimationChoice(it)) }
                )
                LoadingAnimationChoiceDropdown(
                    label = "Insights button",
                    selected = settings.insightAnimationChoice,
                    slot = LoadingAnimationSlot.INSIGHT,
                    onSelect = { onSaveQuiet(settings.withInsightAnimationChoice(it)) }
                )
                Text(
                    text = stringResource(R.string.settings_per_slot_wait_ui_random_off),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Day-change hour — dropdown 12AM (midnight) … 11AM
            val dayChangeOptions = (0..11).toList()
            var dayChangeExpanded by remember { mutableStateOf(false) }
            fun hourLabel(h: Int): String = if (h == 0) context.getString(R.string.settings_12am_midnight) else "${h}AM"
            ExposedDropdownMenuBox(
                expanded = dayChangeExpanded,
                onExpandedChange = { dayChangeExpanded = it }
            ) {
                OutlinedTextField(
                    value = hourLabel(settings.dayChangeHour),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_day_starts_at)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayChangeExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = dayChangeExpanded,
                    onDismissRequest = { dayChangeExpanded = false }
                ) {
                    dayChangeOptions.forEach { h ->
                        DropdownMenuItem(
                            text = { Text(hourLabel(h)) },
                            onClick = {
                                onSave(settings.copy(dayChangeHour = h))
                                dayChangeExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.settings_logs_before_this_hour_count_toward),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            Text(
                text = stringResource(R.string.settings_region),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_personalises_food_log_examples_portion_units),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val currentRegion = AppRegion.fromStored(settings.region) ?: AppRegion.INDIA
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AppRegion.entries.forEachIndexed { index, region ->
                    SegmentedButton(
                        selected = currentRegion == region,
                        onClick = { onRegionChange(region) },
                        shape = SegmentedButtonDefaults.itemShape(index, AppRegion.entries.size)
                    ) {
                        Text(stringResource(region.shortDisplayNameRes()))
                    }
                }
            }
        }

        if (showReminderTimePicker) {
            val pickerState = rememberTimePickerState(
                initialHour = settings.dailyLogReminderHour,
                initialMinute = settings.dailyLogReminderMinute,
                is24Hour = false
            )
            AlertDialog(
                onDismissRequest = { showReminderTimePicker = false },
                title = { Text(stringResource(R.string.settings_reminder_time)) },
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
                    ) { Text(stringResource(R.string.common_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showReminderTimePicker = false }) { Text(stringResource(R.string.common_cancel)) }
                }
            )
        }

        // --- Integrations (Health Connect) ----------------------------------------------
        SettingsCard(
            title = stringResource(R.string.settings_integrations),
            initiallyExpanded = false,
            hintTitle = stringResource(R.string.settings_health_connect),
            hint = stringResource(R.string.settings_health_connect_hint)
        ) {
            DisposableEffect(lifecycleOwner, settings.healthConnectEnabled) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        healthConnectSdkStatus = healthConnectManager.sdkStatus()
                        if (!settings.healthConnectEnabled) return@LifecycleEventObserver
                        if (healthConnectManager.sdkStatus() != HealthConnectSdkStatus.Available) {
                            return@LifecycleEventObserver
                        }
                        // Skip while a permission request is in flight — resume fires when
                        // the HC sheet closes and grants may not be readable yet.
                        if (pendingHealthConnectMetrics != null) return@LifecycleEventObserver
                        coroutineScope.launch {
                            val granted = healthConnectManager.grantedPermissions()
                            if (granted.isEmpty()) return@launch
                            val current = settings.healthConnectMetrics
                            val aligned = healthConnectManager.alignToGranted(current, granted)
                            if (aligned != current) {
                                onHealthConnectMetricsChange(aligned)
                            }
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            fun requestOrApplyMetrics(next: HealthConnectMetricPrefs, alsoEnable: Boolean = false) {
                when (healthConnectManager.sdkStatus()) {
                    HealthConnectSdkStatus.Available -> {
                        coroutineScope.launch {
                            val missing = healthConnectManager.missingPermissions(next)
                            if (missing.isNotEmpty()) {
                                // Always re-show the permission sheet when enabling a metric.
                                pendingHealthConnectMetrics = next
                                pendingEnableHealthConnect = alsoEnable
                                healthConnectPermissionLauncher.launch(missing)
                            } else {
                                // No missing perms for this selection — apply as requested.
                                onHealthConnectMetricsChange(next)
                                if (alsoEnable) onHealthConnectEnabledChange(true)
                            }
                        }
                    }
                    HealthConnectSdkStatus.UpdateRequired,
                    HealthConnectSdkStatus.Unavailable -> {
                        val intent = healthConnectManager.providerInstallOrUpdateIntent()
                        if (intent != null) {
                            runCatching { context.startActivity(intent) }
                        }
                        onPermissionDenied(
                            context.getString(
                                if (healthConnectSdkStatus == HealthConnectSdkStatus.UpdateRequired) {
                                    R.string.settings_health_connect_update_required
                                } else {
                                    R.string.settings_health_connect_unavailable
                                }
                            )
                        )
                    }
                }
            }

            SettingToggleRow(
                title = stringResource(R.string.settings_health_connect_sync),
                checked = settings.healthConnectEnabled,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        onHealthConnectEnabledChange(false)
                        return@SettingToggleRow
                    }
                    // If a prior deny wiped every metric, restore defaults for the ask.
                    val current = settings.healthConnectMetrics
                    val toRequest =
                        if (current.anyImport() || current.anyExport()) current
                        else HealthConnectMetricPrefs()
                    requestOrApplyMetrics(toRequest, alsoEnable = true)
                },
                hintTitle = stringResource(R.string.settings_health_connect_sync),
                hint = stringResource(R.string.settings_health_connect_hint)
            )
            if (settings.healthConnectEnabled) {
                SettingToggleRow(
                    title = stringResource(R.string.settings_health_connect_auto_sync),
                    checked = settings.healthConnectAutoSync,
                    onCheckedChange = onHealthConnectAutoSyncChange,
                    hintTitle = stringResource(R.string.settings_health_connect_auto_sync),
                    hint = stringResource(R.string.settings_health_connect_auto_sync_hint)
                )
            }
            Text(
                text = when (healthConnectSdkStatus) {
                    HealthConnectSdkStatus.Available ->
                        stringResource(R.string.settings_health_connect_available)
                    HealthConnectSdkStatus.UpdateRequired ->
                        stringResource(R.string.settings_health_connect_update_required)
                    HealthConnectSdkStatus.Unavailable ->
                        stringResource(R.string.settings_health_connect_unavailable)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val metrics = settings.healthConnectMetrics
            val metricsEnabled = settings.healthConnectEnabled
            fun setMetric(
                enable: Boolean,
                transform: (HealthConnectMetricPrefs) -> HealthConnectMetricPrefs
            ) {
                val next = transform(metrics).sanitized()
                if (enable) requestOrApplyMetrics(next)
                else onHealthConnectMetricsChange(next)
            }
            var metricsExpanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { metricsExpanded = !metricsExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings_hc_metrics),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (metricsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = metricsExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_hc_import),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_hc_import_weight),
                        checked = metrics.importWeight,
                        enabled = metricsEnabled && HealthConnectMetricAvailability.importWeight,
                        onCheckedChange = { setMetric(it) { m -> m.copy(importWeight = it) } }
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_hc_import_height),
                        checked = metrics.importHeight,
                        enabled = metricsEnabled && HealthConnectMetricAvailability.importHeight,
                        onCheckedChange = { setMetric(it) { m -> m.copy(importHeight = it) } }
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_hc_import_body_fat),
                        checked = metrics.importBodyFat,
                        enabled = metricsEnabled && HealthConnectMetricAvailability.importBodyFat,
                        onCheckedChange = { setMetric(it) { m -> m.copy(importBodyFat = it) } }
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_hc_import_muscle_mass),
                        checked = metrics.importMuscleMass,
                        enabled = metricsEnabled && HealthConnectMetricAvailability.importMuscleMass,
                        onCheckedChange = { setMetric(it) { m -> m.copy(importMuscleMass = it) } }
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_hc_import_exercise),
                        checked = metrics.importExercise,
                        enabled = metricsEnabled && HealthConnectMetricAvailability.importExercise,
                        onCheckedChange = { setMetric(it) { m -> m.copy(importExercise = it) } }
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_hc_import_steps),
                        checked = metrics.importSteps,
                        enabled = metricsEnabled && HealthConnectMetricAvailability.importSteps,
                        onCheckedChange = { setMetric(it) { m -> m.copy(importSteps = it) } }
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_hc_import_hydration),
                        checked = metrics.importHydration,
                        enabled = metricsEnabled && HealthConnectMetricAvailability.importHydration,
                        onCheckedChange = { setMetric(it) { m -> m.copy(importHydration = it) } }
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_hc_import_sleep),
                        checked = metrics.importSleep,
                        enabled = metricsEnabled && HealthConnectMetricAvailability.importSleep,
                        onCheckedChange = { setMetric(it) { m -> m.copy(importSleep = it) } }
                    )

                    Text(
                        text = stringResource(R.string.settings_hc_export),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_hc_export_nutrition),
                        checked = metrics.exportNutrition,
                        enabled = metricsEnabled && HealthConnectMetricAvailability.exportNutrition,
                        onCheckedChange = { setMetric(it) { m -> m.copy(exportNutrition = it) } }
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_hc_export_weight),
                        checked = metrics.exportWeight,
                        enabled = metricsEnabled && HealthConnectMetricAvailability.exportWeight,
                        onCheckedChange = { setMetric(it) { m -> m.copy(exportWeight = it) } }
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_hc_export_height),
                        checked = metrics.exportHeight,
                        enabled = metricsEnabled && HealthConnectMetricAvailability.exportHeight,
                        onCheckedChange = { setMetric(it) { m -> m.copy(exportHeight = it) } }
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_hc_export_body_fat),
                        checked = metrics.exportBodyFat,
                        enabled = metricsEnabled && HealthConnectMetricAvailability.exportBodyFat,
                        onCheckedChange = { setMetric(it) { m -> m.copy(exportBodyFat = it) } }
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_hc_export_muscle_mass),
                        checked = metrics.exportMuscleMass,
                        enabled = metricsEnabled && HealthConnectMetricAvailability.exportMuscleMass,
                        onCheckedChange = { setMetric(it) { m -> m.copy(exportMuscleMass = it) } }
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_hc_export_hydration),
                        checked = false,
                        enabled = false,
                        onCheckedChange = {},
                        hintTitle = stringResource(R.string.settings_hc_export_hydration),
                        hint = stringResource(R.string.settings_hc_coming_soon)
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_hc_export_exercise),
                        checked = metrics.exportExercise,
                        enabled = metricsEnabled && HealthConnectMetricAvailability.exportExercise,
                        onCheckedChange = { setMetric(it) { m -> m.copy(exportExercise = it) } }
                    )
                }
            }

            if (settings.healthConnectEnabled) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            val missing = healthConnectManager.missingPermissions(
                                settings.healthConnectMetrics
                            )
                            if (missing.isNotEmpty()) {
                                pendingHealthConnectMetrics = settings.healthConnectMetrics
                                pendingSyncAfterPermission = true
                                healthConnectPermissionLauncher.launch(missing)
                            } else {
                                onHealthConnectSync()
                            }
                        }
                    },
                    enabled = !healthConnectSyncBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (healthConnectSyncBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_health_connect_syncing))
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_health_connect_sync_now))
                    }
                }
            }
            if (healthConnectSdkStatus == HealthConnectSdkStatus.Available) {
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(healthConnectManager.openHealthConnectSettingsIntent())
                        }
                    }
                ) {
                    Text(stringResource(R.string.settings_health_connect_open))
                }
            } else {
                val installIntent = healthConnectManager.providerInstallOrUpdateIntent()
                if (installIntent != null) {
                    TextButton(
                        onClick = { runCatching { context.startActivity(installIntent) } }
                    ) {
                        Text(stringResource(R.string.settings_health_connect_install))
                    }
                }
            }
        }

        // --- Backup ----------------------------------------------------------------------
        SettingsCard(
            title = context.getString(R.string.settings_backup),
            initiallyExpanded = false,
            hintTitle = context.getString(R.string.settings_backup),
            hint = stringResource(R.string.settings_share_json_backup_anytime_cloud_uploads_use)
        ) {
            Text(
                text = stringResource(R.string.donate_reminder_support_id),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_share_this_if_you_report_a),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = settings.supportId.ifBlank { context.getString(R.string.settings_generating) },
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
                    Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.settings_copy_support_id))
                }
            }

            Text(
                text = stringResource(R.string.settings_local_backups),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = stringResource(R.string.settings_share_a_json_backup_drive_files),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.FileUpload, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.main_share))
            }

            Text(
                text = stringResource(R.string.settings_cloud),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (!cloudVaultAvailable) {
                Text(
                    text = stringResource(R.string.settings_cloud_backup_is_not_available_in),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (BuildConfig.DEBUG) {
                Text(
                    text = stringResource(R.string.settings_cloud_backup_uploads_are_disabled_in),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SettingToggleRow(
                    title = context.getString(R.string.backup_enable_cloud_backup),
                    checked = false,
                    onCheckedChange = {},
                    enabled = false,
                    hintTitle = context.getString(R.string.backup_enable_cloud_backup),
                    hint = stringResource(R.string.settings_unavailable_debug_builds)
                )
                SettingToggleRow(
                    title = context.getString(R.string.settings_auto_upload_on_startup),
                    checked = false,
                    onCheckedChange = {},
                    enabled = false,
                    hintTitle = context.getString(R.string.settings_auto_upload),
                    hint = stringResource(R.string.settings_unavailable_debug_builds)
                )
                Text(
                    text = stringResource(R.string.settings_cloud_backup_off_debug),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                ) {
                    Text(stringResource(R.string.settings_upload_now))
                }
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                ) {
                    Text(stringResource(R.string.settings_change_cloud_backup_password))
                }
            } else {
                SettingToggleRow(
                    title = context.getString(R.string.backup_enable_cloud_backup),
                    checked = settings.cloudBackupEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            onPrepareCloudBackupEnable()
                            showCloudBackupEnableConfirm = true
                        } else {
                            onCloudBackupEnabledChange(false)
                        }
                    },
                    hintTitle = context.getString(R.string.backup_enable_cloud_backup),
                    hint = stringResource(R.string.settings_when_can_upload_manually_automatically_after_log)
                )
                SettingToggleRow(
                    title = context.getString(R.string.settings_auto_upload_on_startup),
                    checked = settings.cloudAutoUploadEnabled,
                    onCheckedChange = onCloudAutoUploadChange,
                    enabled = settings.cloudBackupEnabled,
                    hintTitle = context.getString(R.string.settings_auto_upload),
                    hint = stringResource(R.string.settings_when_enabled_fitbuddy_uploads_app_start_if)
                )
                val statusText = when {
                    !settings.cloudBackupEnabled -> stringResource(R.string.settings_cloud_backup_off)
                    settings.mongoLastUploadAt <= 0L -> stringResource(R.string.settings_enabled_no_upload_yet)
                    settings.mongoLastUploadOk ->
                        stringResource(R.string.settings_last_upload_ok, {formatMongoUploadTime(context, settings.mongoLastUploadAt)})
                    else ->
                        "Last upload failed · ${formatMongoUploadTime(context, settings.mongoLastUploadAt)}" +
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
                    Text(stringResource(R.string.settings_upload_now))
                }
                OutlinedButton(
                    onClick = { showChangeCloudPasswordDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = settings.cloudBackupEnabled && !mongoBackupBusy
                ) {
                    Text(stringResource(R.string.settings_change_cloud_backup_password))
                }
            }
        }

        // --- Updates & support (updates + crash reports) ---------------------------------
        SettingsCard(title = context.getString(R.string.settings_updates_support), initiallyExpanded = false) {
            Text(
                text = "Installed ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SettingToggleRow(
                title = context.getString(R.string.settings_check_for_updates_automatically),
                checked = settings.autoCheckUpdates,
                onCheckedChange = onAutoCheckUpdatesChange,
                hintTitle = context.getString(R.string.settings_automatic_updates),
                hint = if (BuildConfig.IS_FDROID) {
                    context.getString(R.string.settings_looks_newer_build_f_droid_org_shortly)
                } else {
                    context.getString(R.string.settings_looks_newer_github_release_shortly_after_startup)
                }
            )
            OutlinedButton(
                onClick = onCheckForUpdates,
                enabled = !updateState.isChecking,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (updateState.isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.settings_checking))
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.settings_check_for_updates))
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
            var showCrashOffDisablesCloudWarning by remember {
                mutableStateOf(false)
            }
            if (showCrashOffDisablesCloudWarning) {
                AlertDialog(
                    onDismissRequest = { showCrashOffDisablesCloudWarning = false },
                    title = { Text(stringResource(R.string.settings_disable_crash_reports)) },
                    text = {
                        Text(stringResource(R.string.settings_turning_off_crash_reports_will_also)
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showCrashOffDisablesCloudWarning = false
                                onCrashReportingChange(false)
                            }
                        ) { Text(stringResource(R.string.settings_disable_both)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCrashOffDisablesCloudWarning = false }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                )
            }
            SettingToggleRow(
                title = context.getString(R.string.privacy_send_crash_reports),
                checked = settings.crashReportingEnabled || settings.cloudBackupEnabled,
                onCheckedChange = { enabled ->
                    if (!enabled && settings.cloudBackupEnabled) {
                        showCrashOffDisablesCloudWarning = true
                    } else {
                        onCrashReportingChange(enabled)
                    }
                },
                hintTitle = context.getString(R.string.privacy_crash_reports),
                hint = if (settings.cloudBackupEnabled) {
                    context.getString(R.string.settings_cloud_backup_turning_off_also_disable_cloud)
                } else {
                    context.getString(R.string.settings_anonymous_stack_traces_help_fix_bugs_no)
                }
            )
            val diagnosticEntries by DiagnosticLogger.entryCount.collectAsState()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_diagnostic_log),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                HintIconButton(
                    title = context.getString(R.string.settings_diagnostic_log),
                    message = stringResource(R.string.settings_captures_ai_connection_steps_provider_model)
                )
            }
            Text(
                text = when {
                    settings.diagnosticLoggingEnabled && diagnosticEntries > 0 ->
                        stringResource(R.string.settings_logging_lines_stop_export_when_done, diagnosticEntries)
                    settings.diagnosticLoggingEnabled ->
                        stringResource(R.string.settings_logging_reproduce_issue_then_stop_export)
                    diagnosticEntries > 0 ->
                        stringResource(R.string.settings_lines_last_session, diagnosticEntries)
                    else ->
                        stringResource(R.string.settings_off_until_start_logging)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = {
                    if (settings.diagnosticLoggingEnabled) {
                        onStopAndExportDiagnosticLog()
                    } else {
                        onStartDiagnosticLogging()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (settings.diagnosticLoggingEnabled) {
                    Icon(Icons.Filled.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.settings_stop_logging_export))
                } else {
                    Text(stringResource(R.string.settings_start_logging))
                }
            }
        }

        // --- About -----------------------------------------------------------------------
        SettingsCard(
            title = context.getString(R.string.settings_about),
            collapsible = false,
            hintTitle = context.getString(R.string.settings_about_fitbuddy),
            hint = "AI-powered health tracker with region-aware diets. " +
                "Log meals and workouts via photo or loose text; the AI estimates calories " +
                "and macros.\n\nBuilt with Kotlin, Jetpack Compose, MVVM + Room. Data stays " +
                "on your device."
        ) {
            AboutRow(context.getString(R.string.settings_app), context.getString(R.string.settings_fitbuddy))
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
                        name = context.getString(R.string.about_anant),
                        onClick = onAnantClick
                    )
                }
            )
            AboutLinkRow(context.getString(R.string.donor_card_github), context.getString(R.string.settings_github_com_anantdark), "https://github.com/anantdark")
            AboutLinkRow(
                context.getString(R.string.settings_docs),
                context.getString(R.string.settings_anantdark_github_io_fitbuddy),
                "https://anantdark.github.io/FitBuddy/"
            )
            AboutLinkRow(
                context.getString(R.string.settings_privacy),
                context.getString(R.string.settings_privacy_data_deletion),
                "https://anantdark.github.io/FitBuddy/privacy.html"
            )
        }

        if (developerUnlocked) {
            SettingsCard(title = context.getString(R.string.settings_developer), initiallyExpanded = false) {
                Text(
                    text = stringResource(R.string.settings_debug_tools_tap_package_31_times),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                DeveloperSection(
                    title = context.getString(R.string.settings_backup_identity),
                    description = stringResource(R.string.settings_local_import_and_support_id_export)
                ) {
                    OutlinedButton(
                        onClick = onImport,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.settings_import_backup_json))
                    }
                    OutlinedButton(
                        onClick = onRegenerateSupportId,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_generate_new_support_id))
                    }
                    Text(
                        text = stringResource(R.string.settings_new_id_for_crash_reports_and),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DeveloperSection(
                    title = context.getString(R.string.settings_cloud_atlas),
                    description = stringResource(
                        R.string.settings_device_local_db_collection_overrides_defaults,
                        AppSettings.DEFAULT_MONGO_DB_NAME
                    ) + AppSettings.DEFAULT_MONGO_COLLECTION
                ) {
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
                        label = { Text(stringResource(R.string.settings_database_name)) },
                        singleLine = true,
                        placeholder = { Text(AppSettings.DEFAULT_MONGO_DB_NAME) }
                    )
                    OutlinedTextField(
                        value = mongoCollectionDraft,
                        onValueChange = { mongoCollectionDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.settings_collection_name)) },
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
                        Text(stringResource(R.string.settings_save_atlas_db_collection))
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
                        enabled = !BuildConfig.DEBUG &&
                            cloudVaultAvailable &&
                            !mongoBackupBusy
                    ) {
                        Text(stringResource(R.string.settings_download_restore_from_cloud))
                    }
                    if (BuildConfig.DEBUG) {
                        Text(
                            text = stringResource(R.string.settings_debug_builds_restore_from_cloud_only),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onForceNewCloudChunk,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !BuildConfig.DEBUG &&
                            cloudVaultAvailable &&
                            settings.cloudBackupEnabled &&
                            !mongoBackupBusy
                    ) {
                        Text(stringResource(R.string.settings_create_new_cloud_chunk))
                    }
                    Text(
                        text = stringResource(R.string.settings_freezes_tip_log_rows_into_the),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DeveloperSection(
                    title = context.getString(R.string.settings_ai_debugging),
                    description = stringResource(R.string.settings_toggles_and_tools_for_the_live)
                ) {
                    SettingToggleRow(
                        title = context.getString(R.string.settings_force_offline_ai_simulator),
                        checked = settings.forceOfflineAiSimulator,
                        onCheckedChange = {
                            onSave(settings.copy(forceOfflineAiSimulator = it))
                        },
                        hintTitle = context.getString(R.string.settings_force_offline_ai),
                        hint = stringResource(R.string.settings_bypass_live_api_use_bundled_text_simulator)
                    )
                    SettingToggleRow(
                        title = context.getString(R.string.settings_show_raw_ai_json),
                        checked = settings.showRawAiJson,
                        onCheckedChange = {
                            onSave(settings.copy(showRawAiJson = it))
                        },
                        hintTitle = context.getString(R.string.main_raw_ai_json),
                        hint = stringResource(R.string.settings_after_each_analysis_show_last_raw_json)
                    )
                    SettingToggleRow(
                        title = context.getString(R.string.settings_strict_clarification),
                        checked = settings.strictClarification,
                        onCheckedChange = {
                            onSave(settings.copy(strictClarification = it))
                        },
                        hintTitle = context.getString(R.string.settings_strict_clarification),
                        hint = stringResource(R.string.settings_experimental_prefer_asking_portions_when_counts)
                    )
                    SettingToggleRow(
                        title = context.getString(R.string.settings_verbose_http_logging),
                        checked = settings.verboseHttpLogging,
                        onCheckedChange = {
                            onSave(settings.copy(verboseHttpLogging = it))
                        },
                        hintTitle = context.getString(R.string.settings_verbose_http),
                        hint = stringResource(R.string.settings_log_full_okhttp_request_response_bodies_includin)
                    )
                    OutlinedButton(
                        onClick = onClearModelCooldowns,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_clear_model_cooldowns))
                    }
                    OutlinedButton(
                        onClick = onApplyBuiltInModelDefaults,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_apply_built_in_model_defaults))
                    }
                    Text(
                        text = stringResource(R.string.settings_resets_openrouter_gemini_ollama_photo_and),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DeveloperSection(
                    title = context.getString(R.string.settings_ui_preview),
                    description = stringResource(R.string.settings_preview_themes_and_loading_animations_without)
                ) {
                    SettingToggleRow(
                        title = context.getString(R.string.settings_surface_lab),
                        checked = settings.developerUnlockThemes,
                        onCheckedChange = {
                            onSave(settings.copy(developerUnlockThemes = it))
                        },
                        hintTitle = context.getString(R.string.settings_surface_lab),
                        hint = stringResource(R.string.settings_experimental_surface_token_previews_local_qa)
                    )
                    SettingToggleRow(
                        title = context.getString(R.string.settings_keep_animations_visible),
                        checked = settings.forceShowLoadingAnimations,
                        onCheckedChange = {
                            onSaveQuiet(settings.copy(forceShowLoadingAnimations = it))
                        },
                        hintTitle = context.getString(R.string.settings_keep_animations_visible),
                        hint = stringResource(R.string.settings_always_show_analyzing_banner_insight_wait_animat)
                    )
                }

                DeveloperSection(
                    title = context.getString(R.string.settings_testing),
                    description = stringResource(R.string.settings_one_shot_checks_for_update_ui)
                ) {
                    DemoDonorsPreview(donors = com.anant.fitbuddy.data.donors.DemoDonors.all)
                    OutlinedButton(
                        onClick = onShowTestUpdatePrompt,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_show_test_update_prompt))
                    }
                    Text(
                        text = stringResource(R.string.settings_fake_release_dialog_for_backup_before),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            if (
                                needsNotificationPermission &&
                                !notificationPermission.status.isGranted
                            ) {
                                notificationPermission.launchPermissionRequest()
                                return@OutlinedButton
                            }
                            onShowTestDonateNotification()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_show_donate_reminder_notification))
                    }
                    OutlinedButton(
                        onClick = onShowTestDonateReminderDialog,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_show_donate_reminder_dialog))
                    }
                    OutlinedButton(
                        onClick = onShowTestNewDonorsThankYou,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_show_new_donors_thank_you))
                    }
                    OutlinedButton(
                        onClick = onShowTestDonorGallery,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.donate_show_donors))
                    }
                    Text(
                        text = stringResource(R.string.settings_opens_the_supporters_gallery_with_demo),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onShowTestDonationDialog,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_open_donation_dialog))
                    }
                    Text(
                        text = stringResource(
                            R.string.settings_donor_last_seen_lastseendonordonationdate,
                            lastSeenDonorDonationDate
                                ?: stringResource(R.string.settings_cleared_paren)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = onClearLastSeenDonorDonationDate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_clear_donor_last_seen_date))
                    }
                    OutlinedButton(
                        onClick = onSetLastSeenDonorDonationDateToLatest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_set_donor_last_seen_to_latest))
                    }
                    OutlinedButton(
                        onClick = { onSetLastSeenDonorDonationDate("2020-01-01") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_set_donor_last_seen_to_2020))
                    }
                    Text(
                        text = stringResource(R.string.settings_clear_old_date_thank_you_can),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onResetDonateReminderTimers,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_reset_donate_reminder_timers))
                    }
                    Text(
                        text = stringResource(R.string.settings_donate_test_actions_bypass_the_3),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onRestartOnboarding,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_preview_onboarding_flow))
                    }
                    Text(
                        text = stringResource(R.string.settings_opens_the_full_first_run_ui),
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
                        Text(stringResource(R.string.settings_send_test_notification))
                    }
                    OutlinedButton(
                        onClick = {
                            throw RuntimeException(context.getString(R.string.settings_fitbuddy_sentry_test_crash))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_force_test_crash))
                    }
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
                title = { Text(stringResource(R.string.settings_restore_from_cloud)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.settings_enter_the_support_id_of_the)
                        )
                        OutlinedTextField(
                            value = mongoRestoreSupportIdDraft,
                            onValueChange = { mongoRestoreSupportIdDraft = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.donate_reminder_support_id)) },
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.settings_xxxxxxxx_xxxx)) }
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
                        Text(stringResource(R.string.onboarding_restore))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMongoRestoreConfirm = false }) {
                        Text(stringResource(R.string.common_cancel))
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

private fun formatMongoUploadTime(context: android.content.Context, epochMs: Long): String {
    if (epochMs <= 0L) return context.getString(R.string.settings_never)
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
    onSkipBackupAndUpdate: (downloadUrl: String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    updateState.updateInfo?.let { info ->
        val isFdroidChannel = info.channel == UpdateChannel.FDROID
        val highlights = remember(info.releaseNotes) {
            ReleaseNotes.highlights(info.releaseNotes)
        }
        val busy = updateState.isExportingBackup ||
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
            title = { Text(stringResource(R.string.settings_update_available)) },
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
                            text = stringResource(R.string.settings_what_s_new),
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
                            Text(if (isFdroidChannel) stringResource(R.string.settings_view_on_f_droid_org) else stringResource(R.string.settings_view_on_github))
                        }
                    }
                    Text(
                        text = buildString {
                            append(
                                if (cloudBackupEnabled) {
                                    stringResource(R.string.settings_back_up_data_cloud_before_updating)
                                } else {
                                    stringResource(R.string.settings_export_local_backup_before_updating)
                                }
                            )
                            if (isFdroidChannel) {
                                append(stringResource(R.string.settings_then_opens_f_droid_to_install))
                            }
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
                        ) { Text(stringResource(R.string.settings_export_backup_update)) }
                        OutlinedButton(
                            onClick = { onSkipBackupAndUpdate(info.downloadUrl) },
                            enabled = skipEnabled,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (skipCountdownSec > 0) {
                                    stringResource(R.string.settings_skip_backup_update, skipCountdownSec)
                                } else {
                                    stringResource(R.string.settings_skip_backup_update_2)
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
}

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
    val customNeedsUrl = provider == AiProvider.CUSTOM && baseUrl.isBlank()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedModel.ifBlank { stringResource(R.string.settings_select_model) },
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
                                modelsState.isLoading -> stringResource(R.string.settings_loading_models, noun)
                                provider == AiProvider.GEMINI && apiKey.isBlank() ->
                                    stringResource(R.string.settings_enter_api_key_first)
                                ollamaNeedsKey -> stringResource(R.string.settings_enter_ollama_cloud_api_key_first)
                                ollamaNeedsUrl -> stringResource(R.string.settings_enter_ollama_server_url_first)
                                customNeedsUrl -> stringResource(R.string.settings_enter_custom_base_url_first)
                                modelsState.error != null -> stringResource(R.string.settings_couldn_t_load_models)
                                else -> stringResource(R.string.settings_no_models_found, noun)
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
        label = { Text(stringResource(R.string.settings_model_id)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        val statusText = when {
            modelsState.isLoading -> stringResource(R.string.settings_loading_models, noun)
            modelsState.error != null -> "Couldn't load list: ${modelsState.error}"
            options.isNotEmpty() -> "${options.size} $noun models available"
            provider == AiProvider.GEMINI && apiKey.isBlank() -> stringResource(R.string.settings_enter_api_key_load_models)
            ollamaNeedsKey -> stringResource(R.string.settings_enter_ollama_cloud_api_key_load_models)
            ollamaNeedsUrl -> stringResource(R.string.settings_enter_ollama_server_url_load_models)
            modelsState.loaded -> stringResource(R.string.settings_no_reachable_models_type_id_above, noun)
            else -> stringResource(R.string.settings_pick_image_capable_model)
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
            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.settings_reload_models))
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
                    text = if (isConfigured) stringResource(R.string.settings_ai_online) else stringResource(R.string.settings_ai_offline_simulator),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isConfigured) {
                        "Using ${provider.name.lowercase()}"
                    } else {
                        stringResource(R.string.settings_add_key_server_below_enable_live_analysis)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeveloperSection(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        content()
    }
}

@Composable
private fun SettingsCard(
    title: String,
    collapsible: Boolean = true,
    initiallyExpanded: Boolean = true,
    hintTitle: String? = null,
    hint: String? = null,
    /** Optional chip beside the title (e.g. supporter-only sections). */
    titleBadge: (@Composable () -> Unit)? = null,
    /** When false, header is greyed and expand taps call [onLockedExpandAttempt] instead. */
    unlocked: Boolean = true,
    onLockedExpandAttempt: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded || !collapsible) }
    LaunchedEffect(unlocked) {
        if (!unlocked) expanded = false
    }

    fun onHeaderClick() {
        if (!unlocked) {
            onLockedExpandAttempt?.invoke()
            return
        }
        if (collapsible) expanded = !expanded
    }

    val locked = !unlocked
    val cardColors = if (locked) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (locked) Modifier.alpha(0.72f) else Modifier),
        colors = cardColors,
        elevation = CardDefaults.cardElevation(defaultElevation = if (locked) 0.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (collapsible || locked) {
                                Modifier.clickable(onClick = ::onHeaderClick)
                            } else {
                                Modifier
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (locked) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    titleBadge?.invoke()
                }
                if (hint != null && hintTitle != null && unlocked) {
                    HintIconButton(title = hintTitle, message = hint)
                }
                if (collapsible) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) stringResource(R.string.openrouter_collapse) else stringResource(R.string.openrouter_expand),
                        tint = if (locked) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.clickable(onClick = ::onHeaderClick)
                    )
                }
            }
            AnimatedVisibility(visible = (expanded || !collapsible) && unlocked) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    content()
                }
            }
        }
    }
}

/** Compact chip marking Settings sections reserved for public supporters. */
@Composable
private fun SupporterExclusiveBadge(
    muted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val background = if (muted) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val foreground = if (muted) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }
    Text(
        text = stringResource(R.string.settings_supporter_exclusive),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = foreground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(appShape(6.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingAnimationChoiceDropdown(
    label: String,
    selected: String,
    slot: LoadingAnimationSlot,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val randomLabel = stringResource(R.string.settings_random)
    val offLabel = stringResource(R.string.settings_off_spinner)
    val animLabels = LoadingAnimationRegistry.forSlot(slot).associate { it.id to stringResource(it.displayNameRes) }
    val options = remember(slot, randomLabel, offLabel, animLabels) {
        listOf(
            AppSettings.LOADING_ANIM_RANDOM to randomLabel,
            AppSettings.LOADING_ANIM_OFF to offLabel
        ) + LoadingAnimationRegistry.forSlot(slot).map { it.id to (animLabels[it.id] ?: it.id) }
    }
    val displayLabel = options.firstOrNull { it.first == selected }?.second
        ?: selected.ifBlank { randomLabel }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    }
                )
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
        AppSwitch(
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
    val sexOptions = PROFILE_SEX_OPTION_RES.map { it.first to stringResource(it.second) }
    val display = sexOptions.firstOrNull { it.first == selectedValue }?.second
        ?: sexOptions.first().second

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_sex)) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sexOptions.forEach { (value, text) ->
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
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
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
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )
            HintIconButton(
                title = stringResource(R.string.onboarding_api_keys),
                message = stringResource(R.string.settings_paste_multiple_keys_separated_by_commas)
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
                                contentDescription = stringResource(R.string.settings_remove_key),
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
                    label = { Text(stringResource(R.string.settings_add_key_paste_several)) },
                    singleLine = true,
                    isError = isError && keys.isEmpty(),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.weight(1f)
                )
            }
            TextButton(
                onClick = {
                    if (commitRaw(draft)) draft = ""
                }
            ) { Text(stringResource(R.string.food_add)) }
        }
        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
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
            contentDescription = stringResource(R.string.settings_about_2, title),
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
                TextButton(onClick = { show = false }) { Text(stringResource(R.string.settings_got_it)) }
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
                shape = appShape(8.dp)
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
        factory = { ctx ->
            ImageView(ctx).apply {
                contentDescription = ctx.getString(R.string.settings_party_parrot)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                val drawable = ImageDecoder.decodeDrawable(
                    ImageDecoder.createSource(ctx.resources, R.raw.party_parrot)
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
        !lengthValid && password.length < 8 -> stringResource(R.string.main_password_must_be_at_least_8)
        !lengthValid -> stringResource(R.string.main_password_must_be_128_characters_or)
        !isBlank && confirmPassword.isNotEmpty() && !passwordsMatch -> stringResource(R.string.main_passwords_do_not_match)
        else -> null
    }
    val canConfirm = !busy && (isBlank || (lengthValid && passwordsMatch))

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.backup_enable_cloud_backup)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.settings_your_backup_is_encrypted_on_this)
                )
                Text(
                    stringResource(R.string.settings_cloud_backups_are_keyed_by_your)
                )
                Text(
                    text = supportId.ifBlank { stringResource(R.string.settings_generating) },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.settings_optionally_set_a_password_leave_blank),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.main_password_optional)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = validationError != null && confirmPassword.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isBlank) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(R.string.main_confirm_password)) },
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
            ) { Text(stringResource(R.string.settings_copy_id_enable)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.common_cancel)) }
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
        !lengthValid && newPassword.length < 8 -> stringResource(R.string.main_password_must_be_at_least_8)
        !lengthValid -> stringResource(R.string.main_password_must_be_128_characters_or)
        !isBlank && !passwordsMatch && confirmPassword.isNotEmpty() -> stringResource(R.string.main_passwords_do_not_match)
        else -> null
    }
    val canConfirm = !busy && (isBlank || (lengthValid && passwordsMatch))

    AlertDialog(
        onDismissRequest = {
            if (!busy) onDismiss()
        },
        title = { Text(stringResource(R.string.settings_change_cloud_backup_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.settings_set_a_new_password_to_protect),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.settings_new_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = validationError != null && confirmPassword.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.main_confirm_password)) },
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
            ) { Text(stringResource(R.string.settings_change)) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !busy
            ) { Text(stringResource(R.string.common_cancel)) }
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
    val shape = appShape(20.dp)
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
