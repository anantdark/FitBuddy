package com.anant.fitbuddy.widget

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log

/**
 * Glance's list-item trampoline runs in onCreate. Two failures show up from widget taps:
 * the nested target intent is missing (Glance throws), or the target is
 * [Intent.ACTION_CLOSE_SYSTEM_DIALOGS] (the system throws SecurityException — apps may not
 * send that broadcast). Repair both before onCreate. Do not [Activity.startActivity] here:
 * that call dismisses the shade by broadcasting the forbidden action and the tap crashes.
 * Glance is handed an explicit in-app refresh instead. The next widget tap uses the real
 * activity PendingIntent, which the system is allowed to deliver.
 *
 * Keys match androidx.glance.appwidget.action.ActionTrampoline (private constants).
 */
internal object GlanceTrampolineGuard : Application.ActivityLifecycleCallbacks {

    private const val TAG = "GlanceTrampoline"
    private const val ACTION_TYPE = "ACTION_TYPE"
    private const val ACTION_INTENT = "ACTION_INTENT"
    private const val TYPE_BROADCAST = "BROADCAST"
    private const val INVISIBLE =
        "androidx.glance.appwidget.action.InvisibleActionTrampolineActivity"
    private const val VISIBLE =
        "androidx.glance.appwidget.action.ActionTrampolineActivity"

    fun install(app: Application) {
        app.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity.javaClass.name != INVISIBLE && activity.javaClass.name != VISIBLE) return
        val intent = activity.intent ?: return
        if (intent.closesSystemDialogs()) {
            intent.action = null
        }
        val alreadySafe = runCatching { hasSafeTrampolineTarget(intent) }.getOrElse {
            intent.replaceExtras(null)
            false
        }
        if (alreadySafe) return
        Log.w(TAG, "Widget trampoline had no usable target; refreshing widgets")
        // Glance still broadcasts whatever target we leave here. Give it an explicit
        // in-app refresh, never CLOSE_SYSTEM_DIALOGS and never startActivity.
        intent.putExtra(ACTION_TYPE, TYPE_BROADCAST)
        intent.putExtra(
            ACTION_INTENT,
            Intent(activity, WidgetDataChangedReceiver::class.java).apply {
                action = FitBuddyWidgetUpdater.ACTION_WIDGET_DATA_CHANGED
                setPackage(activity.packageName)
            },
        )
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit

    private fun hasSafeTrampolineTarget(intent: Intent): Boolean {
        intent.setExtrasClassLoader(Intent::class.java.classLoader)
        runCatching { intent.extras?.classLoader = Intent::class.java.classLoader }
        if (intent.getStringExtra(ACTION_TYPE).isNullOrEmpty()) return false
        val nested = readNestedIntent(intent) ?: return false
        return !nested.closesSystemDialogs()
    }

    @Suppress("DEPRECATION")
    private fun Intent.closesSystemDialogs(): Boolean =
        action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS

    private fun readNestedIntent(intent: Intent): Intent? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(ACTION_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(ACTION_INTENT)
        }
    } catch (_: RuntimeException) {
        null
    }
}
