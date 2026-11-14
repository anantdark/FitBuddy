package com.anant.fitbuddy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.R
import com.anant.fitbuddy.ui.theme.appShape
import com.anant.fitbuddy.util.formatMacro

/** Sticky hero totals for Food / Meal review. */
@Composable
fun ReviewTotalsHeader(
    calories: Int,
    protein: Double,
    carbs: Double,
    fats: Double,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = appShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "$calories kcal",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ReviewMacroPill(stringResource(R.string.dashboard_protein), protein)
                ReviewMacroPill(stringResource(R.string.dashboard_carbs), carbs)
                ReviewMacroPill(stringResource(R.string.dashboard_fats), fats)
            }
        }
    }
}

@Composable
private fun ReviewMacroPill(label: String, grams: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${grams.formatMacro()}g",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
        )
    }
}

/** Bottom actions: optional secondary, Cancel + primary (Food) or secondary + primary (Meal). */
@Composable
fun ReviewBottomBar(
    primaryLabel: String,
    primaryEnabled: Boolean,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    secondaryEnabled: Boolean = primaryEnabled,
    onSecondary: (() -> Unit)? = null,
    showCancel: Boolean = true,
    onCancel: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (secondaryLabel != null && onSecondary != null) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = secondaryEnabled,
                onClick = onSecondary
            ) {
                Text(secondaryLabel)
            }
        }
        if (showCancel && onCancel != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onCancel
                ) { Text(stringResource(R.string.common_cancel)) }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = primaryEnabled,
                    onClick = onPrimary
                ) { Text(primaryLabel) }
            }
        } else {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = primaryEnabled,
                onClick = onPrimary
            ) { Text(primaryLabel) }
        }
    }
}
