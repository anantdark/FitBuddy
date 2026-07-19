package com.anant.fitbuddy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.anant.fitbuddy.ui.components.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object FitBuddyPillConfig {
    const val DISPLAY_MS = 1_000L
    const val STARTUP_DISPLAY_MS = 750L
}

/** Shows the FitBuddy pill and auto-dismisses after [displayMillis]. */
suspend fun SnackbarHostState.showFitBuddyPill(
    message: String,
    displayMillis: Long = FitBuddyPillConfig.DISPLAY_MS
) {
    coroutineScope {
        val snackbarJob = launch {
            showSnackbar(message = message, duration = SnackbarDuration.Indefinite)
        }
        delay(displayMillis)
        currentSnackbarData?.dismiss()
        snackbarJob.join()
    }
}

/** Nav bar height + gap so the pill sits just above the center (Progress) tab. */
val SnackbarAboveProgressPadding = 86.dp

@Composable
fun FitBuddySnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = SnackbarAboveProgressPadding
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.fillMaxSize(),
        snackbar = { data ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                FitBuddyPillContent(
                    message = data.visuals.message,
                    actionLabel = data.visuals.actionLabel,
                    onAction = { data.performAction() },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = bottomPadding)
                )
            }
        }
    )
}

/**
 * Persistent pill whose text updates in place (e.g. easter-egg tap countdown).
 * [message] null hides the pill.
 */
@Composable
fun FitBuddyLivePill(
    message: String?,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = SnackbarAboveProgressPadding
) {
    if (message == null) return
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        FitBuddyPillContent(
            message = message,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = bottomPadding)
        )
    }
}

@Composable
fun FitBuddyPillContent(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = colors.surfaceContainerHighest,
        contentColor = colors.onSurface,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = colors.primary,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colors.onPrimary
                    )
                }
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            actionLabel?.let { label ->
                TextButton(
                    onClick = { onAction?.invoke() },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary
                    )
                }
            }
        }
    }
}
