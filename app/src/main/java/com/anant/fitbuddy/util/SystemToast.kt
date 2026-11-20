package com.anant.fitbuddy.util

import android.content.Context
import android.widget.Toast

/** Android 12+ system Toast (pill with app icon), shared across the OS. */
object SystemToast {
    private var current: Toast? = null

    fun show(context: Context, message: String, long: Boolean = false) {
        // Replace in-flight toast so rapid taps don't queue.
        current?.cancel()
        current = Toast.makeText(
            context.applicationContext,
            message,
            if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
        ).also { it.show() }
    }
}
