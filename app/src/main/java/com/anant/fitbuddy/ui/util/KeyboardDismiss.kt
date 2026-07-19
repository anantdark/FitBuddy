package com.anant.fitbuddy.ui.util

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/** Clears text focus and hides the IME. Safe to call from any onClick / onCheckedChange. */
@Composable
fun rememberDismissKeyboard(): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return remember(focusManager, keyboardController) {
        {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }
}

/**
 * Clears focus when the user taps empty space (not consumed by a child like a TextField).
 * Attach to screen / dialog roots alongside scroll modifiers.
 */
fun Modifier.dismissKeyboardOnTap(): Modifier = composed {
    val dismiss = rememberDismissKeyboard()
    pointerInput(dismiss) {
        detectTapGestures(onTap = { dismiss() })
    }
}
