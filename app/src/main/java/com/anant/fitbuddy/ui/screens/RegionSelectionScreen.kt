package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.crash.CrashReporter
import com.anant.fitbuddy.data.region.AppRegion
import com.anant.fitbuddy.ui.components.Button
import com.anant.fitbuddy.ui.components.FitBuddySnackbarHost
import com.anant.fitbuddy.ui.components.TextButton
import com.anant.fitbuddy.ui.components.showFitBuddyPill
import com.anant.fitbuddy.ui.region.RegionFlagCanvas
import com.anant.fitbuddy.ui.util.dismissKeyboardOnTap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-page region picker: shown standalone (post-onboarding gate) or as the last onboarding
 * step. Crash reporting is configured on a previous page (or Settings); when it is off, the
 * custom-region request control is disabled with a hint to go back / open Settings.
 */
@Composable
fun RegionSelectionScreen(
    defaultRegion: AppRegion,
    crashReportingEnabled: Boolean,
    supportId: String,
    isSaving: Boolean = false,
    /** True after this install already successfully sent a custom-region request. */
    regionRequestAlreadySent: Boolean = false,
    onFinished: (region: AppRegion) -> Unit,
    onBack: (() -> Unit)? = null,
    requestDisabledHint: String =
        "Enable crash reporting on the previous page to request a custom region.",
    onRequestRegionSent: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedRegion by remember(defaultRegion) { mutableStateOf(defaultRegion) }
    var showRequestDialog by remember { mutableStateOf(false) }
    var requestText by remember { mutableStateOf("") }
    var showConsentDialog by remember { mutableStateOf(false) }
    var sendingRequest by remember { mutableStateOf(false) }
    var alreadySent by remember(regionRequestAlreadySent) {
        mutableStateOf(regionRequestAlreadySent)
    }
    val canRequest = crashReportingEnabled && !alreadySent
    val requestHint = when {
        !crashReportingEnabled -> requestDisabledHint
        alreadySent -> "You've already sent a region request from this install."
        else -> null
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { FitBuddySnackbarHost(snackbarHostState, bottomPadding = 24.dp) }
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
                text = "Choose your region",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "We use this to personalise calorie estimates, staple foods, and " +
                    "coaching tone for your diet and lifestyle.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                RegionFlagCanvas(region = selectedRegion, modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.height(20.dp))

            AppRegion.entries.forEach { region ->
                RegionOptionCard(
                    region = region,
                    selected = region == selectedRegion,
                    onClick = { selectedRegion = region }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = {
                    if (!canRequest) return@TextButton
                    requestText = ""
                    showRequestDialog = true
                },
                enabled = canRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (canRequest) 1f else 0.45f)
            ) {
                Text("Don't see your region? Request another")
            }
            if (requestHint != null) {
                Text(
                    text = requestHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                if (onBack != null) {
                    OutlinedButton(
                        onClick = onBack,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f)
                    ) { Text("Back") }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Button(
                    onClick = { onFinished(selectedRegion) },
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isSaving) "Saving…" else "Continue")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showRequestDialog) {
        AlertDialog(
            onDismissRequest = { showRequestDialog = false },
            title = { Text("Request another region") },
            text = {
                Column {
                    Text(
                        text = "Tell us which country or region's diet to add next.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = requestText,
                        onValueChange = { requestText = it },
                        label = { Text("Region or country") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRequestDialog = false
                        showConsentDialog = true
                    },
                    enabled = requestText.isNotBlank()
                ) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showRequestDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { if (!sendingRequest) showConsentDialog = false },
            title = { Text("Send region request?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "This sends exactly the following, anonymously:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    ConsentLine("Event", "Region request")
                    ConsentLine("Requested region", requestText.trim())
                    ConsentLine("Current / selected region", selectedRegion.displayName())
                    ConsentLine("Anonymous Support ID", supportId.ifBlank { "(none yet)" })
                    ConsentLine("App version", BuildConfig.VERSION_NAME)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No meals, photos, names, or API keys are ever included.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val requested = requestText.trim()
                        val current = selectedRegion.name
                        sendingRequest = true
                        scope.launch {
                            if (alreadySent) {
                                sendingRequest = false
                                showConsentDialog = false
                                snackbarHostState.showFitBuddyPill(
                                    "You've already sent a region request from this install."
                                )
                                return@launch
                            }
                            val result = withContext(Dispatchers.IO) {
                                // UI may have opted in this session before settings persist.
                                if (crashReportingEnabled) {
                                    CrashReporter.setReportingEnabled(true)
                                }
                                CrashReporter.captureRegionRequest(requested, current, supportId)
                            }
                            sendingRequest = false
                            showConsentDialog = false
                            if (result == CrashReporter.RegionRequestResult.Sent) {
                                alreadySent = true
                                onRequestRegionSent?.invoke()
                            }
                            snackbarHostState.showFitBuddyPill(result.userMessage)
                        }
                    },
                    enabled = !sendingRequest
                ) { Text(if (sendingRequest) "Sending…" else "Send") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConsentDialog = false },
                    enabled = !sendingRequest
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RegionOptionCard(region: AppRegion, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                RegionFlagCanvas(region = region, modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = region.displayName(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

@Composable
private fun ConsentLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
