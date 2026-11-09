package com.anant.fitbuddy.ui.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** When false, [AppHapticController] no-ops (Settings → Preferences). Default on. */
val LocalHapticsEnabled = staticCompositionLocalOf { true }

/** Typed haptic helpers that respect [LocalHapticsEnabled]. */
class AppHapticController(
    private val haptics: HapticFeedback,
    private val enabled: Boolean,
) {
    fun click() = perform(HapticFeedbackType.ContextClick)

    fun confirm() = perform(HapticFeedbackType.Confirm)

    fun reject() = perform(HapticFeedbackType.Reject)

    fun longPress() = perform(HapticFeedbackType.LongPress)

    fun segment() = perform(HapticFeedbackType.SegmentTick)

    fun toggle(on: Boolean) = perform(
        if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
    )

    fun virtualKey() = perform(HapticFeedbackType.VirtualKey)

    private fun perform(type: HapticFeedbackType) {
        if (!enabled) return
        haptics.performHapticFeedback(type)
    }
}

@Composable
fun rememberAppHaptics(): AppHapticController {
    val enabled = LocalHapticsEnabled.current
    val haptics = LocalHapticFeedback.current
    return remember(enabled, haptics) { AppHapticController(haptics, enabled) }
}
