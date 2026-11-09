package com.anant.fitbuddy.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Refreshes Glance widgets when meal/exercise/preset data changes. */
class WidgetDataChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != FitBuddyWidgetUpdater.ACTION_WIDGET_DATA_CHANGED) return
        FitBuddyWidgetUpdater.requestUpdate(context)
    }
}
