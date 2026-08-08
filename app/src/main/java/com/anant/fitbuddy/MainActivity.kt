package com.anant.fitbuddy

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.anant.fitbuddy.ui.screens.CrashReportingOptInScreen
import com.anant.fitbuddy.ui.screens.MainScreen
import com.anant.fitbuddy.ui.screens.OnboardingScreen
import com.anant.fitbuddy.ui.screens.RegionSelectionScreen
import com.anant.fitbuddy.ui.theme.FitBuddyTheme
import com.anant.fitbuddy.ui.util.dismissKeyboardOnTap
import com.anant.fitbuddy.ui.viewmodel.MainViewModel
import com.anant.fitbuddy.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    private var openLogHubRequest by mutableStateOf(false)
    private var openRouterOAuthUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openLogHubRequest = intent.consumeOpenLogHub()
        openRouterOAuthUri = intent.data.takeIf { OpenRouterOAuth.isCallback(it) }
        enableEdgeToEdge()
        val app = application as FitBuddyApp
        setContent {
            // Read dynamic-color preference before theming so Material You toggles live.
            val settings by app.settingsRepository.settings.collectAsStateWithLifecycle(AppSettings())
            FitBuddyTheme(dynamicColor = settings.dynamicColor) {
                Box(modifier = Modifier.fillMaxSize().dismissKeyboardOnTap()) {
                    val viewModel: MainViewModel = viewModel(
                        factory = MainViewModelFactory(app.repository, app.settingsRepository, app.updateChecker)
                    )
                    val needsOnboarding by viewModel.needsOnboarding.collectAsStateWithLifecycle()
                    val needsRegionSelection by viewModel.needsRegionSelection.collectAsStateWithLifecycle()
                    val regionSelectionSaving by viewModel.regionSelectionSaving.collectAsStateWithLifecycle()
                    val onboardingAiOnly by viewModel.onboardingAiOnly.collectAsStateWithLifecycle()
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
                            // Same as onboarding: opt-in page only when the build defaults
                            // crash reporting off (F-Droid / debug) and it is still off.
                            val buildDefaultsCrashOff = !AppSettings().crashReportingEnabled
                            var pendingCrashChoice by remember {
                                mutableStateOf<Boolean?>(
                                    if (buildDefaultsCrashOff && !settings.crashReportingEnabled) {
                                        null
                                    } else {
                                        settings.crashReportingEnabled
                                    }
                                )
                            }
                            if (pendingCrashChoice == null) {
                                CrashReportingOptInScreen(
                                    initialEnabled = false,
                                    onContinue = { enabled ->
                                        pendingCrashChoice = enabled
                                        // Apply immediately so region-request send works
                                        // before completeRegionSelection persists settings.
                                        CrashReporter.setReportingEnabled(enabled)
                                    }
                                )
                            } else {
                                RegionSelectionScreen(
                                    defaultRegion = detectedRegion,
                                    crashReportingEnabled = pendingCrashChoice == true,
                                    supportId = settings.supportId,
                                    isSaving = regionSelectionSaving,
                                    regionRequestAlreadySent = settings.regionRequestSentAt > 0L,
                                    onFinished = { region ->
                                        viewModel.completeRegionSelection(
                                            region,
                                            crashReportingEnabled = pendingCrashChoice
                                        )
                                    },
                                    onBack = if (buildDefaultsCrashOff) {
                                        { pendingCrashChoice = null }
                                    } else {
                                        null
                                    },
                                    requestDisabledHint = if (buildDefaultsCrashOff) {
                                        "Enable crash reporting on the previous page to request a custom region."
                                    } else {
                                        "Enable Send crash reports in Settings to request a custom region."
                                    },
                                    onRequestRegionSent = viewModel::markRegionRequestSent
                                )
                            }
                        } else {
                            RequestStartupPermissions(onDenied = onStartupPermissionsDenied)
                            MainScreen(
                                viewModel = viewModel,
                                openLogHubRequest = openLogHubRequest,
                                onOpenLogHubConsumed = { openLogHubRequest = false }
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
        if (intent.consumeOpenLogHub()) {
            openLogHubRequest = true
        }
        intent.data?.takeIf { OpenRouterOAuth.isCallback(it) }?.let {
            openRouterOAuthUri = it
        }
    }

    companion object {
        const val EXTRA_OPEN_LOG_HUB = "open_log_hub"
    }
}

private fun Intent.consumeOpenLogHub(): Boolean {
    if (!getBooleanExtra(MainActivity.EXTRA_OPEN_LOG_HUB, false)) return false
    removeExtra(MainActivity.EXTRA_OPEN_LOG_HUB)
    return true
}
