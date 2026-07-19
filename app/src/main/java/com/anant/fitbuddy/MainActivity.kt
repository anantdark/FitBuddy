package com.anant.fitbuddy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.ui.RequestStartupPermissions
import com.anant.fitbuddy.ui.screens.MainScreen
import com.anant.fitbuddy.ui.screens.OnboardingScreen
import com.anant.fitbuddy.ui.theme.FitBuddyTheme
import com.anant.fitbuddy.ui.viewmodel.MainViewModel
import com.anant.fitbuddy.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    private var openLogHubRequest by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openLogHubRequest = intent.consumeOpenLogHub()
        enableEdgeToEdge()
        val app = application as FitBuddyApp
        setContent {
            // Read dynamic-color preference before theming so Material You toggles live.
            val settings by app.settingsRepository.settings.collectAsStateWithLifecycle(AppSettings())
            FitBuddyTheme(dynamicColor = settings.dynamicColor) {
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModelFactory(app.repository, app.settingsRepository, app.updateChecker)
                )
                val needsOnboarding by viewModel.needsOnboarding.collectAsStateWithLifecycle()
                val onboardingSaving by viewModel.onboardingSaving.collectAsStateWithLifecycle()
                val onboardingValidating by viewModel.onboardingValidating.collectAsStateWithLifecycle()

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
                        RequestStartupPermissions()
                        OnboardingScreen(
                            isSaving = onboardingSaving,
                            isValidating = onboardingValidating,
                            onValidateAi = viewModel::validateOnboardingAi,
                            onComplete = viewModel::completeOnboarding
                        )
                    }

                    false -> {
                        RequestStartupPermissions()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.consumeOpenLogHub()) {
            openLogHubRequest = true
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
