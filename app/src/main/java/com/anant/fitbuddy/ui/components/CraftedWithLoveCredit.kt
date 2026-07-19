package com.anant.fitbuddy.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val ANANT_SITE_URL = "https://anantdark.github.io"

/** Three-line credit with a double-beat heart. Double-tap the heart to celebrate. */
@Composable
fun CraftedWithLoveCredit(
    onHeartDoubleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val beat = rememberInfiniteTransition(label = "craftedHeart")
    val scale by beat.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                1.00f at 0 using FastOutSlowInEasing
                1.28f at 140 using FastOutSlowInEasing
                1.00f at 280 using FastOutSlowInEasing
                1.18f at 400 using FastOutSlowInEasing
                1.00f at 540 using FastOutSlowInEasing
                1.00f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "heartScale"
    )
    // Hue travels pink → red → rose with the same beat timing (values wrap via % 360).
    val hue by beat.animateFloat(
        initialValue = 335f,
        targetValue = 335f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                335f at 0 using FastOutSlowInEasing // soft pink
                358f at 140 using FastOutSlowInEasing // hot red-pink (lub)
                348f at 280 using FastOutSlowInEasing
                368f at 400 using FastOutSlowInEasing // deep red (dub)
                342f at 540 using FastOutSlowInEasing
                335f at 1200 // soft pink
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "heartHue"
    )
    val sat by beat.animateFloat(
        initialValue = 0.62f,
        targetValue = 0.62f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.62f at 0 using FastOutSlowInEasing
                0.82f at 140 using FastOutSlowInEasing
                0.68f at 280 using FastOutSlowInEasing
                0.88f at 400 using FastOutSlowInEasing
                0.70f at 540 using FastOutSlowInEasing
                0.62f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "heartSat"
    )
    val mute = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
    val heartColor = Color.hsv(
        hue = ((hue % 360f) + 360f) % 360f,
        saturation = sat,
        value = 0.95f
    )
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Crafted with",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = mute
        )
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = "Double-tap to celebrate",
            tint = heartColor,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .size(96.dp)
                .scale(scale)
                .semantics { role = Role.Button }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { onHeartDoubleTap() })
                }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "by ",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
            Text(
                text = "Anant",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(role = Role.Button) {
                    uriHandler.openUri(ANANT_SITE_URL)
                }
            )
        }
    }
}
