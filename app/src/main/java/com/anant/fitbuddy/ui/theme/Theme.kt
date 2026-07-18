package com.anant.fitbuddy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimaryDark,
    primaryContainer = GreenPrimaryContainerDark,
    secondary = GreenSecondaryDark,
    tertiary = TealTertiaryDark,
    error = WarmError
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    primaryContainer = GreenPrimaryContainer,
    secondary = GreenSecondary,
    tertiary = TealTertiary,
    error = WarmError
)

// Softer, more modern rounded corners app-wide (cards, buttons, sheets, dialogs).
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

/**
 * Stronger primary wash so Android 12+ sparkle ripples read like the system
 * fingerprint / charge accent shimmer (AuthRipple uses theme accent).
 */
private val PrimarySparkleRippleAlpha = RippleAlpha(
    pressedAlpha = 0.24f,
    focusedAlpha = 0.14f,
    draggedAlpha = 0.18f,
    hoveredAlpha = 0.10f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitBuddyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You on by default: pull wallpaper-based dynamic color on Android 12+.
    // Falls back to FitBuddy green brand scheme on older devices.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Tint Material sparkle ripples with dynamic primary (wallpaper green when
    // Material You is on). Platform RippleDrawable supplies the sparkle on API 31+.
    val primaryRipple = remember(colorScheme.primary) {
        RippleConfiguration(
            color = colorScheme.primary,
            rippleAlpha = PrimarySparkleRippleAlpha
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes
    ) {
        CompositionLocalProvider(LocalRippleConfiguration provides primaryRipple) {
            content()
        }
    }
}
