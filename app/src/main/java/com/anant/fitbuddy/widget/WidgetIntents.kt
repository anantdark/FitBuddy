package com.anant.fitbuddy.widget

import android.content.Context
import android.content.Intent
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import com.anant.fitbuddy.MainActivity

internal object WidgetIntents {

    fun openLogAction(
        context: Context,
        action: String,
        presetId: Int? = null,
        presetKind: String? = null,
    ): Action {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_LOG_ACTION, action)
            if (presetId != null && presetId > 0) {
                putExtra(MainActivity.EXTRA_OPEN_PRESET_ID, presetId)
            }
            if (!presetKind.isNullOrBlank()) {
                putExtra(MainActivity.EXTRA_OPEN_PRESET_KIND, presetKind)
            }
        }
        return actionStartActivity(intent)
    }
}
