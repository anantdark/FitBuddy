package com.anant.fitbuddy.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Requests a refresh of every placed FitBuddy Glance widget. */
object FitBuddyWidgetUpdater {

    const val ACTION_WIDGET_DATA_CHANGED = "com.anant.fitbuddy.action.WIDGET_DATA_CHANGED"

    private const val TAG = "FitBuddyWidgets"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun notifyDataChanged(context: Context) {
        val appContext = context.applicationContext
        appContext.sendBroadcast(
            Intent(ACTION_WIDGET_DATA_CHANGED).setPackage(appContext.packageName)
        )
    }

    fun requestUpdate(context: Context) {
        val appContext = context.applicationContext
        WidgetRefresh.bump()
        scope.launch {
            runCatching {
                QuickSnapWidget().updateAll(appContext)
                DescribeLogWidget().updateAll(appContext)
                CalorieOrbWidget().updateAll(appContext)
                PresetLaunchpadWidget().updateAll(appContext)
                ActionStripWidget().updateAll(appContext)
                WaterIntakeWidget().updateAll(appContext)
            }.onFailure { e ->
                Log.w(TAG, "Widget update failed", e)
            }
        }
    }
}
