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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.R
import com.anant.fitbuddy.data.database.DatabaseDowngradeInfo
import com.anant.fitbuddy.ui.components.Button
import com.anant.fitbuddy.ui.components.OutlinedButton
import com.anant.fitbuddy.ui.util.dismissKeyboardOnTap

/**
 * Blocking gate when a newer FitBuddy left a Room schema this build cannot open.
 * Wipe starts fresh; exit leaves data intact so the user can install a matching build.
 */
@Composable
fun DatabaseDowngradeScreen(
    info: DatabaseDowngradeInfo,
    onWipe: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 24.dp)
            ) {
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.db_downgrade_exit)) }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onWipe,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.db_downgrade_wipe)) }
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
                text = stringResource(R.string.db_downgrade_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(
                    R.string.db_downgrade_body,
                    info.onDiskVersion,
                    info.appVersion,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
