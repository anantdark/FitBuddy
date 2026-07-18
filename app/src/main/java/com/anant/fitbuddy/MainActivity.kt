package com.anant.fitbuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.ui.screens.MainScreen
import com.anant.fitbuddy.ui.screens.OnboardingScreen
import com.anant.fitbuddy.ui.theme.FitBuddyTheme
import com.anant.fitbuddy.ui.viewmodel.MainViewModel
import com.anant.fitbuddy.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                        OnboardingScreen(
                            isSaving = onboardingSaving,
                            onComplete = viewModel::completeOnboarding
                        )
                    }

                    false -> MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
