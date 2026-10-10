package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anant.fitbuddy.FitBuddyApp
import com.anant.fitbuddy.data.pcsync.PcSyncConfig
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** Body of the "PC sync" settings card: pairing with FitBuddy Desktop over LAN or Tailscale. */
@Composable
internal fun PcSyncSettingsContent() {
    val app = LocalContext.current.applicationContext as FitBuddyApp
    val pcSync = app.pcSync
    val config by pcSync.config.collectAsStateWithLifecycle()
    val status by pcSync.status.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var enabled by rememberSaveable(config) { mutableStateOf(config.enabled) }
    var address by rememberSaveable(config) { mutableStateOf(config.address) }
    var token by rememberSaveable(config) { mutableStateOf(config.token) }
    val draft = remember(enabled, address, token) { PcSyncConfig(enabled, address.trim(), token.trim()) }
    val dirty = draft != config

    Text(
        text = "Keep FitBuddy Desktop on your computer in sync. Copy the address and pairing " +
            "code from FitBuddy Desktop → Pair phone; a Tailscale address works from anywhere. " +
            "Syncs right away while the app is open and about every 15 minutes in the background.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Sync with PC", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = enabled, onCheckedChange = { enabled = it })
    }
    OutlinedTextField(
        value = address,
        onValueChange = { address = it },
        label = { Text("PC address") },
        placeholder = { Text("my-pc:${PcSyncConfig.DEFAULT_PORT}") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = token,
        onValueChange = { token = it },
        label = { Text("Pairing code") },
        supportingText = { Text("Shown in FitBuddy Desktop → Pair phone") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { pcSync.saveConfig(draft) }, enabled = dirty) { Text("Save") }
        OutlinedButton(
            onClick = { scope.launch { pcSync.syncNow() } },
            enabled = config.isUsable && !dirty && !status.syncing
        ) { Text(if (status.syncing) "Syncing…" else "Sync now") }
    }
    val statusText = when {
        status.syncing -> "Syncing…"
        status.lastError != null -> "Last attempt failed: ${status.lastError}"
        status.lastSuccessAt > 0 -> buildString {
            append("Last synced ")
            append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(status.lastSuccessAt)))
            if (status.lastReceived > 0) append(" · ${status.lastReceived} new from PC")
        }
        else -> "Not synced yet"
    }
    Text(
        text = statusText,
        style = MaterialTheme.typography.bodySmall,
        color = if (status.lastError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    )
}
