package com.anant.fitbuddy.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * OpenRouter connect UI: primary OAuth button, collapsed API-key editor below.
 * Manual keys are preferred over the OAuth key when both exist.
 */
@Composable
fun OpenRouterConnectSection(
    oauthConnected: Boolean,
    oauthBusy: Boolean,
    hasManualKeys: Boolean,
    onConnect: (Context) -> Unit,
    onDisconnect: () -> Unit,
    context: Context,
    modifier: Modifier = Modifier,
    apiKeysContent: @Composable () -> Unit
) {
    var apiKeysExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (oauthConnected) {
            Text(
                text = "Connected with OpenRouter",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (hasManualKeys) {
                Text(
                    text = "API keys are preferred; OpenRouter sign-in is used only if they fail.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onDisconnect,
                enabled = !oauthBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Disconnect OpenRouter")
            }
        } else {
            Button(
                onClick = { onConnect(context) },
                enabled = !oauthBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (oauthBusy) "Connecting…" else "Continue with OpenRouter")
            }
            Text(
                text = "Sign in or create an OpenRouter account. No API key paste required.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { apiKeysExpanded = !apiKeysExpanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Use API keys instead",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (apiKeysExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (apiKeysExpanded) "Collapse" else "Expand"
            )
        }

        AnimatedVisibility(visible = apiKeysExpanded) {
            apiKeysContent()
        }
    }
}
