package com.anant.fitbuddy.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.anant.fitbuddy.data.settings.AppColorTheme

/**
 * Catppuccin palettes mapped onto Material 3 roles.
 * Colors from https://github.com/catppuccin/catppuccin
 */
object Catppuccin {

    fun colorScheme(theme: AppColorTheme): ColorScheme? = when (theme) {
        AppColorTheme.BRAND -> null
        AppColorTheme.CATPPUCCIN_LATTE -> Latte
        AppColorTheme.CATPPUCCIN_FRAPPE -> Frappe
        AppColorTheme.CATPPUCCIN_MACCHIATO -> Macchiato
        AppColorTheme.CATPPUCCIN_MOCHA -> Mocha
    }

    private val Latte = lightColorScheme(
        primary = Color(0xFF1E66F5), // blue
        onPrimary = Color(0xFFEFF1F5), // base
        primaryContainer = Color(0xFFCCD0DA), // surface0
        onPrimaryContainer = Color(0xFF4C4F69), // text
        secondary = Color(0xFF179299), // teal
        onSecondary = Color(0xFFEFF1F5),
        secondaryContainer = Color(0xFFBCC0CC), // surface1
        onSecondaryContainer = Color(0xFF4C4F69),
        tertiary = Color(0xFF8839EF), // mauve
        onTertiary = Color(0xFFEFF1F5),
        tertiaryContainer = Color(0xFFACB0BE), // surface2
        onTertiaryContainer = Color(0xFF4C4F69),
        error = Color(0xFFD20F39), // red
        onError = Color(0xFFEFF1F5),
        errorContainer = Color(0xFFE64553), // maroon
        onErrorContainer = Color(0xFFEFF1F5),
        background = Color(0xFFEFF1F5), // base
        onBackground = Color(0xFF4C4F69), // text
        surface = Color(0xFFEFF1F5),
        onSurface = Color(0xFF4C4F69),
        surfaceVariant = Color(0xFFBCC0CC), // surface1 (distinct from card high)
        onSurfaceVariant = Color(0xFF6C6F85), // subtext0
        outline = Color(0xFF9CA0B0), // overlay0
        outlineVariant = Color(0xFFACB0BE), // surface2
        inverseSurface = Color(0xFF4C4F69),
        inverseOnSurface = Color(0xFFEFF1F5),
        inversePrimary = Color(0xFF8CAAEE),
        surfaceContainerLowest = Color(0xFFE6E9EF), // mantle
        surfaceContainerLow = Color(0xFFEFF1F5),
        surfaceContainer = Color(0xFFE6E9EF),
        surfaceContainerHigh = Color(0xFFCCD0DA), // surface0
        surfaceContainerHighest = Color(0xFFBCC0CC), // surface1
    )

    private val Frappe = darkColorScheme(
        primary = Color(0xFF8CAAEE),
        onPrimary = Color(0xFF303446),
        primaryContainer = Color(0xFF414559),
        onPrimaryContainer = Color(0xFFC6D0F5),
        secondary = Color(0xFF81C8BE),
        onSecondary = Color(0xFF303446),
        secondaryContainer = Color(0xFF51576D),
        onSecondaryContainer = Color(0xFFC6D0F5),
        tertiary = Color(0xFFCA9EE6),
        onTertiary = Color(0xFF303446),
        tertiaryContainer = Color(0xFF626880),
        onTertiaryContainer = Color(0xFFC6D0F5),
        error = Color(0xFFE78284),
        onError = Color(0xFF303446),
        errorContainer = Color(0xFFEA999C),
        onErrorContainer = Color(0xFF303446),
        background = Color(0xFF303446),
        onBackground = Color(0xFFC6D0F5),
        surface = Color(0xFF303446),
        onSurface = Color(0xFFC6D0F5),
        surfaceVariant = Color(0xFF51576D), // surface1 (distinct from card high)
        onSurfaceVariant = Color(0xFFA5ADCE),
        outline = Color(0xFF737994),
        outlineVariant = Color(0xFF626880),
        inverseSurface = Color(0xFFC6D0F5),
        inverseOnSurface = Color(0xFF303446),
        inversePrimary = Color(0xFF1E66F5),
        surfaceContainerLowest = Color(0xFF232634),
        surfaceContainerLow = Color(0xFF292C3C),
        surfaceContainer = Color(0xFF292C3C),
        surfaceContainerHigh = Color(0xFF414559), // surface0
        surfaceContainerHighest = Color(0xFF51576D), // surface1
    )

