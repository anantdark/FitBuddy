package com.anant.fitbuddy.widget

import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders

/**
 * Material You when available (API 31+); neutral M3 baseline otherwise.
 * Avoids the brand-green flood that made widgets look like mint slabs.
 */
@Composable
internal fun FitBuddyGlanceTheme(content: @Composable () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlanceTheme(content = content)
    } else {
        GlanceTheme(colors = BaselineColors, content = content)
    }
}

private val BaselineColors = ColorProviders(
    light = lightColorScheme(
        primary = Color(0xFF4A635D),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFCCE8E1),
        onPrimaryContainer = Color(0xFF05201C),
        secondary = Color(0xFF4A635F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCCE8E2),
        onSecondaryContainer = Color(0xFF05201C),
        tertiary = Color(0xFF465762),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFC9DBE7),
        onTertiaryContainer = Color(0xFF021E28),
        error = Color(0xFFBA1A1A),
        background = Color(0xFFF5FBF7),
        onBackground = Color(0xFF171D1B),
        surface = Color(0xFFF5FBF7),
        onSurface = Color(0xFF171D1B),
        surfaceVariant = Color(0xFFDBE5E0),
        onSurfaceVariant = Color(0xFF3F4945),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFB1CCC5),
        onPrimary = Color(0xFF1C3530),
        primaryContainer = Color(0xFF334B46),
        onPrimaryContainer = Color(0xFFCCE8E1),
        secondary = Color(0xFFB1CCC6),
        onSecondary = Color(0xFF1C3531),
        secondaryContainer = Color(0xFF334B47),
        onSecondaryContainer = Color(0xFFCCE8E2),
        tertiary = Color(0xFFADC5CB),
        onTertiary = Color(0xFF18343D),
        tertiaryContainer = Color(0xFF2F4A52),
        onTertiaryContainer = Color(0xFFC9DBE7),
        error = Color(0xFFFFB4AB),
        background = Color(0xFF0E1513),
        onBackground = Color(0xFFDEE4E1),
        surface = Color(0xFF0E1513),
        onSurface = Color(0xFFDEE4E1),
        surfaceVariant = Color(0xFF3F4945),
        onSurfaceVariant = Color(0xFFBFC9C4),
    ),
)
