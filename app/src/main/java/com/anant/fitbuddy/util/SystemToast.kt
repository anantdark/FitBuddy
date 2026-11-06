package com.anant.fitbuddy.util

import android.content.Context
import android.widget.Toast
import com.anant.fitbuddy.ui.i18n.AppI18n

/** Android 12+ system Toast (pill with app icon), shared across the OS. */
object SystemToast {
    fun show(context: Context, message: String, long: Boolean = false) {
        Toast.makeText(
            context.applicationContext,
            AppI18n.translate(message),
            if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
        ).show()
    }
}
