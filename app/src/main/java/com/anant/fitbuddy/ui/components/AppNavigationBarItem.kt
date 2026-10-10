package com.anant.fitbuddy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.ui.theme.isMakoStyle
import kotlinx.coroutines.flow.map

/** M3 ActiveIndicator 56×32 around a 24dp icon. */
private val IndicatorWidth = 56.dp
private val IndicatorHeight = 32.dp
private val IndicatorToLabelGap = 4.dp
/** Top inset of the indicator inside an 80dp nav item (M3 NavigationBar). */
private val IndicatorVerticalOffset = 12.dp
private val NavItemMinHeight = 80.dp
private val Sharp = RoundedCornerShape(0.dp)

/**
 * Bottom-nav item: Material pill in Material mode; same expand/fade/ripple behaviour with a
 * sharp rectangle in Mako.
 */
@Composable
fun RowScope.AppNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
) {
    if (isMakoStyle()) {
        MakoNavigationBarItem(
            selected = selected,
            onClick = onClick,
            icon = icon,
            modifier = modifier,
            enabled = enabled,
            label = label,
            alwaysShowLabel = alwaysShowLabel,
        )
    } else {
        NavigationBarItem(
            selected = selected,
            onClick = onClick,
            icon = icon,
            modifier = modifier,
            enabled = enabled,
            label = label,
            alwaysShowLabel = alwaysShowLabel,
            colors = NavigationBarItemDefaults.colors(),
        )
    }
}

@Composable
private fun RowScope.MakoNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)?,
    alwaysShowLabel: Boolean,
) {
    val colors = NavigationBarItemDefaults.colors()
    val interactionSource = remember { MutableInteractionSource() }
    var itemWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    // Remap item presses into indicator-local coords (same trick as M3 NavigationBarItem).
    val deltaOffset = with(density) {
        val indicatorWidthPx = IndicatorWidth.roundToPx()
        Offset(
            x = (itemWidthPx - indicatorWidthPx).toFloat() / 2f,
            y = IndicatorVerticalOffset.toPx(),
        )
    }
    val offsetInteractionSource = remember(interactionSource, deltaOffset) {
        OffsetMappedInteractionSource(interactionSource, deltaOffset)
    }

    // Mirror M3: alpha (effects) vs size (spatial) are separate so width expansion
    // does not starve the ripple — ripple lives on a fixed-size sibling.
    val alphaProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "makoNavAlpha",
    )
    val sizeProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "makoNavSize",
    )
    val iconColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.disabledIconColor
            selected -> colors.selectedIconColor
            else -> colors.unselectedIconColor
        },
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "makoNavIcon",
    )
    val labelColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.disabledTextColor
            selected -> colors.selectedTextColor
            else -> colors.unselectedTextColor
        },
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "makoNavLabel",
    )
    val showLabel = alwaysShowLabel || selected

    Column(
        modifier = modifier
            .weight(1f)
            .defaultMinSize(minHeight = NavItemMinHeight)
            .onSizeChanged { itemWidthPx = it.width }
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(IndicatorWidth, IndicatorHeight),
            contentAlignment = Alignment.Center,
        ) {
            // Expanding fill — width only; no indication here.
            Box(
                modifier = Modifier
                    .height(IndicatorHeight)
                    .width(IndicatorWidth * sizeProgress.coerceAtLeast(0f))
                    .graphicsLayer { alpha = alphaProgress }
                    .background(colors.selectedIndicatorColor, Sharp),
            )
            // Fixed-size ripple target (M3 separates this so re-taps still shimmer).
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(Sharp)
                    .indication(offsetInteractionSource, ripple()),
            )
            CompositionLocalProvider(LocalContentColor provides iconColor) {
                icon()
            }
        }
        if (label != null && showLabel) {
            Spacer(Modifier.height(IndicatorToLabelGap))
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = if (alwaysShowLabel) 1f else alphaProgress
                },
            ) {
                CompositionLocalProvider(LocalContentColor provides labelColor) {
                    label()
                }
            }
        }
    }
}

/**
 * Maps press positions from the full nav item into the indicator's local space so the ripple
 * draws on the sharp rect (same role as M3's internal MappedInteractionSource).
 */
private class OffsetMappedInteractionSource(
    underlying: InteractionSource,
    private val delta: Offset,
) : InteractionSource {
    private val mappedPresses = mutableMapOf<PressInteraction.Press, PressInteraction.Press>()

    override val interactions = underlying.interactions.map { interaction ->
        when (interaction) {
            is PressInteraction.Press -> {
                val mapped = PressInteraction.Press(interaction.pressPosition - delta)
                mappedPresses[interaction] = mapped
                mapped
            }
            is PressInteraction.Release -> {
                val mapped = mappedPresses.remove(interaction.press)
                if (mapped == null) interaction else PressInteraction.Release(mapped)
            }
            is PressInteraction.Cancel -> {
                val mapped = mappedPresses.remove(interaction.press)
                if (mapped == null) interaction else PressInteraction.Cancel(mapped)
            }
            else -> interaction
        }
    }
}
