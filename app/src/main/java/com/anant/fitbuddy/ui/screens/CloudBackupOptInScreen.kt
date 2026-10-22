package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.ui.components.AppSwitch
import com.anant.fitbuddy.ui.components.Button
import com.anant.fitbuddy.ui.components.OutlinedButton
import com.anant.fitbuddy.ui.util.dismissKeyboardOnTap
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

/**
 * Optional onboarding step for personal cloud backup. Enabling it also turns crash reporting on
 * (crash opt-in page is skipped).
 */
@Composable
fun CloudBackupOptInScreen(
    initialEnabled: Boolean,
    onContinue: (enabled: Boolean) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var enabled by remember(initialEnabled) { mutableStateOf(initialEnabled) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 24.dp)
            ) {
                if (onBack != null) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) { Text("Back") }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Button(
                    onClick = { onContinue(enabled) },
                    modifier = Modifier.weight(1f)
                ) { Text("Continue") }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .dismissKeyboardOnTap()
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Cloud backup",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Encrypted backup to FitBuddy’s cloud. Off by default — change anytime in Settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable cloud backup",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    AppSwitch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }
            }
            if (enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Crash reports will also be enabled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
