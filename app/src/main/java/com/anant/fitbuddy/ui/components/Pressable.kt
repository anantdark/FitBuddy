package com.anant.fitbuddy.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.ui.util.rememberDismissKeyboard

/**
 * Material You sparkle ripple (primary-tinted) + light press scale.
 *
 * On Android 12+, [ripple] uses the platform sparkle RippleDrawable — the same
 * family of effect as fingerprint-unlock / charge accent shimmer. Color comes
 * from [MaterialTheme.colorScheme.primary] (dynamic green under Material You).
 *
 * Scale uses [graphicsLayer] so it does not wrap drawContent and break the
 * Material3 ripple node.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    scaleDown: Float = 0.97f,
    onClick: () -> Unit
): Modifier = composed {
    val dismiss = rememberDismissKeyboard()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    val primary = MaterialTheme.colorScheme.primary
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        interactionSource = interactionSource,
        indication = ripple(color = primary),
        enabled = enabled,
        onClick = { dismiss(); onClick() }
    )
}

/**
 * Card keeps resting chrome; press uses Material Card ripple (theme primary
 * via [androidx.compose.material3.LocalRippleConfiguration]) plus scale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PressableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: Dp = 2.dp,
    scaleDown: Float = 0.98f,
    content: @Composable ColumnScope.() -> Unit
) {
    val dismiss = rememberDismissKeyboard()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardPressScale"
    )
    Card(
        onClick = { dismiss(); onClick() },
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        colors = colors,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        content = content
    )
}
