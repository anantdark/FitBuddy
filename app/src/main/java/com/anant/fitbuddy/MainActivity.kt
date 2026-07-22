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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anant.fitbuddy.data.backup.mongo.MongoUriVault
import com.anant.fitbuddy.data.remote.oauth.OpenRouterOAuth
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.ui.RequestStartupPermissions
import com.anant.fitbuddy.ui.screens.MainScreen
import com.anant.fitbuddy.ui.screens.OnboardingScreen
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
                    val onboardingAiOnly by viewModel.onboardingAiOnly.collectAsStateWithLifecycle()
                    val onboardingSaving by viewModel.onboardingSaving.collectAsStateWithLifecycle()
                    val onboardingValidating by viewModel.onboardingValidating.collectAsStateWithLifecycle()
                    val onboardingRestoring by viewModel.onboardingRestoring.collectAsStateWithLifecycle()
                    val openRouterOAuthBusy by viewModel.openRouterOAuthBusy.collectAsStateWithLifecycle()
                    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()

                    val onboardingImportLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.OpenDocument()
                    ) { uri ->
                        if (uri != null) {
                            viewModel.restoreOnboardingFromLocal(uri) { _, _ -> }
                        }
                    }

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
                                cloudRestoreAvailable = MongoUriVault.isAvailable(),
                                openRouterOAuthBusy = openRouterOAuthBusy,
                                openRouterOAuthKey = settings.openRouterOAuthKey,
                                userMessage = analysisState.userMessage,
                                onUserMessageConsumed = viewModel::consumeUserMessage,
                                onConnectOpenRouter = viewModel::startOpenRouterOAuth,
                                onDisconnectOpenRouter = viewModel::disconnectOpenRouterOAuth,
                                onStartGuest = viewModel::startGuestOnboarding,
                                onRestoreCloud = viewModel::restoreOnboardingFromCloud,
                                onRequestLocalRestore = {
                                    onboardingImportLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                onValidateAi = viewModel::validateOnboardingAi,
                                onComplete = viewModel::completeOnboarding,
                                onCompleteAiOnly = viewModel::completeAiSetupOnly
                            )
                        }

                        false -> {
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
