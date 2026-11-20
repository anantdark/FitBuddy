package com.anant.fitbuddy.ui.screens

import com.anant.fitbuddy.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
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
import com.anant.fitbuddy.ui.viewmodel.CloudAttestUi
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay

/**
 * Optional onboarding step for personal cloud backup. Enabling it also turns crash reporting on
 * (crash opt-in page is skipped). Cloud toggle stays off until device attestation succeeds.
 */
@Composable
fun CloudBackupOptInScreen(
    initialEnabled: Boolean,
    onContinue: (enabled: Boolean) -> Unit,
    onBack: (() -> Unit)? = null,
    cloudAttestUi: CloudAttestUi = CloudAttestUi(ready = true),
    onRetryAttest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var enabled by remember(initialEnabled) { mutableStateOf(initialEnabled) }
    val attestReady = cloudAttestUi.ready
    LaunchedEffect(attestReady) {
        if (!attestReady) enabled = false
    }

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(cloudAttestUi.retryAfterMs, cloudAttestUi.busy, cloudAttestUi.ready) {
        if (cloudAttestUi.ready || cloudAttestUi.busy || cloudAttestUi.retryAfterMs <= 0L) return@LaunchedEffect
        while (System.currentTimeMillis() < cloudAttestUi.retryAfterMs) {
            nowMs = System.currentTimeMillis()
            delay(250)
        }
        nowMs = System.currentTimeMillis()
    }
    val retrySecondsLeft = ((cloudAttestUi.retryAfterMs - nowMs + 999) / 1000)
        .toInt()
        .coerceAtLeast(0)
    val canRetry = !cloudAttestUi.busy &&
        !attestReady &&
        cloudAttestUi.message != null &&
        (cloudAttestUi.retryEnabled || retrySecondsLeft <= 0)

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
                    ) { Text(stringResource(R.string.backup_back)) }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Button(
                    onClick = { onContinue(enabled && attestReady) },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.backup_continue)) }
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
                text = stringResource(R.string.backup_cloud_backup),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.backup_encrypted_backup_to_fitbuddy_s_cloud),
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
                        text = stringResource(R.string.backup_enable_cloud_backup),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        color = if (attestReady) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                    AppSwitch(
                        checked = enabled && attestReady,
                        onCheckedChange = { enabled = it },
                        enabled = attestReady && !cloudAttestUi.busy
                    )
                }
            }

            when {
                cloudAttestUi.busy -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.backup_verifying_app),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                !attestReady && cloudAttestUi.message != null -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = cloudAttestUi.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onRetryAttest,
                        enabled = canRetry,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (canRetry || retrySecondsLeft <= 0) {
                                stringResource(R.string.backup_retry)
                            } else {
                                stringResource(R.string.backup_retry_in_seconds, retrySecondsLeft)
                            }
                        )
                    }
                }
            }

            if (enabled && attestReady) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.backup_crash_reports_will_also_be_enabled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
