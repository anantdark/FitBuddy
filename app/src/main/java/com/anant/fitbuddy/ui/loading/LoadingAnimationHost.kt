package com.anant.fitbuddy.ui.loading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anant.fitbuddy.ui.loading.animations.analyzingLabel

/**
 * Shows a loading animation for [slot] according to [animationChoice]
 * (`off` / `random` / animation id). Off → spinner with status text for the slot.
 */
@Composable
fun LoadingAnimationHost(
    slot: LoadingAnimationSlot,
    animationChoice: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    captions: List<String>? = null,
) {
    val animation = remember(slot, animationChoice) {
        LoadingAnimationRegistry.resolve(slot, animationChoice)
    }

    if (animation == null) {
        when (slot) {
            LoadingAnimationSlot.ANALYZING -> AnalyzingSpinnerBanner(
                modelId = label,
                modifier = modifier
            )
            LoadingAnimationSlot.INSIGHT -> InsightSpinnerContent(
                message = captions?.firstOrNull()?.takeIf { it.isNotBlank() }
                    ?: "Generating insight…",
                modifier = modifier
            )
        }
        return
    }

    val resolvedCaptions = when {
        !captions.isNullOrEmpty() -> captions
        animation.defaultCaptions.isNotEmpty() -> animation.defaultCaptions
        else -> emptyList()
    }
    animation.Content(
        LoadingAnimationScope(
            modifier = modifier,
            label = label,
            captions = resolvedCaptions
        )
    )
}

@Composable
private fun AnalyzingSpinnerBanner(
    modelId: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = analyzingLabel(modelId),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
private fun InsightSpinnerContent(
    message: String,
    modifier: Modifier = Modifier
) {
    // Prefer the button's content color so disabled-but-tinted buttons stay readable.
    val contentColor = LocalContentColor.current
    Box(
        modifier = modifier.fillMaxWidth().height(52.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = contentColor
            )
            Text(
                text = message,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}
