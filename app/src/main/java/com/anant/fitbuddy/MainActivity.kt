package com.anant.fitbuddy

import android.content.res.Resources
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anant.fitbuddy.crash.CrashReporter
import com.anant.fitbuddy.data.backup.mongo.MongoUriVault
import com.anant.fitbuddy.data.region.RegionDetector
import com.anant.fitbuddy.data.remote.oauth.OpenRouterOAuth
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.ui.RequestStartupPermissions
import com.anant.fitbuddy.ui.screens.CloudBackupOptInScreen
import com.anant.fitbuddy.ui.screens.CrashReportingOptInScreen
import com.anant.fitbuddy.ui.screens.MainScreen
import com.anant.fitbuddy.ui.screens.OnboardingScreen
import com.anant.fitbuddy.ui.screens.RegionSelectionScreen
import com.anant.fitbuddy.ui.theme.FitBuddyTheme
import com.anant.fitbuddy.ui.util.dismissKeyboardOnTap
import com.anant.fitbuddy.ui.viewmodel.MainViewModel
import com.anant.fitbuddy.ui.viewmodel.MainViewModelFactory
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {

    private var openLogHubRequest by mutableStateOf(false)
    private var openLogActionRequest by mutableStateOf<String?>(null)
    private var openPresetIdRequest by mutableStateOf<Int?>(null)
    private var openPresetKindRequest by mutableStateOf<String?>(null)
    private var openDonateReminderRequest by mutableStateOf(false)
    private var openRouterOAuthUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyLaunchExtras(intent)
        openRouterOAuthUri = intent.data.takeIf { OpenRouterOAuth.isCallback(it) }
        try {
            enableEdgeToEdge()
        } catch (_: Resources.NotFoundException) {
            // Defensive: on some API levels the platform may lack resources that the
            // AndroidX edge-to-edge impl references when compiled against SDK 36.
            // The app is still usable without edge-to-edge styling.
        }
        val app = application as FitBuddyApp
        setContent {
            // Read dynamic-color preference before theming so Material You toggles live.
            val settings by app.settingsRepository.settings.collectAsStateWithLifecycle(AppSettings())
            FitBuddyTheme(
                dynamicColor = settings.dynamicColor,
                colorTheme = settings.colorTheme,
                fontOption = settings.fontOption,
                componentStyle = settings.componentStyle,
            ) {
                Box(modifier = Modifier.fillMaxSize().dismissKeyboardOnTap()) {
                    val viewModel: MainViewModel = viewModel(
                        factory = MainViewModelFactory(app, app.repository, app.settingsRepository, app.updateChecker)
                    )
                    val needsOnboarding by viewModel.needsOnboarding.collectAsStateWithLifecycle()
                    val needsRegionSelection by viewModel.needsRegionSelection.collectAsStateWithLifecycle()
                    val regionSelectionSaving by viewModel.regionSelectionSaving.collectAsStateWithLifecycle()
                    val onboardingAiOnly by viewModel.onboardingAiOnly.collectAsStateWithLifecycle()
                    val onboardingPreviewMode by viewModel.onboardingPreviewMode.collectAsStateWithLifecycle()
                    val onboardingSaving by viewModel.onboardingSaving.collectAsStateWithLifecycle()
                    val onboardingValidating by viewModel.onboardingValidating.collectAsStateWithLifecycle()
                    val onboardingRestoring by viewModel.onboardingRestoring.collectAsStateWithLifecycle()
                    val openRouterOAuthBusy by viewModel.openRouterOAuthBusy.collectAsStateWithLifecycle()
                    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()

                    LaunchedEffect(openRouterOAuthUri) {
                        val uri = openRouterOAuthUri ?: return@LaunchedEffect
                        viewModel.handleOpenRouterOAuthCallback(uri)
                        openRouterOAuthUri = null
                        intent?.data = null
                    }

                    val onStartupPermissionsDenied: (List<String>) -> Unit = { denied ->
                        val notificationsDenied =
                            Manifest.permission.POST_NOTIFICATIONS in denied
                        if (notificationsDenied) {
                            viewModel.disableDailyLogReminder()
                            viewModel.showTransientMessage("Notifications not allowed.")
                        }
                    }

                    when (needsOnboarding) {
                        null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        true -> {
                            RequestStartupPermissions(onDenied = onStartupPermissionsDenied)
                            OnboardingScreen(
                                isSaving = onboardingSaving,
                                isValidating = onboardingValidating,
                                isRestoring = onboardingRestoring,
                                aiOnly = onboardingAiOnly,
                                previewMode = onboardingPreviewMode,
                                initialCrashReportingEnabled = settings.crashReportingEnabled,
                                supportId = settings.supportId,
                                regionRequestAlreadySent = settings.regionRequestSentAt > 0L,
                                onRegionRequestSent = viewModel::markRegionRequestSent,
                                cloudRestoreAvailable = MongoUriVault.isAvailable(),
                                openRouterOAuthBusy = openRouterOAuthBusy,
                                openRouterOAuthKey = settings.openRouterOAuthKey,
                                userMessage = analysisState.userMessage,
                                onUserMessageConsumed = viewModel::consumeUserMessage,
                                onConnectOpenRouter = viewModel::startOpenRouterOAuth,
                                onDisconnectOpenRouter = viewModel::disconnectOpenRouterOAuth,
                                onStartGuest = viewModel::startGuestOnboarding,
                                onRestoreCloud = viewModel::restoreOnboardingFromCloud,
                                onRestoreLocal = viewModel::restoreOnboardingFromLocal,
                                onValidateAi = viewModel::validateOnboardingAi,
                                onComplete = viewModel::completeOnboarding,
                                onCompleteAiOnly = viewModel::completeAiSetupOnly
                            )
                        }

                        false -> if (needsRegionSelection) {
                            RequestStartupPermissions(onDenied = onStartupPermissionsDenied)
                            val context = LocalContext.current
                            val detectedRegion = remember { RegionDetector.detectFromDevice(context) }
                            // Same as onboarding: cloud page when vault is available (non-debug),
                            // crash opt-in when the build defaults reporting off and it is still off.
                            // F-Droid only — GitHub builds skip cloud onboarding.
                            val showCloudStep =
                                !BuildConfig.DEBUG &&
                                    BuildConfig.IS_FDROID &&
                                    MongoUriVault.isAvailable()
                            val buildDefaultsCrashOff = !AppSettings().crashReportingEnabled
                            val needsCrashStep =
                                buildDefaultsCrashOff && !settings.crashReportingEnabled
                            // null = still on that opt-in page; non-null = choice made / skipped.
                            var pendingCloudChoice by remember {
                                mutableStateOf<Boolean?>(
                                    if (showCloudStep && !settings.cloudBackupEnabled) null
                                    else settings.cloudBackupEnabled
                                )
                            }
                            var pendingCrashChoice by remember {
                                mutableStateOf<Boolean?>(
                                    if (needsCrashStep) null else settings.crashReportingEnabled
                                )
                            }
                            when {
                                showCloudStep && pendingCloudChoice == null -> {
                                    CloudBackupOptInScreen(
                                        initialEnabled = false,
                                        onContinue = { enabled ->
                                            pendingCloudChoice = enabled
                                            if (enabled) {
                                                pendingCrashChoice = true
                                                CrashReporter.setReportingEnabled(true)
                                            }
                                        }
                                    )
                                }
                                // Skip crash page when cloud backup is on (crash reports implied).
                                // Existing accounts (region-only path): default the toggle on.
                                needsCrashStep &&
                                    pendingCloudChoice != true &&
                                    pendingCrashChoice == null -> {
                                    CrashReportingOptInScreen(
                                        initialEnabled = true,
                                        onContinue = { enabled ->
                                            pendingCrashChoice = enabled
                                            CrashReporter.setReportingEnabled(enabled)
                                        },
                                        onBack = if (showCloudStep) {
                                            { pendingCloudChoice = null }
                                        } else {
                                            null
                                        }
                                    )
                                }
                                else -> {
                                    val crashOn = pendingCrashChoice == true ||
                                        pendingCloudChoice == true
                                    RegionSelectionScreen(
                                        defaultRegion = detectedRegion,
                                        crashReportingEnabled = crashOn,
                                        supportId = settings.supportId,
                                        isSaving = regionSelectionSaving,
                                        regionRequestAlreadySent =
                                            settings.regionRequestSentAt > 0L,
                                        onFinished = { region ->
                                            viewModel.completeRegionSelection(
                                                region,
                                                crashReportingEnabled = crashOn,
                                                cloudBackupEnabled = pendingCloudChoice == true
                                            )
                                        },
                                        onBack = when {
                                            pendingCloudChoice == true && showCloudStep -> {
                                                { pendingCloudChoice = null }
                                            }
                                            needsCrashStep && pendingCloudChoice != true -> {
                                                { pendingCrashChoice = null }
                                            }
                                            showCloudStep -> {
                                                { pendingCloudChoice = null }
                                            }
                                            else -> null
                                        },
                                        requestDisabledHint = if (needsCrashStep ||
                                            buildDefaultsCrashOff
                                        ) {
                                            "Enable crash reporting on the previous page to " +
                                                "request a custom region."
                                        } else {
                                            "Enable Send crash reports in Settings to request " +
                                                "a custom region."
                                        },
                                        onRequestRegionSent = viewModel::markRegionRequestSent
                                    )
                                }
                            }
                        } else {
                            RequestStartupPermissions(onDenied = onStartupPermissionsDenied)
                            MainScreen(
                                viewModel = viewModel,
                                openLogHubRequest = openLogHubRequest,
                                onOpenLogHubConsumed = { openLogHubRequest = false },
                                openLogActionRequest = openLogActionRequest,
                                openPresetIdRequest = openPresetIdRequest,
                                openPresetKindRequest = openPresetKindRequest,
                                onOpenLogActionConsumed = {
                                    openLogActionRequest = null
                                    openPresetIdRequest = null
                                    openPresetKindRequest = null
                                },
                                openDonateReminderRequest = openDonateReminderRequest,
                                onOpenDonateReminderConsumed = { openDonateReminderRequest = false },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyLaunchExtras(intent)
        intent.data?.takeIf { OpenRouterOAuth.isCallback(it) }?.let {
            openRouterOAuthUri = it
        }
    }

    private fun applyLaunchExtras(intent: Intent) {
        val action = intent.consumeOpenLogAction()
        if (action != null) {
            openLogActionRequest = action
            openPresetIdRequest = intent.consumeOpenPresetId()
            openPresetKindRequest = intent.consumeOpenPresetKind()
            openLogHubRequest = false
        } else if (intent.consumeOpenLogHub()) {
            openLogHubRequest = true
            openLogActionRequest = null
            openPresetIdRequest = null
            openPresetKindRequest = null
        }
        if (intent.consumeOpenDonateReminder()) {
            openDonateReminderRequest = true
        }
    }

    companion object {
        const val EXTRA_OPEN_LOG_HUB = "open_log_hub"
        const val EXTRA_OPEN_LOG_ACTION = "open_log_action"
        const val EXTRA_OPEN_PRESET_ID = "open_preset_id"
        const val EXTRA_OPEN_PRESET_KIND = "open_preset_kind"
        const val EXTRA_OPEN_DONATE_REMINDER = "open_donate_reminder"

        const val LOG_ACTION_CAMERA = "camera"
        const val LOG_ACTION_TEXT = "text"
        const val LOG_ACTION_PRESETS = "presets"
        const val LOG_ACTION_SAVED_FOOD = "saved_food"
        const val LOG_ACTION_BARCODE = "barcode"
        const val LOG_ACTION_HUB = "hub"

        const val PRESET_KIND_MEAL = "meal"
        const val PRESET_KIND_FOOD = "food"
    }
}

private fun Intent.consumeOpenLogHub(): Boolean {
    if (!getBooleanExtra(MainActivity.EXTRA_OPEN_LOG_HUB, false)) return false
    removeExtra(MainActivity.EXTRA_OPEN_LOG_HUB)
    return true
}

private fun Intent.consumeOpenLogAction(): String? {
    val action = getStringExtra(MainActivity.EXTRA_OPEN_LOG_ACTION)?.takeIf { it.isNotBlank() }
        ?: return null
    removeExtra(MainActivity.EXTRA_OPEN_LOG_ACTION)
    return action
}

private fun Intent.consumeOpenPresetId(): Int? {
    if (!hasExtra(MainActivity.EXTRA_OPEN_PRESET_ID)) return null
    val id = getIntExtra(MainActivity.EXTRA_OPEN_PRESET_ID, -1).takeIf { it > 0 }
    removeExtra(MainActivity.EXTRA_OPEN_PRESET_ID)
    return id
}

private fun Intent.consumeOpenPresetKind(): String? {
    val kind = getStringExtra(MainActivity.EXTRA_OPEN_PRESET_KIND)?.takeIf { it.isNotBlank() }
        ?: return null
    removeExtra(MainActivity.EXTRA_OPEN_PRESET_KIND)
    return kind
}

private fun Intent.consumeOpenDonateReminder(): Boolean {
    if (!getBooleanExtra(MainActivity.EXTRA_OPEN_DONATE_REMINDER, false)) return false
    removeExtra(MainActivity.EXTRA_OPEN_DONATE_REMINDER)
    return true
}