    private val Macchiato = darkColorScheme(
        primary = Color(0xFF8AADF4),
        onPrimary = Color(0xFF24273A),
        primaryContainer = Color(0xFF363A4F),
        onPrimaryContainer = Color(0xFFCAD3F5),
        secondary = Color(0xFF8BD5CA),
        onSecondary = Color(0xFF24273A),
        secondaryContainer = Color(0xFF494D64),
        onSecondaryContainer = Color(0xFFCAD3F5),
        tertiary = Color(0xFFC6A0F6),
        onTertiary = Color(0xFF24273A),
        tertiaryContainer = Color(0xFF5B6078),
        onTertiaryContainer = Color(0xFFCAD3F5),
        error = Color(0xFFED8796),
        onError = Color(0xFF24273A),
        errorContainer = Color(0xFFEE99A0),
        onErrorContainer = Color(0xFF24273A),
        background = Color(0xFF24273A),
        onBackground = Color(0xFFCAD3F5),
        surface = Color(0xFF24273A),
        onSurface = Color(0xFFCAD3F5),
        surfaceVariant = Color(0xFF494D64), // surface1 (distinct from card high)
        onSurfaceVariant = Color(0xFFA5ADCB),
        outline = Color(0xFF6E738D),
        outlineVariant = Color(0xFF5B6078),
        inverseSurface = Color(0xFFCAD3F5),
        inverseOnSurface = Color(0xFF24273A),
        inversePrimary = Color(0xFF1E66F5),
        surfaceContainerLowest = Color(0xFF181926),
        surfaceContainerLow = Color(0xFF1E2030),
        surfaceContainer = Color(0xFF1E2030),
        surfaceContainerHigh = Color(0xFF363A4F), // surface0
        surfaceContainerHighest = Color(0xFF494D64), // surface1
    )

    private val Mocha = darkColorScheme(
        primary = Color(0xFF89B4FA),
        onPrimary = Color(0xFF1E1E2E),
        primaryContainer = Color(0xFF313244),
        onPrimaryContainer = Color(0xFFCDD6F4),
        secondary = Color(0xFF94E2D5),
        onSecondary = Color(0xFF1E1E2E),
        secondaryContainer = Color(0xFF45475A),
        onSecondaryContainer = Color(0xFFCDD6F4),
        tertiary = Color(0xFFCBA6F7),
        onTertiary = Color(0xFF1E1E2E),
        tertiaryContainer = Color(0xFF585B70),
        onTertiaryContainer = Color(0xFFCDD6F4),
        error = Color(0xFFF38BA8),
        onError = Color(0xFF1E1E2E),
        errorContainer = Color(0xFFEBA0AC),
        onErrorContainer = Color(0xFF1E1E2E),
        background = Color(0xFF1E1E2E),
        onBackground = Color(0xFFCDD6F4),
        surface = Color(0xFF1E1E2E),
        onSurface = Color(0xFFCDD6F4),
        surfaceVariant = Color(0xFF45475A), // surface1 (distinct from card high)
        onSurfaceVariant = Color(0xFFA6ADC8),
        outline = Color(0xFF6C7086),
        outlineVariant = Color(0xFF585B70),
        inverseSurface = Color(0xFFCDD6F4),
        inverseOnSurface = Color(0xFF1E1E2E),
        inversePrimary = Color(0xFF1E66F5),
        surfaceContainerLowest = Color(0xFF11111B),
        surfaceContainerLow = Color(0xFF181825),
        surfaceContainer = Color(0xFF181825),
        surfaceContainerHigh = Color(0xFF313244), // surface0
        surfaceContainerHighest = Color(0xFF45475A), // surface1
    )
}
