package com.anant.fitbuddy.widget

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * A widget tap that reaches Glance's trampoline without a usable target used to crash:
 * missing ACTION_INTENT throws IllegalArgumentException, and a CLOSE_SYSTEM_DIALOGS target
 * throws SecurityException. The guard rewrites both before Glance's onCreate runs.
 */
@RunWith(RobolectricTestRunner::class)
// No explicit sdk — pinning API 34 forced a mid-test Maven download of android-all that
// flakes on CI (MavenArtifactFetcher IOException). Default SDK from Robolectric is enough.
@Config(application = TrampolineGuardTestApp::class)
class GlanceTrampolineGuardTest {

    @Test
    fun missingTarget_doesNotCrash_andRefreshesWidgets() {
        val activity = launch(Intent())

        val target = nestedIntent(activity)
        assertEquals(WidgetDataChangedReceiver::class.java.name, target.component?.className)
        assertEquals(FitBuddyWidgetUpdater.ACTION_WIDGET_DATA_CHANGED, target.action)
        assertNotEquals(ACTION_CLOSE_SYSTEM_DIALOGS, target.action)
        assertTrue(activity.isFinishing)
        assertNoCloseSystemDialogsBroadcast()
    }

    @Test
    fun closeSystemDialogsTarget_isNotBroadcast() {
        val incoming = Intent(ACTION_CLOSE_SYSTEM_DIALOGS).apply {
            putExtra("ACTION_TYPE", "BROADCAST")
            putExtra("ACTION_INTENT", Intent(ACTION_CLOSE_SYSTEM_DIALOGS))
        }

        val activity = launch(incoming)

        assertEquals(null, activity.intent.action)
        val target = nestedIntent(activity)
        assertEquals(FitBuddyWidgetUpdater.ACTION_WIDGET_DATA_CHANGED, target.action)
        assertNoCloseSystemDialogsBroadcast()
        assertTrue(activity.isFinishing)
    }

    @Test
    fun safeCallback_isLeftAlone() {
        val callback = Intent("com.anant.fitbuddy.test.SAFE_CALLBACK").apply {
            component = ComponentName("com.anant.fitbuddy", "com.anant.fitbuddy.widget.WidgetDataChangedReceiver")
        }
        val incoming = Intent().apply {
            putExtra("ACTION_TYPE", "BROADCAST")
            putExtra("ACTION_INTENT", callback)
        }

        val activity = launch(incoming)

        assertEquals("com.anant.fitbuddy.test.SAFE_CALLBACK", nestedIntent(activity).action)
        assertNoCloseSystemDialogsBroadcast()
    }

    private fun launch(intent: Intent): Activity {
        intent.component = ComponentName(
            ApplicationProvider.getApplicationContext(),
            trampolineClass(),
        )
        return Robolectric.buildActivity(trampolineClass(), intent).create().get()
    }

    private fun nestedIntent(activity: Activity): Intent {
        val nested = activity.intent.getParcelableExtra("ACTION_INTENT", Intent::class.java)
        return requireNotNull(nested)
    }

    private fun assertNoCloseSystemDialogsBroadcast() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val forbidden = shadowOf(app).broadcastIntents.any {
            it.action == ACTION_CLOSE_SYSTEM_DIALOGS
        }
        assertEquals(false, forbidden)
    }

    @Suppress("UNCHECKED_CAST")
    private fun trampolineClass(): Class<out Activity> =
        Class.forName(TRAMPOLINE) as Class<out Activity>

    private companion object {
        // Literal — Intent.ACTION_CLOSE_SYSTEM_DIALOGS is deprecated (API 31+).
        const val ACTION_CLOSE_SYSTEM_DIALOGS = "android.intent.action.CLOSE_SYSTEM_DIALOGS"
        const val TRAMPOLINE =
            "androidx.glance.appwidget.action.InvisibleActionTrampolineActivity"
    }
}

class TrampolineGuardTestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GlanceTrampolineGuard.install(this)
    }
}
