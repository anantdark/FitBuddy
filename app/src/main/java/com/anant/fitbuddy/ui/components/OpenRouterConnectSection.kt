package com.anant.fitbuddy.ui.components

import com.anant.fitbuddy.R

import androidx.compose.ui.res.stringResource

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

private const val OPENROUTER_API_KEY_DOCS_URL =
    "https://anantdark.github.io/FitBuddy/onboarding.html#openrouter"

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
                text = stringResource(R.string.settings_connected_with_openrouter),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (hasManualKeys) {
                Text(
                    text = stringResource(R.string.settings_api_keys_are_preferred_openrouter_sign),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onDisconnect,
                enabled = !oauthBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_disconnect_openrouter))
            }
        } else {
            Button(
                onClick = { onConnect(context) },
                enabled = !oauthBusy && !apiKeysExpanded,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (oauthBusy) "Connecting…" else stringResource(R.string.settings_continue_with_openrouter))
            }
            Text(
                text = if (apiKeysExpanded) {
                    stringResource(R.string.openrouter_collapse_api_keys_below_use_openrouter_sign)
                } else {
                    stringResource(R.string.openrouter_sign_create_openrouter_account_no_api_key)
                },
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
                text = stringResource(R.string.settings_use_api_keys_instead),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (apiKeysExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (apiKeysExpanded) {
                    stringResource(R.string.openrouter_collapse)
                } else {
                    stringResource(R.string.openrouter_expand)
                }
            )
        }

        AnimatedVisibility(visible = apiKeysExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OpenRouterApiKeyDocsLink()
                apiKeysContent()
            }
        }
    }
}

@Composable
private fun OpenRouterApiKeyDocsLink() {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(OPENROUTER_API_KEY_DOCS_URL) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.settings_how_to_create_an_openrouter_api),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
