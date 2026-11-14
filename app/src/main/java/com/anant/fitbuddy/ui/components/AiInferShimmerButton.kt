package com.anant.fitbuddy.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * AI action for name fields: AutoAwesome with a soft looping shimmer when idle,
 * spinner while [loading]. Use as [OutlinedTextField] `trailingIcon`.
 */
@Composable
fun AiInferShimmerButton(
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled && !loading
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            val shift = remember { Animatable(-0.4f) }
            LaunchedEffect(enabled) {
                if (!enabled) {
                    shift.snapTo(-0.4f)
                    return@LaunchedEffect
                }
                shift.animateTo(
                    targetValue = 1.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1600, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
            }
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = contentDescription,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                modifier = Modifier
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        if (!enabled) return@drawWithContent
                        val t = shift.value
                        if (t <= -0.35f || t >= 1.35f) return@drawWithContent
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.85f),
                                    Color.Transparent,
                                ),
                                start = Offset(size.width * (t - 0.28f), 0f),
                                end = Offset(size.width * (t + 0.12f), size.height),
                            ),
                            blendMode = BlendMode.SrcAtop,
                        )
                    }
            )
        }
    }
}
