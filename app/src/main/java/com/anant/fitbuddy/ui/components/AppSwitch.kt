package com.anant.fitbuddy.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.ui.theme.isMakoStyle

private val MakoTrackWidth = 52.dp
private val MakoTrackHeight = 32.dp
private val MakoThumbSize = 20.dp
private val MakoThumbInset = 4.dp
private val Sharp = RoundedCornerShape(0.dp)

/**
 * Theme-aware switch: Material3 pill in Material mode, sharp rectangular track/thumb in Mako.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    if (isMakoStyle()) {
        MakoSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            interactionSource = interactionSource,
        )
    } else {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
        )
    }
}

@Composable
private fun MakoSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val trackColor = when {
        !enabled && checked -> scheme.primary.copy(alpha = 0.38f)
        !enabled -> scheme.surfaceVariant.copy(alpha = 0.38f)
        checked -> scheme.primary
        else -> scheme.surfaceVariant
    }
    val borderColor = when {
        !enabled -> scheme.outline.copy(alpha = 0.38f)
        checked -> scheme.primary
        else -> scheme.outline
    }
    val thumbColor = when {
        !enabled -> scheme.onSurface.copy(alpha = 0.38f)
        checked -> scheme.onPrimary
        else -> scheme.outline
    }

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) {
            MakoTrackWidth - MakoThumbSize - MakoThumbInset
        } else {
            MakoThumbInset
        },
        animationSpec = tween(durationMillis = 180),
        label = "makoSwitchThumb",
    )
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val toggleable = if (onCheckedChange != null) {
        Modifier
            .minimumInteractiveComponentSize()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = source,
                indication = null,
            )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(toggleable)
            .width(MakoTrackWidth)
            .height(MakoTrackHeight)
            .border(1.5.dp, borderColor, Sharp)
            .background(trackColor, Sharp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(MakoThumbSize)
                .background(thumbColor, Sharp)
        )
    }
}
